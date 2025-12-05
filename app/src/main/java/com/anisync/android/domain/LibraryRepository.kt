package com.anisync.android.domain

import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    suspend fun getLibrary(username: String): List<LibraryEntry>
}
