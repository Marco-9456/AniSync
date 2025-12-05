package com.anisync.android.domain

import javax.inject.Inject

class SearchMediaUseCase @Inject constructor(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(query: String): List<LibraryEntry> {
        return repository.searchMedia(query)
    }
}
