package com.anisync.android.domain

interface DetailsRepository {
    suspend fun getMediaDetails(id: Int): Result<MediaDetails>
    suspend fun updateMediaListEntry(mediaId: Int, status: LibraryStatus, progress: Int): Result<Unit>
    suspend fun deleteMediaListEntry(listEntryId: Int): Result<Unit>
}
