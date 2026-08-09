package com.anisync.android.widget.core

import android.content.res.ColorStateList
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import com.anisync.android.R

/**
 * Paints the app scheme onto a widget.
 *
 * The layouts use @color/widget_*, which gets us light/dark and the system palette on API 31+, but
 * not the seed colour the user picked in the app. So anything that has to match the app is applied
 * here instead, per view, per render.
 *
 * All no-ops below API 31, where RemoteViews cannot tint a background. Recolouring the text but not
 * the card under it looks worse than leaving both alone. See [WidgetColors.canTintBackgrounds].
 */
object WidgetTheme {

    /**
     * The bits every widget shares: panel, header icon plate, title, empty state.
     *
     * All five layouts use the same ids for these, so a provider only handles what is its own.
     */
    fun applyChrome(views: RemoteViews, colors: WidgetColors) {
        panel(views, android.R.id.background, colors)
        iconPlate(views, R.id.widget_icon_plate, R.id.widget_icon, colors)
        text(views, R.id.widget_title, colors.onSurface, colors)
        icon(views, R.id.widget_empty_icon, colors.onSurfaceVariant, colors)
        text(views, R.id.widget_empty_title, colors.onSurface, colors)
        text(views, R.id.widget_empty_body, colors.onSurfaceVariant, colors)
    }

    /** A round or pill button in the header, like Trending search. */
    fun iconButton(
        views: RemoteViews,
        @IdRes buttonId: Int,
        @IdRes iconId: Int,
        colors: WidgetColors
    ) {
        tintBackground(views, buttonId, colors.surfaceVariant)
        icon(views, iconId, colors.onSurfaceVariant, colors)
    }

    /**
     * A day in the calendar strip.
     *
     * Unselected days get no background, otherwise the strip looks like seven buttons instead of one
     * row. The selected one is filled with the app primary, not the drawable colour, which is what
     * made it look unrelated before.
     */
    fun daySelection(
        views: RemoteViews,
        @IdRes rootId: Int,
        @IdRes nameId: Int,
        @IdRes numberId: Int,
        selected: Boolean,
        colors: WidgetColors
    ) {
        views.setInt(
            rootId,
            "setBackgroundResource",
            if (selected) R.drawable.widget_day_cell_bg else 0
        )
        if (selected) tintBackground(views, rootId, colors.primary)
        views.setTextColor(nameId, if (selected) colors.onPrimary else colors.onSurfaceVariant)
        views.setTextColor(numberId, if (selected) colors.onPrimary else colors.onSurface)
    }

    /**
     * The three ProgressBar layers: track, aired band, watched fill.
     *
     * The aired band is just the fill colour faded out. A third colour would need explaining, a
     * lighter shade of "watched" reads as "watchable" on its own.
     */
    fun progressBar(views: RemoteViews, @IdRes viewId: Int, colors: WidgetColors) {
        if (!WidgetColors.canTintBackgrounds) return
        views.setColorStateList(
            viewId,
            "setProgressBackgroundTintList",
            ColorStateList.valueOf(colors.surfaceVariant)
        )
        views.setColorStateList(
            viewId,
            "setSecondaryProgressTintList",
            ColorStateList.valueOf(withAlpha(colors.primary, AIRED_ALPHA))
        )
        views.setColorStateList(
            viewId,
            "setProgressTintList",
            ColorStateList.valueOf(colors.primary)
        )
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
            // setColorFilter is remotable everywhere, but only worth using where we tinted the
            // background around it too.
            views.setInt(viewId, "setColorFilter", color)
        }
    }

    /**
     * The call that needs API 31.
     *
     * setBackgroundTintList swaps the shape colour and keeps the corners. Nothing older does that:
     * setBackgroundColor is remotable but flattens the drawable, corners included, which is worse
     * than the wrong colour.
     */
    private fun tintBackground(views: RemoteViews, @IdRes viewId: Int, @ColorInt color: Int) {
        if (!WidgetColors.canTintBackgrounds) return
        views.setColorStateList(viewId, "setBackgroundTintList", ColorStateList.valueOf(color))
    }

    @ColorInt
    private fun withAlpha(@ColorInt color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    /** How strong the aired band is behind the watched fill. */
    private const val AIRED_ALPHA = 110
}
