package com.anisync.android.domain

import com.anisync.android.type.MediaType

interface LibraryRepository {
    suspend fun getLibrary(username: String, type: MediaType = MediaType.ANIME): List<LibraryEntry>
    suspend fun updateProgress(mediaId: Int, progress: Int): Boolean
}
