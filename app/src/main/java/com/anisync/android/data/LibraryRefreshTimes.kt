package com.anisync.android.data

import com.anisync.android.type.MediaType
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * When each account's lists last came down in full, and when single entries were last checked
 * against AniList, for as long as the process lives.
 *
 * Kept in memory on purpose. A cold start should fetch them anyway, and an empty list has no rows
 * whose timestamps could say when it was last asked for. A check that finds an entry unchanged
 * writes nothing to its row either, since the row's `lastUpdated` orders the Watch Progress widget.
 */
@Singleton
class LibraryRefreshTimes @Inject constructor() {

    private val times = ConcurrentHashMap<Pair<Int, MediaType>, Long>()
    private val entryChecks = ConcurrentHashMap<Pair<Int, Int>, Long>()

    fun markRefreshed(ownerId: Int, type: MediaType, at: Long = System.currentTimeMillis()) {
        times[ownerId to type] = at
    }

    fun lastRefreshedAt(ownerId: Int, type: MediaType): Long? = times[ownerId to type]

    fun markEntryChecked(ownerId: Int, mediaId: Int, at: Long = System.currentTimeMillis()) {
        entryChecks[ownerId to mediaId] = at
    }

    fun entryCheckedAt(ownerId: Int, mediaId: Int): Long? = entryChecks[ownerId to mediaId]
}
