package com.anisync.android.domain

interface ActivityRepository {
    suspend fun getActivity(id: Int): Result<ActivityDetail>
    suspend fun sendReply(activityId: Int, text: String): Result<ActivityReply>
    suspend fun toggleActivityLike(id: Int): Result<LikeState>
    suspend fun toggleReplyLike(id: Int): Result<LikeState>
    suspend fun deleteActivity(id: Int): Result<Unit>
    suspend fun deleteReply(id: Int): Result<Unit>
    suspend fun getViewerId(): Int?
    suspend fun toggleSubscription(activityId: Int, subscribe: Boolean): Result<Unit>
    suspend fun saveTextActivity(text: String): Result<Unit>
}
