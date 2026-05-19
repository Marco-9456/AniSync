package com.anisync.android.data.fetcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FetchCooldownTest {

    @Test
    fun `first call always passes`() {
        val clock = FakeClock(initialMs = 1000L)
        val cd = FetchCooldown(gestureFloorMs = 5_000L, autoFloorMs = 15_000L, clock = clock)
        assertTrue(cd.shouldFetch(userInitiated = true))
    }

    @Test
    fun `gesture call within floor is suppressed`() {
        val clock = FakeClock()
        val cd = FetchCooldown(gestureFloorMs = 5_000L, autoFloorMs = 15_000L, clock = clock)
        assertTrue(cd.shouldFetch(userInitiated = true))
        clock.advanceBy(4_000L)
        assertFalse(cd.shouldFetch(userInitiated = true))
    }

    @Test
    fun `gesture call past floor passes`() {
        val clock = FakeClock()
        val cd = FetchCooldown(gestureFloorMs = 5_000L, autoFloorMs = 15_000L, clock = clock)
        assertTrue(cd.shouldFetch(userInitiated = true))
        clock.advanceBy(5_000L)
        assertTrue(cd.shouldFetch(userInitiated = true))
    }

    @Test
    fun `auto floor enforced strictly`() {
        val clock = FakeClock()
        val cd = FetchCooldown(gestureFloorMs = 5_000L, autoFloorMs = 15_000L, clock = clock)
        assertTrue(cd.shouldFetch(userInitiated = false))
        clock.advanceBy(10_000L)
        assertFalse(cd.shouldFetch(userInitiated = false))
        clock.advanceBy(5_000L)
        assertTrue(cd.shouldFetch(userInitiated = false))
    }

    @Test
    fun `gesture and auto share same lastAt timestamp`() {
        val clock = FakeClock()
        val cd = FetchCooldown(gestureFloorMs = 5_000L, autoFloorMs = 15_000L, clock = clock)
        // A successful gesture fetch should also block an immediate auto fetch.
        assertTrue(cd.shouldFetch(userInitiated = true))
        clock.advanceBy(10_000L)
        assertFalse(cd.shouldFetch(userInitiated = false))
    }

    @Test
    fun `reset clears the cooldown`() {
        val clock = FakeClock()
        val cd = FetchCooldown(gestureFloorMs = 5_000L, autoFloorMs = 15_000L, clock = clock)
        assertTrue(cd.shouldFetch(userInitiated = true))
        clock.advanceBy(1_000L)
        assertFalse(cd.shouldFetch(userInitiated = true))
        cd.reset()
        assertTrue(cd.shouldFetch(userInitiated = true))
    }

    @Test
    fun `suppressed call does not advance lastAt`() {
        val clock = FakeClock()
        val cd = FetchCooldown(gestureFloorMs = 5_000L, autoFloorMs = 15_000L, clock = clock)
        // First call passes at t=0, sets lastAt=0.
        assertTrue(cd.shouldFetch(userInitiated = true))
        // t=1000: still inside floor → suppressed, lastAt unchanged.
        clock.advanceBy(1_000L)
        assertFalse(cd.shouldFetch(userInitiated = true))
        // t=5000: 5s since first success → must pass.
        clock.advanceBy(4_000L)
        assertTrue(cd.shouldFetch(userInitiated = true))
    }
}
