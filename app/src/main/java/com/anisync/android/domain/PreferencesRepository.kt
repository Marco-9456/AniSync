package com.anisync.android.domain

interface PreferencesRepository {
    suspend fun getLastNotifiedId(): Int
    suspend fun setLastNotifiedId(id: Int)
    suspend fun getNotifiedPlanningMediaIds(): Set<Int>
    suspend fun markPlanningMediaAsNotified(mediaId: Int)
    /**
     * Remove IDs from the notified set that are no longer in the user's planning list.
     */
    suspend fun cleanupOrphanedPlanningIds(currentPlanningIds: Set<Int>)
    
    // ---- Upcoming airing notifications ----
    
    /**
     * Get airing IDs that have already been notified about upcoming episodes.
     */
    suspend fun getNotifiedUpcomingAiringIds(): Set<Int>
    
    /**
     * Mark an airing as notified for upcoming episode.
     */
    suspend fun markUpcomingAiringNotified(airingId: Int)
    
    /**
     * Clean up old upcoming airing IDs that are no longer relevant.
     */
    suspend fun cleanupOldUpcomingAirings(currentValidIds: Set<Int>)
    
    /**
     * Check if the notification worker has ever completed a baseline sync.
     * Used to suppress notifications on the very first run.
     */
    suspend fun hasNotificationsEverRun(): Boolean
    
    /**
     * Mark that the notification worker has completed its first baseline sync.
     */
    suspend fun markNotificationsHaveRun()
}
