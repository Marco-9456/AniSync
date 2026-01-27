package com.anisync.android.data

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.DrawableRes
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.anisync.android.R
import com.anisync.android.widget.UpNextWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
 * Preferred streaming service for widget icons.
 * Icons are loaded from AniList CDN URLs (same as external links).
 * Note: Icon URLs are from AniList's external links API and may change over time.
 */
enum class StreamingService(
    val displayName: String,
    val iconUrl: String?,
    @DrawableRes val fallbackDrawable: Int,
    val brandColor: String
) {
    NONE(
        displayName = "None",
        iconUrl = null,
        fallbackDrawable = android.R.drawable.ic_media_play,
        brandColor = "#FFFFFF"
    ),
    CRUNCHYROLL(
        displayName = "Crunchyroll",
        iconUrl = "https://s4.anilist.co/file/anilistcdn/link/icon/5-AWN2pVlluCOO.png",
        fallbackDrawable = R.drawable.ic_streaming_fallback,
        brandColor = "#F47521"
    ),
    NETFLIX(
        displayName = "Netflix",
        iconUrl = "https://s4.anilist.co/file/anilistcdn/link/icon/10-rVGPom8RCiwH.png",
        fallbackDrawable = R.drawable.ic_streaming_fallback,
        brandColor = "#E50914"
    ),
    AMAZON_PRIME(
        displayName = "Amazon Prime Video",
        iconUrl = "https://s4.anilist.co/file/anilistcdn/link/icon/21-bDoNIomehkOx.png",
        fallbackDrawable = R.drawable.ic_streaming_fallback,
        brandColor = "#00A8E1"
    ),
    BILIBILI(
        displayName = "Bilibili TV",
        iconUrl = "https://s4.anilist.co/file/anilistcdn/link/icon/119-NCwGvCjFADGQ.png",
        fallbackDrawable = R.drawable.ic_streaming_fallback,
        brandColor = "#00A1D6"
    )
}

/**
 * Centralized app settings manager using SharedPreferences.
 * Provides reactive StateFlows for all settings to enable UI updates.
 */
@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Coroutine scope for widget updates
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
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
    
    // Title language setting
    private val _titleLanguage = MutableStateFlow(
        TitleLanguage.entries.getOrElse(prefs.getInt(KEY_TITLE_LANGUAGE, TitleLanguage.ROMAJI.ordinal)) { TitleLanguage.ROMAJI }
    )
    val titleLanguage: StateFlow<TitleLanguage> = _titleLanguage.asStateFlow()
    
    // Preferred streaming service setting
    private val _preferredStreamingService = MutableStateFlow(
        StreamingService.entries.getOrElse(prefs.getInt(KEY_PREFERRED_STREAMING_SERVICE, StreamingService.NONE.ordinal)) { StreamingService.NONE }
    )
    val preferredStreamingService: StateFlow<StreamingService> = _preferredStreamingService.asStateFlow()

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
    
    /**
     * Set the preferred title language.
     */
    fun setTitleLanguage(language: TitleLanguage) {
        _titleLanguage.value = language
        prefs.edit().putInt(KEY_TITLE_LANGUAGE, language.ordinal).apply()
    }
    
    /**
     * Set the preferred streaming service for widget icons.
     * Automatically refreshes the Up Next widget to reflect the change.
     */
    fun setPreferredStreamingService(service: StreamingService) {
        _preferredStreamingService.value = service
        prefs.edit().putInt(KEY_PREFERRED_STREAMING_SERVICE, service.ordinal).apply()
        
        // Refresh Up Next widget to show the new icon
        scope.launch {
            refreshUpNextWidget()
        }
    }
    
    /**
     * Refresh all Up Next widget instances.
     * Uses updateAppWidgetState to trigger a state change, ensuring the widget re-renders.
     */
    private suspend fun refreshUpNextWidget() {
        try {
            val manager = GlanceAppWidgetManager(context)
            val widgetIds = manager.getGlanceIds(UpNextWidget::class.java)
            val timestampKey = stringPreferencesKey("last_refresh_timestamp")
            
            widgetIds.forEach { glanceId ->
                // Update widget state with a timestamp to force a refresh
                // This is needed because Glance caches state and won't re-render
                // if it thinks nothing has changed
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[timestampKey] = System.currentTimeMillis().toString()
                    }
                }
                UpNextWidget().update(context, glanceId)
            }
        } catch (e: Exception) {
            // Silently fail - widget might not be placed
        }
    }
    
    companion object {
        private const val PREFS_NAME = "anisync_settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_TITLE_LANGUAGE = "title_language"
        private const val KEY_PREFERRED_STREAMING_SERVICE = "preferred_streaming_service"
    }
}

/**
 * Preferred title language options.
 */
enum class TitleLanguage {
    ROMAJI,
    ENGLISH,
    NATIVE
}
