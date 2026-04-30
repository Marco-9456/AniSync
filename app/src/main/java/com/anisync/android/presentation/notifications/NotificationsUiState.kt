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
)

sealed interface NotificationsAction {
    data class SetFilter(val filter: NotificationFilter) : NotificationsAction
    data object Refresh : NotificationsAction
    data object LoadNextPage : NotificationsAction
    data object Retry : NotificationsAction
}
