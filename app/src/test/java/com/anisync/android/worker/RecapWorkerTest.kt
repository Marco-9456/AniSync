package com.anisync.android.worker

import androidx.work.WorkInfo.State
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecapWorkerTest {

    @Test
    fun `a queued run waiting for its day may move to a new time`() {
        assertTrue(RecapWorker.queuedRunMayMove(listOf(State.ENQUEUED to 0)))
        assertTrue(RecapWorker.queuedRunMayMove(listOf(State.SUCCEEDED to 1, State.ENQUEUED to 0)))
        assertTrue(RecapWorker.queuedRunMayMove(emptyList()))
    }

    @Test
    fun `a run under way or waiting to retry is never replaced`() {
        assertFalse(RecapWorker.queuedRunMayMove(listOf(State.RUNNING to 1)))
        assertFalse(RecapWorker.queuedRunMayMove(listOf(State.ENQUEUED to 2)))
        assertFalse(RecapWorker.queuedRunMayMove(listOf(State.RUNNING to 1, State.BLOCKED to 0)))
    }
}
