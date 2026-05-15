package com.anisync.android.ui.theme

import androidx.compose.runtime.Immutable

/**
 * A single OpenType variable-font axis: its 4-character tag, valid range, and default value.
 *
 * Axis tags, ranges, and semantics follow the Google Fonts variable-font glossary —
 * https://fonts.google.com/knowledge/glossary/axis_in_variable_fonts
 */
@Immutable
data class FontAxis(
    val tag: String,
    val range: ClosedFloatingPointRange<Float>,
    val default: Float,
) {
    /** Coerces [value] into this axis' valid [range]. */
    fun clamp(value: Float): Float = value.coerceIn(range)
}

/**
 * The set of variable-font axes a given font file exposes.
 *
 * Each variable font declares the axes it supports as its own [VariableFontAxes] object, so
 * shipping a different variable font is a single new object here — the factory, config, and
 * typography layers stay untouched.
 *
 * An axis the font does not support is `null`; [FontAxes.toVariationSettings] silently drops
 * any axis value whose registry entry is `null`.
 */
interface VariableFontAxes {
    val weight: FontAxis
    val width: FontAxis?
    val opticalSize: FontAxis?
    val slant: FontAxis?
    val roundness: FontAxis?
}

/**
 * Google Sans Flex — the variable font shipped in `res/font/google_sans_flex.ttf`.
 *
 * An extremely flexible variable font: weight, width, optical size, slant, and a rounded
 * (`ROND`) axis. It has no `GRAD` axis. Ranges and defaults match the axis sliders in the
 * design prototype. See the Google Fonts knowledge base linked on [FontAxis].
 */
object GoogleSansFlexAxes : VariableFontAxes {
    override val weight = FontAxis("wght", 100f..1000f, 400f)
    override val width = FontAxis("wdth", 25f..151f, 100f)
    override val opticalSize = FontAxis("opsz", 8f..144f, 24f)
    override val slant = FontAxis("slnt", -10f..0f, 0f)
    override val roundness = FontAxis("ROND", 0f..100f, 0f)
}
