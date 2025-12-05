package com.anisync.android.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.SearchMediaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val results: List<LibraryEntry>) : SearchUiState
    data class Error(val message: String) : SearchUiState
    data object Empty : SearchUiState
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMediaUseCase: SearchMediaUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        _query
            .debounce(500L)
            .distinctUntilChanged()
            .filter { it.isNotBlank() }
            .onEach {
                _uiState.value = SearchUiState.Loading
            }
            .flatMapLatest { query ->
                kotlinx.coroutines.flow.flow {
                    try {
                        val results = searchMediaUseCase(query)
                        emit(results)
                    } catch (e: Exception) {
                        emit(emptyList()) // Ideally handle error properly, simplified for flow
                        _uiState.update { SearchUiState.Error(e.message ?: "Unknown error") }
                    }
                }
            }
            .onEach { results ->
                if (results.isEmpty()) {
                    _uiState.value = SearchUiState.Empty
                } else {
                    _uiState.value = SearchUiState.Success(results)
                }
            }
            .launchIn(viewModelScope)
            
        // Handle clearing query
        _query
            .filter { it.isBlank() }
            .onEach { _uiState.value = SearchUiState.Idle }
            .launchIn(viewModelScope)
    }

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
    }
    
    fun clearQuery() {
        _query.value = ""
    }
}
