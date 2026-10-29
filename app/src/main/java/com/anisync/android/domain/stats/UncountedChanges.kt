package com.anisync.android.domain.stats

/**
 * How many list changes AniList's statistics have not counted yet. AniList recounts about every two
 * days, so the viewer's own lists can be ahead of it: each list that holds more titles than AniList
 * counted for it is that many changes ahead. A title moved between lists counts once, on the list
 * it moved to.
 */
fun uncountedChanges(onLists: Map<String, Int>, counted: Map<String, Int>): Int =
    onLists.entries.sumOf { (status, count) -> (count - (counted[status] ?: 0)).coerceAtLeast(0) }
