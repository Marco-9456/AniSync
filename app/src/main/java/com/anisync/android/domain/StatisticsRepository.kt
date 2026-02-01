package com.anisync.android.domain

/**
 * Repository interface for fetching user statistics.
 */
interface StatisticsRepository {
    /**
     * Fetches comprehensive statistics for the given user.
     * @param userId The AniList user ID
     * @return Result containing UserStatistics or an error
     */
    suspend fun getUserStatistics(userId: Int): Result<UserStatistics>
}
