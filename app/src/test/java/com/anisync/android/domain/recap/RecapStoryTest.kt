package com.anisync.android.domain.recap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

class RecapStoryTest {

    private val september = RecapPeriod.Month(YearMonth.of(2026, 9))
    private val year = RecapPeriod.Year(2025)

    @Test
    fun `a full month tells all eight pages in the frames' order`() {
        assertEquals(
            listOf(
                RecapPage.OPENER, RecapPage.TIME, RecapPage.MOST_WATCHED, RecapPage.TOP_FIVE,
                RecapPage.MOOD, RecapPage.FINISHED, RecapPage.RHYTHM, RecapPage.MONTH_CARD,
            ),
            recapPages(recap(september)),
        )
    }

    @Test
    fun `a month drops the pages it has nothing for`() {
        val one = recap(september, ranked = listOf(ranked(1, minutes = 60)), finished = 0, genres = emptyList())
        assertEquals(
            listOf(RecapPage.OPENER, RecapPage.TIME, RecapPage.MOST_WATCHED, RecapPage.RHYTHM, RecapPage.MONTH_CARD),
            recapPages(one),
        )

        val hidden = recap(september, ranked = emptyList())
        assertFalse(RecapPage.MOST_WATCHED in recapPages(hidden))
        assertFalse(RecapPage.TOP_FIVE in recapPages(hidden))
    }

    @Test
    fun `a year tells six pages and drops the title and genre pages when they are empty`() {
        assertEquals(
            listOf(
                RecapPage.YEAR_OPENER, RecapPage.YEAR_NUMBERS, RecapPage.YEAR_TOP_FIVE,
                RecapPage.YEAR_TASTE, RecapPage.YEAR_HABITS, RecapPage.YEAR_CARD,
            ),
            recapPages(recap(year)),
        )
        assertEquals(
            listOf(RecapPage.YEAR_OPENER, RecapPage.YEAR_NUMBERS, RecapPage.YEAR_HABITS, RecapPage.YEAR_CARD),
            recapPages(recap(year, ranked = emptyList(), genres = emptyList())),
        )
    }

    @Test
    fun `pages take their seeds from the frames`() {
        val month = recapPages(recap(september)).map { it.seed }
        assertEquals(
            listOf(
                PageSeed.Title(0), PageSeed.Title(0), PageSeed.Title(0), PageSeed.TopGenre,
                PageSeed.Title(2), PageSeed.Title(1), PageSeed.App, PageSeed.Title(0),
            ),
            month,
        )
        val years = recapPages(recap(year)).map { it.seed }
        assertEquals(
            listOf(PageSeed.Title(0), PageSeed.Title(3), PageSeed.Title(0), PageSeed.TopGenre, PageSeed.Title(2), PageSeed.Title(0)),
            years,
        )
    }

    @Test
    fun `only the numbers pages sit on the container and only the cards hold still`() {
        assertEquals(setOf(RecapPage.TIME, RecapPage.YEAR_NUMBERS), RecapPage.entries.filter { it.onContainer }.toSet())
        assertEquals(setOf(RecapPage.MONTH_CARD, RecapPage.YEAR_CARD), RecapPage.entries.filter { it.isCard }.toSet())
    }

    @Test
    fun `equal time at the top is a tie that says which were finished`() {
        val tie = recap(year, ranked = listOf(ranked(1, minutes = 312, finished = true), ranked(2, minutes = 312, finished = false), ranked(3, minutes = 200)))
        assertEquals(RankNote.Tie(1, 2, firstFinished = true, secondFinished = false), rankNote(tie, emptyMap()))

        val both = recap(year, ranked = listOf(ranked(1, minutes = 312, finished = true), ranked(2, minutes = 312, finished = true)))
        assertEquals(RankNote.Tie(1, 2, firstFinished = true, secondFinished = true), rankNote(both, emptyMap()))
    }

    @Test
    fun `shorts outside the top five that out-episode it get the note`() {
        val top = (1..5).map { ranked(it, minutes = 200 - it, episodes = 4) }
        val shorts = ranked(9, minutes = 69, episodes = 23)
        val media = mapOf(1 to media(1, duration = 155, format = "MOVIE"), 9 to media(9, duration = 3))

        assertEquals(
            RankNote.Shorts(mediaId = 9, episodes = 23, minutesEach = 3, topIsFilm = true),
            rankNote(recap(september, ranked = top + shorts), media),
        )
    }

    @Test
    fun `shorts that do not out-episode the top five, or sit inside it, leave the plain note`() {
        val top = (1..5).map { ranked(it, minutes = 200 - it, episodes = 30) }
        val fewer = ranked(9, minutes = 69, episodes = 23)
        val media = mapOf(9 to media(9, duration = 3), 3 to media(3, duration = 3))
        assertEquals(RankNote.ByTime, rankNote(recap(september, ranked = top + fewer), media))

        val inside = (1..5).map { ranked(it, minutes = 200 - it, episodes = if (it == 3) 50 else 4) }
        assertEquals(RankNote.ByTime, rankNote(recap(september, ranked = inside), media))

        val long = ranked(9, minutes = 690, episodes = 23)
        assertEquals(RankNote.ByTime, rankNote(recap(september, ranked = top.map { it.copy(episodes = 4) } + long), mapOf(9 to media(9, duration = 30))))
    }

    @Test
    fun `a short is ten minutes or less, and an unknown length is not one`() {
        assertTrue(media(1, duration = 10).isShort())
        assertFalse(media(1, duration = 11).isShort())
        assertFalse(media(1, duration = null).isShort())
        assertFalse(media(1, duration = 0).isShort())
    }

    @Test
    fun `the busiest month runs away at half again the next one`() {
        val ranAway = recap(year, episodesByMonth = months(Month.JANUARY to 112, Month.MARCH to 74, Month.NOVEMBER to 5))
        assertEquals(BusiestMonth(Month.JANUARY, 112, ranAway = true), busiestMonth(ranAway))

        val close = recap(year, episodesByMonth = months(Month.JANUARY to 112, Month.MARCH to 75))
        assertEquals(BusiestMonth(Month.JANUARY, 112, ranAway = false), busiestMonth(close))

        val alone = recap(year, episodesByMonth = months(Month.MAY to 3))
        assertEquals(BusiestMonth(Month.MAY, 3, ranAway = true), busiestMonth(alone))
    }

    @Test
    fun `a month has no busiest month, and an empty year has none either`() {
        assertNull(busiestMonth(recap(september, episodesByMonth = null)))
        assertNull(busiestMonth(recap(year, episodesByMonth = months())))
    }

    @Test
    fun `finished titles past the covers are counted from the first to the last`() {
        val finished = recap(september, finishedTitles = (1..15).toList())
        assertEquals(MoreFinished(count = 9, first = 7, last = 15), moreFinished(finished, shown = 6))
        assertEquals(MoreFinished(count = 1, first = 15, last = 15), moreFinished(finished, shown = 14))
        assertNull(moreFinished(finished, shown = 15))
        assertNull(moreFinished(recap(september, finishedTitles = (1..4).toList()), shown = 6))
    }

    private fun months(vararg counts: Pair<Month, Int>): Map<Month, Int> =
        Month.entries.associateWith { month -> counts.firstOrNull { it.first == month }?.second ?: 0 }

    private fun ranked(id: Int, minutes: Int, episodes: Int = 1, finished: Boolean = false) =
        RankedTitle(mediaId = id, episodes = episodes, minutes = minutes, finishedInPeriod = finished, watched = 1..episodes)

    private fun media(id: Int, duration: Int?, format: String = "TV") = RecapMedia(
        id = id,
        type = RecapMediaType.ANIME,
        title = "Title $id",
        format = format,
        episodes = 12,
        chapters = null,
        duration = duration,
        genres = emptyList(),
        mainStudio = null,
        isAdult = false,
        startDate = null,
        endDate = null,
    )

    private fun recap(
        period: RecapPeriod,
        ranked: List<RankedTitle> = listOf(ranked(1, minutes = 155), ranked(2, minutes = 136), ranked(3, minutes = 128)),
        finished: Int = 3,
        finishedTitles: List<Int> = listOf(1, 2, 3),
        genres: List<NamedMinutes> = listOf(NamedMinutes("Action", 996)),
        episodesByMonth: Map<Month, Int>? = if (period is RecapPeriod.Year) months(Month.JANUARY to 3) else null,
    ): Recap {
        val day = LocalDate.of(2026, 9, 20)
        return Recap(
            period = period,
            episodes = 62,
            minutes = 1340,
            titles = ranked.size,
            finished = finished,
            started = 3,
            activeDays = 17,
            ranked = ranked,
            finishedTitles = finishedTitles,
            genres = genres,
            studios = emptyList(),
            episodesByDay = mapOf(day to 25),
            episodesByWeekday = mapOf(DayOfWeek.SUNDAY to 32),
            streak = Streak(6, day, day.plusDays(5)),
            biggestDay = BiggestDay(day, 25, 1, 24),
            episodesByMonth = episodesByMonth,
            keptUpWhileAiring = null,
            manga = null,
            franchise = null,
        )
    }
}
