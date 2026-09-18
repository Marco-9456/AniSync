package com.anisync.android.data.network

import com.anisync.android.data.util.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimitWindowTest {

    private val clock = FakeClock()
    private val window = RateLimitWindow(clock)

    @Test
    fun `starts at the degraded ceiling until the server says otherwise`() {
        assertEquals(RateLimitWindow.DEFAULT_LIMIT, window.limit)
        assertEquals(RateLimitWindow.DEFAULT_LIMIT, window.headroom())
    }

    @Test
    fun `headroom subtracts what was issued since the count was read`() {
        window.onHeaders(limitHeader = 30, remainingHeader = 20)
        repeat(3) { window.onIssued() }

        // The server still believes 20 are left, but three are already on the wire.
        assertEquals(17, window.headroom())
    }

    @Test
    fun `a fresh count from the server clears the in-flight adjustment`() {
        window.onHeaders(30, 20)
        repeat(3) { window.onIssued() }
        window.onHeaders(30, 17)

        assertEquals(17, window.headroom())
    }

    @Test
    fun `a count that goes up marks the start of a new window`() {
        window.onHeaders(30, 20)
        clock.advanceBy(40_000)
        assertEquals(20_000, window.resetsInMs())

        // AniList never sends a reset timestamp on a 200, so the jump is the only signal.
        window.onHeaders(30, 29)
        assertEquals(RateLimitWindow.WINDOW_MS, window.resetsInMs())
    }

    @Test
    fun `a count that keeps falling leaves the window boundary alone`() {
        window.onHeaders(30, 25)
        clock.advanceBy(10_000)
        window.onHeaders(30, 22)
        clock.advanceBy(10_000)
        window.onHeaders(30, 19)

        assertEquals(RateLimitWindow.WINDOW_MS - 20_000, window.resetsInMs())
    }

    @Test
    fun `garbage headers are ignored rather than believed`() {
        window.onHeaders(30, 20)
        window.onHeaders(limitHeader = null, remainingHeader = null)
        assertEquals(30, window.limit)
        assertEquals(20, window.headroom())

        window.onHeaders(limitHeader = 0, remainingHeader = -5)
        assertEquals(30, window.limit)
        assertEquals(20, window.headroom())
    }

    @Test
    fun `remaining is never believed above the advertised limit`() {
        window.onHeaders(limitHeader = 30, remainingHeader = 500)
        assertEquals(30, window.headroom())
    }

    /**
     * The unset deadline is Long.MIN_VALUE. Subtracting a clock reading from it underflows and
     * wraps positive, which made the gate refuse everything the moment the clock left zero.
     */
    @Test
    fun `a window that was never blocked stays unblocked as the clock advances`() {
        repeat(10) {
            clock.advanceBy(1_000)
            assertEquals(0, window.blockedForMs())
        }
    }

    @Test
    fun `a 429 blocks for the time the server asked for`() {
        window.onHeaders(30, 4)
        window.onRateLimited(retryAfterSeconds = 45)

        assertEquals(45_000, window.blockedForMs())

        clock.advanceBy(45_000)
        assertEquals(0, window.blockedForMs())
    }

    /**
     * The timeout is the window, so surviving it means the budget is fresh. Leaving the count at
     * zero deadlocked the gate: nothing could be sent, so no response could ever arrive to say the
     * window had reset.
     */
    @Test
    fun `the budget is whole again once a 429 timeout expires`() {
        window.onHeaders(30, 0)
        window.onRateLimited(retryAfterSeconds = 45)
        clock.advanceBy(45_000)

        assertEquals(0, window.blockedForMs())
        assertEquals(30, window.headroom())
    }

    @Test
    fun `a 429 without a header falls back to the documented minute`() {
        window.onRateLimited(retryAfterSeconds = null)
        assertEquals(RateLimitWindow.DEFAULT_RETRY_AFTER_SECONDS * 1000, window.blockedForMs())
    }

    @Test
    fun `an absurd retry-after is capped rather than freezing the app`() {
        window.onRateLimited(retryAfterSeconds = 86_400)
        assertEquals(RateLimitWindow.MAX_RETRY_AFTER_SECONDS * 1000, window.blockedForMs())
    }

    @Test
    fun `the next window cannot start before a timeout ends`() {
        window.onRateLimited(retryAfterSeconds = 30)
        assertTrue(window.resetsInMs() >= 30_000)
    }

    @Test
    fun `a timeout that outlived the process is reinstated`() {
        window.restoreBlockedFor(12_000)

        assertEquals(12_000, window.blockedForMs())

        clock.advanceBy(12_000)
        assertEquals(0, window.blockedForMs())
        assertEquals(30, window.headroom())
    }

    @Test
    fun `restoring a timeout that already passed does nothing`() {
        window.restoreBlockedFor(0)
        assertEquals(0, window.blockedForMs())
        assertEquals(RateLimitWindow.DEFAULT_LIMIT, window.headroom())
    }
}
