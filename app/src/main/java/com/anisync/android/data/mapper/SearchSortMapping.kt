package com.anisync.android.data.mapper

import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.SearchSort
import com.anisync.android.domain.SearchSortColumn
import com.anisync.android.type.MediaSort
import com.anisync.android.type.MediaType

/**
 * The AniList sort for one media half of a search.
 *
 * Best match needs a search string to match against, so with nothing typed it falls back to the
 * default order rather than asking AniList to rank by relevance to nothing. A length column reads
 * as episodes and runtime on anime and as chapters and volumes on manga, and the title sorts in
 * the language the viewer reads titles in.
 */
fun SearchSort.toMediaSort(type: MediaType, titleLanguage: TitleLanguage, hasQuery: Boolean): MediaSort {
    val manga = type == MediaType.MANGA
    fun pick(up: MediaSort, down: MediaSort) = if (descending) down else up
    return when (column) {
        SearchSortColumn.BEST_MATCH ->
            if (hasQuery) MediaSort.SEARCH_MATCH else SearchSort().toMediaSort(type, titleLanguage, false)
        SearchSortColumn.POPULARITY -> pick(MediaSort.POPULARITY, MediaSort.POPULARITY_DESC)
        SearchSortColumn.SCORE -> pick(MediaSort.SCORE, MediaSort.SCORE_DESC)
        SearchSortColumn.TRENDING -> pick(MediaSort.TRENDING, MediaSort.TRENDING_DESC)
        SearchSortColumn.FAVOURITES -> pick(MediaSort.FAVOURITES, MediaSort.FAVOURITES_DESC)
        SearchSortColumn.RELEASE_DATE -> pick(MediaSort.START_DATE, MediaSort.START_DATE_DESC)
        SearchSortColumn.UPDATED -> pick(MediaSort.UPDATED_AT, MediaSort.UPDATED_AT_DESC)
        SearchSortColumn.TITLE -> when (titleLanguage) {
            TitleLanguage.ROMAJI -> pick(MediaSort.TITLE_ROMAJI, MediaSort.TITLE_ROMAJI_DESC)
            TitleLanguage.ENGLISH -> pick(MediaSort.TITLE_ENGLISH, MediaSort.TITLE_ENGLISH_DESC)
            TitleLanguage.NATIVE -> pick(MediaSort.TITLE_NATIVE, MediaSort.TITLE_NATIVE_DESC)
        }
        SearchSortColumn.EPISODES, SearchSortColumn.CHAPTERS ->
            if (manga) pick(MediaSort.CHAPTERS, MediaSort.CHAPTERS_DESC)
            else pick(MediaSort.EPISODES, MediaSort.EPISODES_DESC)
        SearchSortColumn.DURATION, SearchSortColumn.VOLUMES ->
            if (manga) pick(MediaSort.VOLUMES, MediaSort.VOLUMES_DESC)
            else pick(MediaSort.DURATION, MediaSort.DURATION_DESC)
    }
}
