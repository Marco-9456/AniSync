package com.anisync.android.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryRepository
import com.anisync.android.type.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Success(val entries: List<LibraryEntry>) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

sealed interface LibraryEvent {
    data class ShowSnackbar(val message: String) : LibraryEvent
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    // MediaType State (Default: Anime)
    private val _mediaType = MutableStateFlow(MediaType.ANIME)
    val mediaType: StateFlow<MediaType> = _mediaType.asStateFlow()

    private val _events = MutableSharedFlow<LibraryEvent>()
    val events: SharedFlow<LibraryEvent> = _events.asSharedFlow()

    init {
        loadLibrary()
    }

    fun loadLibrary() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            try {
                // Pass empty username to get authenticated user's library
                val entries = libraryRepository.getLibrary("", _mediaType.value)
                _uiState.value = LibraryUiState.Success(entries)
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }
    
    fun onMediaTypeChange(type: MediaType) {
        if (_mediaType.value != type) {
            _mediaType.value = type
            loadLibrary()
        }
    }

    fun incrementProgress(mediaId: Int) {
        updateProgress(mediaId, 1)
    }

    fun decrementProgress(mediaId: Int) {
        updateProgress(mediaId, -1)
    }

    private fun updateProgress(mediaId: Int, delta: Int) {
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success) {
            val oldList = currentState.entries
            val entryIndex = oldList.indexOfFirst { it.mediaId == mediaId }
            if (entryIndex == -1) return

            val entry = oldList[entryIndex]
            val newProgress = (entry.progress + delta).coerceAtLeast(0)

            // Optimistic Update
            val newList = oldList.toMutableList()
            newList[entryIndex] = entry.copy(progress = newProgress)
            _uiState.value = LibraryUiState.Success(newList)

            viewModelScope.launch {
                try {
                    val success = libraryRepository.updateProgress(mediaId, newProgress)
                    if (!success) {
                        // Revert
                        _uiState.value = LibraryUiState.Success(oldList)
                        _events.emit(LibraryEvent.ShowSnackbar("Failed to update progress"))
                    }
                } catch (e: Exception) {
                    // Revert
                    _uiState.value = LibraryUiState.Success(oldList)
                    _events.emit(LibraryEvent.ShowSnackbar(e.message ?: "Unknown Error"))
                }
            }
        }
    }
}
