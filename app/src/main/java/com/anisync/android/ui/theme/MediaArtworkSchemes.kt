package com.anisync.android.ui.theme

import android.content.Context
import com.anisync.android.data.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Builds a title's artwork scheme on a background thread before the page that wears it draws, from
 * the same settings the artwork override reads. The first themed frame then finds the scheme cached
 * instead of stalling on it.
 */
class MediaArtworkSchemes @Inject constructor(
    private val appSettings: AppSettings,
    @ApplicationContext private val context: Context,
) {
    /** Returns once the scheme for [coverColor] is cached, or straight away when none applies. */
    suspend fun prepare(coverColor: String?) {
        if (!appSettings.mediaArtworkTheming.value) return
        val seed = mediaArtworkSeedColor(coverColor) ?: return
        val darkTheme = appSettings.themeMode.value.resolveDarkTheme(context.resources.configuration)
        val amoled = appSettings.amoledEnabled.value
        val paletteStyle = appSettings.paletteStyle.value
        if (cachedSeededColorScheme(seed, darkTheme, amoled, paletteStyle) != null) return
        withContext(Dispatchers.Default) {
            seededColorScheme(seed, darkTheme, amoled, paletteStyle)
        }
    }
}
