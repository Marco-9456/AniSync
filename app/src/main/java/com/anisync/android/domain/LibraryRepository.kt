package com.anisync.android.domain

import com.anisync.android.type.MediaType
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    /**
     * Observe library entries from local cache (SSOT).
     * Emits new list whenever data changes.
     */
    fun observeLibrary(username: String, type: MediaType): Flow<List<LibraryEntry>>

    /**
     * Trigger a network refresh. 
     * Fetches from API and updates local cache.
     * Returns Result to indicate success/failure for UI feedback.
     */
    suspend fun refreshLibrary(username: String, type: MediaType): Result<Unit>

    /**
     * Update progress locally (optimistic) and sync to network.
     */
    suspend fun updateProgress(mediaId: Int, progress: Int): Result<Unit>

    /**
     * Update an entire entry (score, status, notes, etc).
     */
    suspend fun updateEntry(entry: LibraryEntry): Result<Unit>

    /**
     * Delete an entry from the library.
     */
    suspend fun deleteEntry(entryId: Int, mediaId: Int): Result<Unit>
}
