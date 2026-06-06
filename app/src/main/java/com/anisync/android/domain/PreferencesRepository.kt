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
    
    // ---- Key-based notification tracking for two-tier system ----
    
    /**
     * Check if a notification has been sent for a specific key.
     * Used for two-tier notification system (advance_123, imminent_123).
     */
    suspend fun hasNotifiedWithKey(key: String): Boolean
    
    /**
     * Mark a notification key as sent.
     */
    suspend fun markNotifiedWithKey(key: String)

    // ---- Social/Forum notification tracking ----

    /**
     * Get the highest notification ID that was already processed for social/forum notifications.
     */
    suspend fun getLastSocialNotifiedId(): Int

    /**
     * Set the highest processed social/forum notification ID.
     */
    suspend fun setLastSocialNotifiedId(id: Int)

    /**
     * Wipe all notification dedup state. Called on account switch so the new account re-establishes
     * its own baseline (preventing both a backlog flood and the previous account's high-water marks
     * silently suppressing the new account's notifications).
     */
    suspend fun clearAll()
}
