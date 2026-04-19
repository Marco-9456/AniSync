package com.anisync.android.presentation.settings

import androidx.compose.ui.graphics.Color
import com.anisync.android.data.AppLocale
import com.anisync.android.data.StreamingService
import com.anisync.android.data.ThemeMode
import com.anisync.android.data.TitleLanguage
import com.anisync.android.data.update.Release
import com.anisync.android.domain.UserProfile
import com.materialkolor.PaletteStyle

sealed interface SettingsAction {
    data class SetThemeMode(val mode: ThemeMode) : SettingsAction
    data class SetTitleLanguage(val language: TitleLanguage) : SettingsAction
    data class SetHapticEnabled(val enabled: Boolean) : SettingsAction
    data class SetPreferredStreamingService(val service: StreamingService) : SettingsAction
    data class SetAppLocale(val locale: AppLocale) : SettingsAction
    data class SetSelectedPalette(val paletteId: String) : SettingsAction
    data class SetCustomSeedColor(val color: Color?) : SettingsAction
    data class SetPaletteStyle(val style: PaletteStyle) : SettingsAction
    
    data class ToggleNotifications(val enabled: Boolean) : SettingsAction
    data class SetWatchingNotificationsEnabled(val enabled: Boolean) : SettingsAction
    data class SetPlanningNotificationsEnabled(val enabled: Boolean) : SettingsAction
    data class SetUpcomingNotificationsEnabled(val enabled: Boolean) : SettingsAction
    data class SetThreadCommentReplyEnabled(val enabled: Boolean) : SettingsAction
    data class SetThreadSubscribedEnabled(val enabled: Boolean) : SettingsAction
    data class SetThreadCommentMentionEnabled(val enabled: Boolean) : SettingsAction
    data class SetThreadLikeEnabled(val enabled: Boolean) : SettingsAction
    data class SetThreadCommentLikeEnabled(val enabled: Boolean) : SettingsAction
    data class SetActivityReplyEnabled(val enabled: Boolean) : SettingsAction
    data class SetActivityMentionEnabled(val enabled: Boolean) : SettingsAction
    data class SetActivityLikeEnabled(val enabled: Boolean) : SettingsAction
    data class SetActivityMessageEnabled(val enabled: Boolean) : SettingsAction
    
    data class SetAutoUpdateEnabled(val enabled: Boolean) : SettingsAction
    data class SetPrereleaseAllowed(val allowed: Boolean) : SettingsAction

    // Update operations (delegated to UpdateManager via ViewModel)
    data object CheckForUpdate : SettingsAction
    data class StartDownload(val release: Release) : SettingsAction
    data object CancelDownload : SettingsAction
    data object InstallUpdate : SettingsAction
    data object DismissUpdate : SettingsAction
    
    data object RefreshCacheSize : SettingsAction
    data object ClearCache : SettingsAction
    data object ResetCacheCleared : SettingsAction
    
    data object SendTestWatchingNotification : SettingsAction
    data object SendTestPlanningNotification : SettingsAction
    data object SendTestAdvanceNotification : SettingsAction
    data object SendTestImminentNotification : SettingsAction
    data object ClearAllNotifications : SettingsAction
    
    data object FetchLatestRelease : SettingsAction
}

data class SettingsUiState(
    val isLoaded: Boolean = false,

    // Look & Feel
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    val hapticEnabled: Boolean = true,
    val preferredStreamingService: StreamingService = StreamingService.CRUNCHYROLL,
    
    // Language
    val appLocale: AppLocale = AppLocale.SYSTEM,
    
    // Theme Palette
    val selectedPaletteId: String = "default",
    val customSeedColor: Color? = null,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    
    // Notifications
    val isNotificationsEnabled: Boolean = false,
    val watchingNotificationsEnabled: Boolean = true,
    val planningNotificationsEnabled: Boolean = false,
    val upcomingNotificationsEnabled: Boolean = true,
    val threadCommentReplyEnabled: Boolean = true,
    val threadSubscribedEnabled: Boolean = true,
    val threadCommentMentionEnabled: Boolean = true,
    val threadLikeEnabled: Boolean = true,
    val threadCommentLikeEnabled: Boolean = true,
    val activityReplyEnabled: Boolean = true,
    val activityMentionEnabled: Boolean = true,
    val activityLikeEnabled: Boolean = true,
    val activityMessageEnabled: Boolean = true,
    
    // Storage
    val cacheSize: String = "0 B",
    val isCacheCleared: Boolean = false,
    val isCacheLoading: Boolean = false,
    val isCacheClearing: Boolean = false,
    
    // Profile
    val userProfile: UserProfile? = null,
    
    // Updates
    val isAutoUpdateEnabled: Boolean = false,
    val isPrereleaseAllowed: Boolean = false
)
