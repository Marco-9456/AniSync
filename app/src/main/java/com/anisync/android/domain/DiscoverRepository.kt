package com.anisync.android.domain

interface DiscoverRepository {
    suspend fun getTrendingAnime(): List<LibraryEntry>
    suspend fun getPopularAnime(): List<LibraryEntry>
}
