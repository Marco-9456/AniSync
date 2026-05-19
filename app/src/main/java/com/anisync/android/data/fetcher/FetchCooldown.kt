package com.anisync.android.data.fetcher

/**
 * Per-resource cooldown gate. Two-tiered floor so user-initiated refresh feels
 * responsive (short floor) while background/auto refreshes are aggressively
 * throttled (long floor). Independent from the 429 toast gate.
 *
 * Promoted from Horizon A's private helper in
 * [com.anisync.android.presentation.profile.ProfileViewModel.FetchCooldown].
 * The promotion adds a [Clock] injection point so tests can drive virtual
 * time, and lets every repository share the same primitive instead of each
 * ViewModel reimplementing it.
 */
class FetchCooldown(
    private val gestureFloorMs: Long = 5_000L,
    private val autoFloorMs: Long = 15_000L,
    private val clock: Clock = Clock.System
) {
    /**
     * Sentinel that distinguishes "never fetched" from "fetched at t=0". Lets
     * the first call always pass regardless of the clock's starting value
     * (relevant for tests using [FakeClock] starting at 0).
     */
    private var firstCall: Boolean = true
    private var lastAt: Long = 0L

    /**
     * Returns true if a fetch is allowed under the configured floor and
     * records the new fetch timestamp. Returns false if within the floor —
     * caller should suppress the fetch and either keep the cached value or
     * skip the work entirely.
     */
    fun shouldFetch(userInitiated: Boolean): Boolean {
        val now = clock.nowMs()
        if (firstCall) {
            firstCall = false
            lastAt = now
            return true
        }
        val floor = if (userInitiated) gestureFloorMs else autoFloorMs
        return (now - lastAt >= floor).also { ok -> if (ok) lastAt = now }
    }

    /** Resets the cooldown so the next [shouldFetch] returns true. */
    fun reset() {
        firstCall = true
        lastAt = 0L
    }
}
