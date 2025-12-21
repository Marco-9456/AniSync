package com.anisync.android.domain

interface PreferencesRepository {
    suspend fun getLastNotifiedId(): Int
    suspend fun setLastNotifiedId(id: Int)
}
