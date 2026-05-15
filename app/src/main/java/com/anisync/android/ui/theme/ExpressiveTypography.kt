package com.anisync.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Editorial typography tokens that sit outside the 15-role MD3 scale.
 *
 * Used for the "editorial treatment" surfaces called out in
 * m3.material.io/styles/typography/editorial-treatments — oversized numerics,
 * uppercase eyebrows, pull-quote leads, and tabular figures for charts.
 */
@Immutable
data class ExpressiveTypography(
    /** 96sp W900 tabular — main hero number (HeroDashboard). */
    val heroNumeric: TextStyle,
    /** 64sp W700 tabular — secondary big numbers (TimeSpent, StdDev card). */
    val statNumericLarge: TextStyle,
    /** 40sp W700 tabular — tertiary stat numbers (EditorialStatBlock). */
    val statNumericMedium: TextStyle,
    /** 11sp W500 uppercase eyebrow with wide tracking. */
    val statLabel: TextStyle,
    /** 20sp W400 — editorial lead / pull-quote ("≈ X days of your life"). */
    val editorialLead: TextStyle,
    /** 14sp W700 tabular — chart axis / bar value labels. */
    val numericMono: TextStyle,
    /** 13sp W400 — section descriptions / bylines. */
    val sectionByline: TextStyle,
)

/**
 * Builds the editorial type set, applying the runtime [overrides] from the developer font
 * playground so hero / stat text tracks the axis sliders together with the rest of the app.
 * The editorial numeric + body families follow the BODY category override.
 */
internal fun defaultExpressiveTypography(
    overrides: TypographyOverrides = TypographyOverrides.None,
): ExpressiveTypography {
    val bodyOverride = overrides.body
    val numericFamily: FontFamily = roleFamily(TypographyAxisConfig.numeric, listOf(400, 700, 900), bodyOverride)
    val bodyFamily: FontFamily = roleFamily(TypographyAxisConfig.body, listOf(400, 500, 700), bodyOverride)
    return ExpressiveTypography(
        heroNumeric = TextStyle(
            fontFamily = numericFamily,
            fontWeight = FontWeight.W900,
            fontSize = 96.sp,
            lineHeight = 96.sp,
            letterSpacing = (-2).sp,
            fontFeatureSettings = "tnum",
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
        statNumericLarge = TextStyle(
            fontFamily = numericFamily,
            fontWeight = FontWeight.W700,
            fontSize = 64.sp,
            lineHeight = 64.sp,
            letterSpacing = (-1).sp,
            fontFeatureSettings = "tnum",
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
        statNumericMedium = TextStyle(
            fontFamily = numericFamily,
            fontWeight = FontWeight.W700,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            letterSpacing = (-0.5).sp,
            fontFeatureSettings = "tnum",
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
        statLabel = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.W500,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 1.5.sp,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
        editorialLead = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.W400,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
        numericMono = TextStyle(
            fontFamily = numericFamily,
            fontWeight = FontWeight.W700,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.sp,
            fontFeatureSettings = "tnum",
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
        sectionByline = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.W400,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.2.sp,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
    )
}

val LocalExpressiveTypography = compositionLocalOf { defaultExpressiveTypography() }

object AppTypeExpressive {
    val current: ExpressiveTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalExpressiveTypography.current
}
