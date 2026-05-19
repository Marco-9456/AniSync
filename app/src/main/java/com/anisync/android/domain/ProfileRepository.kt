package com.anisync.android.domain

import kotlinx.coroutines.flow.Flow

/**
 * Per-phase timings captured during a profile refresh. Emitted to logcat
 * (`AniSyncPerf`) so device-side issue reports can attribute slow refreshes
 * to a specific GraphQL phase instead of a single opaque total.
 */
data class ProfileRefreshTimings(
    val profileQueryMs: Long,
    val activitiesQueryMs: Long,
    val favoritesTotalMs: Long,
    val favoritesFirstPageMs: Long,
    val favoritesRestMs: Long,
    val favoritesPageCount: Int
)

/**
 * Caller-controlled cache policy. Lets callers choose between hitting Apollo's
 * normalized cache and bypassing it without leaking the Apollo `FetchPolicy`
 * type into the domain layer. Maps 1-to-1 to Apollo `FetchPolicy` in the impl.
 *
 * - [CacheFirst]: Apollo cache first; falls through to network on miss. Used on
 *   tab re-entry within the staleness window.
 * - [NetworkOnly]: Always hit network. Used on user-initiated pull-to-refresh.
 * - [NetworkFirst]: Network first; falls back to cache on network failure.
 *   Used as the default for read-only queries that want freshness when possible
 *   but tolerate a stale value when offline or rate-limited.
 */
enum class CachePolicy { CacheFirst, NetworkOnly, NetworkFirst }

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
     * Same as [refreshProfile] but also returns per-phase timings.
     */
    suspend fun refreshProfileTimed(username: String, forceNetwork: Boolean = true): Result<ProfileRefreshTimings>

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
    suspend fun getSocialData(
        userId: Int,
        page: Int = 1,
        policy: CachePolicy = CachePolicy.NetworkFirst
    ): Result<UserSocialPage>

    /**
     * Check whether the authenticated user follows [userId].
     */
    suspend fun getFollowState(
        userId: Int,
        policy: CachePolicy = CachePolicy.NetworkFirst
    ): Result<Boolean>

    /**
     * Toggle follow state for [userId] and return the new state.
     */
    suspend fun toggleFollow(userId: Int): Result<Boolean>

    /**
     * Fetch user's reviews.
     */
    suspend fun getUserReviews(
        userId: Int,
        page: Int = 1,
        policy: CachePolicy = CachePolicy.NetworkFirst
    ): Result<UserReviewsPage>

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
