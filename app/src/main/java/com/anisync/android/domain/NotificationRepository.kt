package com.anisync.android.domain

/**
 * Repository interface for notification-related data operations.
 * Provides methods to fetch user notifications and upcoming episode airings.
 */
interface NotificationRepository {
    /**
     * Fetch paginated notifications (background poller).
     * @param page Page number (1-indexed)
     * @param token AniList token to poll a specific account; null uses the active account's client.
     * @return List of notifications or error
     */
    suspend fun getNotifications(page: Int, token: String? = null): Result<List<Notification>>

    /**
     * Fetch paginated notifications with optional type filtering and pagination metadata.
     * Used by the dedicated Notifications screen.
     * @param page Page number (1-indexed)
     * @param typeFilter Optional list of types (null = all)
     * @param resetUnreadCount Server-side mark-as-read on this fetch
     */
    suspend fun getNotificationsPage(
        page: Int,
        typeFilter: List<com.anisync.android.type.NotificationType>? = null,
        resetUnreadCount: Boolean = false
    ): Result<NotificationPage>

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
