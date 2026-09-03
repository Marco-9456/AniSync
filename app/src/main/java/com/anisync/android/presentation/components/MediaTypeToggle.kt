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
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.type.MediaType

/** Height of the rails this toggle sits in, on both Library and Discover. */
val MediaTypeToggleHeight = 40.dp

/**
 * Anime and manga as two labelled segments rather than a full-width group.
 *
 * Both segments carry their icon and its word: the icons alone sat in a 35x34dp target that was
 * easy to miss and gave nothing to read, and the pair still costs far less width than the
 * full-width group the screens used to spend a row on. The shapes are the connected-group shapes
 * the rest of the app's switchers use: full radius on the selected end, a tight inner corner on
 * the seam.
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
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        MediaTypeSegment(
            icon = Icons.Default.Tv,
            label = stringResource(R.string.media_type_anime),
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
            .height(height)
            .bouncyClickable(onClick = onClick, role = Role.Tab, clipShape = resolved)
            .clearAndSetSemantics {
                role = Role.Tab
                this.selected = selected
                contentDescription = label
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(18.dp)
            )
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
