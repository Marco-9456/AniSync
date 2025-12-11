package com.anisync.android.domain

import com.anisync.android.type.MediaType

interface SearchRepository {
    suspend fun searchMedia(
        query: String, 
        type: MediaType,
        filters: SearchFilters = SearchFilters()
    ): Result<List<LibraryEntry>>
}
