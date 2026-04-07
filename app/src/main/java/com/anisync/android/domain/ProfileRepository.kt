package com.anisync.android.domain

import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    /**
     * Observe user profile from local cache (reactive).
     */
    fun observeProfile(): Flow<UserProfile?>

    /**
     * Fetch fresh profile from network and update cache.
     */
    suspend fun refreshProfile(username: String): Result<Unit>

    /**
     * Fetch user profile from network without saving it to the local cache.
     */
    suspend fun fetchUserProfile(username: String): Result<UserProfile>

    /**
     * Update user's about section.
     */
    suspend fun updateAbout(about: String): Result<Unit>

    /**
     * Fetch social data.
     */
    suspend fun getSocialData(userId: Int, page: Int = 1): Result<UserSocialData>

    /**
     * Check whether the authenticated user follows [userId].
     */
    suspend fun getFollowState(userId: Int): Result<Boolean>

    /**
     * Toggle follow state for [userId] and return the new state.
     */
    suspend fun toggleFollow(userId: Int): Result<Boolean>

    /**
     * Fetch user's reviews.
     */
    suspend fun getUserReviews(userId: Int, page: Int = 1): Result<List<MediaReview>>
}
