package com.anisync.android.presentation.notifications

import com.anisync.android.domain.Notification
import com.anisync.android.domain.NotificationFilter

data class NotificationsUiState(
    val items: List<Notification> = emptyList(),
    val entries: List<NotificationEntry> = emptyList(),
    /** [entries] split by read state, so the screen renders sections rather than deriving them. */
    val newEntries: List<NotificationEntry> = emptyList(),
    val earlierEntries: List<NotificationEntry> = emptyList(),
    val filter: NotificationFilter = NotificationFilter.ALL,
    /**
     * Whether the inbox keeps read state at all. Off means no New section, no Mark all read and no
     * dots: opening the inbox is what clears the count, the way the website behaves.
     */
    val readTrackingEnabled: Boolean = true,
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
    data object MarkAllRead : NotificationsAction
    /** One row was opened, which reads it. [key] is [NotificationEntry.key]. */
    data class MarkRead(val key: String) : NotificationsAction
}
