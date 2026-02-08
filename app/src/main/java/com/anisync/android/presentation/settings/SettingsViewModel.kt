package com.anisync.android.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.data.AuthRepository
import com.anisync.android.data.NotificationPreferences
import com.anisync.android.data.StreamingService
import com.anisync.android.data.ThemeMode
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.GetProfileUseCase
import com.anisync.android.domain.UserProfile
import com.anisync.android.worker.NotificationDebugService
import com.anisync.android.worker.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings screens.
 * Manages app settings, notifications, storage, and account operations.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettings: AppSettings,
    private val notificationPreferences: NotificationPreferences,
    private val notificationScheduler: NotificationScheduler,
    private val notificationDebugService: NotificationDebugService,
    private val authRepository: AuthRepository,
    private val getProfileUseCase: GetProfileUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ==========================================================================
    // USER PROFILE
    // ==========================================================================

    /**
     * Observe user profile from local cache.
     */
    val userProfile: StateFlow<UserProfile?> = getProfileUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // ==========================================================================
    // LOOK & FEEL SETTINGS
    // ==========================================================================

    val themeMode: StateFlow<ThemeMode> = appSettings.themeMode
    val titleLanguage: StateFlow<TitleLanguage> = appSettings.titleLanguage
    val hapticEnabled: StateFlow<Boolean> = appSettings.hapticEnabled
    val preferredStreamingService: StateFlow<StreamingService> = appSettings.preferredStreamingService

    fun setThemeMode(mode: ThemeMode) {
        appSettings.setThemeMode(mode)
    }

    fun setTitleLanguage(language: TitleLanguage) {
        appSettings.setTitleLanguage(language)
    }

    fun setHapticEnabled(enabled: Boolean) {
        appSettings.setHapticEnabled(enabled)
    }

    fun setPreferredStreamingService(service: StreamingService) {
        appSettings.setPreferredStreamingService(service)
    }

    // ==========================================================================
    // NOTIFICATION SETTINGS
    // ==========================================================================

    val isNotificationsEnabled: StateFlow<Boolean> = appSettings.notificationsEnabled
    val watchingNotificationsEnabled: StateFlow<Boolean> = notificationPreferences.watchingEnabled
    val planningNotificationsEnabled: StateFlow<Boolean> = notificationPreferences.planningEnabled
    val upcomingNotificationsEnabled: StateFlow<Boolean> = notificationPreferences.upcomingEnabled

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            appSettings.setNotificationsEnabled(enabled)
            if (enabled) {
                notificationScheduler.schedule()
            } else {
                notificationScheduler.cancel()
            }
        }
    }

    fun setWatchingNotificationsEnabled(enabled: Boolean) {
        notificationPreferences.setWatchingEnabled(enabled)
    }

    fun setPlanningNotificationsEnabled(enabled: Boolean) {
        notificationPreferences.setPlanningEnabled(enabled)
    }

    fun setUpcomingNotificationsEnabled(enabled: Boolean) {
        notificationPreferences.setUpcomingEnabled(enabled)
    }

    // ==========================================================================
    // DEBUG NOTIFICATIONS (Debug builds only)
    // ==========================================================================

    fun sendTestWatchingNotification() {
        notificationDebugService.sendTestWatchingNotification()
    }

    fun sendTestPlanningNotification() {
        notificationDebugService.sendTestPlanningNotification()
    }

    fun sendTestAdvanceNotification() {
        notificationDebugService.sendTestAdvanceNotification()
    }

    fun sendTestImminentNotification() {
        notificationDebugService.sendTestImminentNotification()
    }

    fun clearAllNotifications() {
        notificationDebugService.clearAllNotifications()
    }

    // ==========================================================================
    // STORAGE SETTINGS
    // ==========================================================================

    private val _cacheSize = MutableStateFlow(calculateCacheSize())
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    private val _isCacheCleared = MutableStateFlow(false)
    val isCacheCleared: StateFlow<Boolean> = _isCacheCleared.asStateFlow()

    /**
     * Refreshes the cache size calculation.
     * Call this when returning to the main Settings screen.
     */
    fun refreshCacheSize() {
        _cacheSize.value = calculateCacheSize()
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                context.cacheDir.deleteRecursively()
                context.externalCacheDir?.deleteRecursively()
                _cacheSize.value = "0 B"
                _isCacheCleared.value = true
            } catch (e: Exception) {
                // Silently fail - cache might be in use
            }
        }
    }

    fun resetCacheCleared() {
        _isCacheCleared.value = false
    }

    private fun calculateCacheSize(): String {
        val internalCacheSize = context.cacheDir.walkTopDown().sumOf { it.length() }
        val externalCacheSize = context.externalCacheDir?.walkTopDown()?.sumOf { it.length() } ?: 0L
        val totalSize = internalCacheSize + externalCacheSize
        return formatFileSize(totalSize)
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    // ==========================================================================
    // ACCOUNT SETTINGS
    // ==========================================================================

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}
