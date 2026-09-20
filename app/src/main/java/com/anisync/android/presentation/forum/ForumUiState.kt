package com.anisync.android.presentation.forum

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.anisync.android.R
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
 * Which body of threads the rail is showing. [OVERVIEW] is not a list of its own: it is a summary
 * of the others, so its sections each carry a button that switches to the feed they preview.
 *
 * Every feed is browsable by category and searchable.
 */
enum class ForumFeed(
    @param:StringRes val labelRes: Int,
    /** Shorter label for the pinned rail button, where the row budget is tight. */
    @param:StringRes val shortLabelRes: Int,
    val icon: ImageVector
) {
    OVERVIEW(R.string.forum_feed_overview, R.string.forum_feed_overview_short, Icons.AutoMirrored.Filled.ViewList),
    RECENT(R.string.forum_feed_recent, R.string.forum_feed_recent_short, Icons.Default.Schedule),
    NEW(R.string.forum_feed_new, R.string.forum_feed_new_short, Icons.Default.NewReleases),
    SUBSCRIBED(R.string.forum_feed_subscribed, R.string.forum_feed_subscribed_short, Icons.Default.Notifications),
    SAVED(R.string.forum_feed_saved, R.string.forum_feed_saved_short, Icons.Default.Bookmark)
}

/**
 * The ordering a feed starts on. Recent and New are orderings as much as they are lists, so
 * picking one seeds the sort control rather than leaving it pointing somewhere else. It is a
 * starting point, not a lock, and it is what the search bar compares against to decide whether
 * the sort icon reads as active.
 */
val ForumFeed.defaultSort: ThreadSortOption
    get() = if (this == ForumFeed.NEW) ThreadSortOption.NEWEST else ThreadSortOption.Default

/** The three previews the Overview stacks under its pinned section. */
enum class OverviewSection(@param:StringRes val titleRes: Int, val opens: ForumFeed) {
    RECENTLY_ACTIVE(R.string.forum_section_recently_active, ForumFeed.RECENT),
    RELEASE_DISCUSSION(R.string.forum_section_release_discussion, ForumFeed.RECENT),
    NEWLY_CREATED(R.string.forum_section_newly_created, ForumFeed.NEW)
}

@Immutable
data class ForumUiState(
    // --- Hub state ---
    val isLoading: Boolean = true,
    val isPaginating: Boolean = false,
    val isRefreshing: Boolean = false,
    val threads: ImmutableList<ForumThread> = persistentListOf(),
    val hasNextPage: Boolean = false,
    val currentPage: Int = 1,
    val feed: ForumFeed = ForumFeed.OVERVIEW,
    val selectedCategoryId: Int? = null,
    val savedThreadIds: ImmutableSet<Int> = persistentSetOf(),
    /** Bumped when the Forum tab is reselected, asking the list to scroll back to the top. */
    val scrollToTopRequest: Long = 0L,
    /** Bumped when the tab is double-tapped, asking the search bar to expand. */
    val searchOverlayRequest: Long = 0L,
    val errorMessage: String? = null,

    // --- Overview substate: one short list per section ---
    val overviewPinned: ImmutableList<ForumThread> = persistentListOf(),
    val overviewRecent: ImmutableList<ForumThread> = persistentListOf(),
    val overviewRelease: ImmutableList<ForumThread> = persistentListOf(),
    val overviewNew: ImmutableList<ForumThread> = persistentListOf(),

    /** Pinned threads are collapsed by default — AniList keeps several stickied at all times. */
    val isPinnedExpanded: Boolean = false,

    /** The Overview's sections, in the viewer's order, and the ones switched off. */
    val overviewOrder: List<OverviewSection> = OverviewSection.entries,
    val hiddenOverviewSections: Set<OverviewSection> = emptySet(),
    val isReorderSheetVisible: Boolean = false,

    /** Ordering and structured narrowing applied to the hub list. */
    val hubFilters: ForumSearchFilters = ForumSearchFilters(),

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

    /** Sticky threads lead the flat feeds too, so they are split out there as well. */
    val pinnedThreads: List<ForumThread>
        get() = when (feed) {
            ForumFeed.OVERVIEW -> overviewPinned
            ForumFeed.RECENT, ForumFeed.NEW -> if (hubFilterCount == 0) {
                threads.filter { it.isSticky }
            } else {
                emptyList()
            }

            else -> emptyList()
        }

    val unpinnedThreads: List<ForumThread>
        get() = if (pinnedThreads.isEmpty()) threads else threads.filterNot { it.isSticky }

    val showsPinnedSection: Boolean get() = pinnedThreads.isNotEmpty()

    /** The sections that actually render, in order. */
    val visibleOverviewSections: List<OverviewSection>
        get() = overviewOrder.filterNot { it in hiddenOverviewSections }

    fun overviewThreads(section: OverviewSection): ImmutableList<ForumThread> = when (section) {
        OverviewSection.RECENTLY_ACTIVE -> overviewRecent
        OverviewSection.RELEASE_DISCUSSION -> overviewRelease
        OverviewSection.NEWLY_CREATED -> overviewNew
    }
}

/** The sheets the hub can put over itself. */
enum class ForumSheet { SORT_AND_FILTER, THREAD_ACTIONS, FEED_PICKER }

sealed interface ForumAction {
    data object Refresh : ForumAction
    data object LoadMore : ForumAction
    data class OnFeedChange(val feed: ForumFeed) : ForumAction
    data class OnCategoryChange(val categoryId: Int?) : ForumAction
    data object TogglePinnedExpanded : ForumAction
    data class ToggleSaveThread(val thread: ForumThread) : ForumAction
    data class ToggleSubscribeThread(val thread: ForumThread) : ForumAction
    data class OnThreadClick(val threadId: Int, val threadTitle: String) : ForumAction
    data object OnCreateThreadClick : ForumAction
    data class OnCategoryClick(val category: ForumCategory) : ForumAction

    // --- Hub ordering and narrowing ---
    data class OpenSheet(val sheet: ForumSheet) : ForumAction
    data object DismissSheet : ForumAction
    data class OpenThreadActions(val thread: ForumThread) : ForumAction
    data class OnHubSortChange(val sort: ThreadSortOption) : ForumAction
    data class OnHubCategoryChange(val category: ForumCategory?) : ForumAction
    data object ToggleHubSubscribedOnly : ForumAction
    data object ResetHubFilters : ForumAction
    data object ApplyHubFilters : ForumAction

    // --- Overview section order ---
    data object OpenReorderSections : ForumAction
    data object DismissReorderSections : ForumAction
    data class ReorderOverview(val order: List<OverviewSection>) : ForumAction
    data class SetOverviewSectionHidden(
        val section: OverviewSection,
        val visible: Boolean
    ) : ForumAction
    data object ResetOverviewOrder : ForumAction

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
