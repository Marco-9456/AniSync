package com.anisync.android.domain

/**
 * Notification dedup / baseline state, scoped per account so each signed-in account keeps its own
 * high-water marks (the background worker polls every account every cycle).
 */
interface PreferencesRepository {
    suspend fun getLastNotifiedId(accountId: Int): Int
    suspend fun setLastNotifiedId(accountId: Int, id: Int)
    suspend fun getNotifiedPlanningMediaIds(accountId: Int): Set<Int>
    suspend fun markPlanningMediaAsNotified(accountId: Int, mediaId: Int)
    /**
     * Remove IDs from the notified set that are no longer in the user's planning list.
     */
    suspend fun cleanupOrphanedPlanningIds(accountId: Int, currentPlanningIds: Set<Int>)

    // ---- Upcoming airing notifications ----

    /**
     * Get airing IDs that have already been notified about upcoming episodes.
     */
    suspend fun getNotifiedUpcomingAiringIds(accountId: Int): Set<Int>

    /**
     * Mark an airing as notified for upcoming episode.
     */
    suspend fun markUpcomingAiringNotified(accountId: Int, airingId: Int)

    /**
     * Clean up old upcoming airing IDs that are no longer relevant.
     */
    suspend fun cleanupOldUpcomingAirings(accountId: Int, currentValidIds: Set<Int>)

    /**
     * Check if the notification worker has ever completed a baseline sync for this account.
     * Used to suppress notifications on the very first run.
     */
    suspend fun hasNotificationsEverRun(accountId: Int): Boolean

    /**
     * Mark that the notification worker has completed its first baseline sync for this account.
     */
    suspend fun markNotificationsHaveRun(accountId: Int)

    // ---- Key-based notification tracking for two-tier system ----

    /**
     * Check if a notification has been sent for a specific key.
     * Used for two-tier notification system (advance_123, imminent_123).
     */
    suspend fun hasNotifiedWithKey(accountId: Int, notificationKey: String): Boolean

    /**
     * Mark a notification key as sent.
     */
    suspend fun markNotifiedWithKey(accountId: Int, notificationKey: String)

    // ---- Social/Forum notification tracking ----

    /**
     * Get the highest notification ID that was already processed for social/forum notifications.
     */
    suspend fun getLastSocialNotifiedId(accountId: Int): Int

    /**
     * Set the highest processed social/forum notification ID.
     */
    suspend fun setLastSocialNotifiedId(accountId: Int, id: Int)

    /**
     * Wipe all notification dedup state for a single account (used when the account is removed).
     */
    suspend fun clearForAccount(accountId: Int)
}
