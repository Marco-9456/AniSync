package com.anisync.android.domain

import com.anisync.android.type.NotificationType
import javax.inject.Inject

class GetNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(page: Int = 1): Result<List<Notification>> {
        return repository.getNotifications(page)
    }

    suspend fun getPage(
        page: Int,
        typeFilter: List<NotificationType>? = null,
        resetUnreadCount: Boolean = false
    ): Result<NotificationPage> {
        return repository.getNotificationsPage(page, typeFilter, resetUnreadCount)
    }
}
