package com.anisync.android.domain

import androidx.compose.runtime.Immutable

/**
 * Result page for media search. `total` powers the live "Show N results"
 * count in advanced search; `entries` is empty when only a count was
 * requested (`countOnly`).
 */
@Immutable
data class SearchPage(
    val entries: List<LibraryEntry>,
    val total: Int,
    val hasNextPage: Boolean,
    val currentPage: Int
)
