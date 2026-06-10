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
     * Used on the category browse screen. Thin wrapper over [searchThreads].
     */
    suspend fun getThreadsByCategory(
        categoryId: Int?,
        search: String?,
        page: Int
    ): Result<PaginatedResult<ForumThread>>

    /**
     * Advanced thread search. Any combination of [search], [categoryId],
     * [mediaCategoryId] (a related media id), [userId] (thread author id) and
     * [subscribed] is supported, ordered by [sort]. Backs the forum search
     * overlay, the media-detail Discussions section, and the per-media thread
     * list. Pass `null` for any unused filter.
     */
    suspend fun searchThreads(
        search: String? = null,
        categoryId: Int? = null,
        mediaCategoryId: Int? = null,
        userId: Int? = null,
        subscribed: Boolean? = null,
        sort: ThreadSortOption = ThreadSortOption.Default,
        page: Int = 1
    ): Result<PaginatedResult<ForumThread>>

    /**
     * Fetches the full detail of a single thread (including body).
     */
    suspend fun getThread(threadId: Int): Result<ForumThread>

    /**
     * Fetches a page of top-level comments for a thread.
     */
    suspend fun getComments(threadId: Int, page: Int, sort: String? = null): Result<PaginatedResult<ForumComment>>

    /**
     * Locates which page contains [commentId] in the comments list of [threadId]
     * given the current [sort]. Uses a client-side binary search over the page
     * range because the AniList API does not expose this lookup directly.
     */
    suspend fun findCommentPage(
        threadId: Int,
        commentId: Int,
        sort: String? = null,
        perPage: Int = 25,
    ): Result<Int>

    // =========================================================================
    // WRITE OPERATIONS
    // =========================================================================

    /**
     * Creates or updates a forum thread. Pass [id] to update an existing thread.
     *
     * [mediaCategoryIds] is the list of AniList media IDs related to the thread
     * (separate from forum [categoryIds]). Pass `null` to leave existing media
     * categories untouched on update; pass an empty list to clear them.
     */
    suspend fun createThread(
        title: String,
        body: String,
        categoryIds: List<Int>,
        mediaCategoryIds: List<Int>? = null,
        id: Int? = null
    ): Result<ForumThread>

    /**
     * Creates or updates a top-level comment on a thread. Pass [id] to update.
     */
    suspend fun createComment(threadId: Int, comment: String, id: Int? = null): Result<ForumComment>

    /**
     * Creates or updates a reply to an existing comment. Pass [id] to update.
     */
    suspend fun replyToComment(
        threadId: Int,
        comment: String,
        parentCommentId: Int,
        id: Int? = null
    ): Result<ForumComment>

    /** Permanently deletes a forum thread. */
    suspend fun deleteThread(threadId: Int): Result<Unit>

    /** Permanently deletes a forum comment. */
    suspend fun deleteComment(commentId: Int): Result<Unit>

    /** Returns the authenticated viewer's user id, or null if unauthenticated. */
    suspend fun getViewerId(): Int?

    /**
     * Toggles the like state for a thread.
     */
    suspend fun toggleLikeThread(threadId: Int): Result<Unit>

    /**
     * Toggles the like state for a comment.
     */
    suspend fun toggleLikeComment(commentId: Int): Result<Unit>

    /** List of users who liked the given thread. */
    suspend fun getThreadLikes(threadId: Int): Result<List<UserSummary>>

    /** List of users who liked the given thread comment. */
    suspend fun getThreadCommentLikes(commentId: Int): Result<List<UserSummary>>

    // =========================================================================
    // SUBSCRIBED / SAVED / SUBSCRIPTION
    // =========================================================================

    /**
     * Fetches threads the authenticated user is subscribed to.
     */
    suspend fun getSubscribedThreads(page: Int): Result<PaginatedResult<ForumThread>>

    /**
     * Toggles the thread subscription status on AniList.
     */
    suspend fun toggleThreadSubscription(threadId: Int, subscribe: Boolean): Result<Unit>

    /**
     * Returns all locally saved (bookmarked) threads.
     */
    suspend fun getSavedThreads(): List<ForumThread>

    /**
     * Saves a thread locally.
     */
    suspend fun saveThread(thread: ForumThread)

    /**
     * Removes a saved thread.
     */
    suspend fun unsaveThread(threadId: Int)

    /**
     * Checks if a thread is locally saved.
     */
    suspend fun isThreadSaved(threadId: Int): Boolean
}
