package com.anisync.android.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.util.bouncyClickable

enum class HeaderLevel {
    Screen, // Top of screen (e.g. "Library")
    Section, // Content section (e.g. "Trending", "Characters")
    Subsection // Smaller section (e.g. inside cards)
}

/**
 * A universal header component for screens and sections.
 * 
 * @param title The main title text.
 * @param level Controls the visual hierarchy (Size, Spacing).
 * @param modifier Modifier for the container.
 * @param subtitle Optional description text below the title.
 * @param icon Optional leading icon.
 * @param onActionClick Optional callback for the action button (e.g. "See All").
 * @param actionLabel Optional text for the action button. Defaults to "More" arrow if null but onClick is provided.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    level: HeaderLevel = HeaderLevel.Section,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onActionClick: (() -> Unit)? = null,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    padding: androidx.compose.foundation.layout.PaddingValues? = null,
    /**
     * Optional composable rendered in the trailing area, immediately before the
     * "See all" action button (e.g. an add or write icon). Shown even when
     * [onActionClick] is null, so a section with no "See all" can still expose
     * an action.
     */
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(padding ?: androidx.compose.foundation.layout.PaddingValues(
                horizontal = androidx.compose.ui.res.dimensionResource(com.anisync.android.R.dimen.spacing_large),
                vertical = if (level == HeaderLevel.Screen) androidx.compose.ui.res.dimensionResource(com.anisync.android.R.dimen.spacing_medium) else androidx.compose.ui.res.dimensionResource(com.anisync.android.R.dimen.spacing_small)
            )),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically // Align to center for cleaner look
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Decorative Pill for Section Level (Only if no icon is provided)
            if (level == HeaderLevel.Section && icon == null) {
               Box(
                   modifier = Modifier
                       .size(4.dp, 24.dp)
                       .clip(RoundedCornerShape(4.dp))
                       .background(iconColor)
               )
               Spacer(modifier = Modifier.width(androidx.compose.ui.res.dimensionResource(com.anisync.android.R.dimen.spacing_normal)))
            }

            // Optional Leading Icon (used if provided)
            if (icon != null) {
                Surface(
                    color = iconColor.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(if (level == HeaderLevel.Screen) 40.dp else 32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(if (level == HeaderLevel.Screen) 24.dp else 18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(androidx.compose.ui.res.dimensionResource(com.anisync.android.R.dimen.spacing_normal)))
            }

            Column(
                modifier = Modifier.semantics { heading() }
            ) {
                Text(
                    text = title,
                    style = when (level) {
                        HeaderLevel.Screen -> MaterialTheme.typography.headlineMedium
                        HeaderLevel.Section -> MaterialTheme.typography.titleLarge
                        HeaderLevel.Subsection -> MaterialTheme.typography.titleMedium
                    },
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Trailing area: optional custom action(s) followed by the See All button.
        if (trailingContent != null || onActionClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                trailingContent?.invoke()

                if (onActionClick != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .bouncyClickable(
                                onClick = onActionClick,
                                clipShape = RoundedCornerShape(50)
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = actionLabel ?: "See all $title"
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                if (actionLabel != null) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (actionIcon != null) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(24.dp)
                    )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- PREVIEWS ---

@Preview(showBackground = true, name = "Screen Header (Library)")
@Composable
private fun PreviewScreenHeader() {
    MaterialTheme {
        SectionHeader(
            title = "Library",
            level = HeaderLevel.Screen,
            subtitle = "Your collection",
            icon = null
        )
    }
}

@Preview(showBackground = true, name = "Section Header (Trending)")
@Composable
private fun PreviewSectionHeader() {
    MaterialTheme {
        SectionHeader(
            title = "Trending Now",
            level = HeaderLevel.Section,
            icon = null,
            iconColor = Color(0xFFFF5722),
            onActionClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Section Header (With Icon)")
@Composable
private fun PreviewSectionHeaderWithIcon() {
    MaterialTheme {
        SectionHeader(
            title = "Upcoming",
            level = HeaderLevel.Section,
            icon = Icons.Default.Upcoming,
            onActionClick = {},
            actionLabel = "See All"
        )
    }
}
