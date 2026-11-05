package com.anisync.android.util

import android.content.Context
import android.text.format.Formatter
import com.anisync.android.data.util.AppLocale

/** [bytes] the way the system writes a file size, "12 MB", in the app's language. */
fun formatFileSize(context: Context, bytes: Long): String =
    Formatter.formatShortFileSize(AppLocale.wrap(context), bytes)
