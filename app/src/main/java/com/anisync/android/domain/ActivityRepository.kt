package com.anisync.android.domain

interface ActivityRepository {
    suspend fun getActivity(id: Int): Result<ActivityDetail>
    suspend fun sendReply(activityId: Int, text: String, id: Int? = null): Result<ActivityReply>
    suspend fun toggleActivityLike(id: Int): Result<LikeState>
    suspend fun toggleReplyLike(id: Int): Result<LikeState>
    suspend fun deleteActivity(id: Int): Result<Unit>
    suspend fun deleteReply(id: Int): Result<Unit>
    suspend fun getViewerId(): Int?

    /** Drops the in-memory cached viewer id so the next call re-resolves it (used on account switch). */
    fun clearViewerCache()
    suspend fun toggleSubscription(activityId: Int, subscribe: Boolean): Result<Unit>
    suspend fun saveTextActivity(text: String, id: Int? = null): Result<Unit>

    /**
     * Update an existing MessageActivity. AniList requires [recipientId] even on edit
     * (the messenger cannot change). Pass the id of the activity to update.
     */
    suspend fun saveMessageActivity(
        id: Int,
        recipientId: Int,
        message: String,
        isPrivate: Boolean
    ): Result<Unit>

    /** List of users who liked the given root activity. */
    suspend fun getActivityLikes(activityId: Int): Result<List<UserSummary>>

    /** List of users who liked the given activity reply. */
    suspend fun getActivityReplyLikes(replyId: Int): Result<List<UserSummary>>
}
