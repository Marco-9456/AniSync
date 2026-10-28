package com.anisync.android.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Pins each genre's color to the fills on the Share cards, Stats and Wrapped frames. A seed or a
 * MaterialKolor change that moves a genre off its frame fails here first.
 */
class GenreColorsTest {

    private val drawnDark = mapOf(
        "Action" to (0x764540 to 0xFFDBD7),
        "Adventure" to (0x315B45 to 0xC0EED1),
        "Comedy" to (0x664F1F to 0xFFDFA6),
        "Drama" to (0x3A5471 to 0xD1E5FF),
        "Ecchi" to (0x764634 to 0xFFDCD0),
        "Fantasy" to (0x584C72 to 0xEBDEFF),
        "Mecha" to (0x35566F to 0xCCE6FF),
        "Music" to (0x724929 to 0xFFDDC7),
        "Psychological" to (0x4B5074 to 0xDFE1FF),
        "Romance" to (0x744550 to 0xFFDAE1),
        "Sci-Fi" to (0x285966 to 0xBBEBFA),
        "Slice of Life" to (0x3C5A3A to 0xCAEDC3),
        "Supernatural" to (0x6F465D to 0xFFD9EB),
    )

    private val aniListGenres = listOf(
        "Action", "Adventure", "Comedy", "Drama", "Ecchi", "Fantasy", "Hentai", "Horror",
        "Mahou Shoujo", "Mecha", "Music", "Mystery", "Psychological", "Romance", "Sci-Fi",
        "Slice of Life", "Sports", "Supernatural", "Thriller",
    )

    @Test
    fun `drawn genres match their dark fills`() {
        for ((genre, expected) in drawnDark) {
            val color = genreColor(genre, dark = true)!!
            assertEquals("$genre container", expected.first, rgb(color.container))
            assertEquals("$genre content", expected.second, rgb(color.content))
        }
    }

    @Test
    fun `comedy matches its light fill`() {
        val comedy = genreColor("Comedy", dark = false)!!
        assertEquals(0xFAD286, rgb(comedy.container))
        assertEquals(0x614605, rgb(comedy.content))
    }

    @Test
    fun `drama matches its light fill within one step`() {
        val drama = genreColor("Drama", dark = false)!!
        assertEquals(0xADD2FE, rgb(drama.container))
        assertWithinOneStep(0x20486C, rgb(drama.content))
    }

    @Test
    fun `every AniList genre has its own seed`() {
        aniListGenres.forEach { assertNotNull(it, genreSeed(it)) }
        assertEquals(aniListGenres.size, aniListGenres.mapNotNull { genreSeed(it) }.toSet().size)
    }

    @Test
    fun `an unknown genre has no color`() {
        assertNull(genreColor("Isekai", dark = true))
    }

    private fun rgb(color: Color) = color.toArgb() and 0xFFFFFF

    private fun assertWithinOneStep(expected: Int, actual: Int) {
        for (shift in listOf(16, 8, 0)) {
            val delta = abs(((expected shr shift) and 0xFF) - ((actual shr shift) and 0xFF))
            assertTrue("channel off by $delta", delta <= 1)
        }
    }
}
