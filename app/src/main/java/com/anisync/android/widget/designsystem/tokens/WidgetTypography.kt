package com.anisync.android.widget.designsystem.tokens

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * Widget typography tokens and role builders.
 *
 * Role names mirror the Material 3 type scale — the same role vocabulary the main-app
 * `AppTypography` is built from — so widget and app text stay conceptually aligned. Sizes are
 * intentionally widget-tuned (denser than the main MD3 scale) because Glance surfaces have
 * tight, non-scrolling cells where the full MD3 sp values would overflow.
 *
 * Glance renders via RemoteViews and cannot carry `FontVariation.Settings`, so the
 * variable-font axes (opsz / GRAD / wdth / ROND) that `TypographyAxisConfig` controls in the
 * main app do NOT apply here — widgets share the semantic role naming and sizing only.
 *
 * Prefer the role builders ([titleLarge], [bodyLarge], …) over hand-rolling `TextStyle(...)`
 * at call sites — this keeps widget text styling in one place.
 */
object WidgetTypography {

    // --- Size tokens ------------------------------------------------------------------
    // Raw TextUnit sizes. Exposed for the few APIs that take a configurable size (badges);
    // the role builders below are derived from these same values.

    object Title {
        val large: TextUnit = 18.sp
        val medium: TextUnit = 16.sp
        val small: TextUnit = 14.sp
    }

    object Body {
        val large: TextUnit = 14.sp
        val medium: TextUnit = 12.sp
    }

    object Label {
        val large: TextUnit = 12.sp
        val medium: TextUnit = 10.sp
    }

    val badge: TextUnit = 12.sp
    val countdown: TextUnit = 14.sp

    // --- Role builders ----------------------------------------------------------------
    // One Glance TextStyle per role. `weight` defaults to the role's most common usage;
    // pass an override at the call site when a specific instance differs.

    fun titleLarge(color: ColorProvider, weight: FontWeight = FontWeight.Bold): TextStyle =
        TextStyle(color = color, fontSize = Title.large, fontWeight = weight)

    fun titleMedium(color: ColorProvider, weight: FontWeight = FontWeight.Medium): TextStyle =
        TextStyle(color = color, fontSize = Title.medium, fontWeight = weight)

    fun titleSmall(color: ColorProvider, weight: FontWeight = FontWeight.Medium): TextStyle =
        TextStyle(color = color, fontSize = Title.small, fontWeight = weight)

    fun bodyLarge(color: ColorProvider, weight: FontWeight = FontWeight.Bold): TextStyle =
        TextStyle(color = color, fontSize = Body.large, fontWeight = weight)

    fun bodyMedium(color: ColorProvider, weight: FontWeight = FontWeight.Normal): TextStyle =
        TextStyle(color = color, fontSize = Body.medium, fontWeight = weight)

    fun labelLarge(color: ColorProvider, weight: FontWeight = FontWeight.Bold): TextStyle =
        TextStyle(color = color, fontSize = Label.large, fontWeight = weight)

    fun labelMedium(color: ColorProvider, weight: FontWeight = FontWeight.Bold): TextStyle =
        TextStyle(color = color, fontSize = Label.medium, fontWeight = weight)

    /** Pill / chip badge text. [fontSize] stays configurable for callers that scale badges. */
    fun badgeText(
        color: ColorProvider,
        fontSize: TextUnit = badge,
        weight: FontWeight = FontWeight.Bold,
    ): TextStyle = TextStyle(color = color, fontSize = fontSize, fontWeight = weight)
}
