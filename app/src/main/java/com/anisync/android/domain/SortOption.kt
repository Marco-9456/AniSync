package com.anisync.android.domain

import com.anisync.android.type.MediaSort

/**
 * User-facing sort options for advanced search. Each entry maps to one
 * AniList [MediaSort] value. Curated subset — full enum has 30+ entries
 * most users never reach for.
 */
enum class SortOption(val apiValue: MediaSort) {
    POPULARITY_DESC(MediaSort.POPULARITY_DESC),
    SCORE_DESC(MediaSort.SCORE_DESC),
    TRENDING_DESC(MediaSort.TRENDING_DESC),
    FAVOURITES_DESC(MediaSort.FAVOURITES_DESC),
    START_DATE_DESC(MediaSort.START_DATE_DESC),
    START_DATE(MediaSort.START_DATE),
    UPDATED_AT_DESC(MediaSort.UPDATED_AT_DESC),
    TITLE_ROMAJI(MediaSort.TITLE_ROMAJI),
    TITLE_ENGLISH(MediaSort.TITLE_ENGLISH),
    EPISODES_DESC(MediaSort.EPISODES_DESC),
    DURATION_DESC(MediaSort.DURATION_DESC),
    CHAPTERS_DESC(MediaSort.CHAPTERS_DESC),
    VOLUMES_DESC(MediaSort.VOLUMES_DESC)
}
