package com.anisync.android.data.fetcher

import android.os.SystemClock

/**
 * Monotonic time source. Indirection lets tests inject a fake clock without
 * touching [SystemClock] or `kotlinx.coroutines.test`. Production callers
 * should use [Clock.System].
 */
fun interface Clock {
    fun nowMs(): Long

    companion object {
        val System: Clock = Clock { SystemClock.elapsedRealtime() }
    }
}

/**
 * Mutable clock for tests. Advance manually via [advanceBy].
 */
class FakeClock(initialMs: Long = 0L) : Clock {
    private var current: Long = initialMs
    override fun nowMs(): Long = current
    fun advanceBy(ms: Long) { current += ms }
    fun setTo(ms: Long) { current = ms }
}
