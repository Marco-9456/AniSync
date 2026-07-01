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

/**
 * The mutual follow relationship between the authenticated viewer and a target user.
 *
 * - [isFollowing]: the viewer follows the target (AniList `User.isFollowing`).
 * - [isFollower]: the target follows the viewer (AniList `User.isFollower`).
 *
 * Both true = a mutual follow. [isFollower] is unaffected by the viewer's own
 * follow toggle — it only changes when the target follows/unfollows the viewer.
 */
data class FollowState(
    val isFollowing: Boolean,
    val isFollower: Boolean
) {
    val isMutual: Boolean get() = isFollowing && isFollower
}

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
     * Fetch the follow relationship between the authenticated user and [userId]:
     * whether the viewer follows the target and whether the target follows back.
     */
    suspend fun getFollowState(
        userId: Int,
        policy: CachePolicy = CachePolicy.NetworkFirst
    ): Result<FollowState>

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
     * Fetch one page of the user's activity feed (newest-first), filtered to [types]
     * server-side so a narrower profile filter (status / messages / lists) paginates its
     * own stream and stops when that stream — not the whole feed — is exhausted. Used to
     * load older activities past the initial batch the profile fetch carries — the caller
     * dedupes page 1's overlap by id.
     */
    suspend fun getUserActivitiesPage(
        userId: Int,
        page: Int,
        types: List<ActivityType> = listOf(
            ActivityType.TEXT,
            ActivityType.MESSAGE,
            ActivityType.MEDIA_LIST
        ),
        policy: CachePolicy = CachePolicy.NetworkOnly
    ): Result<UserActivitiesPage>

    /**
     * Fetch the user's full favourite-anime list (all pages). Loaded lazily when
     * the Favorites tab opens; the profile fetch itself only carries page 1, so
     * a normal profile load/refresh no longer fans out across favourite pages.
     */
    suspend fun getFavoriteAnime(
        userId: Int,
        policy: CachePolicy = CachePolicy.NetworkFirst
    ): Result<List<LibraryEntry>>

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
