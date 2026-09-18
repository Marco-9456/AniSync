package com.anisync.android.data.network

import com.anisync.android.data.util.ApiError
import kotlin.random.Random

/** What to do after a failed attempt. */
sealed interface RetryDecision {
    /** Give up and let the error reach the caller. */
    data object Stop : RetryDecision

    /** Try again after [delayMs]. */
    data class After(val delayMs: Long) : RetryDecision
}

/** Tunables for [RetryPolicy]. Separated so tests can pin the backoff and drop the jitter. */
data class RetryConfig(
    /** Total attempts for a retryable server error, including the first. */
    val serverErrorAttempts: Int = 3,

    /** Total attempts for a transport failure, including the first. */
    val transportAttempts: Int = 2,

    /** First backoff step. Doubles each attempt. */
    val backoffBaseMs: Long = 500L,

    /** Ceiling on a single backoff step, before jitter. */
    val backoffMaxMs: Long = 8_000L,

    /** Spread added to each backoff so parallel failures do not retry in lockstep. */
    val jitterMs: Long = 250L,

    /** Longest a rate limit wait may be before retrying is worse than failing with a countdown. */
    val maxRateLimitWaitMs: Long = 20_000L,
)

/**
 * The single place that decides whether an AniList request is tried again.
 *
 * Two rules shape all of it.
 *
 * **A mutation is only retried when the request provably had no effect.** A 429 qualifies, because
 * the limiter rejects the request before it reaches the application. A 5xx or a socket timeout does
 * not: the write may well have landed, and repeating it would post a duplicate comment or double an
 * episode count.
 *
 * **Background work never retries in process.** It has WorkManager, which retries with its own
 * backoff, off the critical path and without holding the budget. Retrying here as well would just
 * spend requests the user is about to want.
 */
class RetryPolicy(
    private val config: RetryConfig = RetryConfig(),
    private val random: Random = Random.Default,
) {

    /**
     * @param attempt how many attempts have already been made, so 1 on the first failure
     * @param blockedForMs what [RateLimitGate] says is left on the current 429 timeout
     */
    fun decide(
        error: ApiError,
        attempt: Int,
        isMutation: Boolean,
        priority: RequestPriority,
        blockedForMs: Long,
    ): RetryDecision {
        if (priority == RequestPriority.Background) return RetryDecision.Stop

        return when (error) {
            is ApiError.RateLimited -> {
                if (attempt >= 2) return RetryDecision.Stop
                val wait = maxOf(blockedForMs, error.retryAfterSeconds * 1000L)
                if (wait > config.maxRateLimitWaitMs) RetryDecision.Stop
                else RetryDecision.After(wait + jitter())
            }

            is ApiError.ServerError -> {
                if (isMutation || attempt >= config.serverErrorAttempts) RetryDecision.Stop
                else RetryDecision.After(backoff(attempt))
            }

            is ApiError.Timeout, is ApiError.Offline -> {
                if (isMutation || attempt >= config.transportAttempts) RetryDecision.Stop
                else RetryDecision.After(backoff(attempt))
            }

            // Deferred means the gate refused on purpose, and everything else is a decision the
            // server will repeat. Neither gets better by asking again.
            else -> RetryDecision.Stop
        }
    }

    private fun backoff(attempt: Int): Long {
        val step = config.backoffBaseMs shl (attempt - 1).coerceIn(0, 16)
        return step.coerceAtMost(config.backoffMaxMs) + jitter()
    }

    private fun jitter(): Long = if (config.jitterMs <= 0) 0 else random.nextLong(config.jitterMs)
}
