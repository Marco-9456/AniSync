package com.anisync.android.presentation.discover

import androidx.compose.runtime.Stable
import com.anisync.android.data.DiscoverViewMode
import com.anisync.android.domain.GroupedSearchResults
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.MediaReview
import com.anisync.android.domain.SearchFilters
import com.anisync.android.type.MediaType

/**
 * Category buckets shown above search results. The "All" entry always renders;
 * other entries appear only when their underlying list is non-empty, so the
 * user can't tap into a section that returned nothing.
 */
enum class ResultCategory { ALL, ANIME, MANGA, CHARACTERS, STAFF, USERS, STUDIOS }

/**
 * Pagination bookkeeping for the universal search. Media buckets page
 * independently; the four entity buckets ride one shared request (and thus one
 * shared page counter), each keeping its own hasNext flag.
 */
@Stable
data class SearchPaging(
    val animePage: Int = 1,
    val animeHasNext: Boolean = false,
    val mangaPage: Int = 1,
    val mangaHasNext: Boolean = false,
    val entitiesPage: Int = 1,
    val charactersHasNext: Boolean = false,
    val staffHasNext: Boolean = false,
    val usersHasNext: Boolean = false,
    val studiosHasNext: Boolean = false,
    val isLoadingMore: Boolean = false
) {
    fun hasNextFor(category: ResultCategory): Boolean = when (category) {
        ResultCategory.ANIME -> animeHasNext
        ResultCategory.MANGA -> mangaHasNext
        ResultCategory.CHARACTERS -> charactersHasNext
        ResultCategory.STAFF -> staffHasNext
        ResultCategory.USERS -> usersHasNext
        ResultCategory.STUDIOS -> studiosHasNext
        ResultCategory.ALL -> false
    }
}

sealed interface DiscoverUiState {
    data object Loading : DiscoverUiState

    @Stable
    data class Success(
        val trending: List<LibraryEntry> = emptyList(),
        val popular: List<LibraryEntry> = emptyList(),
        val upcoming: List<LibraryEntry> = emptyList(),
        val newlyAdded: List<LibraryEntry> = emptyList(),
        val tba: List<LibraryEntry> = emptyList(),
        // Short "Recent Reviews" strip fetched alongside the media feed.
        val recentReviews: List<MediaReview> = emptyList(),
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
        val activeCategory: ResultCategory = ResultCategory.ALL,
        val searchPaging: SearchPaging = SearchPaging(),
        /**
         * Monotonic counter bumped when an external screen asks Discover to open its
         * search overlay with preset filters (see DiscoverSearchLauncher). The screen
         * reacts to changes by clearing the query field and expanding the search bar.
         */
        val searchOverlayRequest: Long = 0L
    ) : DiscoverUiState

    data class Error(val message: String) : DiscoverUiState
}

sealed interface DiscoverAction {
    data class OnMediaTypeChange(val type: MediaType) : DiscoverAction
    data object Refresh : DiscoverAction
    data class OnSearchQueryChange(val query: String) : DiscoverAction
    data class OnSearchActiveChange(val active: Boolean) : DiscoverAction
    data class OnSearch(val query: String) : DiscoverAction
    data object LoadMoreResults : DiscoverAction
    data class UpdateFilters(val filters: SearchFilters) : DiscoverAction
    data object ClearFilters : DiscoverAction
    data object LoadTaxonomy : DiscoverAction
    data class OnViewModeChange(val mode: DiscoverViewMode) : DiscoverAction
    data class OnCategoryChange(val category: ResultCategory) : DiscoverAction
}
