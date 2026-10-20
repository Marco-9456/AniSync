package com.anisync.android.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme

private const val MaxCachedSchemes = 32

private data class SchemeKey(
    val seed: Color,
    val darkTheme: Boolean,
    val amoled: Boolean,
    val paletteStyle: PaletteStyle,
)

private val cachedSchemes =
    object : LinkedHashMap<SchemeKey, ColorScheme>(MaxCachedSchemes, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<SchemeKey, ColorScheme>) =
            size > MaxCachedSchemes
    }

/**
 * A MaterialKolor scheme grown from [seed] in the viewer's polarity and [paletteStyle], with our
 * AMOLED container shift when they run pure black.
 *
 * A full scheme costs tens of milliseconds on a mid-range phone, and a page wearing a title's or a
 * user's colors passes the same seed to every page it opens. Schemes live for the whole process, so
 * those pages and the way back reuse the one already built instead of stalling a frame on it.
 */
fun seededColorScheme(
    seed: Color,
    darkTheme: Boolean,
    amoled: Boolean,
    paletteStyle: PaletteStyle,
): ColorScheme {
    cachedSeededColorScheme(seed, darkTheme, amoled, paletteStyle)?.let { return it }
    val key = SchemeKey(seed, darkTheme, amoled && darkTheme, paletteStyle)
    // isAmoled stays off here for the reason AppTheme leaves it off.
    val base = dynamicColorScheme(
        seedColor = seed,
        isDark = darkTheme,
        isAmoled = false,
        style = paletteStyle,
        specVersion = ColorSpec.SpecVersion.SPEC_2026
    )
    val scheme = if (key.amoled) base.toAmoled() else base
    synchronized(cachedSchemes) { cachedSchemes[key] = scheme }
    return scheme
}

/** The scheme [seededColorScheme] already built for these inputs, or null if it would build one. */
fun cachedSeededColorScheme(
    seed: Color,
    darkTheme: Boolean,
    amoled: Boolean,
    paletteStyle: PaletteStyle,
): ColorScheme? {
    val key = SchemeKey(seed, darkTheme, amoled && darkTheme, paletteStyle)
    return synchronized(cachedSchemes) { cachedSchemes[key] }
}

@Composable
fun rememberSeededColorScheme(
    seed: Color,
    darkTheme: Boolean,
    amoled: Boolean,
    paletteStyle: PaletteStyle,
): ColorScheme = remember(seed, darkTheme, amoled, paletteStyle) {
    seededColorScheme(seed, darkTheme, amoled, paletteStyle)
}
