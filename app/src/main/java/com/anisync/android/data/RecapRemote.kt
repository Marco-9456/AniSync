package com.anisync.android.data

import com.anisync.android.GetRecapAiringQuery
import com.anisync.android.GetRecapMediaQuery
import com.anisync.android.GetRecapUserQuery
import com.anisync.android.GetUserListActivitiesQuery
import com.anisync.android.data.util.dataOrThrow
import com.anisync.android.domain.recap.ActivityWindow
import com.anisync.android.domain.recap.FetchedPage
import com.anisync.android.domain.recap.RecapMedia
import com.apollographql.apollo.ApolloClient
import com.apollographql.cache.normalized.FetchPolicy
import com.apollographql.cache.normalized.doNotStore
import com.apollographql.cache.normalized.fetchPolicy
import javax.inject.Inject

/** One page of airing times: title, episode and when it aired in epoch seconds. */
data class AiringPage(val rows: List<AiredEpisode>, val hasNextPage: Boolean)

data class AiredEpisode(val mediaId: Int, val episode: Int, val airingAt: Long)

/** Whose recap a Developer Tools preview is, for its story's header. */
data class RecapUser(val name: String?, val avatarUrl: String?)

/**
 * The AniList reads a recap is built from, one request each. Pacing, retries and counting stay
 * with the repository, so a build can be tested without a server.
 */
interface RecapRemote {
    suspend fun activityPage(userId: Int, window: ActivityWindow, page: Int): FetchedPage

    /** Up to 50 titles in one request. Titles AniList cannot type are left out. */
    suspend fun media(ids: List<Int>): List<RecapMedia>

    suspend fun airingPage(ids: List<Int>, window: ActivityWindow, page: Int): AiringPage

    suspend fun user(userId: Int): RecapUser?
}

class ApolloRecapRemote @Inject constructor(private val apolloClient: ApolloClient) : RecapRemote {

    override suspend fun activityPage(userId: Int, window: ActivityWindow, page: Int): FetchedPage {
        val data = apolloClient.query(
            GetUserListActivitiesQuery(
                userId = userId,
                from = window.createdAfter.toInt(),
                to = window.createdBefore.toInt(),
                page = page,
            )
        )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .doNotStore(true)
            .execute()
            .dataOrThrow()
        val activities = data.Page?.activities.orEmpty().mapNotNull { it?.onListActivity?.toFetched() }
        return FetchedPage(activities, hasNextPage = data.Page?.pageInfo?.hasNextPage == true)
    }

    override suspend fun media(ids: List<Int>): List<RecapMedia> {
        val data = apolloClient.query(GetRecapMediaQuery(ids = ids, perPage = ids.size))
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .doNotStore(true)
            .execute()
            .dataOrThrow()
        return data.Page?.media.orEmpty().mapNotNull { it?.toRecapMedia() }
    }

    override suspend fun airingPage(ids: List<Int>, window: ActivityWindow, page: Int): AiringPage {
        val data = apolloClient.query(
            GetRecapAiringQuery(
                ids = ids,
                from = window.createdAfter.toInt(),
                to = window.createdBefore.toInt(),
                page = page,
            )
        )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .doNotStore(true)
            .execute()
            .dataOrThrow()
        val rows = data.Page?.airingSchedules.orEmpty().filterNotNull().map {
            AiredEpisode(it.mediaId, it.episode, it.airingAt.toLong())
        }
        return AiringPage(rows, hasNextPage = data.Page?.pageInfo?.hasNextPage == true)
    }

    override suspend fun user(userId: Int): RecapUser? {
        val user = apolloClient.query(GetRecapUserQuery(userId = userId))
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .doNotStore(true)
            .execute()
            .dataOrThrow()
            .User ?: return null
        return RecapUser(user.name, user.avatar?.large ?: user.avatar?.medium)
    }
}
