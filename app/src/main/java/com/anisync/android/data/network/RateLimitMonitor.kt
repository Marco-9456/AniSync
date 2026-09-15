package com.anisync.android.data.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** What the request budget is doing, in terms the UI can act on. */
sealed interface RateLimitStatus {

    /** Requests are going out as fast as they are asked for. */
    data object Clear : RateLimitStatus

    /** The gate is spacing requests out to stay inside the window. Slower, but nothing is failing. */
    data class Pacing(val remaining: Int, val limit: Int) : RateLimitStatus

    /**
     * Nothing may be sent until the window turns over.
     *
     * Covers both ways that happens: AniList returned a 429, or the budget ran out on our side.
     * They are the same thing to the user, who is looking at a screen that will not load, and both
     * need the same countdown and the same pull-to-refresh gate.
     *
     * @param retryAtElapsedMs monotonic instant the wait ends, comparable to
     *   [android.os.SystemClock.elapsedRealtime]
     * @param secondsRemaining seconds left when this was published, for a countdown to start from
     */
    data class Blocked(val retryAtElapsedMs: Long, val secondsRemaining: Long) : RateLimitStatus
}

/**
 * Counters behind the debug readout. Cheap to keep and the only way to tell, on a device, whether
 * the gate is doing its job or merely appearing to.
 */
data class RateLimitStats(
    val limit: Int = 0,
    val remaining: Int = 0,
    val inFlight: Int = 0,
    val windowResetsInMs: Long = 0,
    val blockedForMs: Long = 0,
    val admitted: Long = 0,
    val paced: Long = 0,
    val deferred: Long = 0,
    val refused: Long = 0,
    val retried: Long = 0,
    val rateLimited: Long = 0,
)

/**
 * The observable face of [RateLimitGate].
 *
 * It exists so the gate can report without depending on the UI. The previous interceptor injected
 * `ToastManager` directly, which put a presentation type in the DI graph of the network layer and
 * made pull-to-refresh gate on "is a toast visible" rather than "is the server still refusing us".
 */
@Singleton
class RateLimitMonitor @Inject constructor() {

    private val _status = MutableStateFlow<RateLimitStatus>(RateLimitStatus.Clear)

    /** Collected by the app shell to drive the rate limit notice and the refresh gates. */
    val status: StateFlow<RateLimitStatus> = _status.asStateFlow()

    private val _stats = MutableStateFlow(RateLimitStats())

    /** Debug readout only. Nothing in the shipping UI reads this. */
    val stats: StateFlow<RateLimitStats> = _stats.asStateFlow()

    internal fun publishStatus(status: RateLimitStatus) {
        _status.value = status
    }

    internal fun publishStats(stats: RateLimitStats) {
        _stats.value = stats
    }
}
