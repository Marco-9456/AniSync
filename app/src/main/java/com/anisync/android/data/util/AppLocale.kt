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

    @Volatile
    private var cachedTags: String? = null

    @Volatile
    private var cached: Context? = null

    fun wrap(appContext: Context): Context {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (tags.isEmpty()) return appContext

        cached?.let { if (tags == cachedTags) return it }

        val config = Configuration(appContext.resources.configuration).apply {
            setLocales(LocaleList.forLanguageTags(tags))
        }
        return appContext.createConfigurationContext(config).also {
            cached = it
            cachedTags = tags
        }
    }
}
