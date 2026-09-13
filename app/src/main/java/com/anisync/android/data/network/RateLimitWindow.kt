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
 * Not thread safe on its own. [RateLimitGate] owns an instance and touches it under its mutex,
 * except [onIssued] which is called from the admission path and uses its own counter.
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

    /** Requests we believe are still available right now. */
    fun headroom(): Int = (remaining - issuedSinceObserved).coerceAtLeast(0)

    /**
     * Milliseconds left on a 429 timeout, or 0 when not blocked.
     *
     * The unset check is not decoration. Subtracting a clock reading from [Long.MIN_VALUE]
     * underflows and wraps to a huge positive number, which reads as "blocked for 292 million
     * years" the moment the clock leaves zero.
     */
    fun blockedForMs(): Long {
        if (blockedUntilMs == Long.MIN_VALUE) return 0
        return (blockedUntilMs - clock.nowMs()).coerceAtLeast(0)
    }

    /**
     * Milliseconds until the window is expected to roll over and the budget refills.
     *
     * Falls back to a full window when no reset edge has been observed yet, which is the
     * conservative direction: it makes a caller wait longer rather than fire too early.
     */
    fun resetsInMs(): Long {
        if (windowStartedAtMs == Long.MIN_VALUE) return WINDOW_MS
        val elapsed = clock.nowMs() - windowStartedAtMs
        return (WINDOW_MS - elapsed).coerceIn(0, WINDOW_MS)
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
    }
}
