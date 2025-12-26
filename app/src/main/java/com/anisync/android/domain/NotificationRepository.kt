package com.anisync.android.domain

interface NotificationRepository {
    suspend fun getNotifications(page: Int): Result<List<Notification>>
    suspend fun getFirstEpisodeAirings(mediaIds: List<Int>): Result<List<AiringSchedule>>
    
    /**
     * Get Episode 1 airings scheduled within the next [withinHours] hours.
     * Used for upcoming airing notifications.
     */
    suspend fun getUpcomingFirstEpisodes(
        mediaIds: List<Int>,
        withinHours: Int = 24
    ): Result<List<AiringSchedule>>
}
