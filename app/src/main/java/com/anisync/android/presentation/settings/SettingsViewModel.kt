package com.anisync.android.presentation.settings

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.R
import com.anisync.android.data.AppLocale
import com.anisync.android.data.AppSettings
import com.anisync.android.data.CoverQuality
import com.anisync.android.data.NavBarStyle
import com.anisync.android.data.NotificationPreferences
import com.anisync.android.data.account.AccountManager
import com.anisync.android.data.update.UpdateCheckResult
import com.anisync.android.data.update.UpdateManager
import com.anisync.android.domain.GetProfileUseCase
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.alert.ToastManager
import com.anisync.android.presentation.components.alert.ToastType
import com.anisync.android.presentation.security.AppLockAuthenticator.isAppLockSupported
import com.anisync.android.ui.theme.FontAxisOverrides
import com.anisync.android.worker.NotificationDebugService
import com.anisync.android.worker.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class ThemePaletteState(
    val paletteId: String,
    val customColor: androidx.compose.ui.graphics.Color?,
    val style: com.materialkolor.PaletteStyle,
    val coverQuality: CoverQuality,
    val showAdultContent: Boolean
)

private data class UpdatesAndNavBarState(
    val autoUpdate: Boolean,
    val allowPrerelease: Boolean,
    val navBarStyle: NavBarStyle,
    val navBarShowLabels: Boolean,
    val navBarCornerRadius: Float
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettings: AppSettings,
    private val notificationPreferences: NotificationPreferences,
    private val notificationScheduler: NotificationScheduler,
    private val notificationDebugService: NotificationDebugService,
    private val accountManager: AccountManager,
    private val updateManager: UpdateManager,
    getProfileUseCase: GetProfileUseCase,
    private val toastManager: ToastManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _cacheSize = MutableStateFlow("0 B")
    private val _isCacheCleared = MutableStateFlow(false)
    private val _isCacheLoading = MutableStateFlow(false)
    private val _isCacheClearing = MutableStateFlow(false)

    /** Exposed so the UI can observe download/check state from [UpdateManager]. */
    val updateState = updateManager.updateState

    init {
        refreshCacheSize()
    }

    private val coreUiState: Flow<SettingsUiState> = combine(
        combine(
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
            appSettings.avatarShape,
            appSettings.avatarBackgroundEnabled,
            appSettings.disableAvatarShapeProfile,
            appSettings.respectUserProfileColors
        ) { state, avatarShape, bgEnabled, disableProfile, respectColors ->
            state.copy(
                avatarShape = avatarShape,
                avatarBackgroundEnabled = bgEnabled,
                disableAvatarShapeProfile = disableProfile,
                respectUserProfileColors = respectColors
            )
        },
        combine(
            appSettings.selectedPaletteId,
            appSettings.customSeedColor,
            appSettings.paletteStyle,
            appSettings.coverQuality,
            appSettings.showAdultContent
        ) { paletteId, customColor, style, coverQuality, showAdult ->
            ThemePaletteState(paletteId, customColor, style, coverQuality, showAdult)
        },
        combine(
            combine(
                appSettings.notificationsEnabled,
                notificationPreferences.watchingEnabled,
                notificationPreferences.planningEnabled,
                notificationPreferences.upcomingEnabled,
                notificationPreferences.streamingDelayMinutes
            ) { enabled, watching, planning, upcoming, delay ->
                listOf<Any>(enabled, watching, planning, upcoming, delay)
            },
            combine(
                notificationPreferences.threadCommentReplyEnabled,
                notificationPreferences.threadSubscribedEnabled,
                notificationPreferences.threadCommentMentionEnabled,
                notificationPreferences.threadLikeEnabled,
                notificationPreferences.threadCommentLikeEnabled
            ) { commentReply, subscribed, commentMention, threadLike, commentLike ->
                listOf<Any>(commentReply, subscribed, commentMention, threadLike, commentLike)
            },
            combine(
                notificationPreferences.activityReplyEnabled,
                notificationPreferences.activityMentionEnabled,
                notificationPreferences.activityLikeEnabled,
                notificationPreferences.activityMessageEnabled,
                notificationPreferences.followsEnabled
            ) { reply, mention, like, message, follows ->
                listOf<Any>(reply, mention, like, message, follows)
            }
        ) { airing, forum, activity ->
            airing + forum + activity
        },
        combine(
            appSettings.autoUpdateEnabled,
            appSettings.allowPrerelease,
            appSettings.navBarStyle,
            appSettings.navBarShowLabels,
            appSettings.navBarCornerRadius
        ) { autoUpdate, prerelease, navStyle, navLabels, navRadius ->
            UpdatesAndNavBarState(autoUpdate, prerelease, navStyle, navLabels, navRadius)
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
    ) { lookAndFeel, themePalette, notifications, updatesAndNav, storageAndProfile ->
        val (cacheSize, isCleared, isLoading, isClearing, profile) = storageAndProfile

        lookAndFeel.copy(
            selectedPaletteId = themePalette.paletteId,
            customSeedColor = themePalette.customColor,
            paletteStyle = themePalette.style,
            coverQuality = themePalette.coverQuality,
            showAdultContent = themePalette.showAdultContent,
            isNotificationsEnabled = notifications[0] as Boolean,
            watchingNotificationsEnabled = notifications[1] as Boolean,
            planningNotificationsEnabled = notifications[2] as Boolean,
            upcomingNotificationsEnabled = notifications[3] as Boolean,
            streamingDelayMinutes = notifications[4] as Int,
            threadCommentReplyEnabled = notifications[5] as Boolean,
            threadSubscribedEnabled = notifications[6] as Boolean,
            threadCommentMentionEnabled = notifications[7] as Boolean,
            threadLikeEnabled = notifications[8] as Boolean,
            threadCommentLikeEnabled = notifications[9] as Boolean,
            activityReplyEnabled = notifications[10] as Boolean,
            activityMentionEnabled = notifications[11] as Boolean,
            activityLikeEnabled = notifications[12] as Boolean,
            activityMessageEnabled = notifications[13] as Boolean,
            followsEnabled = notifications[14] as Boolean,
            isAutoUpdateEnabled = updatesAndNav.autoUpdate,
            isPrereleaseAllowed = updatesAndNav.allowPrerelease,
            navBarStyle = updatesAndNav.navBarStyle,
            navBarShowLabels = updatesAndNav.navBarShowLabels,
            navBarCornerRadius = updatesAndNav.navBarCornerRadius,
            cacheSize = cacheSize as String,
            isCacheCleared = isCleared as Boolean,
            isCacheLoading = isLoading as Boolean,
            isCacheClearing = isClearing as Boolean,
            userProfile = profile as UserProfile?,
            isLoaded = true
        )
    }

    // Per-category font playground overrides. Its own flow because the main combine above is
    // already at the 5-argument limit of the typed combine() overload.
    private val fontPlaygroundFlow: Flow<FontPlaygroundUiState> =
        appSettings.typographyOverrides.map { FontPlaygroundUiState(it) }

    val uiState: StateFlow<SettingsUiState> = combine(
        coreUiState,
        fontPlaygroundFlow,
        appSettings.amoledEnabled,
        appSettings.appLockEnabled,
    ) { core, fontPlayground, amoled, appLock ->
        core.copy(fontPlayground = fontPlayground, amoledEnabled = amoled, appLockEnabled = appLock)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetThemeMode -> appSettings.setThemeMode(action.mode)
            is SettingsAction.SetAmoledEnabled -> appSettings.setAmoledEnabled(action.enabled)
            is SettingsAction.SetTitleLanguage -> appSettings.setTitleLanguage(action.language)
            is SettingsAction.SetCoverQuality -> appSettings.setCoverQuality(action.quality)
            is SettingsAction.SetHapticEnabled -> appSettings.setHapticEnabled(action.enabled)
            is SettingsAction.SetAppLockEnabled -> setAppLockEnabled(action.enabled)
            is SettingsAction.SetNavBarStyle -> appSettings.setNavBarStyle(action.style)
            is SettingsAction.SetNavBarShowLabels -> appSettings.setNavBarShowLabels(action.show)
            is SettingsAction.SetNavBarCornerRadius -> appSettings.setNavBarCornerRadius(action.radius)
            is SettingsAction.SetAvatarShape -> appSettings.setAvatarShape(action.shape)
            is SettingsAction.SetAvatarBackgroundEnabled -> appSettings.setAvatarBackgroundEnabled(action.enabled)
            is SettingsAction.SetDisableAvatarShapeProfile -> appSettings.setDisableAvatarShapeProfile(action.disabled)
            is SettingsAction.SetRespectUserProfileColors -> appSettings.setRespectUserProfileColors(action.enabled)
            is SettingsAction.SetShowAdultContent -> appSettings.setShowAdultContent(action.enabled)
            is SettingsAction.SetPreferredStreamingService -> appSettings.setPreferredStreamingService(
                action.service
            )

            is SettingsAction.SetAppLocale -> setAppLocale(action.locale)
            is SettingsAction.SetSelectedPalette -> appSettings.setSelectedPalette(action.paletteId)
            is SettingsAction.SetCustomSeedColor -> appSettings.setCustomSeedColor(action.color)
            is SettingsAction.SetPaletteStyle -> appSettings.setPaletteStyle(action.style)

            is SettingsAction.ToggleNotifications -> toggleNotifications(action.enabled)
            is SettingsAction.SetWatchingNotificationsEnabled -> notificationPreferences.setWatchingEnabled(
                action.enabled
            )

            is SettingsAction.SetPlanningNotificationsEnabled -> notificationPreferences.setPlanningEnabled(
                action.enabled
            )

            is SettingsAction.SetUpcomingNotificationsEnabled -> notificationPreferences.setUpcomingEnabled(
                action.enabled
            )

            is SettingsAction.SetThreadCommentReplyEnabled -> notificationPreferences.setThreadCommentReplyEnabled(
                action.enabled
            )

            is SettingsAction.SetThreadSubscribedEnabled -> notificationPreferences.setThreadSubscribedEnabled(
                action.enabled
            )

            is SettingsAction.SetThreadCommentMentionEnabled -> notificationPreferences.setThreadCommentMentionEnabled(
                action.enabled
            )

            is SettingsAction.SetThreadLikeEnabled -> notificationPreferences.setThreadLikeEnabled(
                action.enabled
            )

            is SettingsAction.SetThreadCommentLikeEnabled -> notificationPreferences.setThreadCommentLikeEnabled(
                action.enabled
            )

            is SettingsAction.SetActivityReplyEnabled -> notificationPreferences.setActivityReplyEnabled(
                action.enabled
            )

            is SettingsAction.SetActivityMentionEnabled -> notificationPreferences.setActivityMentionEnabled(
                action.enabled
            )

            is SettingsAction.SetActivityLikeEnabled -> notificationPreferences.setActivityLikeEnabled(
                action.enabled
            )

            is SettingsAction.SetActivityMessageEnabled -> notificationPreferences.setActivityMessageEnabled(
                action.enabled
            )

            is SettingsAction.SetFollowsEnabled -> notificationPreferences.setFollowsEnabled(
                action.enabled
            )

            is SettingsAction.SetStreamingDelayMinutes -> notificationPreferences.setStreamingDelayMinutes(
                action.minutes
            )

            is SettingsAction.SetAutoUpdateEnabled -> appSettings.setAutoUpdateEnabled(action.enabled)
            is SettingsAction.SetPrereleaseAllowed -> appSettings.setAllowPrerelease(action.allowed)

            // Update operations
            is SettingsAction.CheckForUpdate -> checkForUpdate()
            is SettingsAction.StartDownload -> updateManager.startDownload(
                release = action.release,
                onError = {
                    Toast.makeText(context, R.string.app_update_failed, Toast.LENGTH_SHORT).show()
                }
            )
            is SettingsAction.CancelDownload -> updateManager.cancelDownload()
            is SettingsAction.InstallUpdate -> updateManager.installApk()
            is SettingsAction.DismissUpdate -> updateManager.dismissUpdate()

            SettingsAction.RefreshCacheSize -> refreshCacheSize()
            SettingsAction.ClearCache -> clearCache()
            SettingsAction.ResetCacheCleared -> _isCacheCleared.value = false

            SettingsAction.SendTestWatchingNotification -> notificationDebugService.sendTestWatchingNotification()
            SettingsAction.SendTestPlanningNotification -> notificationDebugService.sendTestPlanningNotification()
            SettingsAction.SendTestAdvanceNotification -> notificationDebugService.sendTestAdvanceNotification()
            SettingsAction.SendTestImminentNotification -> notificationDebugService.sendTestImminentNotification()
            SettingsAction.BumpInboxBadge -> notificationDebugService.bumpInboxBadge()
            SettingsAction.ClearAllNotifications -> notificationDebugService.clearAllNotifications()
            is SettingsAction.ShowTestToast -> {
                val countdown = if (action.code == 429) 60L else null
                toastManager.showToast(action.code, "This is a test message for error code ${action.code}.", countdown)
            }
            SettingsAction.FetchLatestRelease -> fetchLatestRelease()

            is SettingsAction.SetFontAxis -> appSettings.updateTypographyCategory(action.category) {
                it.withAxis(action.axis, action.value)
            }
            is SettingsAction.SetFontAxisAll -> appSettings.updateTypographyAll {
                it.withAxis(action.axis, action.value)
            }
            SettingsAction.ResetFontAxes -> appSettings.resetTypography()
        }
    }

    private fun checkForUpdate() {
        val allowPrerelease = uiState.value.isPrereleaseAllowed
        viewModelScope.launch {
            when (val result = updateManager.checkForUpdate(allowPrerelease)) {
                is UpdateCheckResult.UpToDate -> {
                    Toast.makeText(context, context.getString(R.string.update_is_up_to_date, context.getString(R.string.app_name)), Toast.LENGTH_SHORT)
                        .show()
                }
                is UpdateCheckResult.Error -> {
                    Toast.makeText(context, R.string.app_update_failed, Toast.LENGTH_SHORT).show()
                }
                is UpdateCheckResult.Available -> {
                    // State is already updated in UpdateManager; dialog will react
                }
            }
        }
    }

    private fun fetchLatestRelease() {
        val allowPrerelease = uiState.value.isPrereleaseAllowed
        viewModelScope.launch {
            when (val result = updateManager.fetchLatestRelease(allowPrerelease)) {
                is UpdateCheckResult.UpToDate -> {
                    Toast.makeText(context, context.getString(R.string.update_is_up_to_date, context.getString(R.string.app_name)), Toast.LENGTH_SHORT)
                        .show()
                }
                is UpdateCheckResult.Error -> {
                    Toast.makeText(context, R.string.app_update_failed, Toast.LENGTH_SHORT).show()
                }
                is UpdateCheckResult.Available -> {
                    // State is already updated in UpdateManager; dialog will react
                }
            }
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

    /**
     * Turns the app lock on/off. Refuses to enable it when the device has no usable screen lock —
     * without one there'd be no way to authenticate, which would lock the user out of their own app.
     */
    private fun setAppLockEnabled(enabled: Boolean) {
        if (enabled && !context.isAppLockSupported()) {
            Toast.makeText(context, R.string.app_lock_unavailable, Toast.LENGTH_LONG).show()
            return
        }
        appSettings.setAppLockEnabled(enabled)
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
                toastManager.showToast(ToastType.SUCCESS, message = "Cache cleared")
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
            val externalCacheSize =
                context.externalCacheDir?.walkTopDown()?.sumOf { it.length() } ?: 0L
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
            accountManager.logoutActive()
            onComplete()
        }
    }
}

/** Sets a single variable-font axis on a [FontAxisOverrides], keyed by [FontPlaygroundAxis]. */
private fun FontAxisOverrides.withAxis(axis: FontPlaygroundAxis, value: Float): FontAxisOverrides =
    when (axis) {
        FontPlaygroundAxis.WEIGHT -> copy(weight = value)
        FontPlaygroundAxis.WIDTH -> copy(width = value)
        FontPlaygroundAxis.OPTICAL_SIZE -> copy(opticalSize = value)
        FontPlaygroundAxis.SLANT -> copy(slant = value)
        FontPlaygroundAxis.ROUNDNESS -> copy(roundness = value)
    }
