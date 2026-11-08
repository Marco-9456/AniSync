package com.anisync.android.presentation.components

import androidx.compose.animation.core.spring
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A pull costs a request, so it has to be deliberate: it starts from the top, it has to pass the
 * threshold, and it can be called off by pushing back above it.
 */
class PullRefreshStateTest {

    // Animations wait on frames that never come, so a release leaves the state settling.
    private val scope = CoroutineScope(Dispatchers.Unconfined + BroadcastFrameClock() + Job())
    private var refreshes = 0
    private var refreshing = false
    private var enabled = true
    private val armedChanges = mutableListOf<Boolean>()

    private val state = PullRefreshState(
        scope = scope,
        thresholdPx = THRESHOLD,
        initiallyRefreshing = false,
        isRefreshing = { refreshing },
        enabled = { enabled },
        onRefresh = { refreshes++ },
        onArmedChange = { armedChanges += it },
        fadeSpec = { spring() },
    )

    @After
    fun tearDown() = scope.cancel()

    /** One scroll event of a drag, the list taking [listConsumes] of it. Returns what the pull took. */
    private fun drag(dy: Float, listConsumes: Float = 0f): Float {
        val pre = state.onPreScroll(Offset(0f, dy), NestedScrollSource.UserInput).y
        val left = dy - pre - listConsumes
        val post = state.onPostScroll(
            consumed = Offset(0f, listConsumes),
            available = Offset(0f, left),
            source = NestedScrollSource.UserInput,
        ).y
        return pre + post
    }

    private fun release(velocity: Float = 0f) = runBlocking { state.onPreFling(Velocity(0f, velocity)) }

    @Test
    fun `a pull from the top past the threshold refreshes once on release`() {
        repeat(10) { drag(25f) }
        release()
        assertEquals(1, refreshes)
    }

    @Test
    fun `half the finger travel counts toward the threshold`() {
        drag(2 * THRESHOLD - 1f)
        assertEquals(emptyList<Boolean>(), armedChanges)
        drag(2f)
        assertEquals(listOf(true), armedChanges)
    }

    @Test
    fun `letting go short of the threshold refreshes nothing`() {
        drag(THRESHOLD)
        release()
        assertEquals(0, refreshes)
    }

    @Test
    fun `pushing back above the threshold calls the refresh off`() {
        drag(3 * THRESHOLD)
        assertEquals(-(THRESHOLD + 2f), drag(-(THRESHOLD + 2f)))
        release()
        assertEquals(0, refreshes)
        assertEquals(listOf(true, false), armedChanges)
    }

    @Test
    fun `a swipe that scrolled the list back up never turns into a pull`() {
        drag(40f, listConsumes = 40f)
        assertEquals(0f, drag(3 * THRESHOLD))
        release()
        assertEquals(0, refreshes)
    }

    @Test
    fun `the next gesture from the top pulls again`() {
        drag(40f, listConsumes = 40f)
        drag(3 * THRESHOLD)
        release()
        drag(3 * THRESHOLD)
        release()
        assertEquals(1, refreshes)
    }

    @Test
    fun `nothing is pulled while a refresh runs`() {
        refreshing = true
        assertEquals(0f, drag(3 * THRESHOLD))
        release()
        assertEquals(0, refreshes)
    }

    @Test
    fun `nothing is pulled while the screen turns pulling off`() {
        enabled = false
        assertEquals(0f, drag(3 * THRESHOLD))
        release()
        assertEquals(0, refreshes)
    }

    @Test
    fun `a downward fling that ends a pull is kept from the list`() {
        drag(THRESHOLD)
        assertEquals(Velocity(0f, 500f), release(500f))
    }

    @Test
    fun `an upward fling after a pull is left to the list`() {
        drag(THRESHOLD)
        assertEquals(Velocity.Zero, release(-500f))
    }

    private companion object {
        const val THRESHOLD = 100f
    }
}
