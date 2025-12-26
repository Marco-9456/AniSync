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
     * Update user's about section.
     */
    suspend fun updateAbout(about: String): Result<Unit>
}
