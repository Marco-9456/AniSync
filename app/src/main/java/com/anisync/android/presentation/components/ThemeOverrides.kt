package com.anisync.android.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.ui.theme.mediaArtworkSeedColor
import com.anisync.android.ui.theme.rememberSeededColorScheme
import com.anisync.android.ui.theme.resolveDarkTheme

/**
 * Wraps a page that belongs to a title in a palette seeded from its cover art, when the "Colors from
 * artwork" appearance preference is on. Titles AniList ships no cover color for, and colors too
 * washed out to read as a hue, fall through to the app theme.
 *
 * [coverColor] is AniList's `Media.coverImage.color`.
 */
@Composable
fun MediaArtworkThemeOverride(coverColor: String?, content: @Composable () -> Unit) {
    val enabled by LocalAppSettings.current.mediaArtworkTheming.collectAsStateWithLifecycle()
    SeededThemeOverride(
        seed = if (enabled) mediaArtworkSeedColor(coverColor) else null,
        content = content,
    )
}

/**
 * Swaps only the color scheme, so the viewer's palette style, light or dark choice and AMOLED setting
 * carry over and typography, shapes and motion stay the app's.
 *
 * A null [seed] keeps the current scheme, so callers wrap unconditionally. Branching around [content]
 * instead would drop and rebuild the whole page when its seed arrives with its data.
 */
@Composable
private fun SeededThemeOverride(seed: Color?, content: @Composable () -> Unit) {
    val appSettings = LocalAppSettings.current
    val themeMode by appSettings.themeMode.collectAsStateWithLifecycle()
    val paletteStyle by appSettings.paletteStyle.collectAsStateWithLifecycle()
    val amoledEnabled by appSettings.amoledEnabled.collectAsStateWithLifecycle()
    val scheme = if (seed == null) {
        MaterialTheme.colorScheme
    } else {
        rememberSeededColorScheme(seed, themeMode.resolveDarkTheme(), amoledEnabled, paletteStyle)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
