package com.anisync.android.domain

import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    /**
     * Observe user profile from local cache (reactive).
     */
    fun observeProfile(): Flow<UserProfile?>

    /**
     * Fetch fresh profile and update cache. When [forceNetwork] is false the
     * Apollo normalized cache is consulted first, falling through to network
     * on a miss — used for cold-open paths where instant render beats freshness.
     */
    suspend fun refreshProfile(username: String, forceNetwork: Boolean = true): Result<Unit>

    /**
     * Fetch user profile without saving it to the local cache. [forceNetwork]
     * behaves the same as in [refreshProfile].
     */
    suspend fun fetchUserProfile(username: String, forceNetwork: Boolean = true): Result<UserProfile>

    /**
     * Update user's about section.
     */
    suspend fun updateAbout(about: String): Result<Unit>

    /**
     * Fetch social data.
     */
    suspend fun getSocialData(userId: Int, page: Int = 1): Result<UserSocialPage>

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
    suspend fun getUserReviews(userId: Int, page: Int = 1): Result<UserReviewsPage>

    /**
     * Fetch user's anime list.
     */
    suspend fun getUserAnimeList(username: String): Result<List<LibraryEntry>>

    /**
     * Fetch user's manga list.
     */
    suspend fun getUserMangaList(username: String): Result<List<LibraryEntry>>

    /**
     * Send or update a MessageActivity. Pass [id] to edit an existing message.
     */
    suspend fun sendMessageActivity(
        recipientId: Int,
        message: String,
        isPrivate: Boolean,
        id: Int? = null
    ): Result<Unit>
}
