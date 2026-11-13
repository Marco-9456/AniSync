package com.anisync.android.worker

import androidx.work.Data
import androidx.work.WorkInfo.State
import com.anisync.android.domain.recap.RecapBuildProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

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

    @Test
    fun `a first build's progress comes back as it was reported`() {
        listOf(RecapBuildProgress(0.42f, YearMonth.of(2025, 3)), RecapBuildProgress(0.9f, null)).forEach { progress ->
            assertEquals(progress, RecapWorker.buildProgress(RecapWorker.progressData(progress)))
        }
    }

    @Test
    fun `a build that has not reported yet has no progress`() {
        assertNull(RecapWorker.buildProgress(Data.EMPTY))
    }
}
