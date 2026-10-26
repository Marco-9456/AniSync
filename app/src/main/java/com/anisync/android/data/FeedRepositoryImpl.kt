package com.anisync.android.data

import com.anisync.android.GetActivityFeedQuery
import com.anisync.android.data.account.AccountStore
import com.anisync.android.data.mapper.toDomain
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.FeedFilter
import com.anisync.android.domain.FeedMediaType
import com.anisync.android.domain.FeedPage
import com.anisync.android.domain.FeedRepository
import com.anisync.android.domain.FeedScope
import com.anisync.android.domain.Result
import com.anisync.android.type.ActivityType
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.cache.normalized.FetchPolicy
import com.apollographql.cache.normalized.doNotStore
import com.apollographql.cache.normalized.fetchPolicy
import javax.inject.Inject

class FeedRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val appSettings: AppSettings,
    private val accountStore: AccountStore,
) : FeedRepository {

    override suspend fun getFeed(
        page: Int,
        perPage: Int,
        filter: FeedFilter,
        scope: FeedScope,
        mediaType: FeedMediaType
    ): Result<FeedPage> = safeApiCall {
        // Media type is a property of list activity alone, so it narrows the List chip and nothing
        // else: All has to mean both types, or it drops half the updates without saying so.
        val listType = when (mediaType) {
            FeedMediaType.ANIME -> ActivityType.ANIME_LIST
            FeedMediaType.MANGA -> ActivityType.MANGA_LIST
        }
        val typeIn = when (filter) {
            FeedFilter.ALL -> listOf(
                ActivityType.TEXT,
                ActivityType.ANIME_LIST,
                ActivityType.MANGA_LIST
            )
            FeedFilter.STATUS -> listOf(ActivityType.TEXT)
            FeedFilter.LIST -> listOf(listType)
        }

        val response = apolloClient
            .query(
                GetActivityFeedQuery(
                    page = page,
                    perPage = perPage,
                    type_in = Optional.present(typeIn),
                    isFollowing = Optional.present(scope == FeedScope.FOLLOWING),
                    hasRepliesOrTypeText = Optional.present(scope == FeedScope.GLOBAL)
                )
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .doNotStore(true)
            .execute()

        // A request that never made it carries an exception rather than GraphQL errors, and its
        // data is null. Reading that as an empty page is how a rate limit came out as "no activity
        // yet" instead of saying what happened.
        response.exception?.let { throw it }
        if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Failed to load feed")
        }
        val pageData = response.data?.Page ?: throw Exception("Failed to load feed")

        // Respect the viewer's AniList adult options (mirrored into AppSettings), as the website does.
        // AniList leaves this filtering to each app.
        val items = pageData.activities
            ?.filterNotNull()
            // Hide activity from users the viewer has blocked on AniList (issue #76). isBlocked is
            // selected inline on the feed query (not the shared ActivityFields fragment) — see Feed.graphql.
            ?.filterNot { activity ->
                activity.onListActivity?.user?.isBlocked == true ||
                    activity.onTextActivity?.user?.isBlocked == true ||
                    activity.onMessageActivity?.messenger?.isBlocked == true ||
                    activity.onMessageActivity?.recipient?.isBlocked == true
            }
            ?.mapNotNull { it.activityFields.toDomain() }
            ?.filterAdultActivities(
                showAdultContent = appSettings.showAdultContent.value,
                showOthersAdultActivity = appSettings.othersAdultActivity.value,
                viewerId = accountStore.activeAccount.value?.id,
            )
            ?.distinctBy { it.id }
            ?: emptyList()

        FeedPage(
            items = items,
            hasNextPage = pageData.pageInfo?.hasNextPage == true,
            currentPage = pageData.pageInfo?.currentPage ?: page
        )
    }
}
