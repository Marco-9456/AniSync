package com.anisync.android.widget.core

import android.content.Context

/**
 * dp to px, for the widget layer.
 *
 * RemoteViews sizing arrives from `AppWidgetManager` in dp and everything we rasterise is in px, so
 * this conversion is on every path. Kept as a plain Int helper so the widget package does not pull
 * in Compose's `Dp` for arithmetic it never renders with.
 */
fun Context.widgetPx(dp: Int): Int =
    (dp * resources.displayMetrics.density).toInt().coerceAtLeast(1)

fun Context.widgetPxF(dp: Float): Float = dp * resources.displayMetrics.density
