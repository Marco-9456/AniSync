package com.anisync.android.domain

import com.anisync.android.type.MediaType

interface SearchRepository {
    suspend fun searchMedia(
        query: String,
        type: MediaType,
        filters: SearchFilters = SearchFilters(),
        page: Int = 1,
        perPage: Int = 20
    ): Result<SearchPage>

    suspend fun searchAll(query: String): Result<GroupedSearchResults>

    /**
     * Universal search in a single request. Collapses the former 3-request fan-out
     * (anime media + manga media + entity lookup) into one operation. The caller
     * passes which buckets it wants; unrequested buckets are skipped server-side
     * via @include and come back empty. Used by the discover search overlay.
     * Studios follow the other entities unless asked for on their own.
     */
    suspend fun searchEverything(
        query: String,
        filters: SearchFilters = SearchFilters(),
        page: Int = 1,
        perPage: Int = 20,
        wantAnime: Boolean,
        wantManga: Boolean,
        wantEntities: Boolean,
        wantStudios: Boolean = wantEntities
    ): Result<SearchEverythingResult>

    /** AniList genre vocabulary. Cached aggressively — changes rarely. */
    suspend fun getGenres(): Result<List<String>>

    /** Full AniList tag taxonomy (200+ entries, includes adult tags). */
    suspend fun getTags(): Result<List<MediaTag>>
}
