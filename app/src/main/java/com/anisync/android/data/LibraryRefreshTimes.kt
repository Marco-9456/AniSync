package com.anisync.android.data

import com.anisync.android.type.MediaType
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * When each account's lists last came down in full, for as long as the process lives.
 *
 * Kept in memory on purpose. A cold start should fetch them anyway, and an empty list has no rows
 * whose timestamps could say when it was last asked for.
 */
@Singleton
class LibraryRefreshTimes @Inject constructor() {

    private val times = ConcurrentHashMap<Pair<Int, MediaType>, Long>()

    fun markRefreshed(ownerId: Int, type: MediaType, at: Long = System.currentTimeMillis()) {
        times[ownerId to type] = at
    }

    fun lastRefreshedAt(ownerId: Int, type: MediaType): Long? = times[ownerId to type]
}
