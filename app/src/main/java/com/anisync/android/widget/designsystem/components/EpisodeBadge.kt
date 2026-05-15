package com.anisync.android.widget.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
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

/**
 * Configuration for episode badge styling.
 */
data class EpisodeBadgeStyle(
    val backgroundColor: ColorProvider,
    val textColor: ColorProvider,
    val fontSize: TextUnit = WidgetTypography.badge
)

/**
 * A pill-shaped badge displaying an episode number.
 *
 * @param episodeNumber The episode number to display
 * @param style Badge styling configuration
 * @param modifier Optional GlanceModifier
 * @param showLabel If true, shows "EPISODE X", otherwise just "EP X"
 */
@Composable
fun EpisodeBadge(
    episodeNumber: Int,
    style: EpisodeBadgeStyle,
    modifier: GlanceModifier = GlanceModifier,
    showLabel: Boolean = true
) {
    Box(
        modifier = modifier
            .background(style.backgroundColor)
            .cornerRadius(99.dp) // Pill shape
            .padding(
                horizontal = WidgetDimensions.Badge.paddingHorizontal,
                vertical = WidgetDimensions.Badge.paddingVertical
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (showLabel) "EPISODE $episodeNumber" else "EP $episodeNumber",
            style = WidgetTypography.badgeText(
                color = style.textColor,
                fontSize = style.fontSize,
            )
        )
    }
}

/**
 * Creates a hero-style episode badge (primary colors).
 */
@Composable
fun HeroEpisodeBadge(
    episodeNumber: Int,
    modifier: GlanceModifier = GlanceModifier
) {
    EpisodeBadge(
        episodeNumber = episodeNumber,
        style = EpisodeBadgeStyle(
            backgroundColor = GlanceTheme.colors.primary,
            textColor = GlanceTheme.colors.onPrimary
        ),
        modifier = modifier
    )
}

/**
 * Creates a standard episode badge (tertiary colors).
 */
@Composable
fun StandardEpisodeBadge(
    episodeNumber: Int,
    modifier: GlanceModifier = GlanceModifier
) {
    EpisodeBadge(
        episodeNumber = episodeNumber,
        style = EpisodeBadgeStyle(
            backgroundColor = GlanceTheme.colors.tertiaryContainer,
            textColor = GlanceTheme.colors.onTertiaryContainer
        ),
        modifier = modifier
    )
}
