package de.mrxxxxx.anisyncplus.calendar.parser

import de.mrxxxxx.anisyncplus.calendar.domain.AniWorldReleaseLanguage
import de.mrxxxxx.anisyncplus.calendar.domain.ReleaseDiagnosticStatus
import de.mrxxxxx.anisyncplus.calendar.domain.ReleaseKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class JsoupAniWorldCalendarParserTest {
    private val parser = JsoupAniWorldCalendarParser()
    private val fetchedAt = Instant.parse("2026-07-13T10:00:00Z")

    @Test
    fun `reference page parses German sub dub approximate time and stable order`() {
        val result = parse("aniworld_calendar_reference.html")

        assertEquals(2, result.daySectionCount)
        assertEquals(2, result.releases.size)
        assertEquals(2, result.visibleGermanCount)
        val sub = result.releases[0]
        assertEquals("Example Show", sub.rawTitle)
        assertEquals("example-show", sub.sourceSlug)
        assertEquals(ReleaseKind.EPISODE, sub.releaseKind)
        assertEquals(1, sub.seasonNumber)
        assertEquals(3, sub.episodeNumber)
        assertEquals(AniWorldReleaseLanguage.DE_SUB, sub.language)
        assertTrue(sub.isApproximate)
        assertEquals(Instant.parse("2026-07-13T13:40:00Z"), sub.resolvedInstant)
        assertEquals(AniWorldReleaseLanguage.DE_DUB, result.releases[1].language)
        assertTrue(result.releases.zipWithNext().all { (left, right) -> left.sourceOrder < right.sourceOrder })
    }

    @Test
    fun `same episode at different times remains separate`() {
        val releases = parse("aniworld_calendar_multi_time.html").releases
        assertEquals(2, releases.size)
        assertEquals(2, releases.map { it.sourceLocalTime }.distinct().size)
    }

    @Test
    fun `different episodes at same time remain separate`() {
        val releases = parse("aniworld_calendar_multi_episode.html").releases
        assertEquals(listOf(7, 8), releases.map { it.episodeNumber })
        assertEquals(1, releases.map { it.sourceLocalTime }.distinct().size)
    }

    @Test
    fun `same release merges German sub and dub and removes exact duplicate`() {
        val releases = parse("aniworld_calendar_same_time_bilingual.html").releases
        assertEquals(1, releases.size)
        assertEquals(AniWorldReleaseLanguage.DE_SUB_AND_DUB, releases.single().language)
        assertTrue(releases.single().rawLanguageMarkers.any { "japanese-german" in it })
        assertTrue(releases.single().rawLanguageMarkers.any { "/german.svg" in it })
    }

    @Test
    fun `film special and unknown tokens retain their semantics`() {
        val releases = parse("aniworld_calendar_film_special.html").releases
        assertEquals(ReleaseKind.FILM, releases[0].releaseKind)
        assertEquals(1, releases[0].installmentNumber)
        assertNull(releases[0].episodeNumber)
        assertEquals(ReleaseKind.SPECIAL, releases[1].releaseKind)
        assertEquals(2, releases[1].installmentNumber)
        assertNull(releases[1].episodeNumber)
        assertEquals(ReleaseKind.UNKNOWN, releases[2].releaseKind)
        assertEquals("OVA-X", releases[2].rawInstallmentToken)
    }

    @Test
    fun `English and unknown languages are diagnostic but not visible German`() {
        val result = parse("aniworld_calendar_unknown_language.html")
        assertEquals(AniWorldReleaseLanguage.EN_SUB, result.releases[0].language)
        assertEquals(AniWorldReleaseLanguage.UNKNOWN, result.releases[1].language)
        assertEquals(ReleaseDiagnosticStatus.UNKNOWN_LANGUAGE, result.releases[1].diagnosticStatus)
        assertEquals(0, result.visibleGermanCount)
    }

    @Test
    fun `missing link installment and time are retained without invention`() {
        val release = parse("aniworld_calendar_missing_fields.html").releases.single()
        assertNull(release.sourceUrl)
        assertNull(release.sourceSlug)
        assertEquals(ReleaseKind.UNKNOWN, release.releaseKind)
        assertNull(release.episodeNumber)
        assertNull(release.sourceLocalTime)
        assertNull(release.resolvedInstant)
        assertEquals(ReleaseDiagnosticStatus.MISSING_TIME, release.diagnosticStatus)
    }

    @Test
    fun `formally valid empty calendar is accepted`() {
        val result = parse("aniworld_calendar_empty_valid.html")
        assertTrue(result.releases.isEmpty())
        assertEquals(result.rangeStart, result.rangeEnd)
        assertEquals(1, result.daySectionCount)
    }

    @Test
    fun `block page is rejected separately from DOM change`() {
        val block = assertThrows(AniWorldParseException.BlockPage::class.java) {
            parse("aniworld_calendar_block_page.html")
        }
        assertTrue(block.message.orEmpty().contains("challenge_title_just_a_moment"))
        assertThrows(AniWorldParseException.MissingContainer::class.java) {
            parse("aniworld_calendar_dom_changed.html")
        }
    }

    @Test
    fun `valid calendar structure wins over benign and strong marker words`() {
        val result = parse("aniworld_calendar_security_words_valid.html")

        assertEquals(1, result.daySectionCount)
        assertEquals(1, result.visibleGermanCount)
        assertEquals("Access Denied: Captcha Cloudflare", result.releases.single().rawTitle)
    }

    @Test
    fun `challenge structure is classified only when calendar structure is incomplete`() {
        val challenge = assertThrows(AniWorldParseException.BlockPage::class.java) {
            parser.parse(
                "<div id=\"seriesContainer\"></div><form id=\"challenge-form\"></form>",
                fetchedAt
            )
        }
        assertTrue(challenge.message.orEmpty().contains("cloudflare_challenge_element"))

        assertThrows(AniWorldParseException.MissingContainer::class.java) {
            parser.parse("<p>Access Denied CAPTCHA Cloudflare</p>", fetchedAt)
        }
    }

    @Test
    fun `invalid date and invalid time are rejected`() {
        assertThrows(AniWorldParseException.InvalidDate::class.java) {
            parse("aniworld_calendar_invalid_date.html")
        }
        assertThrows(AniWorldParseException.InvalidTime::class.java) {
            parse("aniworld_calendar_invalid_time.html")
        }
    }

    @Test
    fun `DST gap is not guessed`() {
        val release = parse("aniworld_calendar_dst_gap.html").releases.single()
        assertNull(release.resolvedInstant)
        assertEquals(ReleaseDiagnosticStatus.DST_GAP, release.diagnosticStatus)
    }

    @Test
    fun `DST overlap uses earlier valid offset and records decision`() {
        val release = parse("aniworld_calendar_dst_overlap.html").releases.single()
        assertEquals(ReleaseDiagnosticStatus.DST_OVERLAP_EARLIER_OFFSET, release.diagnosticStatus)
        assertEquals(
            release.sourceDate.atTime(release.sourceLocalTime).toInstant(ZoneOffset.ofHours(2)),
            release.resolvedInstant
        )
        assertFalse(release.isApproximate)
    }

    private fun parse(name: String) = parser.parse(fixture(name), fetchedAt)

    private fun fixture(name: String): String = requireNotNull(
        javaClass.classLoader?.getResource("fixtures/$name")
    ).readText()
}
