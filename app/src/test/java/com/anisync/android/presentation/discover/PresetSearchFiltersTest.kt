package com.anisync.android.presentation.discover

import com.anisync.android.domain.IntRangeFilter
import com.anisync.android.domain.ResultCategory
import com.anisync.android.domain.SearchFilters
import com.anisync.android.domain.SearchSort
import com.anisync.android.domain.SearchSortColumn
import com.anisync.android.presentation.navigation.PresetSearch
import com.anisync.android.type.MediaSeason
import com.anisync.android.type.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pins what a search page opened from a title's genre, tag or ranking searches for. */
class PresetSearchFiltersTest {

    @Test
    fun `a genre or a tag searches for that genre or tag alone`() {
        assertEquals(
            SearchFilters(genresIncluded = setOf("Comedy")),
            PresetSearch(mediaType = "ANIME", genre = "Comedy").toFilters()
        )
        assertEquals(
            SearchFilters(tagsIncluded = setOf("School")),
            PresetSearch(mediaType = "MANGA", tag = "School").toFilters()
        )
    }

    @Test
    fun `a seasonal ranking orders its season and year by the ranking's column`() {
        val filters = PresetSearch(mediaType = "ANIME", sort = "SCORE", season = "FALL", year = 2026).toFilters()

        assertEquals(SearchSort(SearchSortColumn.SCORE), filters.sort)
        assertEquals(MediaSeason.FALL, filters.season)
        assertEquals(IntRangeFilter(min = 2026, max = 2026), filters.yearRange)
    }

    @Test
    fun `a ranking of all time leaves the year and the season open`() {
        assertEquals(
            SearchFilters(sort = SearchSort(SearchSortColumn.SCORE)),
            PresetSearch(mediaType = "ANIME", sort = "SCORE").toFilters()
        )
    }

    @Test
    fun `a value the app does not know is left out rather than matching nothing`() {
        assertEquals(
            SearchFilters(),
            PresetSearch(mediaType = "ANIME", sort = "GONE", season = "MONSOON").toFilters()
        )
    }

    @Test
    fun `the results open on the title's own media type`() {
        assertEquals(MediaType.MANGA, PresetSearch(mediaType = "MANGA").resultType)
        assertEquals(ResultCategory.MANGA, PresetSearch(mediaType = "MANGA").category)
        assertEquals(MediaType.ANIME, PresetSearch(mediaType = "ANIME").resultType)
        assertEquals(ResultCategory.ANIME, PresetSearch(mediaType = "ANIME").category)
    }
}
