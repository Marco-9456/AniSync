package com.anisync.android.widget.core

import android.content.res.ColorStateList
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import com.anisync.android.R

/**
 * Paints the app's scheme onto a widget's views.
 *
 * Widget layouts reference `@color/widget_*`, which the launcher resolves under its own
 * configuration. That is as far as resources can go: they can express light and dark, and the
 * system palette on API 31 and up, but not the seed colour the user picked in AniSync. Anything
 * that has to match the app is therefore applied here, per view, per render.
 *
 * Every call is a no-op below API 31. Backgrounds cannot be tinted through `RemoteViews` there, and
 * recolouring the text on a card whose background stayed put reads worse than leaving both alone,
 * so those devices keep the resource palette wholesale. See [WidgetColors.canTintBackgrounds].
 */
object WidgetTheme {

    /**
     * Everything every widget has in common: the panel, the header icon plate, the title and the
     * empty state.
     *
     * The ids are shared across all five layouts by convention, so each provider only has to deal
     * with what is unique to it.
     */
    fun applyChrome(views: RemoteViews, colors: WidgetColors) {
        panel(views, android.R.id.background, colors)
        iconPlate(views, R.id.widget_icon_plate, R.id.widget_icon, colors)
        text(views, R.id.widget_title, colors.onSurface, colors)
        icon(views, R.id.widget_empty_icon, colors.onSurfaceVariant, colors)
        text(views, R.id.widget_empty_title, colors.onSurface, colors)
        text(views, R.id.widget_empty_body, colors.onSurfaceVariant, colors)
    }

    /** The widget panel itself. */
    fun panel(views: RemoteViews, @IdRes viewId: Int, colors: WidgetColors) =
        tintBackground(views, viewId, colors.background)

    /** A card in a list. */
    fun card(views: RemoteViews, @IdRes viewId: Int, colors: WidgetColors) =
        tintBackground(views, viewId, colors.surface)

    /** The placeholder behind a cover that has not loaded. */
    fun poster(views: RemoteViews, @IdRes viewId: Int, colors: WidgetColors) =
        tintBackground(views, viewId, colors.surfaceVariant)

    /** The rounded square behind a header icon, and the icon on top of it. */
    fun iconPlate(
        views: RemoteViews,
        @IdRes plateId: Int,
        @IdRes iconId: Int,
        colors: WidgetColors
    ) {
        tintBackground(views, plateId, colors.primary)
        icon(views, iconId, colors.onPrimary, colors)
    }

    /** A selectable chip: filled when selected, muted when not. */
    fun chip(
        views: RemoteViews,
        @IdRes viewId: Int,
        selected: Boolean,
        colors: WidgetColors
    ) {
        views.setInt(
            viewId,
            "setBackgroundResource",
            if (selected) R.drawable.widget_pill_bg_accent else R.drawable.widget_pill_bg_muted
        )
        if (WidgetColors.canTintBackgrounds) {
            tintBackground(views, viewId, if (selected) colors.primary else colors.surfaceVariant)
        }
        views.setTextColor(viewId, if (selected) colors.onPrimary else colors.onSurfaceVariant)
    }

    /** A badge that is always filled, such as an episode or a countdown. */
    fun badge(
        views: RemoteViews,
        @IdRes viewId: Int,
        @ColorInt container: Int,
        @ColorInt onContainer: Int,
        colors: WidgetColors
    ) {
        tintBackground(views, viewId, container)
        if (WidgetColors.canTintBackgrounds) views.setTextColor(viewId, onContainer)
    }

    fun text(views: RemoteViews, @IdRes viewId: Int, @ColorInt color: Int, colors: WidgetColors) {
        if (WidgetColors.canTintBackgrounds) views.setTextColor(viewId, color)
    }

    fun icon(views: RemoteViews, @IdRes viewId: Int, @ColorInt color: Int, colors: WidgetColors) {
        if (WidgetColors.canTintBackgrounds) {
            // setColorFilter takes a plain int and is remotable on every version, but it is only
            // used where the background around it was tinted too.
            views.setInt(viewId, "setColorFilter", color)
        }
    }

    /**
     * The one call that needs API 31.
     *
     * `setBackgroundTintList` replaces the shape's solid colour, keeping its corners. There is no
     * pre-31 equivalent: `setBackgroundColor` is remotable but flattens the drawable, corners and
     * all, which is worse than the wrong hue.
     */
    private fun tintBackground(views: RemoteViews, @IdRes viewId: Int, @ColorInt color: Int) {
        if (!WidgetColors.canTintBackgrounds) return
        views.setColorStateList(viewId, "setBackgroundTintList", ColorStateList.valueOf(color))
    }
}
