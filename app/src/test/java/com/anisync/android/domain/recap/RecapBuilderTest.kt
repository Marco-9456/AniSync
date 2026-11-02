package com.anisync.android.domain.recap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.time.ZoneOffset

class RecapBuilderTest {

    private val zone = ZoneOffset.ofHours(3)
    private val september = RecapPeriod.Month(YearMonth.of(2026, 9))

    private val media = listOf(
        title(1, "Shin Evangelion Movie:||", "MOVIE", 1, 155, listOf("Action", "Mecha"), "Khara", relations = listOf(2)),
        title(2, "Evangelion Shin Movie: Kyuu", "MOVIE", 1, 96, listOf("Action", "Mecha", "Sci-Fi"), "Khara", relations = listOf(1)),
        title(3, "Petit Eva: Evangelion@School", "ONA", 24, 3, listOf("Comedy"), relations = listOf(999)),
        title(4, "Armitage III", "OVA", 4, 34, listOf("Action", "Sci-Fi")),
        title(5, "BLEACH", "TV", 366, 24, listOf("Action", "Adventure"), "Pierrot"),
        title(6, "Yuuwaku COUNT DOWN", "OVA", 6, 16, listOf("Hentai"), adult = true),
        title(7, "Jigoku Koushien", "OVA", 1, 40, listOf("Action")),
        title(9, "Holo no Graffiti", "ONA", null, null, listOf("Comedy")),
        RecapMedia(8, RecapMediaType.MANGA, "Gabriel Dropout", "MANGA", null, 140, null, listOf("Comedy"), null, false, null, null),
    ).associateBy { it.id }

    private val records = listOf(
        anime(5, "watched episode", "11", at(8, 30)),
        anime(7, "plans to watch", null, at(9, 1)),
        anime(5, "watched episode", "12", at(9, 2)),
        anime(5, "watched episode", "13", at(9, 3)),
        anime(9, "watched episode", "382", at(9, 4)),
        anime(1, "completed", null, at(9, 5)),
        anime(6, "watched episode", "1 - 5", at(9, 6, 17)),
        anime(6, "completed", null, at(9, 6, 18)),
        anime(2, "completed", null, at(9, 6, 21)),
        anime(4, "watched episode", "1", at(9, 8)),
        anime(4, "watched episode", "2", at(9, 9)),
        anime(4, "watched episode", "3", at(9, 10)),
        anime(4, "completed", null, at(9, 11)),
        anime(5, "rewatched episode", "1 - 2", at(9, 15)),
        anime(3, "watched episode", "1 - 23", at(9, 20, 12)),
        anime(3, "completed", null, at(9, 20, 13)),
        manga(8, "read chapter", "134 - 139", at(9, 27)),
        manga(8, "completed", null, at(9, 28, 10)),
        anime(5, "watched episode", "14 - 15", at(9, 28)),
    )

    private fun build(settings: RecapSettings = RecapSettings()) = buildRecap(september, records, media, settings, zone)!!

    @Test
    fun `a month with nothing watched has no recap`() {
        val planned = listOf(anime(7, "plans to watch", null, at(9, 1)))
        assertNull(buildRecap(september, planned, media, RecapSettings(), zone))
        assertNull(buildRecap(RecapPeriod.Month(YearMonth.of(2026, 7)), records, media, RecapSettings(), zone))
    }

    @Test
    fun `totals count completions and every title watched`() {
        val recap = build()
        assertEquals(41, recap.episodes)
        assertEquals(155 + 96 + 72 + 136 + 96 + 96 + 24, recap.minutes)
        assertEquals(7, recap.titles)
        assertEquals(5, recap.finished)
        assertEquals(5, recap.started)
        assertEquals(11, recap.activeDays)
    }

    @Test
    fun `an unknown duration counts as a TV episode`() {
        assertEquals(DEFAULT_EPISODE_MINUTES, buildRecap(september, records, media, RecapSettings(showAdult = true), zone)!!.ranked.single { it.mediaId == 9 }.minutes)
    }

    @Test
    fun `titles rank by time, a tie going to the finished one`() {
        val ranked = build().ranked
        assertEquals(listOf(1, 4, 2, 5, 3, 9), ranked.map { it.mediaId })
        assertTrue(ranked[2].finishedInPeriod)
        assertEquals(12..15, ranked[3].watched)
    }

    @Test
    fun `among finished ties the one finished first leads`() {
        val ranked = build(RecapSettings(showAdult = true)).ranked
        assertEquals(listOf(1, 4, 6, 2, 5, 3, 9), ranked.map { it.mediaId })
    }

    @Test
    fun `adult titles count but stay unnamed while adult content is off`() {
        val recap = build()
        assertTrue(recap.ranked.none { it.mediaId == 6 })
        assertTrue(recap.genres.none { it.name == "Hentai" })
        assertTrue(6 !in recap.finishedTitles)
        val shown = build(RecapSettings(showAdult = true))
        assertTrue(shown.genres.any { it.name == "Hentai" })
        assertEquals(recap.episodes, shown.episodes)
    }

    @Test
    fun `genre and studio hours overlap across titles`() {
        val recap = build()
        assertEquals(
            listOf(NamedMinutes("Action", 483), NamedMinutes("Mecha", 251), NamedMinutes("Sci-Fi", 232), NamedMinutes("Adventure", 96), NamedMinutes("Comedy", 96)),
            recap.genres,
        )
        assertEquals(listOf(NamedMinutes("Khara", 251), NamedMinutes("Pierrot", 96)), recap.studios)
    }

    @Test
    fun `repeats count only when asked`() {
        assertEquals(43, build(RecapSettings(countRepeats = true)).episodes)
        assertEquals(41, build().episodes)
    }

    @Test
    fun `manga gets its own summary`() {
        val manga = build().manga!!
        assertEquals(7, manga.chapters)
        assertEquals(1, manga.titles)
        assertEquals(8, manga.topMediaId)
        assertNull(build(RecapSettings(includeManga = false)).manga)
    }

    @Test
    fun `the rhythm reads days, weekdays and the biggest day`() {
        val recap = build()
        assertEquals(Streak(5, LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 6)), recap.streak)
        assertEquals(LocalDate.of(2026, 9, 20), recap.biggestDay.date)
        assertEquals(24, recap.biggestDay.episodes)
        assertEquals(3, recap.biggestDay.leadingMediaId)
        assertEquals(listOf(3), recap.biggestDay.leaders)
        assertEquals(DayOfWeek.SUNDAY, recap.busiestWeekday)
        assertEquals(31, recap.episodesByWeekday.getValue(DayOfWeek.SUNDAY))
    }

    @Test
    fun `titles tied on the biggest day are all named, adult ones only when shown`() {
        val tie = records + listOf(
            anime(5, "watched episode", "16 - 39", at(9, 29, 10)),
            anime(4, "rewatched episode", "1 - 4", at(9, 29, 11)),
            anime(6, "rewatched episode", "1 - 6", at(9, 29, 12)),
            anime(9, "watched episode", "383 - 406", at(9, 29, 13)),
            anime(6, "watched episode", "1 - 24", at(9, 29, 14)),
        )
        val hidden = buildRecap(september, tie, media, RecapSettings(), zone)!!.biggestDay
        assertEquals(LocalDate.of(2026, 9, 29), hidden.date)
        assertEquals(24, hidden.leadingEpisodes)
        assertEquals(listOf(5, 9), hidden.leaders)
        assertEquals(5, hidden.leadingMediaId)
        val shown = buildRecap(september, tie, media, RecapSettings(showAdult = true), zone)!!.biggestDay
        assertEquals(listOf(5, 6, 9), shown.leaders)
    }

    @Test
    fun `equal streaks show the most recent`() {
        val days = listOf(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 11))
        assertEquals(Streak(2, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 11)), longestStreak(days))
    }

    @Test
    fun `days follow the device's time zone`() {
        val lateAugustInUtc = LocalDateTime.of(2026, 8, 31, 22, 30).toEpochSecond(ZoneOffset.UTC)
        val records = listOf(anime(4, "watched episode", "1", lateAugustInUtc))
        assertNotNull(buildRecap(september, records, media, RecapSettings(), ZoneOffset.ofHours(3)))
        assertNull(buildRecap(september, records, media, RecapSettings(), ZoneOffset.UTC))
    }

    @Test
    fun `a franchise grows from related titles by the word they share`() {
        val franchise = build().franchise!!
        assertEquals("Evangelion", franchise.name)
        assertEquals(listOf(1, 2, 3), franchise.mediaIds)
    }

    @Test
    fun `unrelated titles make no franchise`() {
        val records = listOf(anime(4, "watched episode", "1", at(9, 8)), anime(5, "watched episode", "12", at(9, 9)))
        assertNull(buildRecap(september, records, media, RecapSettings(), zone)!!.franchise)
    }

    @Test
    fun `a year counts its months and the shows kept up with while airing`() {
        val zenshuu = title(10, "Zenshuu.", "TV", 12, 24, listOf("Comedy"))
        val late = title(11, "Old Show", "TV", 12, 24, listOf("Drama"))
        val records = records + anime(10, "watched episode", "10 - 12", at(9, 25)) + anime(11, "watched episode", "1 - 3", at(6, 2))
        val airing = mapOf(
            10 to mapOf(10 to at(9, 6), 11 to at(9, 13), 12 to at(9, 20)),
            11 to mapOf(1 to at(1, 3), 2 to at(1, 10), 3 to at(1, 17)),
        )
        val year = buildRecap(RecapPeriod.Year(2026), records, media + (10 to zenshuu) + (11 to late), RecapSettings(), zone, airing)!!
        assertEquals(1, year.episodesByMonth!!.getValue(Month.AUGUST))
        assertEquals(44, year.episodesByMonth.getValue(Month.SEPTEMBER))
        assertEquals(Month.SEPTEMBER, year.busiestMonth)
        assertEquals(Month.AUGUST, year.quietestMonth)
        assertEquals(1, year.keptUpWhileAiring)
        assertNull(buildRecap(RecapPeriod.Year(2026), records, media + (10 to zenshuu) + (11 to late), RecapSettings(), zone)!!.keptUpWhileAiring)
        assertNull(build().episodesByMonth)
        assertNull(buildRecap(september, records, media + (10 to zenshuu) + (11 to late), RecapSettings(), zone, airing)!!.keptUpWhileAiring)
    }

    @Test
    fun `keeping up means a new episode within two weeks of airing`() {
        val show = title(10, "Zenshuu.", "TV", 12, 24, listOf("Comedy"))
        val year = RecapPeriod.Year(2026)
        fun keptUp(records: List<ListActivityRecord>, aired: Map<Int, Long>, format: String = "TV") =
            buildRecap(year, records, media + (10 to show.copy(format = format)), RecapSettings(countRepeats = true), zone, mapOf(10 to aired))!!.keptUpWhileAiring
        val fortnightLate = listOf(anime(10, "watched episode", "5", at(9, 25)))
        assertEquals(1, keptUp(fortnightLate, mapOf(5 to at(9, 11))))
        assertEquals(0, keptUp(fortnightLate, mapOf(5 to at(9, 10))))
        assertEquals(1, keptUp(listOf(anime(10, "watched episode", "5", at(9, 9, 21))), mapOf(5 to at(9, 10, 20))))
        assertEquals(0, keptUp(listOf(anime(10, "rewatched episode", "5", at(9, 12))), mapOf(5 to at(9, 11))))
        assertEquals(0, keptUp(listOf(anime(10, "watched episode", "1", at(9, 12))), mapOf(1 to at(9, 11)), format = "MOVIE"))
        val lastDecember = LocalDateTime.of(2025, 12, 28, 20, 0).atZone(zone).toEpochSecond()
        assertEquals(0, keptUp(listOf(anime(10, "watched episode", "12", at(1, 5))), mapOf(12 to lastDecember)))
    }

    @Test
    fun `an update worth more than a day of watching is a catch-up`() {
        val longRunner = title(12, "ONE PIECE", "TV", null, 24, listOf("Action"))
        val records = records + listOf(
            anime(12, "watched episode", "1 - 1175", at(9, 26)),
            anime(12, "watched episode", "1176", at(9, 27)),
        )
        val recap = buildRecap(september, records, media + (12 to longRunner), RecapSettings(), zone)!!
        assertEquals(build().episodes + 1, recap.episodes)
        assertEquals(1, recap.ranked.single { it.mediaId == 12 }.episodes)
        assertEquals(1176..1176, recap.ranked.single { it.mediaId == 12 }.watched)
    }

    @Test
    fun `a completion worth more than a day adds no finish, and manga reads five minutes a chapter`() {
        val long = title(13, "Gintama", "TV", 500, 24, listOf("Comedy"))
        val long2 = RecapMedia(14, RecapMediaType.MANGA, "Kingdom", "MANGA", null, 900, null, listOf("Action"), null, false, null, null)
        val records = records + listOf(
            anime(13, "watched episode", "2", at(8, 3)),
            anime(13, "completed", null, at(9, 26)),
            manga(14, "read chapter", "1 - 300", at(9, 26)),
            manga(14, "read chapter", "301 - 580", at(9, 27)),
        )
        val recap = buildRecap(september, records, media + (13 to long) + (14 to long2), RecapSettings(), zone)!!
        assertEquals(build().episodes, recap.episodes)
        assertEquals(build().finished, recap.finished)
        assertTrue(recap.ranked.none { it.mediaId == 13 })
        assertEquals(build().manga!!.chapters + 280, recap.manga!!.chapters)
    }

    private fun at(month: Int, day: Int, hour: Int = 20) =
        LocalDateTime.of(2026, month, day, hour, 0).atZone(zone).toEpochSecond()

    private var nextId = 1

    private fun anime(mediaId: Int, status: String, progress: String?, at: Long) =
        ListActivityRecord(nextId++, mediaId, RecapMediaType.ANIME, status, progress, at)

    private fun manga(mediaId: Int, status: String, progress: String?, at: Long) =
        ListActivityRecord(nextId++, mediaId, RecapMediaType.MANGA, status, progress, at)

    private fun title(
        id: Int,
        title: String,
        format: String,
        episodes: Int?,
        duration: Int?,
        genres: List<String>,
        studio: String? = null,
        adult: Boolean = false,
        relations: List<Int> = emptyList(),
        start: LocalDate? = null,
        end: LocalDate? = null,
    ) = RecapMedia(id, RecapMediaType.ANIME, title, format, episodes, null, duration, genres, studio, adult, start, end, relations)
}
