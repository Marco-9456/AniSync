package com.anisync.android.widget.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import com.anisync.android.widget.designsystem.tokens.WidgetDimensions
import com.anisync.android.widget.designsystem.tokens.WidgetTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A badge displaying a formatted time (HH:mm).
 * Used in timeline views to show airing times.
 *
 * @param timeSeconds Unix timestamp in seconds
 * @param modifier Optional GlanceModifier
 * @param backgroundColor Background color of the badge
 * @param textColor Text color
 * @param fontSize Font size for the time text
 */
@Composable
fun TimeBadge(
    timeSeconds: Long,
    modifier: GlanceModifier = GlanceModifier,
    backgroundColor: ColorProvider = GlanceTheme.colors.primaryContainer,
    textColor: ColorProvider = GlanceTheme.colors.onPrimaryContainer,
    fontSize: TextUnit = WidgetTypography.badge
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(timeSeconds * 1000))

    Box(
        modifier = modifier
            .background(backgroundColor)
            .cornerRadius(WidgetDimensions.Badge.cornerRadius)
            .padding(
                horizontal = WidgetDimensions.Badge.paddingHorizontal,
                vertical = WidgetDimensions.Badge.paddingVertical
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = timeString,
            style = WidgetTypography.badgeText(
                color = textColor,
                fontSize = fontSize,
            )
        )
    }
}

/**
 * Creates a time badge with raw time string instead of timestamp.
 */
@Composable
fun TimeBadgeFromString(
    timeString: String,
    modifier: GlanceModifier = GlanceModifier,
    backgroundColor: ColorProvider = GlanceTheme.colors.primaryContainer,
    textColor: ColorProvider = GlanceTheme.colors.onPrimaryContainer,
    fontSize: TextUnit = WidgetTypography.badge
) {
    Box(
        modifier = modifier
            .background(backgroundColor)
            .cornerRadius(WidgetDimensions.Badge.cornerRadius)
            .padding(
                horizontal = WidgetDimensions.Badge.paddingHorizontal,
                vertical = WidgetDimensions.Badge.paddingVertical
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = timeString,
            style = WidgetTypography.badgeText(
                color = textColor,
                fontSize = fontSize,
            )
        )
    }
}
