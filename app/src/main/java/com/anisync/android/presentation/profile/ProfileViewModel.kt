package com.anisync.android.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.GetProfileUseCase
import com.anisync.android.domain.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val profile: UserProfile) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile("") // Empty string = use authenticated Viewer
    }

    private fun loadProfile(username: String) {
        viewModelScope.launch {
            _uiState.update { ProfileUiState.Loading }
            try {
                val profile = getProfileUseCase(username)
                if (profile != null) {
                    _uiState.update { ProfileUiState.Success(profile) }
                } else {
                    _uiState.update { ProfileUiState.Error("User not found") }
                }
            } catch (e: Exception) {
                _uiState.update { ProfileUiState.Error(e.message ?: "Unknown error") }
            }
        }
    }
}
