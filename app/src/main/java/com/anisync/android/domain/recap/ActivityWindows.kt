package com.anisync.android.domain.recap

/** A span of activity in epoch seconds, [from] inclusive and [to] exclusive. */
data class ActivityWindow(val from: Long, val to: Long) {

    /** AniList's `createdAt_greater` is strict, so the bound sits one second before [from]. */
    val createdAfter: Long get() = from - 1

    /** AniList's `createdAt_lesser` is strict too, which already excludes [to]. */
    val createdBefore: Long get() = to

    /** Two halves that together cover this window, for when one window holds too many pages. */
    fun split(): Pair<ActivityWindow, ActivityWindow> {
        val middle = from + (to - from) / 2
        return ActivityWindow(from, middle) to ActivityWindow(middle, to)
    }

    val canSplit: Boolean get() = to - from > 1
}

/** Activities are read 50 to a page. */
const val ACTIVITY_PAGE_SIZE = 50

/** AniList refuses any page past its 5000 entry depth limit, which is page 100 at 50 a page. */
const val MAX_ACTIVITY_PAGES = 5000 / ACTIVITY_PAGE_SIZE

/**
 * True when a window still has pages after the last one AniList will serve, so it has to be read
 * again as two smaller windows.
 */
fun mustSplit(pagesRead: Int, hasNextPage: Boolean): Boolean =
    hasNextPage && pagesRead >= MAX_ACTIVITY_PAGES
