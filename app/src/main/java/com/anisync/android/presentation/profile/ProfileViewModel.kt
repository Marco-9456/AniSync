package com.anisync.android.presentation.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.anisync.android.domain.GetProfileUseCase
import com.anisync.android.domain.ProfileRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.UserProfile
import com.anisync.android.worker.NotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
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
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)
    private val prefs = context.getSharedPreferences("anisync_prefs", Context.MODE_PRIVATE)

    private val _isNotificationsEnabled = MutableStateFlow(prefs.getBoolean("notifications_enabled", false))
    val isNotificationsEnabled = _isNotificationsEnabled.asStateFlow()

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

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _isNotificationsEnabled.value = enabled
            prefs.edit().putBoolean("notifications_enabled", enabled).apply()

            if (enabled) {
                val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
                    .build()
                
                workManager.enqueueUniquePeriodicWork(
                    "notification_worker",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
            } else {
                workManager.cancelUniqueWork("notification_worker")
            }
        }
    }
}
