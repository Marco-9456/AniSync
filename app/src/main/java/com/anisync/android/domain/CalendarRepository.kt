package com.anisync.android.domain

/**
 * Repository for the airing calendar. Fetches the anime episodes airing within a
 * time window (typically one week) from the AniList airing schedule.
 */
interface CalendarRepository {
    /**
     * Get every episode airing in the half-open window
     * `[weekStartEpochSec, weekEndEpochSec)`.
     *
     * @param weekStartEpochSec inclusive lower bound, Unix seconds (UTC)
     * @param weekEndEpochSec   exclusive upper bound, Unix seconds (UTC)
     * @return episodes sorted by airing time, or an error
     */
    suspend fun getWeekSchedule(
        weekStartEpochSec: Long,
        weekEndEpochSec: Long
    ): Result<List<AiringEpisode>>
}
