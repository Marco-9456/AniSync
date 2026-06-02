package com.anisync.android.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.Result
import com.anisync.android.domain.SearchRepository
import com.anisync.android.type.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Search state for the "recommend a similar media" sheet. Search is locked to a
 * single [MediaType] (the source media's type) so anime can only recommend anime
 * and manga only manga, and the source media itself is filtered out of results.
 */
data class RecommendSearchState(
    val query: String = "",
    val results: List<LibraryEntry> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RecommendMediaViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecommendSearchState())
    val state: StateFlow<RecommendSearchState> = _state.asStateFlow()

    private var type: MediaType = MediaType.ANIME
    private var excludeMediaId: Int = -1
    private var searchJob: Job? = null

    fun configure(type: MediaType, excludeMediaId: Int) {
        this.type = type
        this.excludeMediaId = excludeMediaId
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, error = null) }
        searchJob?.cancel()
        if (query.trim().length < MIN_QUERY_LENGTH) {
            _state.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runSearch()
        }
    }

    private suspend fun runSearch() {
        _state.update { it.copy(isSearching = true) }
        when (val result = searchRepository.searchMedia(query = _state.value.query.trim(), type = type)) {
            is Result.Success -> _state.update {
                it.copy(
                    results = result.data.entries.filter { entry -> entry.mediaId != excludeMediaId },
                    isSearching = false
                )
            }
            is Result.Error -> _state.update {
                it.copy(isSearching = false, error = result.message, results = emptyList())
            }
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val MIN_QUERY_LENGTH = 2
    }
}
