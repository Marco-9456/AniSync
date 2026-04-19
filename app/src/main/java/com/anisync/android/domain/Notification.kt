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

data class ActivityLikeNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val activityId: Int?
) : Notification

data class ActivityReplyNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val activityId: Int?
) : Notification

data class ActivityReplySubscribedNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val activityId: Int?
) : Notification

data class ActivityReplyLikeNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val activityId: Int?
) : Notification

data class ActivityMentionNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val activityId: Int?
) : Notification

data class ActivityMessageNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val activityId: Int?
) : Notification

data class ThreadCommentReplyNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val threadId: Int,
    val threadTitle: String,
    val commentId: Int?
) : Notification

data class ThreadCommentSubscribedNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val threadId: Int,
    val threadTitle: String,
    val commentId: Int?
) : Notification

data class ThreadCommentMentionNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    val threadId: Int,
    val threadTitle: String,
    val commentId: Int?
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
    val commentId: Int?
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
    val type: com.anisync.android.type.MediaType
)

data class User(
    val id: Int,
    val name: String,
    val avatarUrl: String?
)
