package com.anisync.android.domain

import com.anisync.android.type.MediaType
import kotlinx.coroutines.flow.Flow

interface DetailsRepository {
    /**
     * Observe media details from local cache (reactive).
     */
    fun observeMediaDetails(id: Int): Flow<MediaDetails?>

    /**
     * Fetch fresh media details from network and update cache.
     */
    suspend fun refreshMediaDetails(id: Int): Result<Unit>

    /**
     * Update media list entry (status, progress).
     */
    suspend fun updateMediaListEntry(mediaId: Int, status: LibraryStatus, progress: Int): Result<Unit>

    /**
     * Delete media list entry.
     * @param entryId The list entry ID to delete from the API
     * @param mediaId The media ID to remove from local library cache
     */
    suspend fun deleteMediaListEntry(entryId: Int, mediaId: Int): Result<Unit>

    /**
     * Toggle favorite status for a media.
     */
    suspend fun toggleFavourite(mediaId: Int, mediaType: MediaType): Result<Boolean>

    suspend fun getCharacterDetails(id: Int): Result<CharacterDetails>
}

