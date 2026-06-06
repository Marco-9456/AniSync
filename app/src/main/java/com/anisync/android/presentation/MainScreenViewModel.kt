package com.anisync.android.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.data.NavBarStyle
import com.anisync.android.data.NotificationBadgeStore
import com.anisync.android.data.account.AccountManager
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
    accountManager: AccountManager,
    val toastManager: ToastManager
) : ViewModel() {

    /**
     * After an account switch the MainScreen subtree is rebuilt (session epoch > 0) — land on
     * Profile so the user sees the account they just switched to. A genuinely fresh process
     * (epoch 0) instead restores [startTabKey].
     */
    val startOnProfile: Boolean = accountManager.sessionEpoch.value > 0

    val unreadNotificationCount: StateFlow<Int> = notificationBadgeStore.unreadCount

    val navBarStyle: StateFlow<NavBarStyle> = appSettings.navBarStyle
    val navBarShowLabels: StateFlow<Boolean> = appSettings.navBarShowLabels
    val navBarCornerRadius: StateFlow<Float> = appSettings.navBarCornerRadius

    /**
     * The main bottom-nav tab the user last visited, captured once at startup so the
     * NavHost can open on it. Null on first ever launch (falls back to the default tab).
     */
    val startTabKey: String? = appSettings.lastMainTab.value

    /** Remember the main tab the user switched to, for the next cold launch. */
    fun onMainTabSelected(tabKey: String) {
        appSettings.setLastMainTab(tabKey)
    }

    fun refreshNotificationBadge() {
        viewModelScope.launch { notificationBadgeStore.refresh() }
    }
}
