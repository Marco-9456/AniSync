package com.anisync.android.domain

interface DetailsRepository {
    suspend fun getMediaDetails(id: Int): MediaDetails?
    suspend fun updateMediaListEntry(mediaId: Int, status: LibraryStatus, progress: Int): Boolean
}
