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
 * Which body of threads the rail is showing. The shipped screen had five "feeds" that mixed two
 * different questions: Overview/Recent/New were orderings of the same public list, while
 * Subscribed/Saved were personal collections. The ordering moved to [ForumUiState.sort] and the
 * collections became the two chips of [YOURS], so the pinned rail toggle answers one question the
 * way Library's does.
 */
enum class ForumScope { BROWSE, YOURS }

/** The two collections under [ForumScope.YOURS]. */
enum class YoursTab { SUBSCRIBED, SAVED }

@Immutable
data class ForumUiState(
    // --- Hub (browse) state ---
    val isLoading: Boolean = true,
    val isPaginating: Boolean = false,
    val isRefreshing: Boolean = false,
    val threads: ImmutableList<ForumThread> = persistentListOf(),
    val hasNextPage: Boolean = false,
    val currentPage: Int = 1,
    val scope: ForumScope = ForumScope.BROWSE,
    val yoursTab: YoursTab = YoursTab.SUBSCRIBED,
    val selectedCategoryId: Int? = null,
    val savedThreadIds: ImmutableSet<Int> = persistentSetOf(),
    val errorMessage: String? = null,

    /** Ordering and the structured narrowing applied to the hub list. */
    val hubFilters: ForumSearchFilters = ForumSearchFilters(),

    /** Pinned threads are collapsed by default — AniList keeps several stickied at all times. */
    val isPinnedExpanded: Boolean = false,

    /** Open sheet over the hub, or null. */
    val openSheet: ForumSheet? = null,

    /** The thread whose action sheet is open, or null. */
    val actionSheetThread: ForumThread? = null,

    // --- Advanced search overlay state (independent of the hub list) ---
    val searchFilters: ForumSearchFilters = ForumSearchFilters(),
    val searchResults: ImmutableList<ForumThread> = persistentListOf(),
    val isSearching: Boolean = false,
    val searchHasNextPage: Boolean = false,
    val searchCurrentPage: Int = 1,
    val searchIsPaginating: Boolean = false,
    val searchError: String? = null,

    /** Threads shown on the search screen before anything is typed. */
    val trendingThreads: ImmutableList<ForumThread> = persistentListOf(),

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
) {
    /** Sticky threads lead the Browse list; they are split out so they can be collapsed. */
    val pinnedThreads: List<ForumThread>
        get() = if (showsPinnedSection) threads.filter { it.isSticky } else emptyList()

    val unpinnedThreads: List<ForumThread>
        get() = if (showsPinnedSection) threads.filterNot { it.isSticky } else threads

    /**
     * Pinned threads only lead the unfiltered Browse list. Once a category or a structured filter
     * narrows the list, a "Pinned" heading would be describing something the viewer did not ask for.
     */
    val showsPinnedSection: Boolean
        get() = scope == ForumScope.BROWSE &&
                hubFilterCount == 0 &&
                threads.any { it.isSticky }

    /**
     * How many structured filters narrow the hub list. The category lives in
     * [selectedCategoryId] because the rail owns it, so it is counted here rather than through
     * [ForumSearchFilters.activeCount]. Ordering is not a filter and is not counted.
     */
    val hubFilterCount: Int
        get() = listOf(
            selectedCategoryId != null,
            hubFilters.media != null,
            hubFilters.author != null,
            hubFilters.subscribedOnly
        ).count { it }

    /** True when the hub needs the search endpoint rather than the plain overview query. */
    val hubNeedsSearch: Boolean
        get() = selectedCategoryId != null ||
                hubFilters.media != null ||
                hubFilters.author != null ||
                hubFilters.subscribedOnly
}

/** The sheets the hub can put over itself. */
enum class ForumSheet { SORT_AND_FILTER, THREAD_ACTIONS }

sealed interface ForumAction {
    data object Refresh : ForumAction
    data object LoadMore : ForumAction
    data class OnScopeChange(val scope: ForumScope) : ForumAction
    data class OnYoursTabChange(val tab: YoursTab) : ForumAction
    data class OnCategoryChange(val categoryId: Int?) : ForumAction
    data object TogglePinnedExpanded : ForumAction
    data class ToggleSaveThread(val thread: ForumThread) : ForumAction
    data class ToggleSubscribeThread(val thread: ForumThread) : ForumAction
    data class OnThreadClick(val threadId: Int, val threadTitle: String) : ForumAction
    data object OnCreateThreadClick : ForumAction
    data class OnCategoryClick(val category: ForumCategory) : ForumAction

    // --- Hub ordering and narrowing (the Sort & filter sheet) ---
    data class OpenSheet(val sheet: ForumSheet) : ForumAction
    data object DismissSheet : ForumAction
    data class OpenThreadActions(val thread: ForumThread) : ForumAction
    data class OnHubSortChange(val sort: ThreadSortOption) : ForumAction
    data class OnHubCategoryChange(val category: ForumCategory?) : ForumAction
    data object ToggleHubSubscribedOnly : ForumAction
    data object ResetHubFilters : ForumAction
    data object ApplyHubFilters : ForumAction

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
