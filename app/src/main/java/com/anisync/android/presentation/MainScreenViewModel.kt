package com.anisync.android.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.data.NavBarStyle
import com.anisync.android.data.NotificationBadgeStore
import com.anisync.android.presentation.components.alert.ToastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Top-level ViewModel scoped to [MainScreen]. Surfaces app-wide state
 * the bottom navigation needs — currently the inbox unread count (for
 * the Profile destination badge) and the user's nav bar preferences.
 */
@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val notificationBadgeStore: NotificationBadgeStore,
    appSettings: AppSettings,
    val toastManager: ToastManager
) : ViewModel() {

    val unreadNotificationCount: StateFlow<Int> = notificationBadgeStore.unreadCount

    val navBarStyle: StateFlow<NavBarStyle> = appSettings.navBarStyle
    val navBarShowLabels: StateFlow<Boolean> = appSettings.navBarShowLabels
    val navBarCornerRadius: StateFlow<Float> = appSettings.navBarCornerRadius

    fun refreshNotificationBadge() {
        viewModelScope.launch { notificationBadgeStore.refresh() }
    }
}
