package com.anisync.android.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * AniList's named profile colors mapped to their accent hex. These are the seven presets the
 * AniList site (and our own profile-color picker) offers; the hexes match AniList's palette.
 * Insertion order is preserved so callers can iterate it as the picker's swatch order.
 */
val AniListProfileColors: Map<String, Color> = linkedMapOf(
    "blue" to Color(0xFF3DB4F2),
    "purple" to Color(0xFFC063FF),
    "pink" to Color(0xFFFC9DD6),
    "orange" to Color(0xFFEF881A),
    "red" to Color(0xFFE13333),
    "green" to Color(0xFF4CCB48),
    "gray" to Color(0xFF677B94),
)

/**
 * Resolves an AniList `User.options.profileColor` to a MaterialKolor seed color, or null when the
 * caller should fall back to the app theme.
 *
 * AniList returns either one of the seven named colors above or a donator-set hex (`#rrggbb`).
 * `null`, blank, the literal `"default"`, or anything unparseable all mean "no custom color" → use
 * the app's own theme instead of tinting.
 */
fun aniListProfileSeedColor(profileColor: String?): Color? {
    val raw = profileColor?.trim()?.lowercase()
    if (raw.isNullOrEmpty() || raw == "default") return null
    AniListProfileColors[raw]?.let { return it }
    if (raw.startsWith("#")) return parseHexColor(raw.removePrefix("#"))
    return null
}

/** Parses a `rrggbb` or `aarrggbb` hex string (no leading `#`) into a [Color], or null if invalid. */
private fun parseHexColor(hex: String): Color? {
    val argb = when (hex.length) {
        6 -> runCatching { 0xFF000000.toInt() or hex.toInt(16) }.getOrNull()
        8 -> runCatching { hex.toLong(16).toInt() }.getOrNull()
        else -> null
    } ?: return null
    return Color(argb)
}
