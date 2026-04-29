package com.anisync.android.domain

import kotlinx.serialization.Serializable

/**
 * Lightweight user reference used in lists where we only need an avatar,
 * a name, and a tap target for navigation (likes, followers, etc.).
 */
@Serializable
data class UserSummary(
    val id: Int,
    val name: String,
    val avatarUrl: String?
)
