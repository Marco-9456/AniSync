package com.anisync.android.domain.recap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset

class RecapSlicesTest {

    private val zone = ZoneOffset.ofHours(3)
    private val september = RecapPeriod.Month(YearMonth.of(2026, 9))

    private val media = listOf(
        RecapMedia(1, RecapMediaType.ANIME, "Armitage III", "OVA", 4, null, 34, listOf("Action"), null, false, null, null),
        RecapMedia(2, RecapMediaType.ANIME, "BLEACH", "TV", 366, null, 24, listOf("Action"), null, false, null, null),
        RecapMedia(3, RecapMediaType.ANIME, "K-ON!!", "TV", 24, null, 24, listOf("Music"), null, false, null, null),
        RecapMedia(4, RecapMediaType.MANGA, "Gabriel Dropout", "MANGA", null, 140, null, listOf("Comedy"), null, false, null, null),
    ).associateBy { it.id }

    private var nextId = 1

    private val history = listOf(
        record(1, "watched episode", "1", at(7, 2)),
        record(1, "watched episode", "2", at(8, 20)),
        record(1, "paused watching", null, at(8, 21)),
        record(2, "watched episode", "1 - 10", at(7, 5)),
        record(3, "watched episode", "1 - 12", at(8, 1)),
        record(4, "read chapter", "120 - 134", at(8, 3), RecapMediaType.MANGA),
        record(1, "completed", null, at(9, 3)),
        record(2, "plans to watch", null, at(9, 4)),
        record(4, "completed", null, at(9, 9), RecapMediaType.MANGA),
        record(3, "watched episode", "13", at(10, 2)),
    )

    @Test
    fun `a slice counts exactly like the whole history`() {
        val settings = RecapSettings(includeManga = true)
        val full = buildRecap(september, history, media, settings, zone)
        val sliced = buildRecap(september, sliceForPeriod(september, zone, history), media, settings, zone)
        assertEquals(full, sliced)
        // Armitage III finished with no progress logged that month, so episodes 3 and 4 come from August's 2.
        assertEquals(2, sliced!!.episodes)
        assertEquals(6, sliced.manga!!.chapters)
    }

    @Test
    fun `only what decides where a title stood comes from before`() {
        val slice = sliceForPeriod(september, zone, history)
        val earlier = slice.filter { it.createdAt < september.window(zone).from }
        assertEquals(listOf(4 to "120 - 134", 1 to "2"), earlier.map { it.mediaId to it.progress })
    }

    @Test
    fun `a slice keeps the furthest episode, a finish and a rewatch in progress`() {
        val history = listOf(
            record(3, "watched episode", "1 - 3", at(8, 1)),
            record(3, "watched episode", "1 - 9", at(8, 2)),
            record(3, "watched episode", "4", at(8, 3)),
            record(1, "watched episode", "1 - 4", at(8, 4)),
            record(1, "completed", null, at(8, 5)),
            record(1, "rewatched episode", "1 - 2", at(8, 6)),
            record(3, "watched episode", "10", at(9, 1)),
            record(1, "watched episode", "1 - 3", at(9, 2)),
        )
        val slice = sliceForPeriod(september, zone, history)
        val earlier = slice.filter { it.createdAt < september.window(zone).from }
        assertEquals(listOf(3 to "1 - 9", 1 to null, 1 to "1 - 2"), earlier.map { it.mediaId to it.progress })
        for (countRepeats in listOf(false, true)) {
            val settings = RecapSettings(countRepeats = countRepeats)
            assertEquals(buildRecap(september, history, media, settings, zone), buildRecap(september, slice, media, settings, zone))
        }
        assertEquals(1, buildRecap(september, slice, media, RecapSettings(), zone)!!.episodes)
        assertEquals(2, buildRecap(september, slice, media, RecapSettings(countRepeats = true), zone)!!.episodes)
    }

    @Test
    fun `plans, pauses and later months stay out`() {
        val slice = sliceForPeriod(september, zone, history)
        assertTrue(slice.none { it.status == "plans to watch" || it.status == "paused watching" })
        assertTrue(slice.none { it.mediaId == 3 })
        assertEquals(slice.sortedWith(compareBy({ it.createdAt }, { it.id })), slice)
    }

    private fun at(month: Int, day: Int) = LocalDateTime.of(2026, month, day, 20, 0).toEpochSecond(zone)

    private fun record(mediaId: Int, status: String, progress: String?, at: Long, type: RecapMediaType = RecapMediaType.ANIME) =
        ListActivityRecord(nextId++, mediaId, type, status, progress, at)
}
