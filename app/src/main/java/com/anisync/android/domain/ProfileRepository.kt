package com.anisync.android.domain

interface ProfileRepository {
    suspend fun getProfile(username: String): UserProfile?
}
