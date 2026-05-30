package com.anisync.android.presentation.calendar

import com.anisync.android.domain.AiringEpisode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class CalendarGroupingTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private val weekStart: LocalDate = LocalDate.of(2026, 5, 25)

    private fun episode(
        id: Int,
        dayOffset: Long,
        hour: Int,
        isOnList: Boolean = false
    ): AiringEpisode {
        val airingAt = weekStart.plusDays(dayOffset)
            .atStartOfDay(zone)
            .plusHours(hour.toLong())
            .toEpochSecond()
        return AiringEpisode(
            id = id,
            episode = 1,
            airingAt = airingAt,
            mediaId = id,
            titleRomaji = null,
            titleEnglish = null,
            titleNative = null,
            titleUserPreferred = "Show $id",
            coverImageUrl = null,
            format = "TV",
            averageScore = null,
            isOnList = isOnList,
            listStatus = null,
            isAdult = false
        )
    }

    @Test
    fun `always returns seven ordered days`() {
        val days = buildDays(weekStart, zone, emptyList(), followingOnly = false)
        assertEquals(7, days.size)
        days.forEachIndexed { i, day ->
            assertEquals(weekStart.plusDays(i.toLong()), day.date)
        }
    }

    @Test
    fun `episodes bucket into their local day`() {
        val eps = listOf(
            episode(1, dayOffset = 0, hour = 9),
            episode(2, dayOffset = 0, hour = 22),
            episode(3, dayOffset = 3, hour = 12)
        )
        val days = buildDays(weekStart, zone, eps, followingOnly = false)
        assertEquals(2, days[0].episodes.size)
        assertEquals(0, days[1].episodes.size)
        assertEquals(1, days[3].episodes.size)
    }

    @Test
    fun `episodes within a day are sorted by airing time`() {
        val eps = listOf(
            episode(1, dayOffset = 0, hour = 22),
            episode(2, dayOffset = 0, hour = 6)
        )
        val days = buildDays(weekStart, zone, eps, followingOnly = false)
        assertEquals(listOf(2, 1), days[0].episodes.map { it.id })
    }

    @Test
    fun `following only keeps on-list episodes`() {
        val eps = listOf(
            episode(1, dayOffset = 1, hour = 9, isOnList = true),
            episode(2, dayOffset = 1, hour = 10, isOnList = false)
        )
        val days = buildDays(weekStart, zone, eps, followingOnly = true)
        assertEquals(1, days[1].episodes.size)
        assertEquals(1, days[1].episodes.first().id)
    }

    @Test
    fun `episodes outside the week are dropped`() {
        val eps = listOf(
            episode(1, dayOffset = -1, hour = 9),
            episode(2, dayOffset = 7, hour = 9)
        )
        val days = buildDays(weekStart, zone, eps, followingOnly = false)
        assertEquals(0, days.sumOf { it.episodes.size })
    }
}
