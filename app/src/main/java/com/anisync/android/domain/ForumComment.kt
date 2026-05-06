package com.anisync.android.domain

/**
 * Represents a comment on a forum thread.
 * [childComments] is a raw JSON string from AniList (they return it as serialized JSON),
 * parsed at the repository level into a nested list of comments.
 */
data class ForumComment(
    val id: Int,
    val body: String,
    /** Raw markdown source (asHtml: false). Available for comments the viewer can edit. */
    val bodyMarkdown: String? = null,
    val likeCount: Int,
    val isLiked: Boolean,
    val authorId: Int,
    val authorName: String,
    val authorAvatarUrl: String?,
    val createdAt: Long,
    val siteUrl: String?,
    val childComments: List<ForumComment> = emptyList()
)
