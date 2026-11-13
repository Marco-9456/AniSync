package com.anisync.android.domain.recap

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * How far a first build has got. [fraction] runs from 0 to 1 over the whole build, and [readBackTo]
 * is the oldest month read while the list history is still coming in.
 */
data class RecapBuildProgress(val fraction: Float, val readBackTo: YearMonth?)

/** The list history is most of a first build, the title lookups and airing times share the rest. */
private const val HISTORY_SHARE = 0.8f
private const val TITLES_SHARE = 0.1f

/**
 * Turns a first build's requests into [RecapBuildProgress]. Pages come newest first, so the history
 * read so far is the stretch from [now] back to the oldest activity seen, out of the whole window.
 * [now] is in epoch seconds.
 */
class RecapBuildTracker(
    private val window: ActivityWindow,
    private val now: Long,
    private val zone: ZoneId,
    private val report: suspend (RecapBuildProgress) -> Unit,
) {
    private var oldest = Long.MAX_VALUE

    suspend fun onPage(page: FetchedPage) {
        oldest = minOf(oldest, page.activities.minOfOrNull { it.createdAt } ?: return)
        val end = minOf(now, window.to)
        val span = (end - window.from).coerceAtLeast(1)
        val read = (end - oldest).coerceIn(0, span)
        val month = YearMonth.from(Instant.ofEpochSecond(oldest).atZone(zone))
        report(RecapBuildProgress(HISTORY_SHARE * read / span, month))
    }

    suspend fun onTitles(done: Int, batches: Int) =
        report(RecapBuildProgress(HISTORY_SHARE + TITLES_SHARE * share(done, batches), null))

    suspend fun onAiring(done: Int, batches: Int) =
        report(RecapBuildProgress(HISTORY_SHARE + TITLES_SHARE + (1f - HISTORY_SHARE - TITLES_SHARE) * share(done, batches), null))

    private fun share(done: Int, batches: Int): Float = if (batches <= 0) 1f else done.toFloat() / batches
}
