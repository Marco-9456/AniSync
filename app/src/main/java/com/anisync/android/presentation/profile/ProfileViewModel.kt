package com.anisync.android.presentation.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.domain.GetProfileUseCase
import com.anisync.android.domain.ProfileRepository
import com.anisync.android.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

    private data class ProfileUiLocalState(
        val selectedTab: ProfileTab = ProfileTab.OVERVIEW,
        val selectedActivityFilter: ProfileActivityFilter = ProfileActivityFilter.ALL,
        val selectedSocialTab: ProfileSocialTab = ProfileSocialTab.FOLLOWING,
        val isEditProfileDialogVisible: Boolean = false,
        val isBiographySheetVisible: Boolean = false,
        val isRefreshing: Boolean = false
    )

    private val localState = MutableStateFlow(ProfileUiLocalState())

    private val profileState = getProfileUseCase()
        .map { profileResult ->
            if (profileResult != null) {
                ProfileUiState(
                    isLoading = false,
                    profile = profileResult
                )
            } else {
                ProfileUiState(isLoading = true)
            }
        }
        .onStart { emit(ProfileUiState(isLoading = true)) }
        .catch { e -> emit(ProfileUiState(isLoading = false, errorMessage = e.message ?: "Unknown error")) }

    val uiState: StateFlow<ProfileUiState> = combine(profileState, localState) { remote, local ->
        remote.copy(
            isRefreshing = local.isRefreshing,
            selectedTab = local.selectedTab,
            selectedActivityFilter = local.selectedActivityFilter,
            selectedSocialTab = local.selectedSocialTab,
            isEditProfileDialogVisible = local.isEditProfileDialogVisible,
            isBiographySheetVisible = local.isBiographySheetVisible
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState(isLoading = true)
    )

    init {
        // Trigger initial refresh
        onAction(ProfileAction.Refresh)
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.Refresh -> refresh()
            is ProfileAction.UpdateAbout -> updateAbout(action.about)
            is ProfileAction.SelectTab -> {
                localState.update {
                    it.copy(selectedTab = action.tab)
                }
            }

            is ProfileAction.SelectActivityFilter -> {
                localState.update {
                    it.copy(selectedActivityFilter = action.filter)
                }
            }

            is ProfileAction.SelectSocialTab -> {
                localState.update {
                    it.copy(selectedSocialTab = action.tab)
                }
            }

            is ProfileAction.SetEditProfileDialogVisible -> {
                localState.update {
                    it.copy(isEditProfileDialogVisible = action.visible)
                }
            }

            is ProfileAction.SetBiographySheetVisible -> {
                localState.update {
                    it.copy(isBiographySheetVisible = action.visible)
                }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            localState.update { it.copy(isRefreshing = true) }
            when (val result = profileRepository.refreshProfile("")) {
                is Result.Success -> {
                    // Cache updated, Flow emits automatically
                }
                is Result.Error -> {
                    // Could show snackbar
                }
            }
            localState.update { it.copy(isRefreshing = false) }
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
