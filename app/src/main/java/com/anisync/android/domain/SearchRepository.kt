package com.anisync.android.domain

import com.anisync.android.type.MediaType

/**
 * Repository interface for media search operations.
 * Provides methods to search anime and manga with optional filters.
 */
interface SearchRepository {
    /**
     * Search for anime or manga by query string with optional filters.
     * @param query Search term to look for
     * @param type Media type (ANIME or MANGA)
     * @param filters Optional search filters (genre, year, status, etc.)
     * @return List of matching media entries or error
     */
    suspend fun searchMedia(
        query: String,
        type: MediaType,
        filters: SearchFilters = SearchFilters()
    ): Result<List<LibraryEntry>>

    /**
     * Search for characters, staff, users, and studios.
     */
    suspend fun searchAll(query: String): Result<GroupedSearchResults>
}
