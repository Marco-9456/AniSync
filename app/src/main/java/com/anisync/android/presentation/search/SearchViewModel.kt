package com.anisync.android.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.SearchRepository
import com.anisync.android.type.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    
    private val _mediaType = MutableStateFlow(MediaType.ANIME)
    val mediaType: StateFlow<MediaType> = _mediaType.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        combine(_query, _mediaType) { query, type -> query to type }
            .debounce(500L)
            .distinctUntilChanged()
            .filter { (query, _) -> query.isNotBlank() }
            .onEach {
                _uiState.value = SearchUiState.Loading
            }
            .flatMapLatest { (query, type) ->
                kotlinx.coroutines.flow.flow {
                    try {
                        val results = searchRepository.searchMedia(query, type)
                        emit(results)
                    } catch (e: Exception) {
                        emit(emptyList()) 
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
    
    fun onMediaTypeChange(type: MediaType) {
        _mediaType.value = type
    }
    
    fun clearQuery() {
        _query.value = ""
    }
}
