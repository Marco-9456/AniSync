package com.anisync.android.data

import com.anisync.android.GetNotificationsQuery
import com.anisync.android.GetPlanningFirstEpisodesQuery
import com.anisync.android.GetPlanningUpcomingEpisodesQuery
import com.anisync.android.domain.ActivityLikeNotification
import com.anisync.android.domain.ActivityReplyNotification
import com.anisync.android.domain.AiringNotification
import com.anisync.android.domain.AiringSchedule
import com.anisync.android.domain.FollowingNotification
import com.anisync.android.domain.Media
import com.anisync.android.domain.Notification
import com.anisync.android.domain.NotificationRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.User
import com.anisync.android.type.NotificationType
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.exception.ApolloException
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient
) : NotificationRepository {

    companion object {
        // Only notify about Episode 1 if it aired within the last 7 days
        private const val RECENCY_THRESHOLD_DAYS = 7
        private const val SECONDS_PER_DAY = 24 * 60 * 60
    }

    override suspend fun getNotifications(page: Int): Result<List<Notification>> {
        return try {
            val response = apolloClient.query(
                GetNotificationsQuery(
                    page = Optional.present(page),
                    perPage = Optional.present(20)
                )
            ).execute()

            val notifications = response.data?.Page?.notifications?.filterNotNull()?.mapNotNull { notification ->
                when {
                    notification.onAiringNotification != null -> {
                        val data = notification.onAiringNotification!!
                        AiringNotification(
                            id = data.id ?: 0,
                            type = data.type ?: NotificationType.AIRING,
                            createdAt = data.createdAt ?: 0,
                            episode = data.episode ?: 0,
                            contexts = data.contexts?.filterNotNull() ?: emptyList(),
                            media = data.media?.let { media ->
                                 Media(
                                     id = media.id ?: 0,
                                     title = media.title?.userPreferred ?: "Unknown",
                                     coverUrl = media.coverImage?.large,
                                     type = media.type ?: com.anisync.android.type.MediaType.ANIME
                                 )
                            }
                        )
                    }
                    notification.onFollowingNotification != null -> {
                        val data = notification.onFollowingNotification!!
                        FollowingNotification(
                            id = data.id ?: 0,
                            type = data.type ?: NotificationType.FOLLOWING,
                            createdAt = data.createdAt ?: 0,
                            context = data.context ?: "",
                            user = data.user?.let { user ->
                                User(
                                    id = user.id ?: 0,
                                    name = user.name ?: "Unknown",
                                    avatarUrl = user.avatar?.large
                                )
                            }
                        )
                    }
                    notification.onActivityLikeNotification != null -> {
                        val data = notification.onActivityLikeNotification!!
                        ActivityLikeNotification(
                            id = data.id ?: 0,
                            type = data.type ?: NotificationType.ACTIVITY_LIKE,
                            createdAt = data.createdAt ?: 0,
                            context = data.context ?: "",
                             user = data.user?.let { user ->
                                User(
                                    id = user.id ?: 0,
                                    name = user.name ?: "Unknown",
                                    avatarUrl = user.avatar?.large
                                )
                            }
                        )
                    }
                     notification.onActivityReplyNotification != null -> {
                        val data = notification.onActivityReplyNotification!!
                        ActivityReplyNotification(
                            id = data.id ?: 0,
                            type = data.type ?: NotificationType.ACTIVITY_REPLY,
                            createdAt = data.createdAt ?: 0,
                            context = data.context ?: "",
                             user = data.user?.let { user ->
                                User(
                                    id = user.id ?: 0,
                                    name = user.name ?: "Unknown",
                                    avatarUrl = user.avatar?.large
                                )
                            }
                        )
                    }
                    else -> null
                }
            } ?: emptyList()
            
            Result.Success(notifications)
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    override suspend fun getFirstEpisodeAirings(mediaIds: List<Int>): Result<List<AiringSchedule>> {
        if (mediaIds.isEmpty()) return Result.Success(emptyList())

        return try {
            val currentTime = (System.currentTimeMillis() / 1000).toInt()
            val recencyThreshold = currentTime - (RECENCY_THRESHOLD_DAYS * SECONDS_PER_DAY)
            
            val response = apolloClient.query(
                GetPlanningFirstEpisodesQuery(
                    mediaIds = Optional.present(mediaIds),
                    airingBefore = currentTime
                )
            ).execute()

            // Filter to only include episodes that aired within the recency threshold
            val airings = response.data?.Page?.airingSchedules?.mapNotNull { airing ->
                airing?.let {
                    val airingAt = it.airingAt ?: 0
                    // Only include if Episode 1 aired within the last RECENCY_THRESHOLD_DAYS
                    if (airingAt >= recencyThreshold) {
                        AiringSchedule(
                            id = it.id ?: 0,
                            episode = it.episode ?: 0,
                            airingAt = airingAt.toLong(),
                            mediaId = it.mediaId ?: 0,
                            mediaTitle = it.media?.title?.userPreferred ?: "Unknown",
                            mediaCoverUrl = it.media?.coverImage?.large,
                            mediaType = it.media?.type ?: com.anisync.android.type.MediaType.ANIME
                        )
                    } else {
                        null
                    }
                }
            } ?: emptyList()

            Result.Success(airings)
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    override suspend fun getUpcomingFirstEpisodes(
        mediaIds: List<Int>,
        withinHours: Int
    ): Result<List<AiringSchedule>> {
        if (mediaIds.isEmpty()) return Result.Success(emptyList())

        return try {
            val response = apolloClient.query(
                GetPlanningUpcomingEpisodesQuery(
                    mediaIds = Optional.present(mediaIds)
                )
            ).execute()

            // Filter client-side to only include airings within the specified hours
            val withinSeconds = withinHours * 60 * 60
            
            val airings = response.data?.Page?.airingSchedules?.mapNotNull { airing ->
                airing?.let {
                    val timeUntil = it.timeUntilAiring ?: Int.MAX_VALUE
                    // Only include if airing within the specified time window
                    if (timeUntil in 1..withinSeconds) {
                        AiringSchedule(
                            id = it.id ?: 0,
                            episode = it.episode ?: 0,
                            airingAt = (it.airingAt ?: 0).toLong(),
                            mediaId = it.mediaId ?: 0,
                            mediaTitle = it.media?.title?.userPreferred ?: "Unknown",
                            mediaCoverUrl = it.media?.coverImage?.large,
                            mediaType = it.media?.type ?: com.anisync.android.type.MediaType.ANIME
                        )
                    } else {
                        null
                    }
                }
            } ?: emptyList()

            Result.Success(airings)
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }
}
