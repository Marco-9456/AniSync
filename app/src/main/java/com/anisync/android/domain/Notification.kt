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
    // val activity: Activity? // Simplified for now
) : Notification

data class ActivityReplyNotification(
    override val id: Int,
    override val type: NotificationType,
    override val createdAt: Int,
    val context: String,
    val user: User?,
    // val activity: Activity? // Simplified for now
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
