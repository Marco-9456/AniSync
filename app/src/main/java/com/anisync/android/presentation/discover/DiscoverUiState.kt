package com.anisync.android.presentation.discover

import androidx.compose.runtime.Stable
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.SearchFilters
import com.anisync.android.type.MediaType

sealed interface DiscoverUiState {
    data object Loading : DiscoverUiState

    @Stable
    data class Success(
        val trending: List<LibraryEntry> = emptyList(),
        val popular: List<LibraryEntry> = emptyList(),
        val upcoming: List<LibraryEntry> = emptyList(),
        val tba: List<LibraryEntry> = emptyList(),
        val mediaType: MediaType = MediaType.ANIME,
        val isRefreshing: Boolean = false,
        val searchQuery: String = "",
        val isSearchActive: Boolean = false,
        val searchResults: List<LibraryEntry> = emptyList(),
        val isSearching: Boolean = false,
        val searchFilters: SearchFilters = SearchFilters()
    ) : DiscoverUiState

    data class Error(val message: String) : DiscoverUiState
}

sealed interface DiscoverAction {
    data class OnMediaTypeChange(val type: MediaType) : DiscoverAction
    data object Refresh : DiscoverAction
    data class OnSearchQueryChange(val query: String) : DiscoverAction
    data class OnSearchActiveChange(val active: Boolean) : DiscoverAction
    data class OnSearch(val query: String) : DiscoverAction
    data class UpdateFilters(val filters: SearchFilters) : DiscoverAction
    data object ClearFilters : DiscoverAction
}