package com.anisync.android.presentation.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.data.NotificationPreferences
import com.anisync.android.worker.NotificationDebugService
import com.anisync.android.data.ThemeMode
import com.anisync.android.domain.GetProfileUseCase
import com.anisync.android.domain.ProfileRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val profile: UserProfile) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val profileRepository: ProfileRepository,
    private val authRepository: com.anisync.android.data.AuthRepository,
    private val appSettings: AppSettings,
    private val notificationPreferences: NotificationPreferences,
    private val notificationScheduler: com.anisync.android.worker.NotificationScheduler,
    private val notificationDebugService: NotificationDebugService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Settings from AppSettings
    val titleLanguage: StateFlow<com.anisync.android.data.TitleLanguage> = appSettings.titleLanguage
    val themeMode: StateFlow<ThemeMode> = appSettings.themeMode
    val hapticEnabled: StateFlow<Boolean> = appSettings.hapticEnabled
    val isNotificationsEnabled: StateFlow<Boolean> = appSettings.notificationsEnabled

    // Granular notification settings from NotificationPreferences
    val watchingNotificationsEnabled: StateFlow<Boolean> = notificationPreferences.watchingEnabled
    val planningNotificationsEnabled: StateFlow<Boolean> = notificationPreferences.planningEnabled
    val upcomingNotificationsEnabled: StateFlow<Boolean> = notificationPreferences.upcomingEnabled

    /**
     * Observe profile from local cache via Flow.
     */
    val uiState: StateFlow<ProfileUiState> = getProfileUseCase()
        .map<UserProfile?, ProfileUiState> { profile ->
            if (profile != null) {
                ProfileUiState.Success(profile)
            } else {
                ProfileUiState.Loading
            }
        }
        .onStart { emit(ProfileUiState.Loading) }
        .catch { e -> emit(ProfileUiState.Error(e.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileUiState.Loading
        )

    init {
        // Trigger initial refresh
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = profileRepository.refreshProfile("")) {
                is Result.Success -> {
                    // Cache updated, Flow emits automatically
                }
                is Result.Error -> {
                    // Could show snackbar
                }
            }
        }
    }

    fun updateAbout(about: String, onUnsuccessful: (String) -> Unit) {
        viewModelScope.launch {
            if (profileRepository.updateAbout(about) is Result.Error) {
                onUnsuccessful("Failed to update profile")
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        appSettings.setThemeMode(mode)
    }

    fun setTitleLanguage(language: com.anisync.android.data.TitleLanguage) {
        appSettings.setTitleLanguage(language)
    }

    fun setHapticEnabled(enabled: Boolean) {
        appSettings.setHapticEnabled(enabled)
    }

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

    // Granular notification settings
    fun setWatchingNotificationsEnabled(enabled: Boolean) {
        notificationPreferences.setWatchingEnabled(enabled)
    }

    fun setPlanningNotificationsEnabled(enabled: Boolean) {
        notificationPreferences.setPlanningEnabled(enabled)
    }

    fun setUpcomingNotificationsEnabled(enabled: Boolean) {
        notificationPreferences.setUpcomingEnabled(enabled)
    }

    // Debug notification methods
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
}

