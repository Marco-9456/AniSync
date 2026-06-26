package com.anisync.android.data

import com.anisync.android.GetActivityFeedQuery
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
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import javax.inject.Inject

class FeedRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val appSettings: AppSettings,
) : FeedRepository {

    override suspend fun getFeed(
        page: Int,
        perPage: Int,
        filter: FeedFilter,
        scope: FeedScope,
        mediaType: FeedMediaType
    ): Result<FeedPage> = safeApiCall {
        val typeIn = when (filter) {
            FeedFilter.ALL -> listOf(
                ActivityType.TEXT,
                ActivityType.ANIME_LIST,
                ActivityType.MANGA_LIST
            )
            FeedFilter.STATUS -> listOf(ActivityType.TEXT)
            FeedFilter.LIST -> when (mediaType) {
                FeedMediaType.ANIME -> listOf(ActivityType.ANIME_LIST)
                FeedMediaType.MANGA -> listOf(ActivityType.MANGA_LIST)
            }
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
            .execute()

        if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Failed to load feed")
        }

        // Respect the viewer's AniList "display adult content" option (mirrored into AppSettings):
        // hide list activity for 18+ media, matching the website. Text/message activities carry no
        // media, so mediaIsAdult is false for them and they pass through untouched.
        val showAdult = appSettings.showAdultContent.value
        val pageData = response.data?.Page
        val items = pageData?.activities
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
            ?.filterAdultActivities(showAdult)
            ?.distinctBy { it.id }
            ?: emptyList()

        FeedPage(
            items = items,
            hasNextPage = pageData?.pageInfo?.hasNextPage == true,
            currentPage = pageData?.pageInfo?.currentPage ?: page
        )
    }
}
