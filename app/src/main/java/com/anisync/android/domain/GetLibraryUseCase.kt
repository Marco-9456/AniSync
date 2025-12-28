package com.anisync.android.domain

import com.anisync.android.type.MediaType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing library entries.
 * Returns a Flow that emits whenever the library data changes.
 */
class GetLibraryUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    operator fun invoke(username: String, type: MediaType): Flow<List<LibraryEntry>> {
        return repository.observeLibrary(username, type)
    }
}
