package com.anisync.android.presentation.discover.state

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

sealed interface DiscoverEvent {
    data object OnScreenVisible : DiscoverEvent
    data class OnMediaTypeChange(val type: MediaType) : DiscoverEvent
    data object Refresh : DiscoverEvent
    data class OnSearchQueryChange(val query: String) : DiscoverEvent
    data class OnSearchActiveChange(val active: Boolean) : DiscoverEvent
    data class OnSearch(val query: String) : DiscoverEvent
    data class UpdateFilters(val filters: SearchFilters) : DiscoverEvent
    data object ClearFilters : DiscoverEvent
}
