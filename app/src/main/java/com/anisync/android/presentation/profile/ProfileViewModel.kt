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
        val selectedCastFilter: ProfileCastFilter = ProfileCastFilter.CHARACTERS,
        val selectedSocialTab: ProfileSocialTab = ProfileSocialTab.FOLLOWING,
        val isEditProfileDialogVisible: Boolean = false,
        val isBiographySheetVisible: Boolean = false,
        val isRefreshing: Boolean = false
    )

    private val localState = MutableStateFlow(ProfileUiLocalState())

    private data class SocialState(
        val isSocialLoading: Boolean = false,
        val socialFollowing: List<com.anisync.android.domain.SocialUser> = emptyList(),
        val socialFollowers: List<com.anisync.android.domain.SocialUser> = emptyList(),
        val socialThreads: List<com.anisync.android.domain.ForumThread> = emptyList(),
        val socialComments: List<com.anisync.android.domain.SocialThreadComment> = emptyList(),
        val socialErrorMessage: String? = null,
        val hasFetchedSocialData: Boolean = false
    )

    private val socialState = MutableStateFlow(SocialState())

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

    val uiState: StateFlow<ProfileUiState> = combine(profileState, localState, socialState) { remote, local, social ->
        remote.copy(
            isRefreshing = local.isRefreshing,
            selectedTab = local.selectedTab,
            selectedActivityFilter = local.selectedActivityFilter,
            selectedCastFilter = local.selectedCastFilter,
            selectedSocialTab = local.selectedSocialTab,
            isEditProfileDialogVisible = local.isEditProfileDialogVisible,
            isBiographySheetVisible = local.isBiographySheetVisible,
            socialFollowing = social.socialFollowing,
            socialFollowers = social.socialFollowers,
            socialThreads = social.socialThreads,
            socialComments = social.socialComments,
            isSocialLoading = social.isSocialLoading,
            socialErrorMessage = social.socialErrorMessage
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
                if (action.tab == ProfileTab.SOCIAL && !socialState.value.hasFetchedSocialData) {
                    fetchSocialData()
                }
            }

            is ProfileAction.SelectActivityFilter -> {
                localState.update {
                    it.copy(selectedActivityFilter = action.filter)
                }
            }

            is ProfileAction.SelectCastFilter -> {
                localState.update {
                    it.copy(selectedCastFilter = action.filter)
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
            val refreshJob = launch { profileRepository.refreshProfile("") }
            
            val socialJob = launch {
                if (localState.value.selectedTab == ProfileTab.SOCIAL || socialState.value.hasFetchedSocialData) {
                    fetchSocialData()
                }
            }

            refreshJob.join()
            socialJob.join()
            
            localState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun fetchSocialData() {
        viewModelScope.launch {
            socialState.update { it.copy(isSocialLoading = true, socialErrorMessage = null) }
            val userId = uiState.value.profile?.id ?: return@launch
            when (val result = profileRepository.getSocialData(userId)) {
                is Result.Success -> {
                    socialState.update {
                        it.copy(
                            isSocialLoading = false,
                            socialFollowing = result.data.following,
                            socialFollowers = result.data.followers,
                            socialThreads = result.data.threads,
                            socialComments = result.data.comments,
                            hasFetchedSocialData = true
                        )
                    }
                }
                is Result.Error -> {
                    socialState.update {
                        it.copy(
                            isSocialLoading = false,
                            socialErrorMessage = result.message
                        )
                    }
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
