package com.anisync.android.data

import com.anisync.android.GetNotificationsQuery
import com.anisync.android.GetPlanningFirstEpisodesQuery
import com.anisync.android.GetPlanningUpcomingEpisodesQuery
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.ActivityKind
import com.anisync.android.domain.ActivitySnapshot
import com.anisync.android.domain.ActivityLikeNotification
import com.anisync.android.domain.ActivityMentionNotification
import com.anisync.android.domain.ActivityMessageNotification
import com.anisync.android.domain.ActivityReplyLikeNotification
import com.anisync.android.domain.ActivityReplyNotification
import com.anisync.android.domain.ActivityReplySubscribedNotification
import com.anisync.android.domain.AiringNotification
import com.anisync.android.domain.AiringSchedule
import com.anisync.android.domain.CoverImage
import com.anisync.android.domain.FollowingNotification
import com.anisync.android.domain.Media
import com.anisync.android.domain.MediaDataChangeNotification
import com.anisync.android.domain.MediaDeletionNotification
import com.anisync.android.domain.MediaMergeNotification
import com.anisync.android.domain.Notification
import com.anisync.android.domain.NotificationPage
import com.anisync.android.domain.NotificationRepository
import com.anisync.android.domain.RelatedMediaAdditionNotification
import com.anisync.android.domain.Result
import com.anisync.android.domain.ThreadCommentLikeNotification
import com.anisync.android.domain.ThreadCommentMentionNotification
import com.anisync.android.domain.ThreadCommentReplyNotification
import com.anisync.android.domain.ThreadCommentSubscribedNotification
import com.anisync.android.domain.ThreadLikeNotification
import com.anisync.android.domain.User
import com.anisync.android.fragment.NotifActivity
import com.anisync.android.fragment.NotifMedia
import com.anisync.android.fragment.NotifUser
import com.anisync.android.type.ActivityType
import com.anisync.android.type.MediaType
import com.anisync.android.type.NotificationType
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val tokenedClientFactory: com.anisync.android.data.account.TokenedApolloClientFactory
) : NotificationRepository {

    companion object {
        // Only notify about Episode 1 if it aired within the last 7 days
        private const val RECENCY_THRESHOLD_DAYS = 7
        private const val SECONDS_PER_DAY = 24 * 60 * 60
    }

    override suspend fun getNotifications(page: Int, token: String?): Result<List<Notification>> {
        return safeApiCall {
            // token != null polls a specific (possibly non-active) account via its own client.
            val client = token?.let(tokenedClientFactory::create) ?: apolloClient
            val response = client.query(
                GetNotificationsQuery(
                    page = Optional.present(page),
                    perPage = Optional.present(20),
                    // AniList treats type_in: null as match-nothing, so omitting this
                    // variable returns zero notifications. Pass knownEntries to get all
                    // types — same pattern the inbox screen uses.
                    typeIn = Optional.present(NotificationType.knownEntries),
                    // Worker is a background poll — don't clear the user's unread badge.
                    // Only the inbox screen should reset the count when actually viewed.
                    resetCount = Optional.present(false)
                )
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()

            response.data?.Page?.notifications?.filterNotNull()?.mapNotNull { mapNotification(it) }
                ?: emptyList()
        }
    }

    override suspend fun getNotificationsPage(
        page: Int,
        typeFilter: List<NotificationType>?,
        resetUnreadCount: Boolean
    ): Result<NotificationPage> {
        return safeApiCall {
            val response = apolloClient.query(
                GetNotificationsQuery(
                    page = Optional.present(page),
                    perPage = Optional.present(25),
                    typeIn = if (typeFilter.isNullOrEmpty())
                        Optional.present(NotificationType.knownEntries)
                    else
                        Optional.present(typeFilter),
                    resetCount = Optional.present(resetUnreadCount)
                )
            )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()

            val pageData = response.data?.Page
            val items = pageData?.notifications?.filterNotNull()?.filterNot { it.actorIsBlocked() }?.mapNotNull { mapNotification(it) } ?: emptyList()
            val info = pageData?.pageInfo
            NotificationPage(
                items = items,
                hasNextPage = info?.hasNextPage == true,
                currentPage = info?.currentPage ?: page
            )
        }
    }

    private fun NotifUser.toDomain(): User = User(
        id = id,
        name = name,
        avatarUrl = avatar?.large
    )

    private fun NotifMedia.toDomain(): Media = Media(
        id = id,
        title = title?.userPreferred ?: "Unknown",
        coverUrl = coverImage?.large,
        cover = CoverImage.of(coverImage?.medium, coverImage?.large, coverImage?.extraLarge),
        type = type ?: MediaType.ANIME
    )

    /**
     * Prefer the activity's own type field — AniList serves it directly on
     * ListActivity (ANIME_LIST / MANGA_LIST). Fall back to media.type only
     * when the activity type field is absent (e.g. older cache entries).
     */
    private fun NotifActivity.toSnapshot(): ActivitySnapshot {
        val kind = when {
            onTextActivity != null -> ActivityKind.TEXT
            onMessageActivity != null -> ActivityKind.MESSAGE
            onListActivity != null -> when (onListActivity.type) {
                ActivityType.ANIME_LIST -> ActivityKind.ANIME_LIST
                ActivityType.MANGA_LIST -> ActivityKind.MANGA_LIST
                else -> when (onListActivity.media?.notifMedia?.type) {
                    MediaType.MANGA -> ActivityKind.MANGA_LIST
                    else -> ActivityKind.ANIME_LIST
                }
            }
            else -> ActivityKind.UNKNOWN
        }
        return ActivitySnapshot(
            kind = kind,
            text = onTextActivity?.text,
            message = onMessageActivity?.message,
            listStatus = onListActivity?.status,
            listProgress = onListActivity?.progress,
            listMedia = onListActivity?.media?.notifMedia?.toDomain()
        )
    }

    /**
     * True when this notification was triggered by a user the viewer has blocked on AniList, so it
     * can be hidden (issue #76). Driven by the per-actor `User.isBlocked` flag; media/airing
     * notifications have no actor and are never blocked.
     */
    private fun GetNotificationsQuery.Notification.actorIsBlocked(): Boolean =
        onFollowingNotification?.user?.notifUser?.isBlocked == true ||
            onActivityLikeNotification?.user?.notifUser?.isBlocked == true ||
            onActivityReplyNotification?.user?.notifUser?.isBlocked == true ||
            onActivityReplySubscribedNotification?.user?.notifUser?.isBlocked == true ||
            onActivityReplyLikeNotification?.user?.notifUser?.isBlocked == true ||
            onActivityMentionNotification?.user?.notifUser?.isBlocked == true ||
            onActivityMessageNotification?.user?.notifUser?.isBlocked == true ||
            onThreadCommentReplyNotification?.user?.notifUser?.isBlocked == true ||
            onThreadCommentSubscribedNotification?.user?.notifUser?.isBlocked == true ||
            onThreadCommentMentionNotification?.user?.notifUser?.isBlocked == true ||
            onThreadLikeNotification?.user?.notifUser?.isBlocked == true ||
            onThreadCommentLikeNotification?.user?.notifUser?.isBlocked == true

    @Suppress("CyclomaticComplexMethod")
    private fun mapNotification(notification: GetNotificationsQuery.Notification): Notification? {
        notification.onAiringNotification?.let { data ->
            return AiringNotification(
                id = data.id,
                type = data.type ?: NotificationType.AIRING,
                createdAt = data.createdAt ?: 0,
                episode = data.episode,
                contexts = data.contexts?.filterNotNull() ?: emptyList(),
                media = data.media?.notifMedia?.toDomain()
            )
        }
        notification.onFollowingNotification?.let { data ->
            return FollowingNotification(
                id = data.id,
                type = data.type ?: NotificationType.FOLLOWING,
                createdAt = data.createdAt ?: 0,
                context = data.context ?: "",
                user = data.user?.notifUser?.toDomain()
            )
        }
        notification.onActivityLikeNotification?.let { data ->
            return ActivityLikeNotification(
                id = data.id,
                type = data.type ?: NotificationType.ACTIVITY_LIKE,
                createdAt = data.createdAt ?: 0,
                context = data.context ?: "",
                activityId = data.activityId,
                user = data.user?.notifUser?.toDomain(),
                activity = data.activity?.notifActivity?.toSnapshot()
            )
        }
        notification.onActivityReplyNotification?.let { data ->
            return ActivityReplyNotification(
                id = data.id,
                type = data.type ?: NotificationType.ACTIVITY_REPLY,
                createdAt = data.createdAt ?: 0,
                context = data.context ?: "",
                activityId = data.activityId,
                user = data.user?.notifUser?.toDomain(),
                activity = data.activity?.notifActivity?.toSnapshot()
            )
        }
        notification.onActivityReplySubscribedNotification?.let { data ->
            return ActivityReplySubscribedNotification(
                id = data.id,
                type = data.type ?: NotificationType.ACTIVITY_REPLY_SUBSCRIBED,
                createdAt = data.createdAt ?: 0,
                context = data.context ?: "",
                activityId = data.activityId,
                user = data.user?.notifUser?.toDomain(),
                activity = data.activity?.notifActivity?.toSnapshot()
            )
        }
        notification.onActivityReplyLikeNotification?.let { data ->
            return ActivityReplyLikeNotification(
                id = data.id,
                type = data.type ?: NotificationType.ACTIVITY_REPLY_LIKE,
                createdAt = data.createdAt ?: 0,
                context = data.context ?: "",
                activityId = data.activityId,
                user = data.user?.notifUser?.toDomain(),
                activity = data.activity?.notifActivity?.toSnapshot()
            )
        }
        notification.onActivityMentionNotification?.let { data ->
            return ActivityMentionNotification(
                id = data.id,
                type = data.type ?: NotificationType.ACTIVITY_MENTION,
                createdAt = data.createdAt ?: 0,
                context = data.context ?: "",
                activityId = data.activityId,
                user = data.user?.notifUser?.toDomain(),
                activity = data.activity?.notifActivity?.toSnapshot()
            )
        }
        notification.onActivityMessageNotification?.let { data ->
            return ActivityMessageNotification(
                id = data.id,
                type = data.type ?: NotificationType.ACTIVITY_MESSAGE,
                createdAt = data.createdAt ?: 0,
                context = data.context ?: "",
                activityId = data.activityId,
                user = data.user?.notifUser?.toDomain(),
                messagePreview = data.message?.message
            )
        }
        notification.onThreadCommentReplyNotification?.let { data ->
            return ThreadCommentReplyNotification(
                id = data.id,
                type = data.type ?: NotificationType.THREAD_COMMENT_REPLY,
                createdAt = data.createdAt ?: 0,
                context = data.context ?: "",
                user = data.user?.notifUser?.toDomain(),
                threadId = data.thread?.id ?: 0,
                threadTitle = data.thread?.title ?: "",
                commentId = data.comment?.id,
                commentPreview = data.comment?.comment
            )
        }
        notification.onThreadCommentSubscribedNotification?.let { data ->
            return ThreadCommentSubscribedNotification(
                id = data.id,
                type = data.type ?: NotificationType.THREAD_SUBSCRIBED,
                createdAt = data.createdAt ?: 0,
                context = data.context ?: "",
                user = data.user?.notifUser?.toDomain(),
                threadId = data.thread?.id ?: 0,
                threadTitle = data.thread?.title ?: "",
                commentId = data.comment?.id,
                commentPreview = data.comment?.comment
            )
        }
        notification.onThreadCommentMentionNotification?.let { data ->
            return ThreadCommentMentionNotification(
                id = data.id,
                type = data.type ?: NotificationType.THREAD_COMMENT_MENTION,
                createdAt = data.createdAt ?: 0,
                context = data.context ?: "",
                user = data.user?.notifUser?.toDomain(),
                threadId = data.thread?.id ?: 0,
                threadTitle = data.thread?.title ?: "",
                commentId = data.comment?.id,
                commentPreview = data.comment?.comment
            )
        }
        notification.onThreadLikeNotification?.let { data ->
            return ThreadLikeNotification(
                id = data.id,
                type = data.type ?: NotificationType.THREAD_LIKE,
                createdAt = data.createdAt ?: 0,
                context = data.context ?: "",
                user = data.user?.notifUser?.toDomain(),
                threadId = data.thread?.id ?: 0,
                threadTitle = data.thread?.title ?: ""
            )
        }
        notification.onThreadCommentLikeNotification?.let { data ->
            return ThreadCommentLikeNotification(
                id = data.id,
                type = data.type ?: NotificationType.THREAD_COMMENT_LIKE,
                createdAt = data.createdAt ?: 0,
                context = data.context ?: "",
                user = data.user?.notifUser?.toDomain(),
                threadId = data.thread?.id ?: 0,
                threadTitle = data.thread?.title ?: "",
                commentId = data.comment?.id,
                commentPreview = data.comment?.comment
            )
        }
        notification.onRelatedMediaAdditionNotification?.let { data ->
            return RelatedMediaAdditionNotification(
                id = data.id,
                type = data.type ?: NotificationType.RELATED_MEDIA_ADDITION,
                createdAt = data.createdAt ?: 0,
                context = data.context ?: "",
                mediaId = data.mediaId,
                media = data.media?.notifMedia?.toDomain()
            )
        }
        notification.onMediaDataChangeNotification?.let { data ->
            return MediaDataChangeNotification(
                id = data.id,
                type = data.type ?: NotificationType.MEDIA_DATA_CHANGE,
                createdAt = data.createdAt ?: 0,
                context = data.context ?: "",
                reason = data.reason ?: "",
                mediaId = data.mediaId,
                media = data.media?.notifMedia?.toDomain()
            )
        }
        notification.onMediaMergeNotification?.let { data ->
            return MediaMergeNotification(
                id = data.id,
                type = data.type ?: NotificationType.MEDIA_MERGE,
                createdAt = data.createdAt ?: 0,
                context = data.context ?: "",
                reason = data.reason ?: "",
                mediaId = data.mediaId,
                deletedMediaTitles = data.deletedMediaTitles?.filterNotNull() ?: emptyList(),
                media = data.media?.notifMedia?.toDomain()
            )
        }
        notification.onMediaDeletionNotification?.let { data ->
            return MediaDeletionNotification(
                id = data.id,
                type = data.type ?: NotificationType.MEDIA_DELETION,
                createdAt = data.createdAt ?: 0,
                context = data.context ?: "",
                reason = data.reason ?: "",
                deletedMediaTitle = data.deletedMediaTitle ?: ""
            )
        }
        return null
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
                            mediaCover = CoverImage.of(it.media?.coverImage?.medium, it.media?.coverImage?.large, it.media?.coverImage?.extraLarge),
                            mediaType = it.media?.type ?: MediaType.ANIME
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
                            mediaCover = CoverImage.of(it.media?.coverImage?.medium, it.media?.coverImage?.large, it.media?.coverImage?.extraLarge),
                            mediaType = it.media?.type ?: MediaType.ANIME
                        )
                    } else {
                        null
                    }
                }
            } ?: emptyList()
        }
    }
}
