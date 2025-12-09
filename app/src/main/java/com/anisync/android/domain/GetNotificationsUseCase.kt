package com.anisync.android.domain

import javax.inject.Inject

class GetNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(page: Int = 1): Result<List<Notification>> {
        return repository.getNotifications(page)
    }
}
