package com.anisync.android.widget.designsystem.components

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.text.Text
import com.anisync.android.widget.designsystem.tokens.WidgetDimensions
import com.anisync.android.widget.designsystem.tokens.WidgetTypography

/**
 * A loading state component for widgets.
 * Displays a circular progress indicator with optional message.
 *
 * @param message Optional loading message to display
 * @param modifier Optional GlanceModifier
 * @param showBackground Whether to apply widget background styling
 */
@Composable
fun WidgetLoadingState(
    message: String? = null,
    modifier: GlanceModifier = GlanceModifier,
    showBackground: Boolean = true
) {
    val baseModifier = if (showBackground) {
        modifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
    } else {
        modifier.fillMaxSize()
    }

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = GlanceModifier.size(WidgetDimensions.Icon.xlarge),
                color = GlanceTheme.colors.primary
            )
            
            if (message != null) {
                Spacer(modifier = GlanceModifier.height(WidgetDimensions.Spacer.medium))
                Text(
                    text = message,
                    style = WidgetTypography.bodyMedium(
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
        }
    }
}
