package com.anisync.android.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.GetLibraryUseCase
import com.anisync.android.domain.LibraryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Success(val entries: List<LibraryEntry>) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getLibraryUseCase: GetLibraryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadLibrary("") // Empty string = use authenticated Viewer
    }

    fun loadLibrary(username: String) {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            try {
                val entries = getLibraryUseCase(username)
                _uiState.value = LibraryUiState.Success(entries)
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }
}
