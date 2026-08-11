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
     * Observe which media the active account already has on a list, keyed by media id.
     *
     * Backed by the same local cache as [observeLibrary], so browsing surfaces can mark known
     * titles without asking the API for list data they were not going to fetch.
     */
    fun observeListStatuses(): Flow<Map<Int, LibraryStatus>>

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
     * Update progress ONLY in local storage.
     * Used for immediate UI/Widget updates before network sync.
     */
    suspend fun updateProgressLocal(mediaId: Int, progress: Int): Result<Unit>

    /**
     * Update an entire entry (score, status, notes, etc).
     */
    suspend fun updateEntry(entry: LibraryEntry): Result<Unit>

    /**
     * Delete an entry from the library.
     */
    suspend fun deleteEntry(entryId: Int, mediaId: Int): Result<Unit>

    /**
     * Delete a custom list from AniList.
     */
    suspend fun deleteCustomList(customList: String, type: MediaType): Result<Unit>

    /**
     * Create a new custom list on AniList via UpdateUser.
     */
    suspend fun createCustomList(customList: String, type: MediaType): Result<Unit>
}
