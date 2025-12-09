package com.anisync.android.domain

import javax.inject.Inject

class GetLibraryUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    suspend operator fun invoke(username: String): Result<List<LibraryEntry>> {
        return repository.getLibrary(username)
    }
}
