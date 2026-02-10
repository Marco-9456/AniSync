package com.anisync.android.widget.core

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.anisync.android.MainActivity

/**
 * Centralized intent creation utilities for widgets.
 * Replaces duplicated intent creation code in individual widgets.
 */
object WidgetIntentUtils {

    /**
     * Creates an intent to open the media details screen for a specific anime/manga.
     * 
     * @param context Application context
     * @param mediaId The AniList media ID to display
     * @return Intent configured to open the details screen
     */
    fun createDetailsIntent(context: Context, mediaId: Int): Intent {
        return Intent(
            Intent.ACTION_VIEW,
            "anisync://details/$mediaId".toUri()
        ).apply {
            component = null
            setClass(context, MainActivity::class.java)
        }
    }

    /**
     * Creates an intent to open the main app.
     * Uses explicit intent with context to correctly target debug/release builds.
     *
     * @param context Application context
     * @return Intent configured to launch MainActivity
     */
    fun openMainAppIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }
}
