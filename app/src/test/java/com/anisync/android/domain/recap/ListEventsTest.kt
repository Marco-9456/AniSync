package com.anisync.android.domain.recap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListEventsTest {

    @Test
    fun `progress reads single episodes and merged ranges`() {
        assertEquals(5..5, parseProgress("5"))
        assertEquals(1..3, parseProgress("1 - 3"))
        assertEquals(14..15, parseProgress(" 14 - 15 "))
    }

    @Test
    fun `unreadable progress is null`() {
        assertNull(parseProgress(null))
        assertNull(parseProgress(""))
        assertNull(parseProgress("abc"))
        assertNull(parseProgress("3 - 1"))
        assertNull(parseProgress("1 - 2 - 3"))
    }

    @Test
    fun `a completion adds the episodes after the last one logged`() {
        val events = countEvents(
            listOf(
                anime(1, "watched episode", "1", at = 10),
                anime(2, "watched episode", "2", at = 20),
                anime(3, "watched episode", "3", at = 30),
                anime(4, "completed", null, at = 40),
            )
        ) { 4 }
        assertEquals(listOf(1, 1, 1, 1), events.map { it.units })
        assertEquals(4..4, events.last().range)
        assertTrue(events.last().isCompletion)
    }

    @Test
    fun `a completion after a merged range adds the rest`() {
        val events = countEvents(listOf(anime(1, "watched episode", "1 - 23", at = 10), anime(2, "completed", null, at = 11))) { 24 }
        assertEquals(listOf(23, 1), events.map { it.units })
    }

    @Test
    fun `a completion with nothing logged before counts one episode`() {
        val film = countEvents(listOf(anime(1, "completed", null, at = 10))) { 1 }.single()
        assertEquals(1, film.units)
        assertEquals(1..1, film.range)
        val series = countEvents(listOf(anime(1, "completed", null, at = 10))) { 12 }.single()
        assertEquals(1, series.units)
        assertEquals(12..12, series.range)
    }

    @Test
    fun `a completion of an unknown length counts one episode`() {
        assertEquals(1, countEvents(listOf(anime(1, "completed", null, at = 10))) { null }.single().units)
    }

    @Test
    fun `a completion after the last episode was logged adds nothing`() {
        val events = countEvents(listOf(anime(1, "watched episode", "1 - 12", at = 10), anime(2, "completed", null, at = 11))) { 12 }
        assertEquals(0, events.last().units)
        assertNull(events.last().range)
        assertTrue(events.last().isCompletion)
    }

    @Test
    fun `activity before the period still informs a completion`() {
        val events = countEvents(listOf(anime(2, "completed", null, at = 500), anime(1, "watched episode", "1 - 10", at = 100))) { 12 }
        assertEquals(2, events.last().units)
        assertEquals(11..12, events.last().range)
    }

    @Test
    fun `repeats are flagged`() {
        val events = countEvents(
            listOf(anime(1, "rewatched episode", "4 - 6", at = 10), anime(2, "rewatched", null, at = 11))
        ) { 6 }
        assertTrue(events.all { it.isRepeat })
        assertEquals(listOf(3, 0), events.map { it.units })
    }

    @Test
    fun `planning, pausing and dropping count nothing`() {
        val events = countEvents(
            listOf(
                anime(1, "plans to watch", null, at = 10),
                anime(2, "paused watching", "3", at = 11),
                anime(3, "dropped", "5", at = 12),
            )
        ) { 12 }
        assertTrue(events.isEmpty())
    }

    @Test
    fun `chapters follow the same rules`() {
        val events = countEvents(
            listOf(
                ListActivityRecord(1, 7, RecapMediaType.MANGA, "read chapter", "134 - 139", 10),
                ListActivityRecord(2, 7, RecapMediaType.MANGA, "completed", null, 11),
            )
        ) { 140 }
        assertEquals(listOf(6, 1), events.map { it.units })
        assertFalse(events.first().isCompletion)
    }

    private fun anime(id: Int, status: String, progress: String?, at: Long) =
        ListActivityRecord(id, mediaId = 9, type = RecapMediaType.ANIME, status = status, progress = progress, createdAt = at)
}
