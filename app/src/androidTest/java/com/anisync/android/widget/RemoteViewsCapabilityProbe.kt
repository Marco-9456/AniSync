package com.anisync.android.widget

import android.content.Context
import android.util.Log
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records which scroll containers this device really accepts inside a RemoteViews.
 *
 * Not a regression test. It is here because the answer decides the architecture and the docs are
 * easy to misread. HorizontalScrollView is an ordinary view, it is just not annotated @RemoteView,
 * so RemoteViews.apply throws it out at inflate time with no compile error and no lint warning.
 *
 * Results go to logcat under the tag below.
 */
@RunWith(AndroidJUnit4::class)
class RemoteViewsCapabilityProbe {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    /** The probe layouts live in the test APK, so they resolve by name instead of through R. */
    private val testContext: Context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun reportWhichContainersInflate() {
        LAYOUTS.forEach { (name, layoutName) ->
            val layout = testContext.resources.getIdentifier(
                layoutName,
                "layout",
                testContext.packageName
            )
            val result = runCatching {
                RemoteViews(testContext.packageName, layout).apply(testContext, FrameLayout(context))
            }
            val verdict = result.fold(
                onSuccess = { "ALLOWED" },
                onFailure = { "REJECTED (${it.javaClass.simpleName}: ${it.message})" }
            )
            Log.i(TAG, "$name -> $verdict")
        }
    }

    private companion object {
        const val TAG = "RemoteViewsProbe"

        val LAYOUTS = listOf(
            "ScrollView" to "probe_scrollview",
            "HorizontalScrollView" to "probe_horizontal_scrollview",
            "ListView" to "probe_listview",
        )
    }
}
