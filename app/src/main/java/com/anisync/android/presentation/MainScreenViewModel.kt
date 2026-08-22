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
    private val appSettings: AppSettings,
    val toastManager: ToastManager,
    searchLauncher: com.anisync.android.domain.DiscoverSearchLauncher
) : ViewModel() {

    val unreadNotificationCount: StateFlow<Int> = notificationBadgeStore.unreadCount

    /**
     * "Open Discover search with preset filters" navigation triggers. MainScreen
     * switches to the Discover tab on each emission; DiscoverViewModel separately
     * applies and consumes the filters themselves.
     */
    val discoverSearchNavigations: kotlinx.coroutines.flow.SharedFlow<Unit> =
        searchLauncher.navigationRequests

    val navBarStyle: StateFlow<NavBarStyle> = appSettings.navBarStyle
    val navBarShowLabels: StateFlow<Boolean> = appSettings.navBarShowLabels
    val navBarCornerRadius: StateFlow<Float> = appSettings.navBarCornerRadius

    /**
     * The tab a cold launch opens on, captured once at startup. A pinned Open-on choice wins;
     * otherwise this is the tab the user last visited, which is null on a first ever launch and
     * falls back to the default tab.
     */
    val startTabKey: String? =
        appSettings.startScreen.value.tabKey ?: appSettings.lastMainTab.value

    /** Remember the main tab the user switched to, for the next cold launch. */
    fun onMainTabSelected(tabKey: String) {
        appSettings.setLastMainTab(tabKey)
    }

    fun refreshNotificationBadge() {
        viewModelScope.launch { notificationBadgeStore.refresh() }
    }
}
