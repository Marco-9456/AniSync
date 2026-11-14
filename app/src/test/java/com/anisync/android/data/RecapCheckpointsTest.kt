package com.anisync.android.data

import com.anisync.android.domain.recap.FetchedActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecapCheckpointsTest {

    @Test
    fun `a saved history reads back as it was saved`() {
        val checkpoint = RecapCheckpoint(
            from = 1_735_678_800,
            savedAt = 1_794_622_846_805,
            complete = false,
            activities = listOf(
                FetchedActivity(1, 10, "watched episode", "1 - 4", 1_741_000_000),
                FetchedActivity(2, 20, "completed", null, 1_742_000_000),
            ),
        )
        assertEquals(checkpoint, decodeRecapCheckpoint(checkpoint.encode()))
    }

    @Test
    fun `an unreadable or older saved history reads as none`() {
        assertNull(decodeRecapCheckpoint("not json"))
        assertNull(decodeRecapCheckpoint("""{"v":0,"f":1,"s":2,"c":true,"a":[]}"""))
    }
}
