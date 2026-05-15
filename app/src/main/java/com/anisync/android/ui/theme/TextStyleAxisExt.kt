package com.anisync.android.ui.theme

import androidx.annotation.FontRes
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.anisync.android.R

/**
 * A one-off variable-font axis override for a niche component that needs a tweak outside any
 * defined M3 or editorial role — for example a single label that wants a heavier `GRAD`.
 *
 * Variable-font axes live on [androidx.compose.ui.text.font.Font], not on [TextStyle], so this
 * rebuilds the style's `fontFamily` from a fresh [FontAxes] seeded with the style's current
 * weight. The transform receives that [FontAxes] and returns the adjusted one.
 *
 * Prefer adding a role to [TypographyAxisConfig] (or a field to [ExpressiveTypography]) when a
 * variation is reused in more than one place — reach for this only for genuine one-offs.
 *
 * Usage: `MaterialTheme.typography.headlineLarge.withAxes { copy(grade = 80f) }`
 */
fun TextStyle.withAxes(
    @FontRes resId: Int = R.font.google_sans_flex,
    registry: VariableFontAxes = TypographyAxisConfig.registry,
    transform: FontAxes.() -> FontAxes,
): TextStyle {
    val baseWeight = (fontWeight ?: FontWeight.Normal).weight
    val axes = FontAxes(weight = baseWeight.toFloat()).transform()
    return copy(
        fontFamily = VariableFontFactory.family(
            resId = resId,
            axes = axes,
            weights = listOf(axes.weight.toInt()),
            registry = registry,
        ),
    )
}
