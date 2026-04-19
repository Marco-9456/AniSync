package com.anisync.android.domain

data class CommentNode(
    val id: Int,
    val body: String,
    val likeCount: Int,
    val isLiked: Boolean,
    val authorId: Int,
    val authorName: String,
    val authorAvatarUrl: String?,
    val createdAt: Long,
    val childComments: List<CommentNode> = emptyList()
)

fun ForumComment.toCommentNode(): CommentNode = CommentNode(
    id = id,
    body = body,
    likeCount = likeCount,
    isLiked = isLiked,
    authorId = authorId,
    authorName = authorName,
    authorAvatarUrl = authorAvatarUrl,
    createdAt = createdAt,
    childComments = childComments.map { it.toCommentNode() }
)
