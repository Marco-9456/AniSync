package com.anisync.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the reading of AnimeThemes' `episodes` field, which is free text rather than numbers.
 * Every shape here came off the live API: plain ranges, comma-separated lists with holes in
 * them, single episodes, open ranges written with a trailing dash, and the null the API sends
 * for films and for entries nobody has filled in.
 */
class EpisodeSpanTest {

    @Test
    fun `a plain range parses to one span`() {
        assertEquals(listOf(EpisodeSpan(1, 13)), parseEpisodeSpans("1-13"))
    }

    @Test
    fun `a list keeps the hole between its parts`() {
        val spans = parseEpisodeSpans("20-22, 24")
        assertEquals(listOf(EpisodeSpan(20, 22), EpisodeSpan(24, 24)), spans)
        assertTrue(spans.none { it.contains(23) })
        assertTrue(spans.any { it.contains(24) })
    }

    @Test
    fun `a single episode is a span of one`() {
        assertEquals(listOf(EpisodeSpan(5, 5)), parseEpisodeSpans("5"))
    }

    @Test
    fun `a trailing dash leaves the range open`() {
        val spans = parseEpisodeSpans("13-")
        assertEquals(listOf(EpisodeSpan(13, null)), spans)
        assertTrue(spans.single().isOpen)
        assertTrue(spans.single().contains(999))
    }

    @Test
    fun `missing episode data reads as no spans rather than episode zero`() {
        assertEquals(emptyList<EpisodeSpan>(), parseEpisodeSpans(null))
        assertEquals(emptyList<EpisodeSpan>(), parseEpisodeSpans(""))
        assertEquals(emptyList<EpisodeSpan>(), parseEpisodeSpans("None"))
    }

    @Test
    fun `unparseable parts are dropped instead of guessed at`() {
        assertEquals(listOf(EpisodeSpan(3, 4)), parseEpisodeSpans("3-4, ova, 9-2"))
    }

    @Test
    fun `touching ranges merge so a bar never draws an episode twice`() {
        assertEquals(
            listOf(EpisodeSpan(1, 12)),
            mergeEpisodeSpans(listOf(EpisodeSpan(1, 8), EpisodeSpan(9, 12)))
        )
        assertEquals(
            listOf(EpisodeSpan(1, 8), EpisodeSpan(10, 12)),
            mergeEpisodeSpans(listOf(EpisodeSpan(10, 12), EpisodeSpan(1, 8)))
        )
    }

    @Test
    fun `formatting matches the way the API writes a range`() {
        assertEquals("1–13", formatEpisodeSpans(parseEpisodeSpans("1-13")))
        assertEquals("14–22, 24", formatEpisodeSpans(parseEpisodeSpans("14-22, 24")))
        assertEquals("5", formatEpisodeSpans(parseEpisodeSpans("5")))
        assertEquals("13+", formatEpisodeSpans(parseEpisodeSpans("13-")))
    }

    @Test
    fun `coverage counts episodes and closes an open range against the total`() {
        assertEquals(13, countCoveredEpisodes(parseEpisodeSpans("1-13"), 24))
        assertEquals(10, countCoveredEpisodes(parseEpisodeSpans("14-22, 24"), 24))
        assertEquals(12, countCoveredEpisodes(parseEpisodeSpans("13-"), 24))
        assertEquals(0, countCoveredEpisodes(parseEpisodeSpans("13-"), null))
    }

    @Test
    fun `a theme merges the ranges of all of its versions`() {
        val theme = MediaTheme(
            id = 1,
            type = ThemeType.ED,
            sequence = 2,
            slug = "ED2",
            songTitle = "Kirakira no Hai",
            artists = listOf("Regal Lily"),
            versions = listOf(
                version(id = 1, episodes = "14-19"),
                version(id = 2, episodes = "20-22, 24")
            )
        )

        assertEquals(
            listOf(EpisodeSpan(14, 22), EpisodeSpan(24, 24)),
            theme.episodeSpans
        )
        assertEquals("14–22, 24", formatEpisodeSpans(theme.episodeSpans))
    }

    @Test
    fun `the preferred video is the creditless one at the highest resolution`() {
        val entry = ThemeVersion(
            id = 1,
            version = 1,
            rawEpisodes = "1-13",
            notes = null,
            spoiler = false,
            nsfw = false,
            videos = listOf(
                video(id = 1, creditless = false, resolution = 720),
                video(id = 2, creditless = true, resolution = 1080),
                video(id = 3, creditless = true, resolution = 720)
            )
        )
        assertEquals(2, entry.preferredVideo?.id)
    }

    @Test
    fun `a dub theme keeps the part of its slug the readable name drops`() {
        val dub = theme(slug = "ED11-EN", type = ThemeType.ED, sequence = 11)
        val original = theme(slug = "ED11", type = ThemeType.ED, sequence = 11)
        val edit = theme(slug = "OP1-EN4Kids", type = ThemeType.OP, sequence = 1)

        assertEquals("EN", dub.qualifier)
        assertEquals(null, original.qualifier)
        assertEquals("EN4Kids", edit.qualifier)
    }

    @Test
    fun `a plain cut wins over a lyrics one when neither is creditless`() {
        val entry = ThemeVersion(
            id = 1,
            version = 1,
            rawEpisodes = "493-516",
            notes = null,
            spoiler = false,
            nsfw = false,
            videos = listOf(
                video(id = 1, creditless = false, resolution = 720, lyrics = true),
                video(id = 2, creditless = false, resolution = 720)
            )
        )
        assertEquals(2, entry.preferredVideo?.id)
    }

    @Test
    fun `an airing show measures coverage against the episodes that have aired`() {
        // Bleach: finished, AniList gives the real total.
        assertEquals(366, coverageEpisodeCount(episodes = 366, nextAiringEpisode = null))
        // One Piece: still airing, so the total comes off the next episode number.
        assertEquals(1174, coverageEpisodeCount(episodes = null, nextAiringEpisode = 1175))
        // Nothing has aired yet, which is no scale rather than a scale of zero.
        assertEquals(null, coverageEpisodeCount(episodes = null, nextAiringEpisode = 1))
        assertEquals(null, coverageEpisodeCount(episodes = null, nextAiringEpisode = null))
    }

    private fun theme(slug: String, type: ThemeType, sequence: Int) = MediaTheme(
        id = 1,
        type = type,
        sequence = sequence,
        slug = slug,
        songTitle = "Song",
        artists = emptyList(),
        versions = listOf(version(id = 1, episodes = "1-2"))
    )

    private fun version(id: Int, episodes: String?) = ThemeVersion(
        id = id,
        version = id,
        rawEpisodes = episodes,
        notes = null,
        spoiler = false,
        nsfw = false,
        videos = listOf(video(id = id, creditless = true, resolution = 1080))
    )

    private fun video(
        id: Int,
        creditless: Boolean,
        resolution: Int,
        lyrics: Boolean = false
    ) = ThemeVideo(
        id = id,
        url = "https://v.animethemes.moe/Example-$id.webm",
        resolution = resolution,
        creditless = creditless,
        subbed = false,
        lyrics = lyrics,
        source = "BD"
    )
}
