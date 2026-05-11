package com.anisync.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

private val DefaultLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private fun style(
    family: FontFamily,
    weight: FontWeight,
    sizeSp: Float,
    lineHeightSp: Float,
    trackingSp: Float
): TextStyle = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = sizeSp.sp,
    lineHeight = lineHeightSp.sp,
    letterSpacing = trackingSp.sp,
    lineHeightStyle = DefaultLineHeightStyle,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

// Values match m3.material.io/styles/typography/type-scale-tokens.
// Headlines lifted to W500 (from W400) per MD3 Expressive Application guidance.
val AppTypography = Typography(
    displayLarge   = style(DisplayFontFamily, FontWeight.W400, 57f, 64f, -0.25f),
    displayMedium  = style(DisplayFontFamily, FontWeight.W400, 45f, 52f,  0f),
    displaySmall   = style(DisplayFontFamily, FontWeight.W400, 36f, 44f,  0f),
    headlineLarge  = style(DisplayFontFamily, FontWeight.W500, 32f, 40f,  0f),
    headlineMedium = style(DisplayFontFamily, FontWeight.W500, 28f, 36f,  0f),
    headlineSmall  = style(DisplayFontFamily, FontWeight.W500, 24f, 32f,  0f),
    titleLarge     = style(BodyFontFamily,    FontWeight.W500, 22f, 28f,  0f),
    titleMedium    = style(BodyFontFamily,    FontWeight.W500, 16f, 24f,  0.15f),
    titleSmall     = style(BodyFontFamily,    FontWeight.W500, 14f, 20f,  0.1f),
    bodyLarge      = style(BodyFontFamily,    FontWeight.W400, 16f, 24f,  0.5f),
    bodyMedium     = style(BodyFontFamily,    FontWeight.W400, 14f, 20f,  0.25f),
    bodySmall      = style(BodyFontFamily,    FontWeight.W400, 12f, 16f,  0.4f),
    labelLarge     = style(BodyFontFamily,    FontWeight.W500, 14f, 20f,  0.1f),
    labelMedium    = style(BodyFontFamily,    FontWeight.W500, 12f, 16f,  0.5f),
    labelSmall     = style(BodyFontFamily,    FontWeight.W500, 11f, 16f,  0.5f)
)

fun TextStyle.emphasis(): TextStyle = copy(fontWeight = FontWeight.W700)
