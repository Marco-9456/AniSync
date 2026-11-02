package com.anisync.android.data

import com.anisync.android.GetRecapMediaQuery
import com.anisync.android.domain.recap.ListActivityRecord
import com.anisync.android.domain.recap.RecapMedia
import com.anisync.android.domain.recap.RecapMediaType
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaSeason
import com.anisync.android.type.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RecapPayloadTest {

    private val zone = ZoneId.of("Asia/Riyadh")

    private val film = RecapMedia(
        id = 1, type = RecapMediaType.ANIME, title = "Shin Evangelion Movie:||", format = "MOVIE",
        episodes = 1, chapters = null, duration = 155, genres = listOf("Action", "Mecha"), mainStudio = "Khara",
        isAdult = false, startDate = LocalDate.of(2021, 3, 8), endDate = LocalDate.of(2021, 3, 8),
        relationIds = listOf(2, 3), titleRomaji = "Shin Evangelion Gekijouban:||", titleEnglish = "Evangelion: 3.0+1.0",
        titleNative = "シン・エヴァンゲリオン劇場版", coverUrl = "https://s4.anilist.co/cover.jpg", color = "#e4a15d",
        season = "WINTER", seasonYear = 2021,
    )
    private val manga = RecapMedia(8, RecapMediaType.MANGA, "Gabriel Dropout", "MANGA", null, 140, null, listOf("Comedy"), null, false, null, null)

    private val records = listOf(
        ListActivityRecord(10, 1, RecapMediaType.ANIME, "completed", null, 1_790_000_000),
        ListActivityRecord(11, 8, RecapMediaType.MANGA, "read chapter", "134 - 139", 1_790_100_000),
    )

    @Test
    fun `a stored payload reads back as the same activities and titles`() {
        val stored = recapPayload(zone, records, mapOf(1 to film, 8 to manga, 99 to film.copy(id = 99))).encode()
        val payload = decodeRecapPayload(stored)!!
        assertEquals(zone, payload.zoneId)
        assertEquals(mapOf(1 to film, 8 to manga), payload.mediaById())
        assertEquals(records, payload.records())
    }

    @Test
    fun `defaults and missing facts stay out of the stored text`() {
        val stored = recapPayload(zone, records.take(1), mapOf(1 to film.copy(color = null, season = null))).encode()
        assertTrue("\"cl\"" !in stored && "\"se\"" !in stored)
        assertTrue("\"x\"" !in stored)
        assertTrue(stored.startsWith("{\"v\":$RECAP_PAYLOAD_VERSION,"))
    }

    @Test
    fun `an activity whose title is missing is dropped`() {
        val payload = recapPayload(zone, records, mapOf(1 to film))
        assertEquals(listOf(10), payload.records().map { it.id })
    }

    @Test
    fun `another payload version or broken text reads as nothing`() {
        val other = recapPayload(zone, records, mapOf(1 to film)).copy(version = RECAP_PAYLOAD_VERSION + 1).encode()
        assertNull(decodeRecapPayload(other))
        assertNull(decodeRecapPayload("{\"v\":1"))
    }

    @Test
    fun `fuzzy dates widen to the span they allow`() {
        assertEquals(LocalDate.of(2025, 1, 1), fuzzyStart(2025, null, null))
        assertEquals(LocalDate.of(2025, 12, 31), fuzzyEnd(2025, null, null))
        assertEquals(LocalDate.of(2024, 2, 29), fuzzyEnd(2024, 2, null))
        assertEquals(LocalDate.of(2025, 4, 6), fuzzyStart(2025, 4, 6))
        assertNull(fuzzyStart(null, 4, 6))
    }

    @Test
    fun `a media batch item maps every fact a recap uses`() {
        val item = GetRecapMediaQuery.Medium(
            __typename = "Media", id = 1, type = MediaType.ANIME, format = MediaFormat.MOVIE, episodes = 1, chapters = null,
            duration = 155, genres = listOf("Action", null, "Mecha"), isAdult = null, season = MediaSeason.WINTER, seasonYear = 2021,
            title = GetRecapMediaQuery.Title("MediaTitle", "Shin Evangelion Gekijouban:||", "Evangelion: 3.0+1.0", "シン・エヴァンゲリオン劇場版", "Shin Evangelion Movie:||"),
            startDate = GetRecapMediaQuery.StartDate("FuzzyDate", 2021, 3, 8),
            endDate = GetRecapMediaQuery.EndDate("FuzzyDate", 2021, 3, 8),
            coverImage = GetRecapMediaQuery.CoverImage("MediaCoverImage", "https://s4.anilist.co/cover.jpg", "#e4a15d"),
            studios = GetRecapMediaQuery.Studios("StudioConnection", listOf(null, GetRecapMediaQuery.Node("Studio", "Khara", 6145))),
            relations = GetRecapMediaQuery.Relations("MediaConnection", listOf(GetRecapMediaQuery.Node1("Media", 2), null, GetRecapMediaQuery.Node1("Media", 3))),
        )
        assertEquals(film, item.toRecapMedia())
        assertNull(item.copy(type = null).toRecapMedia())
    }
}
