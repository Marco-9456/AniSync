package com.anisync.android.widget.designsystem.components

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import com.anisync.android.widget.designsystem.tokens.WidgetDimensions
import com.anisync.android.widget.designsystem.tokens.WidgetTypography

/**
 * An error state component for widgets.
 * Displays an error message with optional retry action.
 *
 * @param title Error title
 * @param message Optional error description
 * @param iconResId Optional error icon resource
 * @param retryAction Optional action to retry the failed operation
 * @param modifier Optional GlanceModifier
 */
@Composable
fun WidgetErrorState(
    title: String,
    message: String? = null,
    iconResId: Int? = null,
    retryAction: Action? = null,
    modifier: GlanceModifier = GlanceModifier
) {
    val baseModifier = modifier
        .fillMaxSize()
        .appWidgetBackground()
        .background(GlanceTheme.colors.surface)
    
    val finalModifier = if (retryAction != null) {
        baseModifier.clickable(retryAction)
    } else {
        baseModifier
    }

    Box(
        modifier = finalModifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.padding(WidgetDimensions.paddingLarge)
        ) {
            if (iconResId != null) {
                Image(
                    provider = ImageProvider(iconResId),
                    contentDescription = null,
                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.error),
                    modifier = GlanceModifier.size(WidgetDimensions.Icon.xlarge)
                )
                Spacer(modifier = GlanceModifier.height(WidgetDimensions.Spacer.small))
            }
            
            Text(
                text = title,
                style = WidgetTypography.titleSmall(
                    color = GlanceTheme.colors.onSurface
                )
            )
            
            if (message != null) {
                Spacer(modifier = GlanceModifier.height(WidgetDimensions.Spacer.xsmall))
                Text(
                    text = message,
                    style = WidgetTypography.bodyMedium(
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
            
            if (retryAction != null) {
                Spacer(modifier = GlanceModifier.height(WidgetDimensions.Spacer.medium))
                Box(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(WidgetDimensions.cornerRadiusSmall)
                        .padding(
                            horizontal = WidgetDimensions.paddingMedium,
                            vertical = WidgetDimensions.paddingSmall
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap to retry",
                        style = WidgetTypography.bodyMedium(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            weight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
