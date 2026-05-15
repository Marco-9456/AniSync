package com.anisync.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontVariation

/**
 * A resolved set of variable-font axis values for one font instance.
 *
 * Every axis except [weight] is nullable: a `null` axis is omitted from the generated
 * [FontVariation.Settings], so the font falls back to its own built-in default for that axis.
 * Values are clamped to the owning [VariableFontAxes] registry's valid ranges when the
 * settings are built, so callers never need to bounds-check by hand.
 */
@Immutable
data class FontAxes(
    val weight: Float,
    val width: Float? = null,
    val opticalSize: Float? = null,
    val slant: Float? = null,
    val roundness: Float? = null,
    /** Escape hatch for parametric axes (XOPQ, XTRA, YTAS, …) not modelled as named fields. */
    val extra: List<FontVariation.Setting> = emptyList(),
) {
    /**
     * Builds [FontVariation.Settings] for [registry], emitting only the axes that registry
     * actually supports and that are non-null here — each clamped to its valid range.
     */
    fun toVariationSettings(registry: VariableFontAxes): FontVariation.Settings {
        val settings = buildList {
            add(FontVariation.weight(registry.weight.clamp(weight).toInt()))
            axisSetting(registry.width, width)?.let(::add)
            axisSetting(registry.opticalSize, opticalSize)?.let(::add)
            axisSetting(registry.slant, slant)?.let(::add)
            axisSetting(registry.roundness, roundness)?.let(::add)
            addAll(extra)
        }
        return FontVariation.Settings(*settings.toTypedArray())
    }

    private fun axisSetting(axis: FontAxis?, value: Float?): FontVariation.Setting? {
        if (axis == null || value == null) return null
        return FontVariation.Setting(axis.tag, axis.clamp(value))
    }
}
