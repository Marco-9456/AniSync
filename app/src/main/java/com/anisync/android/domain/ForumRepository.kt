package com.anisync.android.domain

/**
 * Repository interface for all AniList Forum operations.
 * Implementations talk to the Apollo GraphQL client.
 */
interface ForumRepository {

    // =========================================================================
    // READ OPERATIONS
    // =========================================================================

    /**
     * Fetches the Forum hub — recent/sticky threads across all categories.
     * Used on the main Forum tab screen.
     */
    suspend fun getRecentThreads(page: Int, sort: String? = null): Result<PaginatedResult<ForumThread>>

    /**
     * Fetches threads filtered by [categoryId] and an optional [search] query.
     * Used on the category browse screen.
     */
    suspend fun getThreadsByCategory(
        categoryId: Int?,
        search: String?,
        page: Int
    ): Result<PaginatedResult<ForumThread>>

    /**
     * Fetches the full detail of a single thread (including body).
     */
    suspend fun getThread(threadId: Int): Result<ForumThread>

    /**
     * Fetches a page of top-level comments for a thread.
     */
    suspend fun getComments(threadId: Int, page: Int): Result<PaginatedResult<ForumComment>>

    // =========================================================================
    // WRITE OPERATIONS
    // =========================================================================

    /**
     * Creates a new forum thread.
     */
    suspend fun createThread(
        title: String,
        body: String,
        categoryIds: List<Int>
    ): Result<ForumThread>

    /**
     * Posts a top-level comment on a thread.
     */
    suspend fun createComment(threadId: Int, comment: String): Result<ForumComment>

    /**
     * Posts a reply to an existing comment.
     */
    suspend fun replyToComment(
        threadId: Int,
        comment: String,
        parentCommentId: Int
    ): Result<ForumComment>

    /**
     * Toggles the like state for a thread.
     */
    suspend fun toggleLikeThread(threadId: Int): Result<Unit>

    /**
     * Toggles the like state for a comment.
     */
    suspend fun toggleLikeComment(commentId: Int): Result<Unit>
}
