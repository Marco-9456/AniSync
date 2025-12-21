package com.anisync.android.domain

import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaType

interface DiscoverRepository {
    suspend fun getTrending(type: MediaType): Result<List<LibraryEntry>>
    suspend fun getPopular(type: MediaType): Result<List<LibraryEntry>>
    suspend fun getUpcoming(type: MediaType): Result<List<LibraryEntry>>
    suspend fun getTBA(type: MediaType): Result<List<LibraryEntry>>
    
    /**
     * Fetches paginated media for grid screens with optional format filtering.
     * @param sectionType One of: "trending", "popular", "upcoming", "tba"
     * @param type MediaType (ANIME or MANGA)
     * @param page Page number (1-indexed)
     * @param format Optional format filter
     */
    suspend fun getPaginatedSection(
        sectionType: String,
        type: MediaType,
        page: Int,
        format: MediaFormat? = null
    ): Result<PaginatedResult<LibraryEntry>>
}
