package com.anisync.android.presentation.library

import androidx.compose.runtime.Stable
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.presentation.util.LIBRARY_ALL_TAB_ID
import com.anisync.android.type.MediaType

@Stable
data class LibraryUiState(
    val mediaType: MediaType = MediaType.ANIME,
    val sortOption: LibrarySort = LibrarySort.AIRING_SOON,
    val isAscending: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    val userScoreFormat: ScoreFormat = ScoreFormat.POINT_100,
    /** Every visible entry across all status lists, sorted — feeds the synthetic "All" tab. */
    val entries: List<LibraryEntry> = emptyList(),
    val groupedEntries: Map<LibraryStatus, List<LibraryEntry>> = emptyMap(),
    val customListNames: List<String> = emptyList(),
    val customListEntries: Map<String, List<LibraryEntry>> = emptyMap(),
    val favoriteEntries: List<LibraryEntry> = emptyList(),
    val hiddenListNames: Set<String> = emptySet(),
    val tabOrder: List<String> = emptyList(),
    /** Raw entry count per tab id (incl. [LIBRARY_ALL_TAB_ID]); unaffected by [searchQuery]. */
    val tabCounts: Map<String, Int> = emptyMap(),
    /** Flat list of all entries matching [searchQuery] (across every status list). */
    val searchMatches: List<LibraryEntry> = emptyList(),
    /** Query matches grouped by tab id (status ids, favorites, custom names); non-empty only. */
    val searchMatchesByCategory: Map<String, List<LibraryEntry>> = emptyMap(),
    /** The search category chip currently selected; [LIBRARY_ALL_TAB_ID] shows everything. */
    val activeSearchCategory: String = LIBRARY_ALL_TAB_ID,
    val showPrivateEntries: Boolean = true,
    val isGridView: Boolean = true,
    val initialTabId: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

enum class LibrarySort {
    TITLE,
    PROGRESS,
    AIRING_SOON,
    SCORE,
    LAST_UPDATED,
    LAST_ADDED,
    START_DATE,
    RELEASE_DATE
}

sealed interface LibraryAction {
    data object OnScreenVisible : LibraryAction
    data object Refresh : LibraryAction
    data class OnMediaTypeChange(val type: MediaType) : LibraryAction
    data class OnSortOptionChange(val sort: LibrarySort, val ascending: Boolean) : LibraryAction
    data class OnSearchQueryChange(val query: String) : LibraryAction
    data class OnSearchCategoryChange(val categoryId: String) : LibraryAction
    data class OnSearchOpened(val currentTabId: String) : LibraryAction
    data class IncrementProgress(val mediaId: Int) : LibraryAction
    data class DecrementProgress(val mediaId: Int) : LibraryAction
    data class UpdateEntry(val entry: LibraryEntry) : LibraryAction
    data class DeleteEntry(val entryId: Int, val mediaId: Int) : LibraryAction
    data class ToggleListVisibility(val listName: String, val hidden: Boolean) : LibraryAction
    data class ReorderTabs(val tabOrder: List<String>) : LibraryAction
    data class CreateCustomList(val listName: String, val type: MediaType) : LibraryAction
    data class DeleteCustomList(val listName: String) : LibraryAction
    data class TogglePrivateVisibility(val show: Boolean) : LibraryAction
    data class SetGridView(val isGrid: Boolean) : LibraryAction
    data class OnTabSelected(val tabId: String) : LibraryAction
    data object ConsumeInitialTab : LibraryAction
}
