package com.anisync.android.domain

interface NotificationRepository {
    suspend fun getNotifications(page: Int): Result<List<Notification>>
}
