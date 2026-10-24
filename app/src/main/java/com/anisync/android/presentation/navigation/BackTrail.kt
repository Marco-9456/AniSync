package com.anisync.android.presentation.navigation

/**
 * A back stack entry reduced to what the back trail needs.
 *
 * @param entryId the entry's own id, so a row can pop back to that exact entry
 * @param key the destination and its id, such as `media:21`, so a page visited twice is listed once.
 *   Null for entries that are not pages, like the graph root.
 * @param isTab whether the entry is one of the five main tabs, where a trail ends
 */
data class TrailNode(val entryId: String, val key: String?, val isTab: Boolean = false)

/**
 * The pages behind the current one, newest first, and the tab under them. [tab] is null in a
 * two-pane detail, whose last row is the list the pane was opened from.
 */
data class BackTrail(val pages: List<TrailNode>, val tab: TrailNode?)

/**
 * The trail for the page on top of [stack], which runs from the bottom of the back stack to the
 * top. Null when fewer than two rows would show, so a long press keeps doing what a tap does.
 *
 * It walks down to the nearest tab and stops there, because a tab switch can leave another tab
 * underneath. [endsOnList] marks the stack of a two-pane detail, which holds no tab and ends on its
 * list instead.
 */
fun backTrailOf(stack: List<TrailNode>, endsOnList: Boolean = false): BackTrail? {
    val current = stack.lastOrNull() ?: return null
    val currentKey = current.key ?: return null
    if (current.isTab) return null

    val seen = mutableSetOf(currentKey)
    val pages = mutableListOf<TrailNode>()
    var tab: TrailNode? = null
    for (index in stack.lastIndex - 1 downTo 0) {
        val node = stack[index]
        if (node.isTab) {
            tab = node
            break
        }
        val key = node.key ?: continue
        if (seen.add(key)) pages += node
    }

    val rows = pages.size + if (tab != null || endsOnList) 1 else 0
    return if (rows >= 2) BackTrail(pages, tab) else null
}
