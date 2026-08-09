package com.anisync.android.widget.core

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.anisync.android.R
import com.anisync.android.data.ThemeMode
import com.anisync.android.ui.theme.widgetColorScheme

/**
 * The palette a widget renders with.
 *
 * Built from the app's own theme, not the device's. AniSync's colours come from a user-chosen seed
 * through MaterialKolor, plus a palette style, a light/dark choice and AMOLED, and none of that
 * reaches a launcher: it resolves `@color` resources under its own configuration and has no idea
 * which seed the user picked. So the scheme is resolved here and applied to each view at render
 * time by [WidgetTheme].
 *
 * [canTintBackgrounds] is the catch. Tinting a rounded background from `RemoteViews` needs
 * `setColorStateList`, which is API 31 and up. Below that the layouts keep their resource colours,
 * so those devices follow the app's static palette rather than the user's seed. Text and icons
 * would tint on any version, but tinting only half of a card looks worse than tinting none of it.
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

        /** Can this device tint a rounded background at all. */
        val canTintBackgrounds: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        fun of(context: Context): WidgetColors =
            if (canTintBackgrounds) fromAppTheme(context) else fromResources(context)

        /** The live app scheme: seed, palette style, light/dark and AMOLED as the user set them. */
        private fun fromAppTheme(context: Context): WidgetColors {
            val settings = context.widgetDeps().appSettings()
            val dark = when (settings.themeMode.value) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                // Following the system here means following the launcher config, which is what the
                // widget is drawn under.
                ThemeMode.SYSTEM -> systemIsDark()
            }
            val scheme = widgetColorScheme(
                context = context,
                dark = dark,
                amoled = settings.amoledEnabled.value,
                seedColor = settings.customSeedColor.value,
                paletteStyle = settings.paletteStyle.value,
                // Same default as AppTheme, so someone with no custom seed still gets the wallpaper
                // palette instead of the static brand colours.
                dynamicColor = true
            )
            return WidgetColors(
                background = scheme.background.toArgb(),
                // Not scheme.surface. Surface and background are nearly the same colour, fine in a
                // scrolling screen but it makes the cards disappear into the panel.
                surface = scheme.surfaceContainer.toArgb(),
                surfaceVariant = scheme.surfaceVariant.toArgb(),
                onSurface = scheme.onSurface.toArgb(),
                onSurfaceVariant = scheme.onSurfaceVariant.toArgb(),
                outline = scheme.outline.toArgb(),
                primary = scheme.primary.toArgb(),
                onPrimary = scheme.onPrimary.toArgb(),
                primaryContainer = scheme.primaryContainer.toArgb(),
                onPrimaryContainer = scheme.onPrimaryContainer.toArgb(),
                secondaryContainer = scheme.secondaryContainer.toArgb(),
                onSecondaryContainer = scheme.onSecondaryContainer.toArgb(),
                tertiaryContainer = scheme.tertiaryContainer.toArgb(),
                onTertiaryContainer = scheme.onTertiaryContainer.toArgb(),
            )
        }

        /**
         * The resource palette, for API 26 to 30 and anything drawn into a bitmap.
         *
         * Read through a context whose night mode follows the system, since the launcher inflates
         * our layouts under its own config. Using the app context would give light colours to a
         * bitmap sitting on a dark layout whenever the two disagree.
         */
        private fun fromResources(context: Context): WidgetColors {
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

        private fun systemIsDark(): Boolean =
            (Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

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
