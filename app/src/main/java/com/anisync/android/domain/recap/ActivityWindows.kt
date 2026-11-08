package com.anisync.android.domain.recap

/** A span of activity in epoch seconds, [from] inclusive and [to] exclusive. */
data class ActivityWindow(val from: Long, val to: Long) {

    /** AniList's `createdAt_greater` is strict, so the bound sits one second before [from]. */
    val createdAfter: Long get() = from - 1

    /** AniList's `createdAt_lesser` is strict too, which already excludes [to]. */
    val createdBefore: Long get() = to

    /**
     * The part of this window up to and including [createdAt], or null when that would not make it
     * any smaller.
     */
    fun endingAt(createdAt: Long): ActivityWindow? =
        ActivityWindow(from, createdAt + 1).takeIf { it.to < to && it.to > from }
}

/** Activities are read 50 to a page. */
const val ACTIVITY_PAGE_SIZE = 50

/** AniList refuses any page past its 5000 entry depth limit, which is page 100 at 50 a page. */
const val MAX_ACTIVITY_PAGES = 5000 / ACTIVITY_PAGE_SIZE

/**
 * True when a window still has pages after the last one AniList will serve, so the rest has to be
 * read as a window of its own.
 */
fun mustSplit(pagesRead: Int, hasNextPage: Boolean): Boolean =
    hasNextPage && pagesRead >= MAX_ACTIVITY_PAGES

/** A list activity as the activity query returns it, before its title says anime or manga. */
data class FetchedActivity(
    val id: Int,
    val mediaId: Int,
    val status: String?,
    val progress: String?,
    /** Epoch seconds. */
    val createdAt: Long,
)

/** One page of a window, newest first. */
data class FetchedPage(val activities: List<FetchedActivity>, val hasNextPage: Boolean)

/**
 * Reads every activity in [window], one page at a time.
 *
 * Pages come newest first, so when page [MAX_ACTIVITY_PAGES] still has more behind it, what is left
 * is everything logged up to the oldest activity read so far. Reading carries on in that narrower
 * window from its first page. Activities from the second the cut falls on come back twice and are
 * kept once.
 */
suspend fun readActivityWindow(
    window: ActivityWindow,
    fetchPage: suspend (window: ActivityWindow, page: Int) -> FetchedPage,
): List<FetchedActivity> {
    val read = LinkedHashMap<Int, FetchedActivity>()
    var current = window
    var page = 1
    var oldest = Long.MAX_VALUE
    while (true) {
        val result = fetchPage(current, page)
        for (activity in result.activities) {
            read.putIfAbsent(activity.id, activity)
            oldest = minOf(oldest, activity.createdAt)
        }
        if (!result.hasNextPage || result.activities.isEmpty()) break
        if (!mustSplit(page, result.hasNextPage)) {
            page++
            continue
        }
        current = current.endingAt(oldest) ?: break
        page = 1
        oldest = Long.MAX_VALUE
    }
    return read.values.toList()
}
