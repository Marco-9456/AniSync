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
import com.anisync.android.domain.FeedFilter
import com.anisync.android.domain.FeedScope
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.domain.media.MediaHost
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.FontAxisOverrides
import com.anisync.android.ui.theme.TypeCategory
import com.anisync.android.ui.theme.TypographyOverrides
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
import kotlinx.serialization.json.Json
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
 * Layout mode used by the discover search results overlay. Persisted so the
 * user's preferred density (rows vs grid of posters) survives app restarts.
 */
enum class DiscoverViewMode {
    LIST,
    GRID
}

/**
 * Visual style of the bottom navigation bar.
 *
 *  - [ANCHORED]: bar pinned to the bottom edge with rounded top corners.
 *  - [FLOATING]: pill-shaped bar detached from the edges with margins.
 */
enum class NavBarStyle {
    ANCHORED,
    FLOATING
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

    // Avatar shape setting
    private val _avatarShape = MutableStateFlow(readAvatarShape())
    val avatarShape: StateFlow<AvatarShape> = _avatarShape.asStateFlow()

    private fun readAvatarShape(): AvatarShape {
        val name = runCatching { prefs.getString(KEY_AVATAR_SHAPE, null) }.getOrNull()
        // Legacy: the 8-leaf clover used to be stored as "CLOVER"; map it forward.
        if (name == "CLOVER") return AvatarShape.CLOVER_8_LEAF
        return runCatching { AvatarShape.valueOf(name ?: AvatarShape.CLOVER_8_LEAF.name) }
            .getOrDefault(AvatarShape.CLOVER_8_LEAF)
    }

    // Avatar background enabled setting
    private val _avatarBackgroundEnabled = MutableStateFlow(prefs.getBoolean(KEY_AVATAR_BACKGROUND_ENABLED, true))
    val avatarBackgroundEnabled: StateFlow<Boolean> = _avatarBackgroundEnabled.asStateFlow()
    
    // Disable avatar shape for own profile setting
    private val _disableAvatarShapeProfile = MutableStateFlow(prefs.getBoolean(KEY_DISABLE_AVATAR_SHAPE_PROFILE, false))
    val disableAvatarShapeProfile: StateFlow<Boolean> = _disableAvatarShapeProfile.asStateFlow()
    
    // Haptic feedback setting
    private val _hapticEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC_ENABLED, true))
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    // Navigation bar style (anchored rounded top vs floating pill)
    private val _navBarStyle = MutableStateFlow(readNavBarStyle())
    val navBarStyle: StateFlow<NavBarStyle> = _navBarStyle.asStateFlow()

    // Whether nav bar labels are shown
    private val _navBarShowLabels = MutableStateFlow(prefs.getBoolean(KEY_NAV_BAR_LABELS, true))
    val navBarShowLabels: StateFlow<Boolean> = _navBarShowLabels.asStateFlow()

    // Nav bar corner radius (dp). Range constrained in UI; persisted as float.
    private val _navBarCornerRadius = MutableStateFlow(
        prefs.getFloat(KEY_NAV_BAR_CORNER_RADIUS, DEFAULT_NAV_BAR_CORNER_RADIUS)
    )
    val navBarCornerRadius: StateFlow<Float> = _navBarCornerRadius.asStateFlow()

    private fun readNavBarStyle(): NavBarStyle {
        val name = runCatching { prefs.getString(KEY_NAV_BAR_STYLE, null) }.getOrNull()
        return runCatching { NavBarStyle.valueOf(name ?: NavBarStyle.ANCHORED.name) }
            .getOrDefault(NavBarStyle.ANCHORED)
    }
    
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

    // Staff & character name language. Mirrored from the AniList account option
    // (UserOptions.staffNameLanguage); applied client-side when rendering staff/character names.
    private val _staffNameLanguage = MutableStateFlow(
        StaffNameLanguage.entries.getOrElse(
            prefs.getInt(KEY_STAFF_NAME_LANGUAGE, StaffNameLanguage.ROMAJI_WESTERN.ordinal)
        ) { StaffNameLanguage.ROMAJI_WESTERN }
    )
    val staffNameLanguage: StateFlow<StaffNameLanguage> = _staffNameLanguage.asStateFlow()

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

    // Effective "show adult content" flag, followed by search + the activity feed. Mirrored from the
    // local-first options state (UserOptions.displayAdultContent); see UserOptionsRepositoryImpl.
    private val _showAdultContent = MutableStateFlow(
        prefs.getBoolean(KEY_SHOW_ADULT_CONTENT, false)
    )
    val showAdultContent: StateFlow<Boolean> = _showAdultContent.asStateFlow()

    private val _discoverSearchViewMode = MutableStateFlow(
        DiscoverViewMode.entries.getOrElse(
            prefs.getInt(KEY_DISCOVER_SEARCH_VIEW_MODE, DiscoverViewMode.LIST.ordinal)
        ) { DiscoverViewMode.LIST }
    )
    val discoverSearchViewMode: StateFlow<DiscoverViewMode> = _discoverSearchViewMode.asStateFlow()

    // Last selected library tab (per media type)
    private val _lastSelectedAnimeTab = MutableStateFlow(
        prefs.getString(KEY_LAST_SELECTED_ANIME_TAB, null)
    )
    val lastSelectedAnimeTab: StateFlow<String?> = _lastSelectedAnimeTab.asStateFlow()

    private val _lastSelectedMangaTab = MutableStateFlow(
        prefs.getString(KEY_LAST_SELECTED_MANGA_TAB, null)
    )
    val lastSelectedMangaTab: StateFlow<String?> = _lastSelectedMangaTab.asStateFlow()

    // Last selected activity-feed scope (Global vs Following). Persisted so the feed
    // opens on the tab the user actually reads instead of always resetting to Global.
    private val _lastFeedScope = MutableStateFlow(readFeedScope())
    val lastFeedScope: StateFlow<FeedScope> = _lastFeedScope.asStateFlow()

    private fun readFeedScope(): FeedScope {
        val name = runCatching { prefs.getString(KEY_FEED_SCOPE, null) }.getOrNull()
        return runCatching { FeedScope.valueOf(name ?: FeedScope.GLOBAL.name) }
            .getOrDefault(FeedScope.GLOBAL)
    }

    // Library view density: grid of posters (true) vs single-column list (false).
    // Persisted so the chosen layout survives app restarts instead of resetting to grid.
    private val _libraryGridView = MutableStateFlow(prefs.getBoolean(KEY_LIBRARY_GRID_VIEW, true))
    val libraryGridView: StateFlow<Boolean> = _libraryGridView.asStateFlow()

    // Last selected feed content filter (All / Status / List).
    private val _feedFilter = MutableStateFlow(readFeedFilter())
    val feedFilter: StateFlow<FeedFilter> = _feedFilter.asStateFlow()

    private fun readFeedFilter(): FeedFilter {
        val name = runCatching { prefs.getString(KEY_FEED_FILTER, null) }.getOrNull()
        return runCatching { FeedFilter.valueOf(name ?: FeedFilter.ALL.name) }
            .getOrDefault(FeedFilter.ALL)
    }

    // Last selected media type (Anime vs Manga), stored per surface so the Library
    // and Discover screens each keep their own preference. Encoded as a boolean
    // (true = Manga) to stay independent of the generated MediaType enum's encoding.
    private val _libraryMediaType = MutableStateFlow(
        if (prefs.getBoolean(KEY_LIBRARY_MEDIA_TYPE_MANGA, false)) MediaType.MANGA else MediaType.ANIME
    )
    val libraryMediaType: StateFlow<MediaType> = _libraryMediaType.asStateFlow()

    private val _discoverMediaType = MutableStateFlow(
        if (prefs.getBoolean(KEY_DISCOVER_MEDIA_TYPE_MANGA, false)) MediaType.MANGA else MediaType.ANIME
    )
    val discoverMediaType: StateFlow<MediaType> = _discoverMediaType.asStateFlow()

    // Last selected forum feed tab (stored by enum name) and category filter
    // (stored as the AniList category id, or absent when browsing all categories).
    private val _forumFeed = MutableStateFlow(prefs.getString(KEY_FORUM_FEED, null))
    val forumFeed: StateFlow<String?> = _forumFeed.asStateFlow()

    private val _forumCategoryId = MutableStateFlow(
        prefs.getInt(KEY_FORUM_CATEGORY_ID, -1).takeIf { it >= 0 }
    )
    val forumCategoryId: StateFlow<Int?> = _forumCategoryId.asStateFlow()

    // Last visited main bottom-nav tab key ("library" / "discover" / "feed" / "forum").
    // Restored as the nav-graph start destination on cold launch so the app reopens
    // on the screen the user left. Profile and Settings are intentionally never stored.
    private val _lastMainTab = MutableStateFlow(prefs.getString(KEY_LAST_MAIN_TAB, null))
    val lastMainTab: StateFlow<String?> = _lastMainTab.asStateFlow()

    // ==========================================================================
    // MEDIA UPLOAD SETTINGS — third-party host config for in-composer attach
    // ==========================================================================

    private val _mediaHost = MutableStateFlow(readMediaHost())
    val mediaHost: StateFlow<MediaHost> = _mediaHost.asStateFlow()

    private val _litterboxDuration = MutableStateFlow(
        prefs.getString(KEY_LITTERBOX_DURATION, "1h").orEmpty().ifBlank { "1h" }
    )
    val litterboxDuration: StateFlow<String> = _litterboxDuration.asStateFlow()

    private val _customHostUrl = MutableStateFlow(prefs.getString(KEY_CUSTOM_HOST_URL, "").orEmpty())
    val customHostUrl: StateFlow<String> = _customHostUrl.asStateFlow()

    private val _customHostFileField = MutableStateFlow(
        prefs.getString(KEY_CUSTOM_HOST_FIELD, "fileToUpload").orEmpty()
    )
    val customHostFileField: StateFlow<String> = _customHostFileField.asStateFlow()

    private val _customHostAuthHeader = MutableStateFlow(
        prefs.getString(KEY_CUSTOM_HOST_AUTH, "").orEmpty()
    )
    val customHostAuthHeader: StateFlow<String> = _customHostAuthHeader.asStateFlow()

    private val _customHostResponseJsonPath = MutableStateFlow(
        prefs.getString(KEY_CUSTOM_HOST_JSON_PATH, "").orEmpty()
    )
    val customHostResponseJsonPath: StateFlow<String> = _customHostResponseJsonPath.asStateFlow()

    // Optional Catbox account userhash. When set, uploads are bound to the user's
    // Catbox account so they can view/manage them at catbox.moe; blank = anonymous.
    private val _catboxUserHash = MutableStateFlow(prefs.getString(KEY_CATBOX_USERHASH, "").orEmpty())
    val catboxUserHash: StateFlow<String> = _catboxUserHash.asStateFlow()

    private fun readMediaHost(): MediaHost {
        val name = runCatching { prefs.getString(KEY_MEDIA_HOST, null) }.getOrNull()
        return runCatching { MediaHost.valueOf(name ?: MediaHost.CATBOX.name) }
            .getOrDefault(MediaHost.CATBOX)
    }

    // ==========================================================================
    // FONT PLAYGROUND — live per-category variable-font axis overrides (developer screen)
    // ==========================================================================

    // The whole TypographyOverrides object is persisted as a single JSON string. With five
    // categories x five axes that is far less boilerplate than 25 typed preference keys, and
    // the structure can evolve without a migration as long as unknown keys are ignored.
    private val typographyJson = Json { ignoreUnknownKeys = true }
    private val _typographyOverrides = MutableStateFlow(readTypographyOverrides())
    val typographyOverrides: StateFlow<TypographyOverrides> = _typographyOverrides.asStateFlow()

    private fun readTypographyOverrides(): TypographyOverrides {
        val json = prefs.getString(KEY_TYPOGRAPHY_OVERRIDES, null) ?: return TypographyOverrides.None
        return runCatching { typographyJson.decodeFromString<TypographyOverrides>(json) }
            .getOrDefault(TypographyOverrides.None)
    }

    private fun persistTypographyOverrides(value: TypographyOverrides) {
        _typographyOverrides.value = value
        prefs.edit()
            .putString(KEY_TYPOGRAPHY_OVERRIDES, typographyJson.encodeToString(value))
            .apply()
    }

    /** Update one M3 role [category]'s axis overrides (used by the per-category sliders). */
    fun updateTypographyCategory(
        category: TypeCategory,
        transform: (FontAxisOverrides) -> FontAxisOverrides,
    ) {
        val current = _typographyOverrides.value
        persistTypographyOverrides(
            current.withCategory(category, transform(current.forCategory(category))),
        )
    }

    /** "All" shortcut — apply the same axis [transform] to every category at once. */
    fun updateTypographyAll(transform: (FontAxisOverrides) -> FontAxisOverrides) {
        val current = _typographyOverrides.value
        persistTypographyOverrides(
            TypographyOverrides(
                display = transform(current.display),
                headline = transform(current.headline),
                title = transform(current.title),
                body = transform(current.body),
                label = transform(current.label),
            ),
        )
    }

    /** Clears the playground — every category back to its per-role preset typography. */
    fun resetTypography() {
        _typographyOverrides.value = TypographyOverrides.None
        prefs.edit().remove(KEY_TYPOGRAPHY_OVERRIDES).apply()
    }

    // ==========================================================================
    // DEVELOPER TOOLS — unlock flag (lets release builds reach the dev screens)
    // ==========================================================================

    private val _devToolsUnlocked = MutableStateFlow(
        prefs.getBoolean(KEY_DEV_TOOLS_UNLOCKED, false)
    )
    val devToolsUnlocked: StateFlow<Boolean> = _devToolsUnlocked.asStateFlow()

    /** Permanently reveals the Developer Tools entry — triggered by the hidden tap gesture. */
    fun unlockDevTools() {
        _devToolsUnlocked.value = true
        prefs.edit().putBoolean(KEY_DEV_TOOLS_UNLOCKED, true).apply()
    }

    /**
     * Re-hides the Developer Tools entry. In debug builds the entry is still shown because
     * [SettingsScreen] also checks `BuildConfig.DEBUG`, so this only has a visible effect on
     * release builds where the gesture-unlock is the gate.
     */
    fun lockDevTools() {
        _devToolsUnlocked.value = false
        prefs.edit().putBoolean(KEY_DEV_TOOLS_UNLOCKED, false).apply()
    }

    /**
     * Set the app theme mode.
     */
    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putInt(KEY_THEME_MODE, mode.ordinal).apply()
    }

    fun setAvatarShape(shape: AvatarShape) {
        _avatarShape.value = shape
        prefs.edit().putString(KEY_AVATAR_SHAPE, shape.name).apply()
    }
    
    fun setAvatarBackgroundEnabled(enabled: Boolean) {
        _avatarBackgroundEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AVATAR_BACKGROUND_ENABLED, enabled).apply()
    }

    fun setDisableAvatarShapeProfile(disabled: Boolean) {
        _disableAvatarShapeProfile.value = disabled
        prefs.edit().putBoolean(KEY_DISABLE_AVATAR_SHAPE_PROFILE, disabled).apply()
    }
    
    /**
     * Enable or disable haptic feedback.
     */
    fun setHapticEnabled(enabled: Boolean) {
        _hapticEnabled.value = enabled
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
    }

    fun setNavBarStyle(style: NavBarStyle) {
        _navBarStyle.value = style
        prefs.edit().putString(KEY_NAV_BAR_STYLE, style.name).apply()
    }

    fun setNavBarShowLabels(show: Boolean) {
        _navBarShowLabels.value = show
        prefs.edit().putBoolean(KEY_NAV_BAR_LABELS, show).apply()
    }

    fun setNavBarCornerRadius(radius: Float) {
        val coerced = radius.coerceIn(MIN_NAV_BAR_CORNER_RADIUS, MAX_NAV_BAR_CORNER_RADIUS)
        _navBarCornerRadius.value = coerced
        prefs.edit().putFloat(KEY_NAV_BAR_CORNER_RADIUS, coerced).apply()
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
     * Set the staff/character name language. Normally mirrored from the AniList account option.
     */
    fun setStaffNameLanguage(language: StaffNameLanguage) {
        _staffNameLanguage.value = language
        prefs.edit().putInt(KEY_STAFF_NAME_LANGUAGE, language.ordinal).apply()
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

    fun setShowAdultContent(show: Boolean) {
        _showAdultContent.value = show
        prefs.edit().putBoolean(KEY_SHOW_ADULT_CONTENT, show).apply()
    }

    fun setDiscoverSearchViewMode(mode: DiscoverViewMode) {
        _discoverSearchViewMode.value = mode
        prefs.edit().putInt(KEY_DISCOVER_SEARCH_VIEW_MODE, mode.ordinal).apply()
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
     * Persist the last selected activity-feed scope so the feed reopens on it.
     */
    fun setLastFeedScope(scope: FeedScope) {
        _lastFeedScope.value = scope
        prefs.edit().putString(KEY_FEED_SCOPE, scope.name).apply()
    }

    /**
     * Persist the library view density (grid vs list).
     */
    fun setLibraryGridView(isGrid: Boolean) {
        _libraryGridView.value = isGrid
        prefs.edit().putBoolean(KEY_LIBRARY_GRID_VIEW, isGrid).apply()
    }

    /**
     * Persist the last selected feed content filter.
     */
    fun setFeedFilter(filter: FeedFilter) {
        _feedFilter.value = filter
        prefs.edit().putString(KEY_FEED_FILTER, filter.name).apply()
    }

    /**
     * Persist the last selected Library media type (Anime vs Manga).
     */
    fun setLibraryMediaType(type: MediaType) {
        _libraryMediaType.value = type
        prefs.edit().putBoolean(KEY_LIBRARY_MEDIA_TYPE_MANGA, type == MediaType.MANGA).apply()
    }

    /**
     * Persist the last selected Discover media type (Anime vs Manga).
     */
    fun setDiscoverMediaType(type: MediaType) {
        _discoverMediaType.value = type
        prefs.edit().putBoolean(KEY_DISCOVER_MEDIA_TYPE_MANGA, type == MediaType.MANGA).apply()
    }

    /**
     * Persist the last selected forum feed tab (by [ForumFeed] enum name).
     */
    fun setForumFeed(feedName: String) {
        _forumFeed.value = feedName
        prefs.edit().putString(KEY_FORUM_FEED, feedName).apply()
    }

    /**
     * Persist the last selected forum category filter. Pass null to clear it
     * (browsing all categories).
     */
    fun setForumCategoryId(categoryId: Int?) {
        _forumCategoryId.value = categoryId
        prefs.edit().apply {
            if (categoryId != null) putInt(KEY_FORUM_CATEGORY_ID, categoryId)
            else remove(KEY_FORUM_CATEGORY_ID)
        }.apply()
    }

    /**
     * Persist the last visited main bottom-nav tab so the app reopens on it.
     * Only Library/Discover/Feed/Forum are ever stored.
     */
    fun setLastMainTab(tabKey: String) {
        _lastMainTab.value = tabKey
        prefs.edit().putString(KEY_LAST_MAIN_TAB, tabKey).apply()
    }
    
    /**
     * Set the media upload host. The new value applies to all subsequent attach
     * operations across every composer surface.
     */
    fun setMediaHost(host: MediaHost) {
        _mediaHost.value = host
        prefs.edit().putString(KEY_MEDIA_HOST, host.name).apply()
    }

    /** [duration] must be `"1h"`, `"24h"`, or `"72h"` to match Litterbox's API. */
    fun setLitterboxDuration(duration: String) {
        _litterboxDuration.value = duration
        prefs.edit().putString(KEY_LITTERBOX_DURATION, duration).apply()
    }

    fun setCustomHostUrl(value: String) {
        _customHostUrl.value = value
        prefs.edit().putString(KEY_CUSTOM_HOST_URL, value).apply()
    }

    fun setCustomHostFileField(value: String) {
        _customHostFileField.value = value
        prefs.edit().putString(KEY_CUSTOM_HOST_FIELD, value).apply()
    }

    fun setCustomHostAuthHeader(value: String) {
        _customHostAuthHeader.value = value
        prefs.edit().putString(KEY_CUSTOM_HOST_AUTH, value).apply()
    }

    fun setCustomHostResponseJsonPath(value: String) {
        _customHostResponseJsonPath.value = value
        prefs.edit().putString(KEY_CUSTOM_HOST_JSON_PATH, value).apply()
    }

    fun setCatboxUserHash(value: String) {
        _catboxUserHash.value = value
        prefs.edit().putString(KEY_CATBOX_USERHASH, value).apply()
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
    
    /**
     * Resets the per-account view preferences on account switch so one account's custom-list
     * layout / hidden lists / last-opened tabs don't bleed into another. Account-agnostic
     * preferences (theme, locale, cover quality, nav bar, etc.) are left untouched.
     *
     * Score format is intentionally NOT reset here — it self-heals from the next library load,
     * which reads the new account's MediaListOptions (see LibraryRepositoryImpl).
     */
    fun clearAccountScoped() {
        _hiddenAnimeLists.value = emptySet()
        _hiddenMangaLists.value = emptySet()
        _animeListOrder.value = emptyList()
        _mangaListOrder.value = emptyList()
        _lastSelectedAnimeTab.value = null
        _lastSelectedMangaTab.value = null
        _showPrivateEntries.value = true
        // Land on the default home tab after a switch instead of the other account's last tab.
        _lastMainTab.value = null
        prefs.edit()
            .remove(KEY_LAST_MAIN_TAB)
            .remove(KEY_HIDDEN_ANIME_LISTS)
            .remove(KEY_HIDDEN_MANGA_LISTS)
            .remove(KEY_ANIME_LIST_ORDER)
            .remove(KEY_MANGA_LIST_ORDER)
            .remove(KEY_LAST_SELECTED_ANIME_TAB)
            .remove(KEY_LAST_SELECTED_MANGA_TAB)
            .remove(KEY_SHOW_PRIVATE_ENTRIES)
            .apply()
    }

companion object {
        private const val PREFS_NAME = "anisync_settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_AVATAR_SHAPE = "avatar_shape"
        private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        private const val KEY_NAV_BAR_STYLE = "nav_bar_style"
        private const val KEY_NAV_BAR_LABELS = "nav_bar_show_labels"
        private const val KEY_NAV_BAR_CORNER_RADIUS = "nav_bar_corner_radius"

        const val MIN_NAV_BAR_CORNER_RADIUS = 0f
        const val MAX_NAV_BAR_CORNER_RADIUS = 36f
        const val DEFAULT_NAV_BAR_CORNER_RADIUS = 28f

        // Font playground axis ranges + defaults (Google Sans Flex).
        const val MIN_FONT_WEIGHT = 100f
        const val MAX_FONT_WEIGHT = 1000f
        const val DEFAULT_FONT_WEIGHT = 400f
        const val MIN_FONT_WIDTH = 25f
        const val MAX_FONT_WIDTH = 151f
        const val DEFAULT_FONT_WIDTH = 100f
        const val MIN_FONT_OPSZ = 8f
        const val MAX_FONT_OPSZ = 144f
        const val DEFAULT_FONT_OPSZ = 24f
        const val MIN_FONT_SLANT = -10f
        const val MAX_FONT_SLANT = 0f
        const val DEFAULT_FONT_SLANT = 0f
        const val MIN_FONT_ROUNDNESS = 0f
        const val MAX_FONT_ROUNDNESS = 100f
        // AniSync ships fully rounded — see TypographyAxisConfig.
        const val DEFAULT_FONT_ROUNDNESS = 100f

        private const val KEY_TYPOGRAPHY_OVERRIDES = "typography_overrides"
        private const val KEY_DEV_TOOLS_UNLOCKED = "dev_tools_unlocked"

        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_TITLE_LANGUAGE = "title_language"
        private const val KEY_AVATAR_BACKGROUND_ENABLED = "avatar_background_enabled"
        private const val KEY_DISABLE_AVATAR_SHAPE_PROFILE = "disable_avatar_shape_profile"
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
        private const val KEY_SHOW_ADULT_CONTENT = "show_adult_content"
        private const val KEY_STAFF_NAME_LANGUAGE = "staff_name_language"
        private const val KEY_DISCOVER_SEARCH_VIEW_MODE = "discover_search_view_mode"
        private const val KEY_LAST_SELECTED_ANIME_TAB = "last_selected_anime_tab"
        private const val KEY_LAST_SELECTED_MANGA_TAB = "last_selected_manga_tab"
        private const val KEY_FEED_SCOPE = "feed_scope"
        private const val KEY_FEED_FILTER = "feed_filter"
        private const val KEY_LIBRARY_GRID_VIEW = "library_grid_view"
        private const val KEY_LIBRARY_MEDIA_TYPE_MANGA = "library_media_type_manga"
        private const val KEY_DISCOVER_MEDIA_TYPE_MANGA = "discover_media_type_manga"
        private const val KEY_FORUM_FEED = "forum_feed"
        private const val KEY_FORUM_CATEGORY_ID = "forum_category_id"
        private const val KEY_LAST_MAIN_TAB = "last_main_tab"
        private const val KEY_MEDIA_HOST = "media_host"
        private const val KEY_LITTERBOX_DURATION = "litterbox_duration"
        private const val KEY_CUSTOM_HOST_URL = "custom_host_url"
        private const val KEY_CUSTOM_HOST_FIELD = "custom_host_field"
        private const val KEY_CUSTOM_HOST_AUTH = "custom_host_auth"
        private const val KEY_CUSTOM_HOST_JSON_PATH = "custom_host_json_path"
        private const val KEY_CATBOX_USERHASH = "catbox_userhash"
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
 * Staff & character name language options. Mirrors AniList's `UserStaffNameLanguage`
 * ([com.anisync.android.domain.AniListStaffNameLanguage]) but kept as a local enum so the
 * presentation layer doesn't depend on generated GraphQL types.
 */
enum class StaffNameLanguage {
    ROMAJI_WESTERN,
    ROMAJI,
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
    ARABIC("ar", "العربية"),
    SPANISH("es", "Español"),
    PORTUGUESE_BR("pt-BR", "Português (Brasil)"),
    PORTUGUESE("pt", "Português")
}
