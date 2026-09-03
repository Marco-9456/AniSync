package com.anisync.android.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.type.MediaType

/** Height of the rails this toggle sits in, on both Library and Discover. */
val MediaTypeToggleHeight = 40.dp

/** Width of a segment on compact widths, where it carries its icon alone. */
private val IconOnlySegmentWidth = 44.dp

/**
 * Anime and manga as two segments rather than a full-width group.
 *
 * Wider windows have the room to spell the two words out, so they do; a phone rail does not, and
 * keeps the icons alone beside the list chips that share its row. Both cases sit in the same 40dp
 * target, up from the 35x34dp square that was easy to miss. The shapes are the connected-group
 * shapes the rest of the app's switchers use: full radius on the selected end, a tight inner
 * corner on the seam.
 *
 * Shared by the Library rail and Discover's browse rail so the two cannot drift apart.
 */
@Composable
fun MediaTypeToggle(
    selected: MediaType,
    onSelect: (MediaType) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = MediaTypeToggleHeight
) {
    val haptic = rememberHapticFeedback()
    val showLabels = !LocalAdaptiveInfo.current.isCompact
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        MediaTypeSegment(
            icon = Icons.Default.Tv,
            label = stringResource(R.string.media_type_anime),
            showLabel = showLabels,
            selected = selected == MediaType.ANIME,
            shape = RoundedCornerShape(
                topStart = 17.dp,
                bottomStart = 17.dp,
                topEnd = 7.dp,
                bottomEnd = 7.dp
            ),
            selectedShape = CircleShape,
            height = height,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelect(MediaType.ANIME)
            }
        )
        MediaTypeSegment(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            label = stringResource(R.string.media_type_manga),
            showLabel = showLabels,
            selected = selected == MediaType.MANGA,
            shape = RoundedCornerShape(
                topEnd = 17.dp,
                bottomEnd = 17.dp,
                topStart = 7.dp,
                bottomStart = 7.dp
            ),
            selectedShape = CircleShape,
            height = height,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelect(MediaType.MANGA)
            }
        )
    }
}

@Composable
private fun MediaTypeSegment(
    icon: ImageVector,
    label: String,
    showLabel: Boolean,
    selected: Boolean,
    shape: Shape,
    selectedShape: Shape,
    height: Dp,
    onClick: () -> Unit
) {
    val resolved = if (selected) selectedShape else shape
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        shape = resolved,
        modifier = Modifier
            .then(
                if (showLabel) {
                    Modifier.height(height)
                } else {
                    Modifier.size(width = IconOnlySegmentWidth, height = height)
                }
            )
            .bouncyClickable(onClick = onClick, role = Role.Tab, clipShape = resolved)
            .clearAndSetSemantics {
                role = Role.Tab
                this.selected = selected
                contentDescription = label
            }
    ) {
        Row(
            modifier = if (showLabel) Modifier.padding(horizontal = 14.dp) else Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (showLabel) {
                Arrangement.spacedBy(6.dp)
            } else {
                Arrangement.Center
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(18.dp)
            )
            if (showLabel) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = content,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
