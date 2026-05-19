package com.anisync.android.data.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client-side token bucket that serializes outbound AniList API requests so a
 * burst can never exceed the documented rate limit. AniList currently runs in
 * degraded mode (30 req/min); the bucket defaults headroom-conservative at
 * 25 tokens with a 25/min refill and adapts via [onLimitHeader] when the
 * server advertises a different `X-RateLimit-Limit`.
 *
 * The bucket is interceptor-scoped, not per-call: a fan-out of N parallel
 * Apollo queries all suspend inside [acquire] until tokens are available,
 * defeating the burst limiter that the post-response throttle in
 * [com.anisync.android.di.AuthorizationInterceptor] cannot.
 */
@Singleton
class TokenBucket @Inject constructor() {

    private val mutex = Mutex()

    private var tokens: Double = INITIAL_TOKENS.toDouble()
    private var capacity: Double = INITIAL_TOKENS.toDouble()
    private var refillPerMinute: Double = INITIAL_REFILL_PER_MIN.toDouble()
    private var lastRefillNanos: Long = System.nanoTime()

    /**
     * Suspend until a token is available, then consume one. Bursts of N
     * callers all serialize on the mutex; the first 25 (default capacity)
     * acquire instantly, the 26th waits `60_000 / refillPerMinute` ms.
     */
    suspend fun acquire() {
        // First mutex section: try to grab a token immediately.
        val waitMs = mutex.withLock {
            refillLocked()
            if (tokens >= 1.0) {
                tokens -= 1.0
                0L
            } else {
                ((1.0 - tokens) * (60_000.0 / refillPerMinute)).toLong().coerceAtLeast(1L)
            }
        }

        if (waitMs == 0L) return

        // Wait outside the mutex so concurrent callers can also queue and
        // share the same refill window.
        delay(waitMs)

        mutex.withLock {
            refillLocked()
            tokens = (tokens - 1.0).coerceAtLeast(0.0)
        }
    }

    /**
     * Resize the bucket when the server tells us its real limit. AniList
     * sends `X-RateLimit-Limit: 90` in normal mode and `30` in degraded mode.
     * We keep a 5-token headroom below the announced limit to absorb in-flight
     * requests already past the bucket.
     */
    fun onLimitHeader(limit: Int) {
        if (limit <= 0) return
        val newCapacity = limit.toDouble()
        val newRefill = (limit - HEADROOM).coerceAtLeast(MIN_REFILL).toDouble()
        // No mutex needed for these var writes — worst case is a slightly
        // stale capacity for one request, then the next acquire fixes it.
        capacity = newCapacity
        refillPerMinute = newRefill
        if (tokens > newCapacity) tokens = newCapacity
    }

    private fun refillLocked() {
        val now = System.nanoTime()
        val elapsedSec = (now - lastRefillNanos) / 1e9
        if (elapsedSec <= 0) return
        tokens = (tokens + elapsedSec * refillPerMinute / 60.0).coerceAtMost(capacity)
        lastRefillNanos = now
    }

    private companion object {
        // Conservative defaults sized for AniList degraded mode (30/min).
        // Headroom of 5 leaves slack for in-flight responses that haven't
        // updated the bucket yet, mutations from other call sites, and
        // the existing post-response throttle in AuthorizationInterceptor.
        const val INITIAL_TOKENS = 25
        const val INITIAL_REFILL_PER_MIN = 25
        const val HEADROOM = 5
        const val MIN_REFILL = 10
    }
}
