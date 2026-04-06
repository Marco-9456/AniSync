package com.anisync.android.data

import com.anisync.android.GetNotificationsQuery
import com.anisync.android.GetPlanningFirstEpisodesQuery
import com.anisync.android.GetPlanningUpcomingEpisodesQuery
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.ActivityLikeNotification
import com.anisync.android.domain.ActivityReplyNotification
import com.anisync.android.domain.AiringNotification
import com.anisync.android.domain.AiringSchedule
import com.anisync.android.domain.FollowingNotification
import com.anisync.android.domain.Media
import com.anisync.android.domain.Notification
import com.anisync.android.domain.NotificationRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.ThreadCommentLikeNotification
import com.anisync.android.domain.ThreadCommentMentionNotification
import com.anisync.android.domain.ThreadCommentReplyNotification
import com.anisync.android.domain.ThreadCommentSubscribedNotification
import com.anisync.android.domain.ThreadLikeNotification
import com.anisync.android.domain.User
import com.anisync.android.type.NotificationType
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
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
        return safeApiCall {
            val response = apolloClient.query(
                GetNotificationsQuery(
                    page = Optional.present(page),
                    perPage = Optional.present(20)
                )
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()

            response.data?.Page?.notifications?.filterNotNull()?.mapNotNull { notification ->
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
                    notification.onThreadCommentReplyNotification != null -> {
                        val data = notification.onThreadCommentReplyNotification!!
                        ThreadCommentReplyNotification(
                            id = data.id ?: 0,
                            type = data.type ?: NotificationType.THREAD_COMMENT_REPLY,
                            createdAt = data.createdAt ?: 0,
                            context = data.context ?: "",
                            user = data.user?.let { user ->
                                User(
                                    id = user.id ?: 0,
                                    name = user.name ?: "Unknown",
                                    avatarUrl = user.avatar?.large
                                )
                            },
                            threadId = data.thread?.id ?: 0,
                            threadTitle = data.thread?.title ?: "",
                            commentId = data.comment?.id
                        )
                    }
                    notification.onThreadCommentSubscribedNotification != null -> {
                        val data = notification.onThreadCommentSubscribedNotification!!
                        ThreadCommentSubscribedNotification(
                            id = data.id ?: 0,
                            type = data.type ?: NotificationType.THREAD_SUBSCRIBED,
                            createdAt = data.createdAt ?: 0,
                            context = data.context ?: "",
                            user = data.user?.let { user ->
                                User(
                                    id = user.id ?: 0,
                                    name = user.name ?: "Unknown",
                                    avatarUrl = user.avatar?.large
                                )
                            },
                            threadId = data.thread?.id ?: 0,
                            threadTitle = data.thread?.title ?: "",
                            commentId = data.comment?.id
                        )
                    }
                    notification.onThreadCommentMentionNotification != null -> {
                        val data = notification.onThreadCommentMentionNotification!!
                        ThreadCommentMentionNotification(
                            id = data.id ?: 0,
                            type = data.type ?: NotificationType.THREAD_COMMENT_MENTION,
                            createdAt = data.createdAt ?: 0,
                            context = data.context ?: "",
                            user = data.user?.let { user ->
                                User(
                                    id = user.id ?: 0,
                                    name = user.name ?: "Unknown",
                                    avatarUrl = user.avatar?.large
                                )
                            },
                            threadId = data.thread?.id ?: 0,
                            threadTitle = data.thread?.title ?: "",
                            commentId = data.comment?.id
                        )
                    }
                    notification.onThreadLikeNotification != null -> {
                        val data = notification.onThreadLikeNotification!!
                        ThreadLikeNotification(
                            id = data.id ?: 0,
                            type = data.type ?: NotificationType.THREAD_LIKE,
                            createdAt = data.createdAt ?: 0,
                            context = data.context ?: "",
                            user = data.user?.let { user ->
                                User(
                                    id = user.id ?: 0,
                                    name = user.name ?: "Unknown",
                                    avatarUrl = user.avatar?.large
                                )
                            },
                            threadId = data.thread?.id ?: 0,
                            threadTitle = data.thread?.title ?: ""
                        )
                    }
                    notification.onThreadCommentLikeNotification != null -> {
                        val data = notification.onThreadCommentLikeNotification!!
                        ThreadCommentLikeNotification(
                            id = data.id ?: 0,
                            type = data.type ?: NotificationType.THREAD_COMMENT_LIKE,
                            createdAt = data.createdAt ?: 0,
                            context = data.context ?: "",
                            user = data.user?.let { user ->
                                User(
                                    id = user.id ?: 0,
                                    name = user.name ?: "Unknown",
                                    avatarUrl = user.avatar?.large
                                )
                            },
                            threadId = data.thread?.id ?: 0,
                            threadTitle = data.thread?.title ?: "",
                            commentId = data.comment?.id
                        )
                    }
                    else -> null
                }
            } ?: emptyList()
        }
    }

    override suspend fun getFirstEpisodeAirings(mediaIds: List<Int>): Result<List<AiringSchedule>> {
        if (mediaIds.isEmpty()) return Result.Success(emptyList())

        return safeApiCall {
            val currentTime = (System.currentTimeMillis() / 1000).toInt()
            val recencyThreshold = currentTime - (RECENCY_THRESHOLD_DAYS * SECONDS_PER_DAY)
            
            // Use server-side filtering with airingAfter for recency
            val response = apolloClient.query(
                GetPlanningFirstEpisodesQuery(
                    mediaIds = Optional.present(mediaIds),
                    airingBefore = currentTime,
                    airingAfter = recencyThreshold
                )
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()

            // Server now filters by recency, but we still need to verify it's in the past
            // to avoid race conditions with episodes airing exactly now
            response.data?.Page?.airingSchedules?.mapNotNull { airing ->
                airing?.let {
                    val airingAt = it.airingAt ?: 0
                    // Verify episode has actually aired (in the past)
                    if (airingAt < currentTime) {
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
        }
    }

    override suspend fun getUpcomingFirstEpisodes(
        mediaIds: List<Int>,
        withinHours: Int
    ): Result<List<AiringSchedule>> {
        if (mediaIds.isEmpty()) return Result.Success(emptyList())

        return safeApiCall {
            val currentTime = (System.currentTimeMillis() / 1000).toInt()
            val maxAiringTime = currentTime + (withinHours * 60 * 60)
            
            // Use server-side filtering with airingBefore for time window
            val response = apolloClient.query(
                GetPlanningUpcomingEpisodesQuery(
                    mediaIds = Optional.present(mediaIds),
                    airingBefore = maxAiringTime
                )
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()

            // Server now handles time filtering, results are already sorted by TIME
            response.data?.Page?.airingSchedules?.mapNotNull { airing ->
                airing?.let {
                    val timeUntil = it.timeUntilAiring ?: Int.MAX_VALUE
                    // Only include if actually in the future (timeUntil > 0)
                    if (timeUntil > 0) {
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
        }
    }
}
