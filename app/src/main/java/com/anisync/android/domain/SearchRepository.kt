package com.anisync.android.domain

interface SearchRepository {
    suspend fun searchMedia(query: String): List<LibraryEntry>
}
