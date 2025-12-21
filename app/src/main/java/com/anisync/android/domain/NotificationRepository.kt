package com.anisync.android.domain

interface NotificationRepository {
    suspend fun getNotifications(page: Int): Result<List<Notification>>
    suspend fun getFirstEpisodeAirings(mediaIds: List<Int>): Result<List<AiringSchedule>>
}
