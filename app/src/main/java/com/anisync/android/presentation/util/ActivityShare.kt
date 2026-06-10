package com.anisync.android.presentation.util

import android.content.Context
import android.content.Intent

/**
 * Shares an activity's canonical AniList URL via the system share sheet. Activities
 * don't carry a `siteUrl` on the list models, but the URL is deterministic from the
 * id, so this works for any activity card (status / message / list).
 */
fun shareActivity(context: Context, activityId: Int) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "https://anilist.co/activity/$activityId")
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}
