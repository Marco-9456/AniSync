package com.anisync.android.domain

import kotlinx.coroutines.flow.Flow

interface SupportersRepository {
    /** Everyone who has left a tip, newest first, starting from the list bundled with the build. */
    fun observeSupporters(): Flow<List<Supporter>>

    /** Fetches the current list from the repository, at most once a day, and stays quiet on failure. */
    suspend fun refreshSupporters()
}
