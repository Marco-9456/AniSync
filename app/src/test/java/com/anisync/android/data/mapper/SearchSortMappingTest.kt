package com.anisync.android.data.mapper

import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.SearchSort
import com.anisync.android.domain.SearchSortColumn
import com.anisync.android.type.MediaSort
import com.anisync.android.type.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins how a Discover sort column and direction reach AniList: each half of a search gets its own
 * sort, so a length column has to mean the right thing on anime and on manga alike.
 */
class SearchSortMappingTest {

    private fun SearchSort.on(type: MediaType, hasQuery: Boolean = true, language: TitleLanguage = TitleLanguage.ROMAJI) =
        toMediaSort(type, language, hasQuery)

    @Test
    fun `the default sort is popularity, most popular first`() {
        val sort = SearchSort()
        assertEquals(SearchSortColumn.POPULARITY, sort.column)
        assertTrue(sort.descending)
        assertEquals(MediaSort.POPULARITY_DESC, sort.on(MediaType.ANIME))
    }

    @Test
    fun `a picked column starts in its natural direction`() {
        assertTrue(SearchSort(SearchSortColumn.SCORE).descending)
        assertTrue(SearchSort(SearchSortColumn.RELEASE_DATE).descending)
        assertFalse(SearchSort(SearchSortColumn.TITLE).descending)
    }

    @Test
    fun `direction picks the plain or the descending sort`() {
        assertEquals(MediaSort.SCORE, SearchSort(SearchSortColumn.SCORE, descending = false).on(MediaType.ANIME))
        assertEquals(MediaSort.SCORE_DESC, SearchSort(SearchSortColumn.SCORE, descending = true).on(MediaType.ANIME))
        assertEquals(MediaSort.START_DATE, SearchSort(SearchSortColumn.RELEASE_DATE, false).on(MediaType.MANGA))
        assertEquals(MediaSort.UPDATED_AT_DESC, SearchSort(SearchSortColumn.UPDATED, true).on(MediaType.MANGA))
    }

    @Test
    fun `best match needs a query and falls back to the default without one`() {
        val sort = SearchSort(SearchSortColumn.BEST_MATCH)
        assertFalse(SearchSortColumn.BEST_MATCH.hasDirection)
        assertEquals(MediaSort.SEARCH_MATCH, sort.on(MediaType.ANIME, hasQuery = true))
        assertEquals(MediaSort.POPULARITY_DESC, sort.on(MediaType.ANIME, hasQuery = false))
    }

    @Test
    fun `title sorts in the language the viewer reads titles in`() {
        val sort = SearchSort(SearchSortColumn.TITLE)
        assertEquals(MediaSort.TITLE_ROMAJI, sort.on(MediaType.ANIME, language = TitleLanguage.ROMAJI))
        assertEquals(MediaSort.TITLE_ENGLISH, sort.on(MediaType.ANIME, language = TitleLanguage.ENGLISH))
        assertEquals(MediaSort.TITLE_NATIVE_DESC, sort.copy(descending = true).on(MediaType.MANGA, language = TitleLanguage.NATIVE))
    }

    @Test
    fun `a length column reads as episodes on anime and chapters on manga`() {
        val episodes = SearchSort(SearchSortColumn.EPISODES)
        assertEquals(MediaSort.EPISODES_DESC, episodes.on(MediaType.ANIME))
        assertEquals(MediaSort.CHAPTERS_DESC, episodes.on(MediaType.MANGA))

        val volumes = SearchSort(SearchSortColumn.VOLUMES, descending = false)
        assertEquals(MediaSort.DURATION, volumes.on(MediaType.ANIME))
        assertEquals(MediaSort.VOLUMES, volumes.on(MediaType.MANGA))
    }
}
