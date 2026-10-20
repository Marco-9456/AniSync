package com.anisync.android.ui.theme

import androidx.compose.ui.graphics.Color

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
