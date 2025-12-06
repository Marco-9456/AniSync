package com.anisync.android.domain

import com.anisync.android.type.MediaType

interface DiscoverRepository {
    suspend fun getTrending(type: MediaType): List<LibraryEntry>
    suspend fun getPopular(type: MediaType): List<LibraryEntry>
    suspend fun getUpcoming(type: MediaType): List<LibraryEntry>
}
