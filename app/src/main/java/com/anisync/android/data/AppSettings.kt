package com.anisync.android.data

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.anisync.android.R
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.widget.UpNextWidget
import com.materialkolor.PaletteStyle
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
 * Cover-image quality picked from AniList's [CoverImage] sizes. Applied app-wide
 * via a Coil interceptor that rewrites AniList CDN cover URLs to the chosen size,
 * so every cover (cards, lists, detail screens) follows this preference.
 *
 * Order matches the picker (largest first). Persisted by name so reordering
 * doesn't shift saved values.
 */
enum class CoverQuality {
    EXTRA_LARGE,
    LARGE,
    MEDIUM
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

    // Score Format setting
    private val _userScoreFormat = MutableStateFlow(
        ScoreFormat.entries.getOrElse(prefs.getInt(KEY_USER_SCORE_FORMAT, ScoreFormat.POINT_100.ordinal)) { ScoreFormat.POINT_100 }
    )
    val userScoreFormat: StateFlow<ScoreFormat> = _userScoreFormat.asStateFlow()
    
    // Title language setting
    private val _titleLanguage = MutableStateFlow(
        TitleLanguage.entries.getOrElse(prefs.getInt(KEY_TITLE_LANGUAGE, TitleLanguage.ROMAJI.ordinal)) { TitleLanguage.ROMAJI }
    )
    val titleLanguage: StateFlow<TitleLanguage> = _titleLanguage.asStateFlow()

    // Cover image quality setting
    private val _coverQuality = MutableStateFlow(readCoverQuality())
    val coverQuality: StateFlow<CoverQuality> = _coverQuality.asStateFlow()

    /**
     * Reads the cover quality preference, migrating older builds that stored it as an
     * ordinal Int. The dev branch shipped both encodings during development; without
     * this fallback, an upgrade from the Int-encoded build crashes on launch with a
     * ClassCastException inside [SharedPreferences.getString].
     */
    private fun readCoverQuality(): CoverQuality {
        val nameValue = runCatching { prefs.getString(KEY_COVER_QUALITY, null) }.getOrNull()
        if (nameValue != null) {
            return runCatching { CoverQuality.valueOf(nameValue) }.getOrDefault(CoverQuality.LARGE)
        }
        // Legacy ordinal-encoded value, or never-set. The original ordering was
        // [MEDIUM, LARGE, EXTRA_LARGE]; the new [CoverQuality] reorders these.
        // Map by the original ordinal so saved values keep their meaning.
        val legacyOrdinal = runCatching { prefs.getInt(KEY_COVER_QUALITY, 1) }.getOrDefault(1)
        val migrated = when (legacyOrdinal) {
            0 -> CoverQuality.MEDIUM
            2 -> CoverQuality.EXTRA_LARGE
            else -> CoverQuality.LARGE
        }
        prefs.edit().putString(KEY_COVER_QUALITY, migrated.name).apply()
        return migrated
    }
    
// Preferred streaming service setting
    private val _preferredStreamingService = MutableStateFlow(
        StreamingService.entries.getOrElse(prefs.getInt(KEY_PREFERRED_STREAMING_SERVICE, StreamingService.NONE.ordinal)) { StreamingService.NONE }
    )
    val preferredStreamingService: StateFlow<StreamingService> = _preferredStreamingService.asStateFlow()
    
    // ==========================================================================
    // THEME PALETTE SETTINGS
    // ==========================================================================
    
    // Selected palette ID (e.g., "dynamic", "pink", "blue", or "custom")
    private val _selectedPaletteId = MutableStateFlow(
        prefs.getString(KEY_SELECTED_PALETTE, "dynamic") ?: "dynamic"
    )
    val selectedPaletteId: StateFlow<String> = _selectedPaletteId.asStateFlow()
    
    // Custom seed color (when user picks their own color)
    // Use prefs.contains() instead of sentinel value to avoid collision with Color(0x00000000)
    private val _customSeedColor = MutableStateFlow<Color?>(
        if (prefs.contains(KEY_CUSTOM_SEED_COLOR)) Color(prefs.getInt(KEY_CUSTOM_SEED_COLOR, 0)) else null
    )
    val customSeedColor: StateFlow<Color?> = _customSeedColor.asStateFlow()
    
    // Palette style for color generation
    private val _paletteStyle = MutableStateFlow(
        PaletteStyle.entries.getOrElse(prefs.getInt(KEY_PALETTE_STYLE, 0)) { PaletteStyle.TonalSpot }
    )
    val paletteStyle: StateFlow<PaletteStyle> = _paletteStyle.asStateFlow()

    // App locale setting for in-app language switching
    private val _appLocale = MutableStateFlow(
        AppLocale.entries.getOrElse(prefs.getInt(KEY_APP_LOCALE, AppLocale.SYSTEM.ordinal)) { AppLocale.SYSTEM }
    )
    val appLocale: StateFlow<AppLocale> = _appLocale.asStateFlow()

    // Updates settings
    private val _autoUpdateEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_UPDATE_ENABLED, false))
    val autoUpdateEnabled: StateFlow<Boolean> = _autoUpdateEnabled.asStateFlow()

    private val _allowPrerelease = MutableStateFlow(prefs.getBoolean(KEY_ALLOW_PRERELEASE, false))
    val allowPrerelease: StateFlow<Boolean> = _allowPrerelease.asStateFlow()

    // Library Custom Lists settings separated by media type
    private val _animeListOrder = MutableStateFlow(
        prefs.getString(KEY_ANIME_LIST_ORDER, "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    )
    val animeListOrder: StateFlow<List<String>> = _animeListOrder.asStateFlow()

    private val _mangaListOrder = MutableStateFlow(
        prefs.getString(KEY_MANGA_LIST_ORDER, "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    )
    val mangaListOrder: StateFlow<List<String>> = _mangaListOrder.asStateFlow()

    private val _hiddenAnimeLists = MutableStateFlow(
        prefs.getStringSet(KEY_HIDDEN_ANIME_LISTS, emptySet()) ?: emptySet()
    )
    val hiddenAnimeLists: StateFlow<Set<String>> = _hiddenAnimeLists.asStateFlow()

    private val _hiddenMangaLists = MutableStateFlow(
        prefs.getStringSet(KEY_HIDDEN_MANGA_LISTS, emptySet()) ?: emptySet()
    )
    val hiddenMangaLists: StateFlow<Set<String>> = _hiddenMangaLists.asStateFlow()

    private val _showPrivateEntries = MutableStateFlow(
        prefs.getBoolean(KEY_SHOW_PRIVATE_ENTRIES, true)
    )
    val showPrivateEntries: StateFlow<Boolean> = _showPrivateEntries.asStateFlow()

    // Last selected library tab (per media type)
    private val _lastSelectedAnimeTab = MutableStateFlow(
        prefs.getString(KEY_LAST_SELECTED_ANIME_TAB, null)
    )
    val lastSelectedAnimeTab: StateFlow<String?> = _lastSelectedAnimeTab.asStateFlow()

    private val _lastSelectedMangaTab = MutableStateFlow(
        prefs.getString(KEY_LAST_SELECTED_MANGA_TAB, null)
    )
    val lastSelectedMangaTab: StateFlow<String?> = _lastSelectedMangaTab.asStateFlow()

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
     * Set the preferred score format.
     */
    fun setUserScoreFormat(format: ScoreFormat) {
        _userScoreFormat.value = format
        prefs.edit().putInt(KEY_USER_SCORE_FORMAT, format.ordinal).apply()
    }
    
    /**
     * Set the preferred title language.
     */
    fun setTitleLanguage(language: TitleLanguage) {
        _titleLanguage.value = language
        prefs.edit().putInt(KEY_TITLE_LANGUAGE, language.ordinal).apply()
    }

    /**
     * Set the preferred media cover image quality.
     */
    fun setCoverQuality(quality: CoverQuality) {
        _coverQuality.value = quality
        prefs.edit().putString(KEY_COVER_QUALITY, quality.name).apply()
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
    
    // ==========================================================================
    // THEME PALETTE SETTERS
    // ==========================================================================
    
    /**
     * Set the selected theme palette by ID.
     */
    fun setSelectedPalette(paletteId: String) {
        _selectedPaletteId.value = paletteId
        prefs.edit().putString(KEY_SELECTED_PALETTE, paletteId).apply()
    }
    
    /**
     * Set a custom seed color for theme generation.
     * Pass null to clear the custom color.
     */
    fun setCustomSeedColor(color: Color?) {
        _customSeedColor.value = color
        prefs.edit().apply {
            if (color != null) {
                putInt(KEY_CUSTOM_SEED_COLOR, color.toArgb())
            } else {
                remove(KEY_CUSTOM_SEED_COLOR)
            }
        }.apply()
        // Auto-reset palette if clearing custom color while "custom" is selected,
        // to prevent an orphaned state where no palette circle is highlighted
        if (color == null && _selectedPaletteId.value == "custom") {
            setSelectedPalette("dynamic")
        }
    }
    
    /**
     * Set the palette style for MaterialKolor color generation.
     */
    fun setPaletteStyle(style: PaletteStyle) {
        _paletteStyle.value = style
        prefs.edit().putInt(KEY_PALETTE_STYLE, style.ordinal).apply()
    }

    /**
     * Set the app locale for in-app language switching.
     * The caller is responsible for applying the locale via AppCompatDelegate.
     */
    fun setAppLocale(locale: AppLocale) {
        _appLocale.value = locale
        prefs.edit().putInt(KEY_APP_LOCALE, locale.ordinal).apply()
    }
    
    fun setAutoUpdateEnabled(enabled: Boolean) {
        _autoUpdateEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_UPDATE_ENABLED, enabled).apply()
    }

    fun setAllowPrerelease(allowed: Boolean) {
        _allowPrerelease.value = allowed
        prefs.edit().putBoolean(KEY_ALLOW_PRERELEASE, allowed).apply()
    }

    fun setAnimeListOrder(order: List<String>) {
        _animeListOrder.value = order
        prefs.edit().putString(KEY_ANIME_LIST_ORDER, order.joinToString(",")).apply()
    }

    fun setMangaListOrder(order: List<String>) {
        _mangaListOrder.value = order
        prefs.edit().putString(KEY_MANGA_LIST_ORDER, order.joinToString(",")).apply()
    }

    fun setHiddenAnimeLists(hidden: Set<String>) {
        _hiddenAnimeLists.value = hidden
        prefs.edit().putStringSet(KEY_HIDDEN_ANIME_LISTS, hidden).apply()
    }

    fun setHiddenMangaLists(hidden: Set<String>) {
        _hiddenMangaLists.value = hidden
        prefs.edit().putStringSet(KEY_HIDDEN_MANGA_LISTS, hidden).apply()
    }
    
    fun setShowPrivateEntries(show: Boolean) {
        _showPrivateEntries.value = show
        prefs.edit().putBoolean(KEY_SHOW_PRIVATE_ENTRIES, show).apply()
    }

    /**
     * Persist the last selected library tab for anime.
     */
    fun setLastSelectedAnimeTab(tabId: String?) {
        _lastSelectedAnimeTab.value = tabId
        prefs.edit().apply {
            if (tabId != null) putString(KEY_LAST_SELECTED_ANIME_TAB, tabId)
            else remove(KEY_LAST_SELECTED_ANIME_TAB)
        }.apply()
    }

    /**
     * Persist the last selected library tab for manga.
     */
    fun setLastSelectedMangaTab(tabId: String?) {
        _lastSelectedMangaTab.value = tabId
        prefs.edit().apply {
            if (tabId != null) putString(KEY_LAST_SELECTED_MANGA_TAB, tabId)
            else remove(KEY_LAST_SELECTED_MANGA_TAB)
        }.apply()
    }
    
    /**
     * Get the preferred streaming service directly from SharedPreferences.
     * Use this for widgets to ensure the latest value is always read.
     */
    fun getPreferredStreamingServiceDirect(): StreamingService {
        return StreamingService.entries.getOrElse(
            prefs.getInt(KEY_PREFERRED_STREAMING_SERVICE, StreamingService.NONE.ordinal)
        ) { StreamingService.NONE }
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
        private const val KEY_COVER_QUALITY = "cover_quality"
        private const val KEY_PREFERRED_STREAMING_SERVICE = "preferred_streaming_service"
        private const val KEY_SELECTED_PALETTE = "selected_palette"
        private const val KEY_CUSTOM_SEED_COLOR = "custom_seed_color"
        private const val KEY_PALETTE_STYLE = "palette_style"
        private const val KEY_APP_LOCALE = "app_locale"
        private const val KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled"
        private const val KEY_ALLOW_PRERELEASE = "allow_prerelease"
        private const val KEY_ANIME_LIST_ORDER = "anime_list_order"
        private const val KEY_MANGA_LIST_ORDER = "manga_list_order"
        private const val KEY_HIDDEN_ANIME_LISTS = "hidden_anime_lists"
        private const val KEY_HIDDEN_MANGA_LISTS = "hidden_manga_lists"
        private const val KEY_USER_SCORE_FORMAT = "user_score_format"
        private const val KEY_SHOW_PRIVATE_ENTRIES = "show_private_entries"
        private const val KEY_LAST_SELECTED_ANIME_TAB = "last_selected_anime_tab"
        private const val KEY_LAST_SELECTED_MANGA_TAB = "last_selected_manga_tab"
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

/**
 * App UI locale options for in-app language switching.
 * Each entry maps to a BCP 47 language tag.
 * [displayName] is shown in native script so users can always identify their language.
 */
enum class AppLocale(val tag: String, val displayName: String) {
    SYSTEM("", "System Default"),
    ENGLISH("en", "English"),
    GERMAN("de", "Deutsch"),
    ARABIC("ar", "العربية")
}
