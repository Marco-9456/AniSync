package com.anisync.android.data.util

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate

/**
 * A context that speaks the language chosen in the app, not the one the device is set to.
 *
 * The app picks its language with [AppCompatDelegate.setApplicationLocales]. That reconfigures
 * activity contexts, but **not** the application context, so anything outside the composition that
 * reads a string from `@ApplicationContext` gets the system locale. On a German phone with the app
 * set to English that means an English app showing a German error.
 *
 * Composables do not need this: `stringResource` reads through the composition, which already
 * follows the in-app locale.
 *
 * Results are cached per locale because callers sit on paths that run often.
 */
object AppLocale {

    /**
     * The tags and the context they were built for, written as one value.
     *
     * Two fields meant two writes. Two threads missing at once for different locales could
     * interleave them and leave the surviving tags describing the other locale's context, which
     * then answered every caller in the wrong language until the next miss. `describe()` runs on
     * every error the repositories raise, so the paths that get here do overlap.
     */
    private class Cached(val tags: String, val context: Context)

    @Volatile
    private var cached: Cached? = null

    fun wrap(appContext: Context): Context {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (tags.isEmpty()) return appContext

        cached?.let { if (tags == it.tags) return it.context }

        val config = Configuration(appContext.resources.configuration).apply {
            setLocales(LocaleList.forLanguageTags(tags))
        }
        return appContext.createConfigurationContext(config).also {
            cached = Cached(tags, it)
        }
    }
}
