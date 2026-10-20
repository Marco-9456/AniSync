package com.anisync.android.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.ui.theme.AppThemeSpec
import com.anisync.android.ui.theme.LocalAppThemeSpec
import com.anisync.android.ui.theme.aniListProfileSeedColor
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
 * Wraps a page that belongs to another user in a palette seeded from their AniList profile color,
 * when the "Use profile colors" appearance preference is on. A null [profileColor], the default
 * color and the toggle being off all fall through to the app theme, so the viewer's own pages pass
 * null.
 */
@Composable
fun ProfileColorThemeOverride(profileColor: String?, content: @Composable () -> Unit) {
    val appSettings = LocalAppSettings.current
    val respect by appSettings.respectUserProfileColors.collectAsStateWithLifecycle()
    val paletteStyle by appSettings.paletteStyle.collectAsStateWithLifecycle()
    val seed = if (respect) aniListProfileSeedColor(profileColor) else null
    // Share cards rebuild the page's palette in a forced polarity from this spec.
    val spec = LocalAppThemeSpec.current
    CompositionLocalProvider(
        LocalAppThemeSpec provides if (seed == null) spec else AppThemeSpec(
            dynamicColor = false,
            seedColor = seed,
            paletteStyle = paletteStyle,
        )
    ) {
        SeededThemeOverride(seed = seed, content = content)
    }
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
