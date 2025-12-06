package com.anisync.android.domain

import com.anisync.android.type.MediaType
import javax.inject.Inject

/**
 * This UseCase is deprecated and kept for legacy reasons. 
 * SearchViewModel now calls the repository directly.
 */
class SearchMediaUseCase @Inject constructor(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(query: String, type: MediaType = MediaType.ANIME): List<LibraryEntry> {
        return repository.searchMedia(query, type)
    }
}
