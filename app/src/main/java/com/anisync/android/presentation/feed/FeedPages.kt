package com.anisync.android.presentation.feed

import com.anisync.android.domain.UserActivity

/**
 * Lays a fresh first page over the activity already loaded, keeping the pages below it.
 *
 * AniList pages by number, so new activity pushes the end of the old first page down into page two.
 * The page joins [loaded] at the last activity the two share. Whatever [loaded] holds above that
 * point and the page lacks was deleted or filtered out since, and everything below it stays.
 *
 * Null when the two share nothing, which means more is new than one page holds. Joining them would
 * skip whatever came in between.
 */
fun mergeFirstPage(loaded: List<UserActivity>, firstPage: List<UserActivity>): List<UserActivity>? {
    if (loaded.isEmpty()) return firstPage
    if (firstPage.isEmpty()) return loaded
    val loadedIds = loaded.mapTo(HashSet()) { it.id }
    val seam = firstPage.lastOrNull { it.id in loadedIds } ?: return null
    val pageIds = firstPage.mapTo(HashSet()) { it.id }
    val below = loaded.drop(loaded.indexOfFirst { it.id == seam.id } + 1)
    return firstPage + below.filterNot { it.id in pageIds }
}
