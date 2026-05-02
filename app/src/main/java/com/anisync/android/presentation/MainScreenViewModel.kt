package com.anisync.android.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.NotificationBadgeStore
import com.anisync.android.presentation.components.alert.ToastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Top-level ViewModel scoped to [MainScreen]. Surfaces app-wide state
 * the bottom navigation needs — currently the inbox unread count, which
 * decorates the Profile destination with a badge.
 */
@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val notificationBadgeStore: NotificationBadgeStore,
    val toastManager: ToastManager
) : ViewModel() {

    val unreadNotificationCount: StateFlow<Int> = notificationBadgeStore.unreadCount

    fun refreshNotificationBadge() {
        viewModelScope.launch { notificationBadgeStore.refresh() }
    }
}
