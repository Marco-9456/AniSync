package com.anisync.android.domain

import kotlinx.serialization.Serializable

/**
 * Represents a forum category on AniList (e.g., Anime, Manga, General).
 */
@Serializable
data class ForumCategory(
    val id: Int,
    val name: String
)
