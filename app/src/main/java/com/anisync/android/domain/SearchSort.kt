package com.anisync.android.domain

import androidx.compose.runtime.Immutable

/**
 * The columns a Discover search can be ordered by. Every column sorts both ways except
 * [BEST_MATCH], which only means something while a query is typed and has no direction.
 *
 * [EPISODES] and [DURATION] are the anime half of a pair whose manga half is [CHAPTERS] and
 * [VOLUMES], so a search that spans both types orders each half by its own length.
 */
enum class SearchSortColumn(val naturallyDescending: Boolean) {
    BEST_MATCH(true),
    POPULARITY(true),
    SCORE(true),
    TRENDING(true),
    FAVOURITES(true),
    RELEASE_DATE(true),
    UPDATED(true),
    TITLE(false),
    EPISODES(true),
    DURATION(true),
    CHAPTERS(true),
    VOLUMES(true);

    val hasDirection: Boolean get() = this != BEST_MATCH
}

/** A column and which way it runs. Picking a column starts it in its natural direction. */
@Immutable
data class SearchSort(
    val column: SearchSortColumn = SearchSortColumn.POPULARITY,
    val descending: Boolean = column.naturallyDescending
)
