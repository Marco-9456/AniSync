package com.anisync.android.data.network

import android.util.Log

/**
 * The network layer's logging seam.
 *
 * `android.util.Log` is a stub on the JVM that throws rather than returning, which is why the gate
 * and the interceptors could not be unit tested without switching the whole test source set to
 * `returnDefaultValues`. That flag silences every Android stub in every test, so a test that
 * wandered into an Android API would quietly get a zero instead of failing loudly. One seam here is
 * cheaper than that trade.
 *
 * Tests set [enabled] to false. Nothing else touches it.
 */
internal object NetLog {

    @Volatile
    var enabled: Boolean = true

    fun d(tag: String, message: () -> String) {
        if (enabled) Log.d(tag, message())
    }

    fun i(tag: String, message: () -> String) {
        if (enabled) Log.i(tag, message())
    }

    fun w(tag: String, message: () -> String) {
        if (enabled) Log.w(tag, message())
    }
}
