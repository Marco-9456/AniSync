package com.anisync.android.domain

data class ActivityReply(
    val id: Int,
    val body: String,
    /** Raw markdown source (asHtml: false). Available for replies the viewer can edit. */
    val bodyMarkdown: String? = null,
    val likeCount: Int,
    val isLiked: Boolean,
    val authorId: Int,
    val authorName: String,
    val authorAvatarUrl: String?,
    val createdAt: Long
)

data class ActivityDetail(
    val id: Int,
    val body: String,
    /** Raw markdown source (asHtml: false). Only populated for TextActivity. */
    val bodyMarkdown: String? = null,
    val createdAt: Long,
    val likeCount: Int,
    val isLiked: Boolean,
    val replyCount: Int,
    val siteUrl: String?,
    val isMessage: Boolean,
    val isPrivate: Boolean,
    val isSubscribed: Boolean = false,
    val authorId: Int,
    val authorName: String,
    val authorAvatarUrl: String?,
    val recipientId: Int?,
    val recipientName: String?,
    val recipientAvatarUrl: String?,
    val isAuthorMod: Boolean = false,
    val replies: List<ActivityReply>
)

data class LikeState(val id: Int, val likeCount: Int, val isLiked: Boolean)
