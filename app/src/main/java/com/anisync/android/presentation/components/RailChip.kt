package com.anisync.android.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.rememberHapticFeedback

/**
 * One chip of a [PinnedRail]: a 16dp glyph and its label, with the match count after it where the
 * rail has one. The Library tints the selected chip with the list's own colour, everything else
 * takes secondaryContainer.
 */
@Composable
fun RailChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    selectedContainer: Color = MaterialTheme.colorScheme.secondaryContainer,
    selectedContent: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    val haptic = rememberHapticFeedback()
    val container by animateColorAsState(
        targetValue = if (selected) selectedContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "RailChipContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) selectedContent else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "RailChipContent"
    )

    Surface(
        color = container,
        shape = CircleShape,
        modifier = modifier
            .height(ConnectedToggleDefaults.Height)
            .bouncyClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                role = Role.Tab,
                clipShape = CircleShape
            )
            .clearAndSetSemantics {
                role = Role.Tab
                this.selected = selected
                contentDescription = buildString {
                    append(label)
                    if (count != null) append(", $count")
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = content,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (count != null) {
                Surface(shape = CircleShape, color = content.copy(alpha = 0.16f)) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = content,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
