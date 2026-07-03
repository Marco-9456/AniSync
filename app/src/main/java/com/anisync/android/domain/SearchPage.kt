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

/**
 * Combined result of a single [SearchRepository.searchEverything] request. Carries
 * the anime + manga media pages and the non-media entity buckets together so the
 * universal search overlay renders from one round-trip instead of three. Buckets
 * not requested for a given search (via the `want*` gates) come back empty.
 */
@Immutable
data class SearchEverythingResult(
    val anime: List<LibraryEntry> = emptyList(),
    val manga: List<LibraryEntry> = emptyList(),
    val animeHasNextPage: Boolean = false,
    val mangaHasNextPage: Boolean = false,
    val charactersHasNextPage: Boolean = false,
    val staffHasNextPage: Boolean = false,
    val usersHasNextPage: Boolean = false,
    val studiosHasNextPage: Boolean = false,
    val grouped: GroupedSearchResults = GroupedSearchResults()
)
