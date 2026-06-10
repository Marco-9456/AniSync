package com.anisync.android.presentation.forum

import androidx.compose.runtime.Immutable
import com.anisync.android.domain.ForumCategory
import com.anisync.android.domain.ForumSearchFilters
import com.anisync.android.domain.ForumThread
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.SearchResult
import com.anisync.android.domain.ThreadSortOption
import com.anisync.android.type.MediaType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * Feed modes matching AniList's forum hub tabs.
 */
enum class ForumFeed(val label: String) {
    OVERVIEW("Overview"),
    RECENT("Recent"),
    NEW("New"),
    SUBSCRIBED("Subscribed"),
    SAVED("Saved")
}

@Immutable
data class ForumUiState(
    // --- Hub (browse) state ---
    val isLoading: Boolean = true,
    val isPaginating: Boolean = false,
    val isRefreshing: Boolean = false,
    val threads: ImmutableList<ForumThread> = persistentListOf(),
    val hasNextPage: Boolean = false,
    val currentPage: Int = 1,
    val selectedFeed: ForumFeed = ForumFeed.OVERVIEW,
    val selectedCategoryId: Int? = null,
    val savedThreadIds: ImmutableSet<Int> = persistentSetOf(),
    val errorMessage: String? = null,

    // --- Advanced search overlay state (independent of the hub list) ---
    val searchFilters: ForumSearchFilters = ForumSearchFilters(),
    val searchResults: ImmutableList<ForumThread> = persistentListOf(),
    val isSearching: Boolean = false,
    val searchHasNextPage: Boolean = false,
    val searchCurrentPage: Int = 1,
    val searchIsPaginating: Boolean = false,
    val searchError: String? = null,

    // --- Media picker substate (the Media filter sheet) ---
    val mediaPickerType: MediaType = MediaType.ANIME,
    val mediaPickerQuery: String = "",
    val mediaPickerResults: ImmutableList<LibraryEntry> = persistentListOf(),
    val isMediaPickerSearching: Boolean = false,

    // --- Author picker substate (the Author filter sheet) ---
    val authorPickerQuery: String = "",
    val authorPickerResults: ImmutableList<SearchResult.UserResult> = persistentListOf(),
    val isAuthorPickerSearching: Boolean = false,

    /** Shared error for the picker sheets. */
    val pickerError: String? = null
)

sealed interface ForumAction {
    data object Refresh : ForumAction
    data object LoadMore : ForumAction
    data class OnFeedChange(val feed: ForumFeed) : ForumAction
    data class OnCategoryChange(val categoryId: Int?) : ForumAction
    data class ToggleSaveThread(val thread: ForumThread) : ForumAction
    data class ToggleSubscribeThread(val thread: ForumThread) : ForumAction
    data class OnThreadClick(val threadId: Int, val threadTitle: String) : ForumAction
    data object OnCreateThreadClick : ForumAction
    data class OnCategoryClick(val category: ForumCategory) : ForumAction

    // --- Advanced search ---
    /** Text typed into the search bar; debounced into a thread search. */
    data class OnSearchQueryChange(val query: String) : ForumAction
    data object LoadMoreSearch : ForumAction
    data object ClearSearchFilters : ForumAction
    data class OnSortChange(val sort: ThreadSortOption) : ForumAction
    data class OnCategoryFilterChange(val category: ForumCategory?) : ForumAction
    data object ToggleSubscribedOnly : ForumAction

    // Media filter
    data class OnMediaPickerQueryChange(val query: String) : ForumAction
    data class OnMediaPickerTypeChange(val type: MediaType) : ForumAction
    data class SelectMediaFilter(val entry: LibraryEntry) : ForumAction
    data object ClearMediaFilter : ForumAction

    // Author filter
    data class OnAuthorPickerQueryChange(val query: String) : ForumAction
    data class SelectAuthorFilter(val user: SearchResult.UserResult) : ForumAction
    data object ClearAuthorFilter : ForumAction

    /** Start a thread pre-attached to the given media (navigation). */
    data class OnCreateThreadForMedia(
        val mediaId: Int,
        val mediaTitle: String,
        val mediaCoverUrl: String?
    ) : ForumAction
}
