package com.anisync.android.presentation.discover

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.anisync.android.data.DiscoverViewMode
import com.anisync.android.domain.GroupedSearchResults
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.ResultCategory
import com.anisync.android.domain.SearchFilters
import com.anisync.android.presentation.discover.components.BrowseChip
import com.anisync.android.type.MediaType

/**
 * How many titles match in each media half, across every page. A category preview shows three of
 * them, and a filter sheet's footer says how many a filter leaves.
 */
@Immutable
data class SearchTotals(val anime: Int = 0, val manga: Int = 0)

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

@Stable
data class DiscoverSearchUiState(
    val searchQuery: String = "",
    /** What the results on screen were searched for. Typing waits for the search key. */
    val submittedQuery: String = "",
    val searchAnime: List<LibraryEntry> = emptyList(),
    val searchManga: List<LibraryEntry> = emptyList(),
    val groupedResults: GroupedSearchResults = GroupedSearchResults(),
    val searchTotals: SearchTotals = SearchTotals(),
    val isSearching: Boolean = false,
    val searchFilters: SearchFilters = SearchFilters(),
    val searchError: String? = null,
    val viewMode: DiscoverViewMode = DiscoverViewMode.LIST,
    val activeCategory: ResultCategory = ResultCategory.ALL,
    val searchPaging: SearchPaging = SearchPaging(),
    val recentSearches: List<String> = emptyList(),
    /**
     * Bumped each time a preset opens, from a browse chip or as a search page opens. The screen
     * clears the field and opens the search on it.
     */
    val presetRequest: Long = 0L,
    /**
     * Lists titles with nothing typed or filtered, where an empty search shows recent searches. A
     * page opened on a ranking does, since a ranking of all time by popularity asks for nothing
     * but the default order.
     */
    val listsWhenEmpty: Boolean = false
) {
    val hasSearchResults: Boolean
        get() = searchAnime.isNotEmpty() || searchManga.isNotEmpty() || !groupedResults.isEmpty

    /** Text typed since the last search, which runs once the search key is pressed. */
    val isTyping: Boolean
        get() = searchQuery.isNotEmpty() && searchQuery != submittedQuery

    /** The recent searches holding what is being typed, to pick one instead of finishing it. */
    val matchingRecentSearches: List<String>
        get() = searchQuery.trim().let { typed -> recentSearches.filter { it.contains(typed, ignoreCase = true) } }

    /**
     * The rail's categories: All, every category with results, and the one a preset opened on even
     * before its results arrive, so the rail already says what is being searched.
     */
    val railCategories: List<ResultCategory>
        get() = ResultCategory.entries.filter { category ->
            category == ResultCategory.ALL || category == activeCategory || when (category) {
                ResultCategory.ANIME -> searchAnime.isNotEmpty()
                ResultCategory.MANGA -> searchManga.isNotEmpty()
                ResultCategory.CHARACTERS -> groupedResults.characters.isNotEmpty()
                ResultCategory.STAFF -> groupedResults.staff.isNotEmpty()
                ResultCategory.USERS -> groupedResults.users.isNotEmpty()
                ResultCategory.STUDIOS -> groupedResults.studios.isNotEmpty()
                ResultCategory.ALL -> true
            }
        }

    /** Titles the filters leave in the category on screen, for a filter sheet's footer. */
    val matchingTitles: Int
        get() = when (activeCategory) {
            ResultCategory.ANIME -> searchTotals.anime
            ResultCategory.MANGA -> searchTotals.manga
            else -> searchTotals.anime + searchTotals.manga
        }
}

sealed interface DiscoverSearchAction {
    data class OnSearchQueryChange(val query: String) : DiscoverSearchAction
    data class OnSearch(val query: String) : DiscoverSearchAction
    data object LoadMoreResults : DiscoverSearchAction
    data class UpdateFilters(val filters: SearchFilters) : DiscoverSearchAction
    data object ClearFilters : DiscoverSearchAction
    data object LoadTaxonomy : DiscoverSearchAction
    data class OnViewModeChange(val mode: DiscoverViewMode) : DiscoverSearchAction
    data class OnCategoryChange(val category: ResultCategory) : DiscoverSearchAction

    data class RemoveRecentSearch(val query: String) : DiscoverSearchAction
    data object ClearRecentSearches : DiscoverSearchAction

    /**
     * A browse chip in Discover's header rail, read for the media type the tab shows. Schedule is
     * handled by the screen and never gets here.
     */
    data class OnBrowseChip(val chip: BrowseChip, val mediaType: MediaType) : DiscoverSearchAction

    /** A result's list button: put the title straight on Planning. */
    data class AddToPlanning(val mediaId: Int) : DiscoverSearchAction
}
