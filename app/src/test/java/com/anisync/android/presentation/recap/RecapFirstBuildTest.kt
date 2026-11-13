package com.anisync.android.presentation.recap

import androidx.work.WorkInfo.State
import com.anisync.android.domain.recap.RecapBuildProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class RecapFirstBuildTest {

    private val since = YearMonth.of(2025, 1)

    @Test
    fun `a queued build is running without progress until the worker reports`() {
        assertEquals(RecapBuild.Running(since), firstBuildOf(listOf(State.ENQUEUED to null), since))
    }

    @Test
    fun `a running build carries what it last reported`() {
        val progress = RecapBuildProgress(0.3f, YearMonth.of(2026, 4))
        assertEquals(RecapBuild.Running(since, 0.3f, YearMonth.of(2026, 4)), firstBuildOf(listOf(State.RUNNING to progress), since))
    }

    @Test
    fun `a build that gave up is failed, one that finished or was cancelled is over`() {
        assertEquals(RecapBuild.Failed, firstBuildOf(listOf(State.FAILED to null), since))
        assertNull(firstBuildOf(listOf(State.SUCCEEDED to null), since))
        assertNull(firstBuildOf(listOf(State.CANCELLED to null), since))
        assertNull(firstBuildOf(emptyList(), since))
    }
}
