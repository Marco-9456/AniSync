package com.anisync.android.presentation.library

import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.MediaType
import de.mrxxxxx.anisyncplus.calendar.domain.AniWorldReleaseLanguage
import de.mrxxxxx.anisyncplus.calendar.domain.EffectiveRelease
import de.mrxxxxx.anisyncplus.calendar.domain.MatchStatus
import de.mrxxxxx.anisyncplus.calendar.domain.ReleaseKind
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class AniWorldLibraryResolverTest {
    @Test
    fun animeCurrentUsesAniWorldCountdownAndBehindData() {
        val entry = entry(MediaType.ANIME, LibraryStatus.CURRENT)
        val next = release(episode = 8, instant = Instant.parse("2026-07-14T18:00:00Z"), approximate = true)

        val result = applyAniWorldLibraryReleases(
            MediaType.ANIME,
            listOf(entry),
            mapOf(entry.mediaId to next),
            mapOf(entry.mediaId to 7)
        ).single()

        assertEquals(8, result.nextAiringEpisode)
        assertEquals(next.instant!!.epochSecond, result.nextAiringEpisodeTime)
        assertEquals(7, result.latestReleasedEpisode)
        assertTrue(result.nextAiringIsApproximate)
        assertNull(result.timeUntilAiring)
    }

    @Test
    fun missingAniWorldDataClearsAniListAiringFallbackForAnimeCurrent() {
        val result = applyAniWorldLibraryReleases(
            MediaType.ANIME,
            listOf(entry(MediaType.ANIME, LibraryStatus.CURRENT)),
            emptyMap(),
            emptyMap()
        ).single()

        assertNull(result.nextAiringEpisode)
        assertNull(result.nextAiringEpisodeTime)
        assertNull(result.latestReleasedEpisode)
        assertNull(result.timeUntilAiring)
    }

    @Test
    fun mangaAndOtherAnimeStatusesRemainUnchanged() {
        val manga = entry(MediaType.MANGA, LibraryStatus.CURRENT)
        val planning = entry(MediaType.ANIME, LibraryStatus.PLANNING)

        assertSame(
            manga,
            applyAniWorldLibraryReleases(MediaType.MANGA, listOf(manga), emptyMap(), emptyMap()).single()
        )
        assertSame(
            planning,
            applyAniWorldLibraryReleases(MediaType.ANIME, listOf(planning), emptyMap(), emptyMap()).single()
        )
    }

    @Test
    fun airingSoonSortKeepsNullLastInBothDirections() {
        assertTrue(compareAiringSoon(null, 10L, ascending = true) > 0)
        assertTrue(compareAiringSoon(null, 10L, ascending = false) > 0)
        assertTrue(compareAiringSoon(10L, 20L, ascending = true) < 0)
        assertTrue(compareAiringSoon(10L, 20L, ascending = false) > 0)
    }

    private fun entry(type: MediaType, status: LibraryStatus) = LibraryEntry(
        id = 1,
        mediaId = 11,
        titleRomaji = "Test",
        titleEnglish = null,
        titleNative = null,
        titleUserPreferred = "Test",
        coverUrl = null,
        progress = 3,
        totalEpisodes = 12,
        totalChapters = null,
        totalVolumes = null,
        type = type,
        status = status,
        nextAiringEpisode = 99,
        timeUntilAiring = 999,
        nextAiringEpisodeTime = 999L
    )

    private fun release(episode: Int, instant: Instant, approximate: Boolean) = EffectiveRelease(
        aniListMediaId = 11,
        sourceTitle = "Test",
        aniListTitle = "Test",
        coverImageUrl = null,
        averageScore = null,
        releaseKind = ReleaseKind.EPISODE,
        seasonNumber = 1,
        episodeNumber = episode,
        installmentNumber = null,
        instant = instant,
        sourceDate = LocalDate.of(2026, 7, 14),
        sourceLocalTime = LocalTime.of(20, 0),
        isApproximate = approximate,
        language = AniWorldReleaseLanguage.DE_SUB,
        matchStatus = MatchStatus.MATCHED,
        libraryStatus = "CURRENT"
    )
}
