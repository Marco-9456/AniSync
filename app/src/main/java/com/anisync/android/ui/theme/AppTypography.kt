package com.anisync.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import kotlin.math.roundToInt

/*
 * Material 3 typography for AniSync, assembled from two centralized inputs:
 *   - TypographyAxisConfig — variable-font axis values per role group (the global axis knob)
 *   - M3TypeScale          — size / line-height / tracking tokens per role
 *
 * To change how a role group LOOKS (weight, optical size, width, slant, roundness), edit
 * TypographyAxisConfig — not this file. To change a role's SIZE, edit M3TypeScale. To change
 * the axes at runtime (the developer "font playground"), pass TypographyOverrides to
 * buildAppTypography() — each M3 role category is overridden independently.
 *
 * Headlines are lifted to W500 (from the MD3 default W400) per MD3 Expressive guidance.
 */

private val FONT_RES_ID = R.font.google_sans_flex

private val DefaultLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

// FontFamilies for the *default* (no-override) typography. These public vals back the few
// callers that reference a family directly; the live theme uses buildAppTypography() instead.
val DisplayFontFamily: FontFamily = roleFamily(TypographyAxisConfig.display, listOf(300, 400, 500, 700, 900))
val HeadlineFontFamily: FontFamily = roleFamily(TypographyAxisConfig.headline, listOf(400, 500, 700))
val TitleFontFamily: FontFamily = roleFamily(TypographyAxisConfig.title, listOf(400, 500, 700))
val BodyFontFamily: FontFamily = roleFamily(TypographyAxisConfig.body, listOf(400, 500, 700))
val LabelFontFamily: FontFamily = roleFamily(TypographyAxisConfig.label, listOf(400, 500, 700))
val NumericFontFamily: FontFamily = roleFamily(TypographyAxisConfig.numeric, listOf(400, 700, 900))

/**
 * Builds the FontFamily for one role group: its [TypographyAxisConfig] preset merged with the
 * runtime [overrides], spread across [weights]. opsz / width / slant / roundness come from the
 * (possibly overridden) preset.
 *
 * When the weight axis is overridden, the family collapses to a single member at that weight —
 * otherwise [VariableFontFactory.family]'s per-member `axes.copy(weight = w)` would overwrite
 * the override and the weight slider would do nothing.
 */
internal fun roleFamily(
    preset: FontAxes,
    weights: List<Int>,
    overrides: FontAxisOverrides = FontAxisOverrides.None,
): FontFamily {
    val merged = preset.merge(overrides)
    val effectiveWeights = overrides.weight?.let { listOf(it.roundToInt()) } ?: weights
    return VariableFontFactory.family(
        resId = FONT_RES_ID,
        axes = merged,
        weights = effectiveWeights,
        registry = TypographyAxisConfig.registry,
    )
}

private fun textStyle(
    family: FontFamily,
    weight: FontWeight,
    token: TypeScaleToken,
): TextStyle = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = token.sizeSp.sp,
    lineHeight = token.lineHeightSp.sp,
    letterSpacing = token.trackingSp.sp,
    lineHeightStyle = DefaultLineHeightStyle,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

/**
 * Assembles the Material 3 [Typography] for the app, applying the per-category runtime
 * [overrides] from the developer font playground. Called by [AppTheme] inside a
 * `remember(overrides)` so the whole app re-renders when an axis slider moves.
 */
fun buildAppTypography(overrides: TypographyOverrides = TypographyOverrides.None): Typography {
    val displayFamily = roleFamily(TypographyAxisConfig.display, listOf(300, 400, 500, 700, 900), overrides.display)
    val headlineFamily = roleFamily(TypographyAxisConfig.headline, listOf(400, 500, 700), overrides.headline)
    val titleFamily = roleFamily(TypographyAxisConfig.title, listOf(400, 500, 700), overrides.title)
    val bodyFamily = roleFamily(TypographyAxisConfig.body, listOf(400, 500, 700), overrides.body)
    val labelFamily = roleFamily(TypographyAxisConfig.label, listOf(400, 500, 700), overrides.label)
    return Typography(
        displayLarge = textStyle(displayFamily, FontWeight.W400, M3TypeScale.displayLarge),
        displayMedium = textStyle(displayFamily, FontWeight.W400, M3TypeScale.displayMedium),
        displaySmall = textStyle(displayFamily, FontWeight.W400, M3TypeScale.displaySmall),
        headlineLarge = textStyle(headlineFamily, FontWeight.W500, M3TypeScale.headlineLarge),
        headlineMedium = textStyle(headlineFamily, FontWeight.W500, M3TypeScale.headlineMedium),
        headlineSmall = textStyle(headlineFamily, FontWeight.W500, M3TypeScale.headlineSmall),
        titleLarge = textStyle(titleFamily, FontWeight.W500, M3TypeScale.titleLarge),
        titleMedium = textStyle(titleFamily, FontWeight.W500, M3TypeScale.titleMedium),
        titleSmall = textStyle(titleFamily, FontWeight.W500, M3TypeScale.titleSmall),
        bodyLarge = textStyle(bodyFamily, FontWeight.W400, M3TypeScale.bodyLarge),
        bodyMedium = textStyle(bodyFamily, FontWeight.W400, M3TypeScale.bodyMedium),
        bodySmall = textStyle(bodyFamily, FontWeight.W400, M3TypeScale.bodySmall),
        labelLarge = textStyle(labelFamily, FontWeight.W500, M3TypeScale.labelLarge),
        labelMedium = textStyle(labelFamily, FontWeight.W500, M3TypeScale.labelMedium),
        labelSmall = textStyle(labelFamily, FontWeight.W500, M3TypeScale.labelSmall),
    )
}

/** The default (no-override) Material 3 [Typography]. Used by `PreviewTheme` and previews. */
val AppTypography: Typography = buildAppTypography()

/** Bumps any [TextStyle] to a heavier weight for inline emphasis. */
fun TextStyle.emphasis(): TextStyle = copy(fontWeight = FontWeight.W700)
