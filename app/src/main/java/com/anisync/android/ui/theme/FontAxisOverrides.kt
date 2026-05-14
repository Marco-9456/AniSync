package com.anisync.android.ui.theme

import androidx.compose.runtime.Immutable

/**
 * Global, runtime variable-font axis overrides driven by the developer "font playground".
 *
 * Each axis is nullable: a `null` axis means "leave the per-role [TypographyAxisConfig] preset
 * alone". A non-null axis replaces that axis for **every** role app-wide — so, for example,
 * dragging the width slider widens display, headline, body, and label text together.
 *
 * When [isActive] is `false` ([None]), the app renders with its normal per-role typography.
 */
@Immutable
data class FontAxisOverrides(
    val weight: Float? = null,
    val width: Float? = null,
    val opticalSize: Float? = null,
    val slant: Float? = null,
    val roundness: Float? = null,
) {
    /** True when at least one axis is being overridden. */
    val isActive: Boolean
        get() = weight != null || width != null || opticalSize != null ||
            slant != null || roundness != null

    companion object {
        /** The neutral state — no axis overridden; the app uses its per-role presets. */
        val None = FontAxisOverrides()
    }
}

/**
 * Applies non-null [overrides] on top of a per-role [FontAxes] preset.
 *
 * Note the weight override flattens the W400/W500/W700 hierarchy while active — that is the
 * intended playground behaviour; clearing the override (Reset) restores the hierarchy.
 */
fun FontAxes.merge(overrides: FontAxisOverrides): FontAxes = copy(
    weight = overrides.weight ?: weight,
    width = overrides.width ?: width,
    opticalSize = overrides.opticalSize ?: opticalSize,
    slant = overrides.slant ?: slant,
    roundness = overrides.roundness ?: roundness,
)
