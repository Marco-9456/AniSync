package com.anisync.android.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Theme mode options for the app.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Centralized app settings manager using SharedPreferences.
 * Provides reactive StateFlows for all settings to enable UI updates.
 */
@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Theme setting
    private val _themeMode = MutableStateFlow(
        ThemeMode.entries.getOrElse(prefs.getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.ordinal)) { ThemeMode.SYSTEM }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    
    // Haptic feedback setting
    private val _hapticEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC_ENABLED, true))
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()
    
    // Notifications setting
    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()
    
    /**
     * Set the app theme mode.
     */
    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putInt(KEY_THEME_MODE, mode.ordinal).apply()
    }
    
    /**
     * Enable or disable haptic feedback.
     */
    fun setHapticEnabled(enabled: Boolean) {
        _hapticEnabled.value = enabled
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
    }
    
    /**
     * Enable or disable notifications.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }
    
    companion object {
        private const val PREFS_NAME = "anisync_settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    }
}
