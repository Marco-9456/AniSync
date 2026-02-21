package com.anisync.android.presentation.profile

import com.anisync.android.domain.UserProfile

/**
 * UI state for the Profile screen.
 */
sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val profile: UserProfile) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

/**
 * Actions that the Profile screen can dispatch to [ProfileViewModel].
 */
sealed interface ProfileAction {
    data object Refresh : ProfileAction
    data class UpdateAbout(val about: String) : ProfileAction
}
