package com.anisync.android.presentation.discover

import com.anisync.android.domain.GroupedSearchResults
import com.anisync.android.domain.IntRangeFilter
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ResultCategory
import com.anisync.android.domain.SearchFilters
import com.anisync.android.domain.SearchResult
import com.anisync.android.domain.SearchSort
import com.anisync.android.domain.SearchSortColumn
import com.anisync.android.presentation.discover.components.label
import com.anisync.android.presentation.discover.components.on
import com.anisync.android.presentation.discover.components.sortColumns
import com.anisync.android.type.MediaFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins the rules Discover search's rail, filter bar and sort sheet are built on. */
class SearchFiltersUiTest {

    private fun entry(id: Int) = LibraryEntry(
        id = 0,
        mediaId = id,
        titleRomaji = "Title $id",
        titleEnglish = null,
        titleNative = null,
        titleUserPreferred = "Title $id",
        coverUrl = null,
        progress = 0,
        totalEpisodes = null,
        totalChapters = null,
        totalVolumes = null,
        type = null,
        status = LibraryStatus.UNKNOWN
    )

    @Test
    fun `a sort alone searches but is not a filter the clear chip clears`() {
        val sorted = SearchFilters(sort = SearchSort(SearchSortColumn.SCORE))
        assertTrue(sorted.hasActiveFilters)
        assertFalse(sorted.hasNarrowingFilters)

        val narrowed = SearchFilters(formats = setOf(MediaFormat.TV))
        assertTrue(narrowed.hasActiveFilters)
        assertTrue(narrowed.hasNarrowingFilters)

        assertFalse(SearchFilters().hasActiveFilters)
    }

    @Test
    fun `best match joins the columns only while a query is typed`() {
        assertTrue(SearchSortColumn.BEST_MATCH in sortColumns(ResultCategory.ALL, hasQuery = true))
        assertFalse(SearchSortColumn.BEST_MATCH in sortColumns(ResultCategory.ALL, hasQuery = false))
    }

    @Test
    fun `length columns follow the category and all leaves them out`() {
        val anime = sortColumns(ResultCategory.ANIME, hasQuery = false)
        assertEquals(listOf(SearchSortColumn.EPISODES, SearchSortColumn.DURATION), anime.takeLast(2))
        val manga = sortColumns(ResultCategory.MANGA, hasQuery = false)
        assertEquals(listOf(SearchSortColumn.CHAPTERS, SearchSortColumn.VOLUMES), manga.takeLast(2))
        val all = sortColumns(ResultCategory.ALL, hasQuery = false)
        assertEquals(7, all.size)
        assertTrue(all.none { it == SearchSortColumn.EPISODES || it == SearchSortColumn.CHAPTERS })
    }

    @Test
    fun `a length column is named the way the category reads it`() {
        assertEquals(SearchSortColumn.CHAPTERS, SearchSortColumn.EPISODES.on(ResultCategory.MANGA))
        assertEquals(SearchSortColumn.DURATION, SearchSortColumn.VOLUMES.on(ResultCategory.ANIME))
        assertEquals(SearchSortColumn.EPISODES, SearchSortColumn.EPISODES.on(ResultCategory.ALL))
        assertEquals(SearchSortColumn.SCORE, SearchSortColumn.SCORE.on(ResultCategory.MANGA))
    }

    @Test
    fun `year ranges read as a year, a span or an open end`() {
        assertEquals("2024", IntRangeFilter(2024, 2024).label())
        assertEquals("2020–2024", IntRangeFilter(2020, 2024).label())
        assertEquals("2020+", IntRangeFilter(min = 2020).label())
        assertEquals("≤2020", IntRangeFilter(max = 2020).label())
        assertNull(IntRangeFilter().label())
    }

    @Test
    fun `the rail lists all, every category with results, and the one a preset opened on`() {
        val state = DiscoverUiState(
            searchAnime = listOf(entry(1)),
            groupedResults = GroupedSearchResults(
                users = listOf(SearchResult.UserResult(id = 7, displayName = "fan", imageUrl = null))
            ),
            activeCategory = ResultCategory.STUDIOS
        )
        assertEquals(
            listOf(ResultCategory.ALL, ResultCategory.ANIME, ResultCategory.USERS, ResultCategory.STUDIOS),
            state.railCategories
        )
    }

    @Test
    fun `the footer counts the titles in the category on screen`() {
        val totals = SearchTotals(anime = 120, manga = 30)
        assertEquals(150, DiscoverUiState(searchTotals = totals).matchingTitles)
        assertEquals(120, DiscoverUiState(searchTotals = totals, activeCategory = ResultCategory.ANIME).matchingTitles)
        assertEquals(30, DiscoverUiState(searchTotals = totals, activeCategory = ResultCategory.MANGA).matchingTitles)
    }
}
