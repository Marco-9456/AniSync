package com.anisync.android.presentation.library

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.presentation.util.LIBRARY_ALL_TAB_ID
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaType

/**
 * The filters the library can apply on top of a list.
 *
 * Every field here is answerable from data already cached on [LibraryEntry], which is the whole
 * constraint: a filter that needed a network round trip would be a different feature.
 */
@Immutable
data class LibraryFilters(
    val formats: Set<MediaFormat> = emptySet(),
    /** Raw AniList `MediaStatus` names — RELEASING, FINISHED, NOT_YET_RELEASED, … */
    val airingStatuses: Set<String> = emptySet(),
    val genres: Set<String> = emptySet()
) {
    val activeCount: Int get() = formats.size + airingStatuses.size + genres.size
    val isEmpty: Boolean get() = activeCount == 0

    fun matches(entry: LibraryEntry): Boolean {
        if (formats.isNotEmpty() && entry.format !in formats) return false
        if (airingStatuses.isNotEmpty() && entry.mediaStatus !in airingStatuses) return false
        if (genres.isNotEmpty() && entry.genres.none { it in genres }) return false
        return true
    }

    companion object {
        val None = LibraryFilters()
    }
}

/** Which of the two batch operations that cost one request per entry is running. */
enum class BulkKind { ADD_TO_LIST, REMOVE }

/**
 * Progress of a running batch.
 *
 * Only the per-entry operations report progress. Status, score and private are a single
 * `UpdateMediaListEntries` call and complete without a dialog.
 */
@Immutable
data class BulkOperation(
    val kind: BulkKind,
    val done: Int,
    val total: Int,
    /** The custom list being filled, for [BulkKind.ADD_TO_LIST]. */
    val listName: String? = null
)

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
    /** The viewer's advanced scoring categories for the open tab; empty when the feature is off. */
    val advancedScoringCategories: List<String> = emptyList(),
    val customListEntries: Map<String, List<LibraryEntry>> = emptyMap(),
    val favoriteEntries: List<LibraryEntry> = emptyList(),
    val hiddenListNames: Set<String> = emptySet(),
    val tabOrder: List<String> = emptyList(),
    /** Raw entry count per tab id (incl. [LIBRARY_ALL_TAB_ID]); unaffected by [searchQuery]. */
    val tabCounts: Map<String, Int> = emptyMap(),
    /**
     * Per-tab counts before [filters] are applied.
     *
     * An empty tab has two possible causes once filtering exists, and only the difference between
     * these two maps tells them apart.
     */
    val unfilteredTabCounts: Map<String, Int> = emptyMap(),
    /** Flat list of all entries matching [searchQuery] (across every status list). */
    val searchMatches: List<LibraryEntry> = emptyList(),
    /** Query matches grouped by tab id (status ids, favorites, custom names); non-empty only. */
    val searchMatchesByCategory: Map<String, List<LibraryEntry>> = emptyMap(),
    /** The search category chip currently selected; [LIBRARY_ALL_TAB_ID] shows everything. */
    val activeSearchCategory: String = LIBRARY_ALL_TAB_ID,
    val showPrivateEntries: Boolean = true,
    val showScoreOnCards: Boolean = true,
    /** Poster grid (true) or single-column rows. One choice for every list, not one per list. */
    val isGridView: Boolean = true,
    val filters: LibraryFilters = LibraryFilters.None,
    /** Every genre present in the loaded library, sorted, for the filter sheet. */
    val availableGenres: List<String> = emptyList(),
    /** Every format present in the loaded library, in declaration order, for the filter sheet. */
    val availableFormats: List<MediaFormat> = emptyList(),
    /** Every AniList media status present in the loaded library, for the filter sheet. */
    val availableAiringStatuses: List<String> = emptyList(),
    /**
     * Media list entry ids ([LibraryEntry.id]) in the current selection.
     *
     * Entry ids, not media ids: `UpdateMediaListEntries` and `DeleteMediaListEntry` both key off
     * the list entry, while the +1 path keys off the media.
     */
    val selectedEntryIds: Set<Int> = emptySet(),
    /** The tab the selection was started in. Selection never spans lists. */
    val selectionTabId: String? = null,
    val bulkOperation: BulkOperation? = null,
    val initialTabId: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val isSelectionMode: Boolean get() = selectionTabId != null
}

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

    data class SetFilters(val filters: LibraryFilters) : LibraryAction
    data object ClearFilters : LibraryAction

    /** Long-press on a row. Starts selection in [tabId] with [entryId] already ticked. */
    data class EnterSelection(val entryId: Int, val tabId: String) : LibraryAction
    data class ToggleSelection(val entryId: Int) : LibraryAction
    data class SelectAll(val entryIds: List<Int>) : LibraryAction
    data object ClearSelection : LibraryAction

    /** One `UpdateMediaListEntries` call, whatever the selection size. */
    data class BulkSetStatus(val status: LibraryStatus) : LibraryAction
    data class BulkSetScore(val score: Double) : LibraryAction
    data class BulkSetPrivate(val isPrivate: Boolean) : LibraryAction

    /** One request per entry. Reports progress and can be cancelled. */
    data class BulkAddToCustomList(val listName: String) : LibraryAction
    data object BulkRemove : LibraryAction
    data object CancelBulkOperation : LibraryAction
}
