package com.anisync.android.domain

enum class FeedFilter { ALL, STATUS, LIST }

enum class FeedScope { GLOBAL, FOLLOWING }

enum class FeedMediaType { ANIME, MANGA }

data class FeedPage(
    val items: List<UserActivity>,
    val hasNextPage: Boolean,
    val currentPage: Int
)
