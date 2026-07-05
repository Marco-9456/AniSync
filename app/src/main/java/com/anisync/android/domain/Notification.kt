package com.anisync.android.domain

import com.anisync.android.type.NotificationType

sealed interface Notification {
    val id: Int
    val type: NotificationType
    val createdAt: Int
}

data class AiringNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val episode: Int,
    val contexts: List<String>,
    val media: Media?
) : Notification

data class FollowingNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?
) : Notification

/**
 * Discriminator for the AniList ActivityUnion. Lets the UI pick a precise
 * verb ("status update" vs "anime list update" vs "message") instead of the
 * generic "activity" string AniList serves.
 */
enum class ActivityKind { TEXT, ANIME_LIST, MANGA_LIST, MESSAGE, UNKNOWN }

/** Noun phrase for the activity subtype, shared by the inbox cards and system notifications. */
fun ActivityKind?.noun(): String = when (this) {
    ActivityKind.TEXT -> "status update"
    ActivityKind.ANIME_LIST -> "anime list update"
    ActivityKind.MANGA_LIST -> "manga list update"
    ActivityKind.MESSAGE -> "message"
    ActivityKind.UNKNOWN, null -> "post"
}

/** [noun] with the right indefinite article ("an anime list update", "a post"). */
fun ActivityKind?.indefiniteNoun(): String =
    noun().let { if (it.first() in "aeiou") "an $it" else "a $it" }

data class ActivitySnapshot(
    val kind: ActivityKind,
    val text: String? = null,        // TextActivity body
    val message: String? = null,     // MessageActivity body
    val listStatus: String? = null,  // ListActivity verb (e.g. "Watched", "Plans to watch")
    val listProgress: String? = null,// ListActivity progress (e.g. "5", "5/12")
    val listMedia: Media? = null     // ListActivity target media
)

data class ActivityLikeNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val activityId: Int?,
    val activity: ActivitySnapshot?
) : Notification

data class ActivityReplyNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val activityId: Int?,
    val activity: ActivitySnapshot?
) : Notification

data class ActivityReplySubscribedNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val activityId: Int?,
    val activity: ActivitySnapshot?
) : Notification

data class ActivityReplyLikeNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val activityId: Int?,
    val activity: ActivitySnapshot?
) : Notification

data class ActivityMentionNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val activityId: Int?,
    val activity: ActivitySnapshot?
) : Notification

data class ActivityMessageNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val activityId: Int?,
    val messagePreview: String?
) : Notification

data class ThreadCommentReplyNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val threadId: Int,
    val threadTitle: String,
    val commentId: Int?,
    val commentPreview: String?
) : Notification

data class ThreadCommentSubscribedNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val threadId: Int,
    val threadTitle: String,
    val commentId: Int?,
    val commentPreview: String?
) : Notification

data class ThreadCommentMentionNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val threadId: Int,
    val threadTitle: String,
    val commentId: Int?,
    val commentPreview: String?
) : Notification

data class ThreadLikeNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val threadId: Int,
    val threadTitle: String
) : Notification

data class ThreadCommentLikeNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val threadId: Int,
    val threadTitle: String,
    val commentId: Int?,
    val commentPreview: String?
) : Notification

data class RelatedMediaAdditionNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val mediaId: Int,
    val media: Media?
) : Notification

data class MediaDataChangeNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val reason: String,
    val mediaId: Int,
    val media: Media?
) : Notification

data class MediaMergeNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val reason: String,
    val mediaId: Int,
    val deletedMediaTitles: List<String>,
    val media: Media?
) : Notification

data class MediaDeletionNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val reason: String,
    val deletedMediaTitle: String
) : Notification

data class UnknownNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int
) : Notification

// Reusing existing models where possible, or defining simplified versions for Notifications
data class Media(
    val id: Int,
    val title: String,
    val coverUrl: String?,
    val cover: CoverImage? = null,
    val type: com.anisync.android.type.MediaType
)

data class User(
    val id: Int,
    val name: String,
    val avatarUrl: String?
)
