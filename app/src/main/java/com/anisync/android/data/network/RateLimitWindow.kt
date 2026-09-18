package com.anisync.android.data.network

import com.anisync.android.data.util.Clock

/**
 * What the server has told us about the current rate limit window, plus what we have spent since
 * it last told us.
 *
 * AniList sends `X-RateLimit-Limit` and `X-RateLimit-Remaining` on every response, but
 * `X-RateLimit-Reset` only on a 429. So the window boundary has to be inferred: `remaining`
 * decreases monotonically within a window, and the instant it jumps back up is the instant a new
 * window began. That self-calibrates after one window with no guessing about clock alignment.
 *
 * `remaining` is authoritative but always one round trip stale, so [headroom] subtracts the
 * requests issued since it was observed. Without that, a fan-out of parallel calls all read the
 * same pre-flight number and collectively overshoot.
 *
 * Not thread safe on its own. [RateLimitGate] owns an instance and every read and write of these
 * fields happens under its mutex. Anything that reports outside the lock goes through [snapshot].
 */
class RateLimitWindow(private val clock: Clock) {

    /** Requests a window allows. AniList advertises 30 while degraded, 90 otherwise. */
    var limit: Int = DEFAULT_LIMIT
        private set

    /** The server's own count, as of [observedAtMs]. */
    var remaining: Int = DEFAULT_LIMIT
        private set

    /** When [remaining] was read from a response header. */
    var observedAtMs: Long = Long.MIN_VALUE
        private set

    /** Requests admitted since [remaining] was observed. These are not reflected in it yet. */
    var issuedSinceObserved: Int = 0
        private set

    /** Best estimate of when the current window began, from the last observed reset edge. */
    var windowStartedAtMs: Long = Long.MIN_VALUE
        private set

    /** While in the future, the server has told us to stop entirely. Set by a 429. */
    var blockedUntilMs: Long = Long.MIN_VALUE
        private set

    /**
     * False while [remaining] is a guess rather than something a response said.
     *
     * A rollover and the end of a 429 timeout both restore the full budget without having been
     * told. Spending that on a burst is how a wrong guess turns into a wrong guess plus a fresh
     * timeout, so the gate lets a single request out and waits for its headers first.
     */
    var budgetConfirmed: Boolean = true
        private set

    /** Requests we believe are still available right now. */
    fun headroom(): Int = (remaining - issuedSinceObserved).coerceAtLeast(0)

    /**
     * An immutable read of the window.
     *
     * [RateLimitGate] mutates this under its mutex but reports outside it, from threads that never
     * take the lock. Publishing from one of these instead of from the live fields is what stops a
     * report mixing values from either side of a 429.
     */
    fun snapshot(): Snapshot = Snapshot(limit, headroom(), blockedUntilMs, windowStartedAtMs)

    /** @see snapshot */
    data class Snapshot(
        val limit: Int,
        val headroom: Int,
        val blockedUntilMs: Long,
        val windowStartedAtMs: Long,
    ) {
        /** @see RateLimitWindow.blockedForMs */
        fun blockedForMs(nowMs: Long): Long = blockedForMsAt(blockedUntilMs, nowMs)

        /** @see RateLimitWindow.resetsInMs */
        fun resetsInMs(nowMs: Long): Long = resetsInMsAt(windowStartedAtMs, nowMs)
    }

    /**
     * Milliseconds left on a 429 timeout, or 0 when not blocked.
     *
     * The unset check is not decoration. Subtracting a clock reading from [Long.MIN_VALUE]
     * underflows and wraps to a huge positive number, which reads as "blocked for 292 million
     * years" the moment the clock leaves zero.
     */
    fun blockedForMs(): Long = blockedForMsAt(blockedUntilMs, clock.nowMs())

    /**
     * Milliseconds until the window is expected to roll over and the budget refills.
     *
     * Falls back to a full window when no reset edge has been observed yet, which is the
     * conservative direction: it makes a caller wait longer rather than fire too early.
     */
    fun resetsInMs(): Long = resetsInMsAt(windowStartedAtMs, clock.nowMs())

    /**
     * Rolls the window over once its length has passed with no response to say so.
     *
     * [remaining] only moves when a response carries the header, and a response can only arrive if a
     * request goes out. Once the budget is spent that is circular: the gate holds every request back
     * waiting for a refill that needs a request to happen, [resetsInMs] decays to zero, and the wait
     * handed back is zero, so the caller spins instead of ever being admitted. [onRateLimited]
     * already guards the 429 path for exactly this reason. This is the same guard for a window that
     * simply ran out.
     *
     * The refill is a guess, not an observation, so [observedAtMs] is left alone and
     * [budgetConfirmed] is cleared. Our boundary is inferred and the server's is not obliged to
     * agree with it, so the whole budget is only safe to assume once a response has said so: the
     * gate lets one request out and holds the rest until its headers land. Spacing alone is not
     * that guarantee, because a queue drains at one request per gap and a round trip is longer.
     */
    fun rolloverIfElapsed() {
        if (blockedForMs() > 0) return
        if (windowStartedAtMs == Long.MIN_VALUE) return
        val now = clock.nowMs()
        if (now - windowStartedAtMs < WINDOW_MS) return
        windowStartedAtMs = now
        remaining = limit
        issuedSinceObserved = 0
        budgetConfirmed = false
    }

    /** Records that a request has been let through and will consume budget the server hasn't counted yet. */
    fun onIssued() {
        issuedSinceObserved++
    }

    /**
     * Folds a response's rate limit headers into the window.
     *
     * @param limitHeader `X-RateLimit-Limit`
     * @param remainingHeader `X-RateLimit-Remaining`
     */
    fun onHeaders(limitHeader: Int?, remainingHeader: Int?) {
        val now = clock.nowMs()
        if (limitHeader != null && limitHeader > 0) limit = limitHeader
        if (remainingHeader == null || remainingHeader < 0) return

        // A count that went up is the only signal AniList gives that the window rolled over.
        val hadObservation = observedAtMs != Long.MIN_VALUE
        if (!hadObservation || remainingHeader > remaining) {
            windowStartedAtMs = now
        }
        remaining = remainingHeader.coerceAtMost(limit)
        observedAtMs = now
        issuedSinceObserved = 0
        budgetConfirmed = true
    }

    /**
     * Applies a 429. The documented penalty is a one minute timeout, and `Retry-After` says how
     * much of it is left, so we trust the header and fall back to the documented default.
     *
     * The wait is capped: AniList should never ask for more than a minute, and a proxy returning
     * something absurd must not freeze the app for an hour.
     */
    fun onRateLimited(retryAfterSeconds: Long?) {
        val now = clock.nowMs()
        val wait = (retryAfterSeconds ?: DEFAULT_RETRY_AFTER_SECONDS)
            .coerceIn(1L, MAX_RETRY_AFTER_SECONDS)
        blockedUntilMs = now + wait * 1000L
        observedAtMs = now
        issuedSinceObserved = 0
        // The timeout is the window. Nothing may be sent until it ends, and when it does the budget
        // is fresh, so the count is restored rather than zeroed. Leaving it at zero would deadlock:
        // no request could go out, so no response could ever arrive to say the window had reset.
        remaining = limit
        budgetConfirmed = false
        windowStartedAtMs = blockedUntilMs
    }

    /**
     * Restores a 429 timeout that outlived the process.
     *
     * Without this, force-stopping the app during a timeout and reopening it starts a fresh gate
     * that walks straight back into the limiter.
     */
    fun restoreBlockedFor(remainingMs: Long) {
        if (remainingMs <= 0) return
        val now = clock.nowMs()
        blockedUntilMs = now + remainingMs.coerceAtMost(MAX_RETRY_AFTER_SECONDS * 1000L)
        remaining = limit
        issuedSinceObserved = 0
        budgetConfirmed = false
        windowStartedAtMs = blockedUntilMs
    }

    companion object {
        /** AniList's degraded ceiling. Corrected by the first response that carries a limit header. */
        const val DEFAULT_LIMIT = 30

        /** The documented window length. */
        const val WINDOW_MS = 60_000L

        /** AniList documents a one minute timeout when a 429 arrives without `Retry-After`. */
        const val DEFAULT_RETRY_AFTER_SECONDS = 60L

        /** Nothing legitimate asks for longer than this. */
        const val MAX_RETRY_AFTER_SECONDS = 120L

        /**
         * Shared by the live window and by [Snapshot] so a published reading and the gate's own
         * cannot drift apart.
         *
         * The unset check is not decoration. Subtracting a clock reading from [Long.MIN_VALUE]
         * underflows and wraps to a huge positive number, which reads as "blocked for 292 million
         * years" the moment the clock leaves zero.
         */
        private fun blockedForMsAt(blockedUntilMs: Long, nowMs: Long): Long {
            if (blockedUntilMs == Long.MIN_VALUE) return 0
            return (blockedUntilMs - nowMs).coerceAtLeast(0)
        }

        /** @see blockedForMsAt */
        private fun resetsInMsAt(windowStartedAtMs: Long, nowMs: Long): Long {
            if (windowStartedAtMs == Long.MIN_VALUE) return WINDOW_MS
            return (WINDOW_MS - (nowMs - windowStartedAtMs)).coerceIn(0, WINDOW_MS)
        }
    }
}
