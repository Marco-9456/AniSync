package com.anisync.android.widget.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider
import com.anisync.android.widget.designsystem.tokens.WidgetDimensions

/**
 * A horizontal progress bar component for widgets.
 * Shows progress as a filled portion of a track.
 *
 * @param progress Progress value from 0f to 1f
 * @param modifier Optional GlanceModifier
 * @param height Height of the progress bar
 * @param trackColor Background color of the track
 * @param progressColor Color of the filled progress portion
 */
@Composable
fun WidgetProgressBar(
    progress: Float,
    modifier: GlanceModifier = GlanceModifier,
    height: Dp = WidgetDimensions.progressBarHeight,
    trackColor: ColorProvider = GlanceTheme.colors.surfaceVariant,
    progressColor: ColorProvider = GlanceTheme.colors.primary
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val cornerRadius = height / 2

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .cornerRadius(cornerRadius)
            .background(trackColor)
    ) {
        if (clampedProgress > 0f) {
            // Calculate fill width as percentage of container
            // Using toInt().dp as approximation since Glance doesn't support fractional widths well
            val fillWidthDp = (clampedProgress * 100).toInt().dp
            Box(
                modifier = GlanceModifier
                    .width(fillWidthDp)
                    .height(height)
                    .cornerRadius(cornerRadius)
                    .background(progressColor)
            ) {}
        }
    }
}
