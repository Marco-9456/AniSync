package com.anisync.android.domain

/**
 * Repository interface for notification-related data operations.
 * Provides methods to fetch user notifications and upcoming episode airings.
 */
interface NotificationRepository {
    /**
     * Fetch paginated notifications for the current user.
     * @param page Page number (1-indexed)
     * @return List of notifications or error
     */
    suspend fun getNotifications(page: Int): Result<List<Notification>>

    /**
     * Get first episode airings for media in the user's planning list.
     * @param mediaIds List of media IDs to check
     * @return List of airing schedules for Episode 1 or error
     */
    suspend fun getFirstEpisodeAirings(mediaIds: List<Int>): Result<List<AiringSchedule>>
    
    /**
     * Get Episode 1 airings scheduled within the next [withinHours] hours.
     * Used for upcoming airing notifications.
     * @param mediaIds List of media IDs to check
     * @param withinHours Hours window to look ahead (default: 24)
     * @return List of upcoming airing schedules or error
     */
    suspend fun getUpcomingFirstEpisodes(
        mediaIds: List<Int>,
        withinHours: Int = 24
    ): Result<List<AiringSchedule>>
}
