package com.anisync.android.widget.core

import android.content.Context
import android.widget.RemoteViews
import com.anisync.android.R

/**
 * Selected and unselected looks for the filter chips.
 *
 * setBackgroundResource and setTextColor are both remotable, so chip state is part of the render
 * rather than a second layout to swap in. That is the point: the chip and the list under it are
 * decided in the same pass and cannot disagree.
 */
object WidgetChips {

    fun apply(
        views: RemoteViews,
        context: Context,
        viewId: Int,
        selected: Boolean,
        colors: WidgetColors
    ) = WidgetTheme.chip(views, viewId, selected, colors)
}
