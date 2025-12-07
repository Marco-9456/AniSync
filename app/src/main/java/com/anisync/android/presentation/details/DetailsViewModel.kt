package com.anisync.android.presentation.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.GetMediaDetailsUseCase
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data class Success(val details: MediaDetails) : DetailsUiState
    data class Error(val message: String) : DetailsUiState
}

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val getMediaDetailsUseCase: GetMediaDetailsUseCase,
    private val detailsRepository: DetailsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // Get the ID directly from the navigation route "details/{mediaId}"
    private val mediaId: Int = checkNotNull(savedStateHandle["mediaId"]) {
        "Media ID is required for DetailsViewModel"
    }

    init {
        loadDetails()
    }

    // Kept for compatibility with DetailsScreen's LaunchedEffect.
    // Since we load in init, this acts as a safeguard or refresh mechanism.
    fun loadMedia(id: Int) {
        // If the ID matches what we already have and we are not in an error state,
        // we can skip reloading to avoid double-fetching.
        if (id == mediaId && _uiState.value !is DetailsUiState.Error) {
            return
        }
        loadDetails()
    }

    private fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { DetailsUiState.Loading }
            try {
                val details = getMediaDetailsUseCase(mediaId)
                if (details != null) {
                    _uiState.update { DetailsUiState.Success(details) }
                } else {
                    _uiState.update { DetailsUiState.Error("Media not found") }
                }
            } catch (e: Exception) {
                _uiState.update { DetailsUiState.Error(e.message ?: "Unknown error") }
            }
        }
    }

    fun saveMediaListEntry(status: LibraryStatus, progress: Int) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val success = detailsRepository.updateMediaListEntry(mediaId, status, progress)
                if (success) {
                    loadDetails()
                }
            } catch (e: Exception) {
                // Ideally emit a one-time event for error (e.g., Snackbar)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteMediaListEntry() {
        viewModelScope.launch {
            val details = (_uiState.value as? DetailsUiState.Success)?.details ?: return@launch
            val listEntryId = details.listEntryId ?: return@launch

            _isSaving.value = true
            try {
                val success = detailsRepository.deleteMediaListEntry(listEntryId)
                if (success) {
                    loadDetails()
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isSaving.value = false
            }
        }
    }
}