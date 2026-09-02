package com.anisync.android.presentation.discover

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.anisync.android.data.DiscoverViewMode
import com.anisync.android.domain.AiringEpisode
import com.anisync.android.domain.DiscoverSection
import com.anisync.android.domain.GroupedSearchResults
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.MediaReview
import com.anisync.android.domain.SearchFilters
import com.anisync.android.presentation.discover.components.BrowseChip
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

/**
 * One rail's own load state.
 *
 * Discover used to gate the whole screen on all five media requests succeeding, so a single failed
 * list replaced everything with one centred error. Each rail now carries its own state and a
 * failure hides that rail alone.
 */
@Immutable
data class SectionFeed<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    /** True once the rail has finished and has nothing worth drawing. */
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()

    /** The rail finished and failed, which is what earns an inline message instead of silence. */
    val hasFailed: Boolean get() = !isLoading && error != null
}

/**
 * Every rail's contents. Keyed by concrete field rather than a map so the screen reads each one
 * with its own element type.
 */
@Stable
data class DiscoverFeeds(
    val trending: SectionFeed<LibraryEntry> = SectionFeed(),
    val airingToday: SectionFeed<AiringEpisode> = SectionFeed(),
    val releasing: SectionFeed<LibraryEntry> = SectionFeed(),
    val popular: SectionFeed<LibraryEntry> = SectionFeed(),
    val notYetReleased: SectionFeed<LibraryEntry> = SectionFeed(),
    val newlyAdded: SectionFeed<LibraryEntry> = SectionFeed(),
    val reviews: SectionFeed<MediaReview> = SectionFeed()
) {
    /** Nothing has come back yet, which is the only time the screen shows a shimmer. */
    val isInitialLoad: Boolean
        get() = trending.isLoading && popular.isLoading && notYetReleased.isLoading &&
            newlyAdded.isLoading

    fun of(section: DiscoverSection): SectionFeed<*> = when (section) {
        DiscoverSection.TRENDING -> trending
        DiscoverSection.AIRING_TODAY -> airingToday
        DiscoverSection.RELEASING_NOW -> releasing
        DiscoverSection.POPULAR -> popular
        DiscoverSection.NOT_YET_RELEASED -> notYetReleased
        DiscoverSection.NEWLY_ADDED -> newlyAdded
        DiscoverSection.REVIEWS -> reviews
    }

    fun markLoading(section: DiscoverSection): DiscoverFeeds = when (section) {
        DiscoverSection.TRENDING -> copy(trending = trending.retrying())
        DiscoverSection.AIRING_TODAY -> copy(airingToday = airingToday.retrying())
        DiscoverSection.RELEASING_NOW -> copy(releasing = releasing.retrying())
        DiscoverSection.POPULAR -> copy(popular = popular.retrying())
        DiscoverSection.NOT_YET_RELEASED -> copy(notYetReleased = notYetReleased.retrying())
        DiscoverSection.NEWLY_ADDED -> copy(newlyAdded = newlyAdded.retrying())
        DiscoverSection.REVIEWS -> copy(reviews = reviews.retrying())
    }
}

private fun <T> SectionFeed<T>.retrying(): SectionFeed<T> = copy(isLoading = true, error = null)

@Stable
data class DiscoverUiState(
    val feeds: DiscoverFeeds = DiscoverFeeds(),
    val mediaType: MediaType = MediaType.ANIME,
    /**
     * The rails to draw, in the viewer's order and already stripped of hidden ones and of
     * anything the current tab cannot show.
     */
    val sectionOrder: List<DiscoverSection> = emptyList(),
    /** Rails switched off for this tab, kept so the reorder sheet can list them. */
    val hiddenSections: Set<DiscoverSection> = emptySet(),
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
) {
    /**
     * Nothing is left to draw and it is not because requests are still out. Distinguishing this
     * from "every section hidden" matters: one is a failure to report, the other is a choice the
     * viewer made and can reverse.
     */
    val hasNothingToShow: Boolean
        get() = !feeds.isInitialLoad && sectionOrder.none { feeds.of(it).items.isNotEmpty() }
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

    /** Reorder sheet: the full visible-plus-hidden order for the current tab. */
    data class ReorderSections(val order: List<DiscoverSection>) : DiscoverAction
    data class SetSectionVisible(val section: DiscoverSection, val visible: Boolean) : DiscoverAction
    data object ResetSectionOrder : DiscoverAction

    /** A browse chip in the header rail. Schedule is handled by the screen, not the viewmodel. */
    data class OnBrowseChip(val chip: BrowseChip) : DiscoverAction

    /** Spotlight's list button: put the title straight on Planning without leaving Discover. */
    data class AddToPlanning(val mediaId: Int) : DiscoverAction

    /** Re-run one rail's request, rather than the whole screen's. */
    data class RetrySection(val section: DiscoverSection) : DiscoverAction
}
