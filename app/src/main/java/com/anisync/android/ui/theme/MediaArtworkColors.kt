package com.anisync.android.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.rememberDynamicColorScheme

/**
 * The lowest saturation a cover color may have and still be worth a palette.
 *
 * MaterialKolor keeps the seed's hue and forces its own chroma, so a grey seed (monochrome covers,
 * the odd white background poster) grows a grey scheme: the same flat page an artwork palette
 * exists to replace, minus the app's own accent.
 */
private const val MinSeedSaturation = 0.10f

/**
 * Resolves AniList's `Media.coverImage.color` to a MaterialKolor seed, or null to keep the app
 * theme. AniList derives that `#rrggbb` hex from the cover art itself, so a media page can wear
 * its own artwork without decoding a bitmap.
 */
fun mediaArtworkSeedColor(coverColor: String?): Color? {
    val hex = coverColor?.trim()?.removePrefix("#") ?: return null
    if (hex.length != 6) return null
    val rgb = hex.toIntOrNull(16) ?: return null
    val red = (rgb shr 16) and 0xFF
    val green = (rgb shr 8) and 0xFF
    val blue = rgb and 0xFF
    val max = maxOf(red, green, blue)
    val saturation = if (max == 0) 0f else (max - minOf(red, green, blue)).toFloat() / max
    if (saturation < MinSeedSaturation) return null
    return Color(0xFF000000.toInt() or rgb)
}

/**
 * A MaterialKolor scheme grown from [seed] in the viewer's own polarity and [paletteStyle], with
 * our AMOLED container shift when they run pure black. Only the hue changes.
 *
 * A null [seed] hands back the current scheme untouched so a caller can wrap unconditionally. On a
 * page whose artwork color arrives with its data, branching around the content instead would drop
 * and rebuild the whole subtree in the middle of the shared element transition.
 */
@Composable
fun mediaArtworkColorScheme(
    seed: Color?,
    darkTheme: Boolean,
    amoled: Boolean,
    paletteStyle: PaletteStyle,
): ColorScheme {
    if (seed == null) return MaterialTheme.colorScheme
    // isAmoled stays off here for the reason AppTheme leaves it off.
    val base = rememberDynamicColorScheme(
        seedColor = seed,
        isDark = darkTheme,
        isAmoled = false,
        style = paletteStyle,
        specVersion = ColorSpec.SpecVersion.SPEC_2026
    )
    return if (amoled && darkTheme) base.toAmoled() else base
}
