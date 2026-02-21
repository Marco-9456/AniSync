package com.anisync.android.presentation.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
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

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val profileRepository: ProfileRepository,
    private val authRepository: com.anisync.android.data.AuthRepository,
    private val appSettings: AppSettings,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Settings from AppSettings (needed for display in profile)
    val titleLanguage: StateFlow<com.anisync.android.data.TitleLanguage> = appSettings.titleLanguage

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
        onAction(ProfileAction.Refresh)
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.Refresh -> refresh()
            is ProfileAction.UpdateAbout -> updateAbout(action.about)
        }
    }

    private fun refresh() {
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

    private fun updateAbout(about: String) {
        viewModelScope.launch {
            if (profileRepository.updateAbout(about) is Result.Error) {
                // In a real app, send a one-off UI event (e.g. Snackbar) here
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}
