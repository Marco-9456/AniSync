package com.anisync.android.util

import android.os.Build
import com.anisync.android.BuildConfig

object AppInfo {

    fun formatted(): String {
        val abis = Build.SUPPORTED_ABIS.joinToString(", ")
        return buildString {
            append("App version: ").append(BuildConfig.VERSION_NAME).append('\n')
            append("Device information: Android ")
                .append(Build.VERSION.RELEASE)
                .append(" (API ")
                .append(Build.VERSION.SDK_INT)
                .append(")\n")
            append("Supported ABIs: (").append(abis).append(')')
        }
    }
}
