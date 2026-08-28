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
     * Apply one field to a whole selection in a single request.
     *
     * Backed by AniList's `UpdateMediaListEntries(ids:)`, which takes **media list entry ids**
     * ([LibraryEntry.id]) rather than media ids, and which accepts only the fields that make sense
     * for every entry at once. Progress and dates are deliberately absent: the same progress value
     * across different titles is meaningless.
     */
    suspend fun bulkUpdateEntries(
        entryIds: List<Int>,
        status: LibraryStatus? = null,
        score: Double? = null,
        isPrivate: Boolean? = null
    ): Result<Unit>

    /**
     * Add every entry to [listName].
     *
     * One `SaveMediaListEntry` per entry, because `UpdateMediaListEntries` has no `customLists`
     * argument. [onProgress] reports entries committed so far; cancelling the calling coroutine
     * stops the run and leaves the already-committed ones in place.
     */
    suspend fun bulkAddToCustomList(
        entries: List<LibraryEntry>,
        listName: String,
        onProgress: (Int) -> Unit
    ): Result<Int>

    /**
     * Remove every entry from the library.
     *
     * One `DeleteMediaListEntry` per entry — AniList exposes no bulk delete — so this is paced by
     * the client token bucket and can take minutes for a large selection. Each success is committed
     * locally as it lands, so a cancel or a failure leaves Room and AniList agreeing on the part
     * that finished.
     */
    suspend fun bulkDeleteEntries(
        entries: List<LibraryEntry>,
        onProgress: (Int) -> Unit
    ): Result<Int>

    /**
     * Delete a custom list from AniList.
     */
    suspend fun deleteCustomList(customList: String, type: MediaType): Result<Unit>

    /**
     * Create a new custom list on AniList via UpdateUser.
     */
    suspend fun createCustomList(customList: String, type: MediaType): Result<Unit>
}
