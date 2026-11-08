package com.anisync.android.domain.recap

import com.anisync.android.domain.MediaTheme
import com.anisync.android.domain.ThemeAudio
import com.anisync.android.domain.ThemeType
import com.anisync.android.domain.ThemeVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThemeSongPickTest {

    private val bleach = listOf(
        theme(1, ThemeType.OP, 1, "1-25"),
        theme(2, ThemeType.OP, 2, "26-51"),
        theme(3, ThemeType.ED, 1, "1-13"),
    )

    @Test
    fun `the opening that covers the episodes watched plays`() {
        assertEquals("OP2", pick(26..35, bleach)!!.theme.slug)
        assertEquals("OP1", pick(3..9, bleach)!!.theme.slug)
    }

    @Test
    fun `a range across two openings takes the one covering more of it`() {
        assertEquals("OP1", pick(20..30, bleach)!!.theme.slug)
        assertEquals("OP2", pick(24..30, bleach)!!.theme.slug)
    }

    @Test
    fun `episodes past every known opening take the latest one`() {
        assertEquals("OP2", pick(400..402, bleach)!!.theme.slug)
    }

    @Test
    fun `no episode range plays the first opening`() {
        assertEquals("OP1", pick(null, bleach)!!.theme.slug)
    }

    @Test
    fun `a film plays its first ending`() {
        val film = listOf(theme(1, ThemeType.OP, 1, null), theme(2, ThemeType.ED, 2, null), theme(3, ThemeType.ED, 1, null))
        val choice = pickThemeSong(listOf(ThemeSongCandidate(7, isFilm = true, watched = 1..1))) { film }!!
        assertEquals("ED1", choice.theme.slug)
        assertEquals(7, choice.mediaId)
    }

    @Test
    fun `a title without an opening plays its first ending`() {
        val endingsOnly = listOf(theme(1, ThemeType.ED, 1, "1-12"), theme(2, ThemeType.IN, null, null))
        assertEquals("ED1", pick(1..12, endingsOnly)!!.theme.slug)
    }

    @Test
    fun `a title AnimeThemes lacks passes to the next of the top five`() {
        val candidates = listOf(
            ThemeSongCandidate(1, isFilm = false, watched = 1..3),
            ThemeSongCandidate(2, isFilm = false, watched = 1..3),
            ThemeSongCandidate(3, isFilm = false, watched = 26..30),
        )
        val noAudio = listOf(theme(9, ThemeType.OP, 1, "1-12", audio = false))
        val choice = pickThemeSong(candidates) { id ->
            when (id) {
                1 -> null
                2 -> noAudio
                else -> bleach
            }
        }!!
        assertEquals(3, choice.mediaId)
        assertEquals("OP2", choice.theme.slug)
    }

    @Test
    fun `no match means silence`() {
        assertNull(pickThemeSong(listOf(ThemeSongCandidate(1, false, 1..3))) { emptyList() })
        assertNull(pickThemeSong(emptyList()) { bleach })
    }

    @Test
    fun `the plain cut wins over a dub of the same slot`() {
        val withDub = listOf(theme(2, ThemeType.OP, 1, "1-25", slug = "OP1-EN"), theme(1, ThemeType.OP, 1, "1-25"))
        assertEquals("OP1", pick(1..5, withDub)!!.theme.slug)
    }

    @Test
    fun `spoiler and NSFW flags on the video do not stop the song`() {
        val themes = listOf(theme(1, ThemeType.OP, 1, "1-25", spoiler = true, nsfw = true), theme(2, ThemeType.OP, 2, "26-51"))
        assertEquals("OP1", pick(1..5, themes)!!.theme.slug)
    }

    @Test
    fun `a film plays its spoiler flagged ending`() {
        val shinEvangelion = listOf(theme(1, ThemeType.ED, 1, null, spoiler = true))
        val choice = pickThemeSong(listOf(ThemeSongCandidate(3786, isFilm = true, watched = 1..1))) { shinEvangelion }!!
        assertEquals("ED1", choice.theme.slug)
    }

    @Test
    fun `the cut covering the episodes is the one played`() {
        val twoCuts = MediaTheme(
            id = 1, type = ThemeType.OP, sequence = 1, slug = "OP1", songTitle = "Song", artists = emptyList(),
            versions = listOf(version(10, "1-10", audio = true), version(11, "11-24", audio = true)),
        )
        val choice = pick(15..20, listOf(twoCuts))!!
        assertEquals(11, choice.version.id)
        assertEquals("https://a.animethemes.moe/11.ogg", choice.audio.url)
    }

    private fun pick(watched: IntRange?, themes: List<MediaTheme>) =
        pickThemeSong(listOf(ThemeSongCandidate(1, isFilm = false, watched = watched))) { themes }

    private fun theme(
        id: Int,
        type: ThemeType,
        sequence: Int?,
        episodes: String?,
        slug: String = type.name + (sequence ?: ""),
        audio: Boolean = true,
        spoiler: Boolean = false,
        nsfw: Boolean = false,
    ) = MediaTheme(
        id = id,
        type = type,
        sequence = sequence,
        slug = slug,
        songTitle = "Song $id",
        artists = listOf("Artist"),
        versions = listOf(version(id, episodes, audio, spoiler, nsfw)),
    )

    private fun version(id: Int, episodes: String?, audio: Boolean, spoiler: Boolean = false, nsfw: Boolean = false) = ThemeVersion(
        id = id,
        version = 1,
        rawEpisodes = episodes,
        notes = null,
        spoiler = spoiler,
        nsfw = nsfw,
        videos = emptyList(),
        audio = if (audio) ThemeAudio("https://a.animethemes.moe/$id.ogg", 3_500_000) else null,
    )
}
