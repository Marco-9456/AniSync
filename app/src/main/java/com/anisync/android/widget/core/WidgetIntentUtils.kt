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
     * 
     * @return Intent configured to launch MainActivity
     */
    fun openMainAppIntent(): Intent {
        return Intent(Intent.ACTION_MAIN).apply {
            setClassName("com.anisync.android", "com.anisync.android.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
