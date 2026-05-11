package com.anisync.android.domain

import com.anisync.android.type.MediaType

interface SearchRepository {
    /**
     * Search media. When [countOnly] is true the response carries only the
     * total result count and an empty `entries` list — used to drive the
     * live "Show N results" preview in advanced search without paying for
     * the full media payload on every keystroke.
     */
    suspend fun searchMedia(
        query: String,
        type: MediaType,
        filters: SearchFilters = SearchFilters(),
        page: Int = 1,
        perPage: Int = 20,
        countOnly: Boolean = false
    ): Result<SearchPage>

    suspend fun searchAll(query: String): Result<GroupedSearchResults>

    /** AniList genre vocabulary. Cached aggressively — changes rarely. */
    suspend fun getGenres(): Result<List<String>>

    /** Full AniList tag taxonomy (200+ entries, includes adult tags). */
    suspend fun getTags(): Result<List<MediaTag>>
}
