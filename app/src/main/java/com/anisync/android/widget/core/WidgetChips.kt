package com.anisync.android.widget.core

import android.content.Context
import android.widget.RemoteViews
import com.anisync.android.R

/**
 * Selected and unselected looks for the filter chips.
 *
 * `setBackgroundResource` and `setTextColor` are both remotable, so a chip's state is a property of
 * the render rather than a separate layout to swap in. That matters here: the whole point of the
 * rewrite is that the chip and the list below it are decided in the same pass and can never
 * disagree.
 */
object WidgetChips {

    fun apply(
        views: RemoteViews,
        context: Context,
        viewId: Int,
        selected: Boolean,
        colors: WidgetColors
    ) {
        views.setInt(
            viewId,
            "setBackgroundResource",
            if (selected) R.drawable.widget_pill_bg_accent else R.drawable.widget_pill_bg_muted
        )
        views.setTextColor(viewId, if (selected) colors.onPrimary else colors.onSurfaceVariant)
    }
}
