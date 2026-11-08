package com.anisync.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.materialkolor.PaletteStyle
import java.util.concurrent.ConcurrentHashMap

/**
 * Every AniList genre has a fixed seed run through TonalSpot, so a genre keeps its hue in light,
 * dark and every app palette. The drawn genres were matched to the design's fills exactly.
 */
private val GenreSeeds: Map<String, Color> = mapOf(
    "Action" to Color(0xFF9D6A65),
    "Adventure" to Color(0xFF008859),
    "Comedy" to Color(0xFF8D7444),
    "Drama" to Color(0xFF617995),
    "Ecchi" to Color(0xFFBA5D38),
    "Fantasy" to Color(0xFF806DA9),
    "Hentai" to Color(0xFFA76900),
    "Horror" to Color(0xFF658022),
    "Mahou Shoujo" to Color(0xFF9D60AB),
    "Mecha" to Color(0xFF457CA4),
    "Music" to Color(0xFFB95F00),
    "Mystery" to Color(0xFF3F8F8F),
    "Psychological" to Color(0xFF6A73AD),
    "Romance" to Color(0xFF9A6A75),
    "Sci-Fi" to Color(0xFF00829A),
    "Slice of Life" to Color(0xFF627E5E),
    "Sports" to Color(0xFF837900),
    "Supernatural" to Color(0xFF946B81),
    "Thriller" to Color(0xFF008673),
)

/** The fill and the content color a genre wears, from its seed's primary container roles. */
@Immutable
data class GenreColor(val container: Color, val content: Color)

private val resolved = ConcurrentHashMap<Pair<String, Boolean>, GenreColor>()

/** The seed behind [genre], or null for a genre AniList does not list. */
fun genreSeed(genre: String): Color? = GenreSeeds[genre]

/** [genre]'s colors in the given polarity, or null when the genre has no seed. */
fun genreColor(genre: String, dark: Boolean): GenreColor? {
    val seed = GenreSeeds[genre] ?: return null
    return resolved.getOrPut(genre to dark) {
        val roles = paletteRoles(seed, dark, PaletteStyle.TonalSpot)
        GenreColor(roles.primaryContainer(), roles.onPrimaryContainer())
    }
}

/** [genre]'s colors in the polarity of the surrounding scheme, so a light card gets light ones. */
@Composable
@ReadOnlyComposable
fun genreColor(genre: String): GenreColor? =
    genreColor(genre, dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f)
