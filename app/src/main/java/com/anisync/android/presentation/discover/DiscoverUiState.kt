package com.anisync.android.presentation.discover

import androidx.compose.runtime.Stable
import com.anisync.android.data.DiscoverViewMode
import com.anisync.android.domain.GroupedSearchResults
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.SearchFilters
import com.anisync.android.type.MediaType

/**
 * Category buckets shown above search results. The "All" entry always renders;
 * other entries appear only when their underlying list is non-empty, so the
 * user can't tap into a section that returned nothing.
 */
enum class ResultCategory { ALL, ANIME, MANGA, CHARACTERS, STAFF, USERS, STUDIOS }

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
        val searchAnime: List<LibraryEntry> = emptyList(),
        val searchManga: List<LibraryEntry> = emptyList(),
        val groupedResults: GroupedSearchResults = GroupedSearchResults(),
        val isSearching: Boolean = false,
        val searchFilters: SearchFilters = SearchFilters(),
        val searchError: String? = null,
        val viewMode: DiscoverViewMode = DiscoverViewMode.LIST,
        val activeCategory: ResultCategory = ResultCategory.ALL
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
    data object LoadTaxonomy : DiscoverAction
    data class OnViewModeChange(val mode: DiscoverViewMode) : DiscoverAction
    data class OnCategoryChange(val category: ResultCategory) : DiscoverAction
}
