package com.anisync.android.domain

/**
 * Represents a forum thread on AniList.
 * @param body Nullable — not fetched in list queries, only in detail queries.
 * @param updatedAt Unix timestamp of last update. Falls back to createdAt if null.
 */
data class ForumThread(
    val id: Int,
    val title: String,
    val body: String?,
    /** Raw markdown source (asHtml: false). Available for threads the viewer can edit. */
    val bodyMarkdown: String? = null,
    val replyCount: Int,
    val viewCount: Int,
    val likeCount: Int,
    val isLiked: Boolean,
    val isSubscribed: Boolean,
    val isLocked: Boolean,
    val isSticky: Boolean = false,
    val authorId: Int,
    val authorName: String,
    val authorAvatarUrl: String?,
    val repliedAt: Long? = null,
    val replyUserName: String? = null,
    val replyUserAvatarUrl: String? = null,
    val replyCommentId: Int? = null,
    val categories: List<ForumCategory>,
    val createdAt: Long,
    val updatedAt: Long,
    val siteUrl: String?,
    val mediaTitle: String? = null,
    val mediaCoverUrl: String? = null,
    val mediaCover: CoverImage? = null
)
