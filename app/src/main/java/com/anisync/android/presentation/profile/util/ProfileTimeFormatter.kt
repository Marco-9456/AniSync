package com.anisync.android.presentation.profile.util

import android.text.format.DateUtils

fun formatProfileRelativeTime(timeMillis: Long?): String {
    if (timeMillis == null || timeMillis == 0L) return "Unknown"
    return try {
        val now = System.currentTimeMillis()
        DateUtils.getRelativeTimeSpanString(timeMillis, now, DateUtils.MINUTE_IN_MILLIS).toString()
    } catch (e: Exception) {
        "Unknown"
    }
}
