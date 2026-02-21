package com.anisync.android.presentation.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppLocale
import com.anisync.android.data.AppSettings
import com.anisync.android.data.AuthRepository
import com.anisync.android.data.NotificationPreferences
import com.anisync.android.domain.GetProfileUseCase
import com.anisync.android.domain.UserProfile
import com.anisync.android.worker.NotificationDebugService
import com.anisync.android.worker.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettings: AppSettings,
    private val notificationPreferences: NotificationPreferences,
    private val notificationScheduler: NotificationScheduler,
    private val notificationDebugService: NotificationDebugService,
    private val authRepository: AuthRepository,
    getProfileUseCase: GetProfileUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _cacheSize = MutableStateFlow("0 B")
    private val _isCacheCleared = MutableStateFlow(false)
    private val _isCacheLoading = MutableStateFlow(false)
    private val _isCacheClearing = MutableStateFlow(false)

    init {
        refreshCacheSize()
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            appSettings.themeMode,
            appSettings.titleLanguage,
            appSettings.hapticEnabled,
            appSettings.preferredStreamingService,
            appSettings.appLocale
        ) { theme, title, haptic, streaming, locale ->
            SettingsUiState(
                themeMode = theme,
                titleLanguage = title,
                hapticEnabled = haptic,
                preferredStreamingService = streaming,
                appLocale = locale
            )
        },
        combine(
            appSettings.selectedPaletteId,
            appSettings.customSeedColor,
            appSettings.paletteStyle
        ) { paletteId, customColor, style ->
            Triple(paletteId, customColor, style)
        },
        combine(
            appSettings.notificationsEnabled,
            notificationPreferences.watchingEnabled,
            notificationPreferences.planningEnabled,
            notificationPreferences.upcomingEnabled
        ) { enabled, watching, planning, upcoming ->
            listOf(enabled, watching, planning, upcoming)
        },
        combine(
            _cacheSize,
            _isCacheCleared,
            _isCacheLoading,
            _isCacheClearing,
            getProfileUseCase()
        ) { size, cleared, loading, clearing, profile ->
            listOf(size, cleared, loading, clearing, profile)
        }
    ) { lookAndFeel, themePalette, notifications, storageAndProfile ->
        val (paletteId, customColor, style) = themePalette
        val (notifEnabled, watching, planning, upcoming) = notifications
        val (cacheSize, isCleared, isLoading, isClearing, profile) = storageAndProfile
        
        lookAndFeel.copy(
            selectedPaletteId = paletteId,
            customSeedColor = customColor,
            paletteStyle = style,
            isNotificationsEnabled = notifEnabled as Boolean,
            watchingNotificationsEnabled = watching as Boolean,
            planningNotificationsEnabled = planning as Boolean,
            upcomingNotificationsEnabled = upcoming as Boolean,
            cacheSize = cacheSize as String,
            isCacheCleared = isCleared as Boolean,
            isCacheLoading = isLoading as Boolean,
            isCacheClearing = isClearing as Boolean,
            userProfile = profile as UserProfile?,
            isLoaded = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetThemeMode -> appSettings.setThemeMode(action.mode)
            is SettingsAction.SetTitleLanguage -> appSettings.setTitleLanguage(action.language)
            is SettingsAction.SetHapticEnabled -> appSettings.setHapticEnabled(action.enabled)
            is SettingsAction.SetPreferredStreamingService -> appSettings.setPreferredStreamingService(action.service)
            is SettingsAction.SetAppLocale -> setAppLocale(action.locale)
            is SettingsAction.SetSelectedPalette -> appSettings.setSelectedPalette(action.paletteId)
            is SettingsAction.SetCustomSeedColor -> appSettings.setCustomSeedColor(action.color)
            is SettingsAction.SetPaletteStyle -> appSettings.setPaletteStyle(action.style)
            
            is SettingsAction.ToggleNotifications -> toggleNotifications(action.enabled)
            is SettingsAction.SetWatchingNotificationsEnabled -> notificationPreferences.setWatchingEnabled(action.enabled)
            is SettingsAction.SetPlanningNotificationsEnabled -> notificationPreferences.setPlanningEnabled(action.enabled)
            is SettingsAction.SetUpcomingNotificationsEnabled -> notificationPreferences.setUpcomingEnabled(action.enabled)
            
            SettingsAction.RefreshCacheSize -> refreshCacheSize()
            SettingsAction.ClearCache -> clearCache()
            SettingsAction.ResetCacheCleared -> _isCacheCleared.value = false
            
            SettingsAction.SendTestWatchingNotification -> notificationDebugService.sendTestWatchingNotification()
            SettingsAction.SendTestPlanningNotification -> notificationDebugService.sendTestPlanningNotification()
            SettingsAction.SendTestAdvanceNotification -> notificationDebugService.sendTestAdvanceNotification()
            SettingsAction.SendTestImminentNotification -> notificationDebugService.sendTestImminentNotification()
            SettingsAction.ClearAllNotifications -> notificationDebugService.clearAllNotifications()
        }
    }

    private fun setAppLocale(locale: AppLocale) {
        appSettings.setAppLocale(locale)
        val localeList = if (locale == AppLocale.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(locale.tag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    private fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            appSettings.setNotificationsEnabled(enabled)
            if (enabled) {
                notificationScheduler.schedule()
            } else {
                notificationScheduler.cancel()
            }
        }
    }

    private fun refreshCacheSize() {
        viewModelScope.launch {
            _isCacheLoading.value = true
            _cacheSize.value = calculateCacheSizeAsync()
            _isCacheLoading.value = false
        }
    }

    private fun clearCache() {
        viewModelScope.launch {
            _isCacheClearing.value = true
            try {
                context.cacheDir.deleteRecursively()
                context.externalCacheDir?.deleteRecursively()
                _cacheSize.value = "0 B"
                _isCacheCleared.value = true
            } catch (e: Exception) {
                // Silently fail - cache might be in use
            } finally {
                _isCacheClearing.value = false
            }
        }
    }

    private suspend fun calculateCacheSizeAsync(): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val internalCacheSize = context.cacheDir.walkTopDown().sumOf { it.length() }
            val externalCacheSize = context.externalCacheDir?.walkTopDown()?.sumOf { it.length() } ?: 0L
            val totalSize = internalCacheSize + externalCacheSize
            formatFileSize(totalSize)
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}
