package de.mrxxxxx.anisyncplus.calendar.matching

import de.mrxxxxx.anisyncplus.calendar.domain.AniListMatchCandidate
import de.mrxxxxx.anisyncplus.calendar.domain.AniWorldRelease
import de.mrxxxxx.anisyncplus.calendar.domain.AniWorldReleaseLanguage
import de.mrxxxxx.anisyncplus.calendar.domain.MATCHER_VERSION
import de.mrxxxxx.anisyncplus.calendar.domain.MatchStatus
import de.mrxxxxx.anisyncplus.calendar.domain.ReleaseDiagnosticStatus
import de.mrxxxxx.anisyncplus.calendar.domain.ReleaseKind
import de.mrxxxxx.anisyncplus.calendar.parser.TitleNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DefaultAniWorldTitleMatcherTest {
    private val matcher = DefaultAniWorldTitleMatcher()

    @Test
    fun `matches exact preferred English Romaji native and synonym variants`() {
        val variants = listOf(
            candidate(1, preferred = "Source Title"),
            candidate(2, preferred = "Other", english = "Source Title"),
            candidate(3, preferred = "Other", romaji = "Source Title"),
            candidate(4, preferred = "Other", native = "Source Title"),
            candidate(5, preferred = "Other", synonyms = listOf("Source Title"))
        )
        variants.forEach { candidate ->
            val result = matcher.match(release("Source Title"), listOf(candidate))
            assertEquals(candidate.mediaId, result.candidate?.mediaId)
            assertEquals(MatchStatus.MATCHED, result.status)
            assertEquals("exact_normalized_title", result.reason)
            assertEquals(MATCHER_VERSION, result.matcherVersion)
        }
    }

    @Test
    fun `apostrophes unicode dashes and punctuation normalize deterministically`() {
        val source = release("Café D’Art — Re-Born!")
        val candidate = candidate(1, preferred = "Cafe D'Art Re Born")
        assertEquals(MatchStatus.MATCHED, matcher.match(source, listOf(candidate)).status)
    }

    @Test
    fun `season notation and season context disambiguate exact candidates`() {
        val source = release("Example Staffel II", season = 2)
        val first = candidate(1, preferred = "Example Season 2", season = 1)
        val second = candidate(2, preferred = "Example Season 2", season = 2)
        val result = matcher.match(source, listOf(first, second))
        assertEquals(MatchStatus.MATCHED, result.status)
        assertEquals(2, result.candidate?.mediaId)
        assertEquals("exact_title_and_season", result.reason)
    }

    @Test
    fun `same named remakes fail closed as ambiguous`() {
        val result = matcher.match(
            release("Remake"),
            listOf(candidate(1, "Remake"), candidate(2, "Remake"))
        )
        assertEquals(MatchStatus.AMBIGUOUS, result.status)
        assertNull(result.candidate)
        assertEquals(0.0, result.scoreMargin!!, 0.0)
    }

    @Test
    fun `similarity needs threshold and clear score margin`() {
        val matched = matcher.match(
            release("The Long Adventure Chronicle"),
            listOf(
                candidate(1, "The Long Adventure Chronicles"),
                candidate(2, "Unrelated Cooking Show")
            )
        )
        assertEquals(MatchStatus.MATCHED, matched.status)
        assertTrue(matched.scoreMargin!! >= 0.08)

        val ambiguous = matcher.match(
            release("Adventure Chronicle"),
            listOf(
                candidate(3, "Adventure Chronicles"),
                candidate(4, "Adventure Chronicle Z")
            )
        )
        assertEquals(MatchStatus.AMBIGUOUS, ambiguous.status)
        assertNull(ambiguous.candidate)
    }

    @Test
    fun `years and title additions prevent false automatic match`() {
        val result = matcher.match(
            release("Legend 2026 Rebirth"),
            listOf(candidate(1, "Legend 1999"), candidate(2, "Legend Rebirth Side Story"))
        )
        assertTrue(result.status != MatchStatus.MATCHED)
        assertNull(result.candidate)
    }

    @Test
    fun `no candidates is unmatched with versioned diagnostics`() {
        val result = matcher.match(release("Nothing"), emptyList())
        assertEquals(MatchStatus.UNMATCHED, result.status)
        assertEquals(0, result.candidateCount)
        assertEquals("no_candidates", result.reason)
        assertEquals(MATCHER_VERSION, result.matcherVersion)
    }

    private fun release(title: String, season: Int? = null) = AniWorldRelease(
        localId = "local",
        snapshotId = "snapshot",
        sourceSeriesKey = "title:${TitleNormalizer.normalize(title)}",
        sourceSlug = null,
        sourceUrl = null,
        rawTitle = title,
        normalizedTitle = TitleNormalizer.normalize(title),
        sourceDate = LocalDate.of(2026, 7, 13),
        sourceLocalTime = LocalTime.NOON,
        sourceZoneId = ZoneId.of("Europe/Berlin"),
        resolvedInstant = Instant.parse("2026-07-13T10:00:00Z"),
        isApproximate = false,
        releaseKind = ReleaseKind.EPISODE,
        seasonNumber = season,
        episodeNumber = 1,
        installmentNumber = null,
        rawInstallmentToken = "S01E01",
        language = AniWorldReleaseLanguage.DE_SUB,
        rawLanguageMarkers = emptySet(),
        sourceOrder = 0,
        diagnosticStatus = ReleaseDiagnosticStatus.VALID
    )

    private fun candidate(
        id: Int,
        preferred: String,
        english: String? = null,
        romaji: String? = null,
        native: String? = null,
        synonyms: List<String> = emptyList(),
        season: Int? = null
    ) = AniListMatchCandidate(
        mediaId = id,
        titleUserPreferred = preferred,
        titleEnglish = english,
        titleRomaji = romaji,
        titleNative = native,
        synonyms = synonyms,
        seasonNumber = season
    )
}
