package com.anisync.android.domain

/**
 * Repository interface for fetching user statistics.
 */
interface StatisticsRepository {
    /**
     * Fetches comprehensive statistics for the given user.
     *
     * @param userId The AniList user ID
     * @param policy Cache strategy. Defaults to [CachePolicy.NetworkFirst] so the
     *   activity-history heatmap (which rides this payload) stays fresh; callers pass
     *   [CachePolicy.NetworkOnly] on an explicit pull-to-refresh. A cache-only default
     *   would freeze the heatmap at first fetch — the SQLite normalized cache never
     *   revalidates on its own.
     * @return Result containing UserStatistics or an error
     */
    suspend fun getUserStatistics(
        userId: Int,
        policy: CachePolicy = CachePolicy.NetworkFirst
    ): Result<UserStatistics>
}
