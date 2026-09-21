package com.anisync.android.presentation

import android.view.ViewConfiguration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.data.NavBarStyle
import com.anisync.android.data.NotificationBadgeStore
import com.anisync.android.data.NotificationReadStore
import com.anisync.android.data.network.RateLimitMonitor
import com.anisync.android.domain.MainTab
import com.anisync.android.domain.TabReselectBus
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
    private val notificationReadStore: NotificationReadStore,
    private val appSettings: AppSettings,
    val toastManager: ToastManager,
    /** Drives the rate limit notice and the pull-to-refresh gates. */
    val rateLimitMonitor: RateLimitMonitor,
    private val tabReselectBus: TabReselectBus,
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

    /** When a navigation item was last tapped, for telling a second tap from a first. */
    private var lastTap: Pair<MainTab, Long>? = null

    /**
     * A tap on a navigation item, whether or not [tab] was already the open one.
     *
     * Two taps inside the platform's double-tap window open that tab's search, and the count runs
     * across the switch: the gesture means the same thing from the tab you are on and from any
     * other, which is what the shortcut was asked for. A tab with nothing to search answers the
     * second tap the way it answers the first.
     *
     * A single tap on the tab you are already on scrolls it back to the top, immediately rather
     * than waiting to see whether a second follows: holding it back would put the double-tap
     * timeout in front of a gesture people make constantly, and scrolling to the top is a fine
     * prelude to searching anyway. A tap that switches tabs scrolls nothing, since that tab's
     * position is restored with it and throwing it away is not what the tap asked for.
     */
    fun onTabTapped(tab: MainTab, alreadySelected: Boolean) {
        val now = System.currentTimeMillis()
        val previous = lastTap
        val isSecondTap = previous != null &&
            previous.first == tab &&
            now - previous.second <= DOUBLE_TAP_WINDOW_MS

        if (isSecondTap && tab.hasSearch && appSettings.navBarDoubleTapSearch.value) {
            lastTap = null
            tabReselectBus.requestSearch(tab)
            return
        }

        lastTap = tab to now
        if (alreadySelected) tabReselectBus.requestScrollToTop(tab)
    }

    /** Opens [tab]'s search without the gesture, for the navigation item's accessibility action. */
    fun onTabSearchRequested(tab: MainTab) {
        if (tab.hasSearch) tabReselectBus.requestSearch(tab)
    }

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

    /**
     * The tip card the app floats on the first launch after an update. Checked here rather than in
     * the Application: this is the point where there is a window to float it over.
     */
    val supportPromptVisible: StateFlow<Boolean> = appSettings.supportPromptVisible

    init {
        appSettings.noteAppVersion()
        // Rows already read on this device are subtracted from AniList's count, which only ever
        // clears all at once. Applied here so the badge is right before the inbox is ever opened.
        notificationReadStore.syncBadge()
    }

    fun onSupportPromptDismissed() {
        appSettings.dismissSupportPrompt()
    }

    private companion object {
        /** Follows the platform (and the user's accessibility timing), rather than a fixed 300. */
        val DOUBLE_TAP_WINDOW_MS = ViewConfiguration.getDoubleTapTimeout().toLong()
    }
}
