package com.anisync.android.widget.core

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.anisync.android.R

/**
 * The widget palette, resolved to ARGB ints.
 *
 * Layout XML can reference `@color/widget_*` directly and needs nothing from here. This exists for
 * the two places that cannot: bitmaps we draw ourselves (see [WidgetProgressRenderer]) and runtime
 * tinting of a view whose colour depends on state, such as the selected day in the calendar strip.
 *
 * Colours are read through [themedContext] rather than the app context. The launcher inflates our
 * layouts under *its* configuration, so it picks `values-night` from the system's night setting. If
 * the user has pinned AniSync itself to light while the system is dark, resolving in the app
 * context would hand back light colours for bitmaps sitting on a dark layout.
 */
data class WidgetColors(
    @param:ColorInt val background: Int,
    @param:ColorInt val surface: Int,
    @param:ColorInt val surfaceVariant: Int,
    @param:ColorInt val onSurface: Int,
    @param:ColorInt val onSurfaceVariant: Int,
    @param:ColorInt val outline: Int,
    @param:ColorInt val primary: Int,
    @param:ColorInt val onPrimary: Int,
    @param:ColorInt val primaryContainer: Int,
    @param:ColorInt val onPrimaryContainer: Int,
    @param:ColorInt val secondaryContainer: Int,
    @param:ColorInt val onSecondaryContainer: Int,
    @param:ColorInt val tertiaryContainer: Int,
    @param:ColorInt val onTertiaryContainer: Int,
) {
    companion object {
        fun of(context: Context): WidgetColors {
            val themed = themedContext(context)
            fun color(id: Int) = ContextCompat.getColor(themed, id)
            return WidgetColors(
                background = color(R.color.widget_background),
                surface = color(R.color.widget_surface),
                surfaceVariant = color(R.color.widget_surface_variant),
                onSurface = color(R.color.widget_on_surface),
                onSurfaceVariant = color(R.color.widget_on_surface_variant),
                outline = color(R.color.widget_outline),
                primary = color(R.color.widget_primary),
                onPrimary = color(R.color.widget_on_primary),
                primaryContainer = color(R.color.widget_primary_container),
                onPrimaryContainer = color(R.color.widget_on_primary_container),
                secondaryContainer = color(R.color.widget_secondary_container),
                onSecondaryContainer = color(R.color.widget_on_secondary_container),
                tertiaryContainer = color(R.color.widget_tertiary_container),
                onTertiaryContainer = color(R.color.widget_on_tertiary_container),
            )
        }

        /** A context whose night mode matches the system, which is what the launcher renders under. */
        private fun themedContext(context: Context): Context {
            val systemNight = Resources.getSystem().configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
            val appConfig = context.resources.configuration
            if ((appConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) == systemNight) return context
            val corrected = Configuration(appConfig).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or systemNight
            }
            return context.createConfigurationContext(corrected)
        }
    }
}
