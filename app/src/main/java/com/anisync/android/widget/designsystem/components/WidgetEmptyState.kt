package com.anisync.android.widget.designsystem.components

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import com.anisync.android.widget.core.SizeClass
import com.anisync.android.widget.designsystem.tokens.WidgetDimensions
import com.anisync.android.widget.designsystem.tokens.WidgetTypography

/**
 * Configuration for the empty state component.
 * Allows customization of icon, title, subtitle, and click action.
 */
data class EmptyStateConfig(
    val iconResId: Int? = null,
    val title: String,
    val subtitle: String? = null,
    val action: Action? = null
)

/**
 * Reusable empty state component for widgets.
 * Automatically adapts its layout based on the provided SizeClass.
 *
 * @param config Configuration containing icon, title, subtitle, and optional action
 * @param sizeClass Current widget size class (COMPACT, MEDIUM, EXPANDED)
 * @param modifier Optional GlanceModifier for additional styling
 */
@Composable
fun WidgetEmptyState(
    config: EmptyStateConfig,
    sizeClass: SizeClass,
    modifier: GlanceModifier = GlanceModifier
) {
    when (sizeClass) {
        SizeClass.COMPACT -> EmptyStateCompact(config, modifier)
        SizeClass.MEDIUM -> EmptyStateMedium(config, modifier)
        SizeClass.EXPANDED -> EmptyStateExpanded(config, modifier)
    }
}

@Composable
private fun EmptyStateCompact(
    config: EmptyStateConfig,
    modifier: GlanceModifier
) {
    val baseModifier = modifier
        .fillMaxSize()
        .appWidgetBackground()
        .background(GlanceTheme.colors.surface)
    
    val finalModifier = if (config.action != null) {
        baseModifier.clickable(config.action)
    } else {
        baseModifier
    }

    Box(
        modifier = finalModifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = config.title,
            style = WidgetTypography.bodyMedium(
                color = GlanceTheme.colors.onSurface,
                weight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun EmptyStateMedium(
    config: EmptyStateConfig,
    modifier: GlanceModifier
) {
    val baseModifier = modifier
        .fillMaxSize()
        .appWidgetBackground()
        .background(GlanceTheme.colors.surface)
    
    val finalModifier = if (config.action != null) {
        baseModifier.clickable(config.action)
    } else {
        baseModifier
    }

    Box(
        modifier = finalModifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (config.iconResId != null) {
                Image(
                    provider = ImageProvider(config.iconResId),
                    contentDescription = null,
                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                    modifier = GlanceModifier.size(WidgetDimensions.Icon.large)
                )
                Spacer(modifier = GlanceModifier.height(WidgetDimensions.Spacer.small))
            }
            Text(
                text = config.title,
                style = WidgetTypography.titleSmall(
                    color = GlanceTheme.colors.onSurface,
                    weight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun EmptyStateExpanded(
    config: EmptyStateConfig,
    modifier: GlanceModifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (config.iconResId != null) {
                Image(
                    provider = ImageProvider(config.iconResId),
                    contentDescription = null,
                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                    modifier = GlanceModifier.size(WidgetDimensions.Icon.xlarge)
                )
                Spacer(modifier = GlanceModifier.height(WidgetDimensions.Spacer.small))
            }
            Text(
                text = config.title,
                style = WidgetTypography.titleMedium(
                    color = GlanceTheme.colors.onSurface
                )
            )
            if (config.subtitle != null) {
                Spacer(modifier = GlanceModifier.height(WidgetDimensions.Spacer.xsmall))
                Text(
                    text = config.subtitle,
                    style = WidgetTypography.bodyMedium(
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
        }
    }
}
