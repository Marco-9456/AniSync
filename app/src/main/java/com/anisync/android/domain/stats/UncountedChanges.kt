package com.anisync.android.domain.stats

/**
 * How many list changes AniList's statistics have not counted yet. AniList recounts about every two
 * days, so the viewer's own lists can be ahead of it: each list that holds more titles than AniList
 * counted for it is that many changes ahead. A title moved between lists counts once, on the list
 * it moved to.
 */
fun uncountedChanges(onLists: Map<String, Int>, counted: Map<String, Int>): Int {
    val lists = onLists.foldRewatching()
    val stats = counted.foldRewatching()
    return lists.entries.sumOf { (status, count) -> (count - (stats[status] ?: 0)).coerceAtLeast(0) }
}

/** AniList's statistics count a title being rewatched or reread under Watching or Reading. */
private fun Map<String, Int>.foldRewatching(): Map<String, Int> {
    val repeating = this[REPEATING] ?: return this
    return (this - REPEATING) + (CURRENT to (this[CURRENT] ?: 0) + repeating)
}

private const val REPEATING = "REPEATING"
private const val CURRENT = "CURRENT"
