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

    private val mediaId: Int = checkNotNull(savedStateHandle["mediaId"])

    init {
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
                    // Reload details to get updated list entry
                    loadDetails()
                }
            } catch (e: Exception) {
                // Handle error silently or show toast
            } finally {
                _isSaving.value = false
            }
        }
    }
}
