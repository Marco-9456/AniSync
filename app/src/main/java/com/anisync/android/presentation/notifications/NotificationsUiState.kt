package com.anisync.android.presentation.notifications

import com.anisync.android.domain.Notification
import com.anisync.android.domain.NotificationFilter

data class NotificationsUiState(
    val items: List<Notification> = emptyList(),
    val entries: List<NotificationEntry> = emptyList(),
    val filter: NotificationFilter = NotificationFilter.ALL,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isPaginating: Boolean = false,
    val hasNextPage: Boolean = true,
    val errorMessage: String? = null
) {
    /**
     * Unread rows in the list currently on screen, which is what the count beside the title shows.
     * Filtering narrows it, so the pill and the New section always agree. The inbox-wide count
     * stays on the Profile tab badge.
     */
    val newCount: Int get() = entries.count { it.isUnread }
}

sealed interface NotificationsAction {
    data class SetFilter(val filter: NotificationFilter) : NotificationsAction
    data object Refresh : NotificationsAction
    data object LoadNextPage : NotificationsAction
    data object Retry : NotificationsAction
    data object MarkAllRead : NotificationsAction
}
