package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.anisync.android.presentation.components.menu.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Pill-shaped engagement metric (like/comment count) for activity cards.
 *
 * Touch target: M3 requires interactive elements to expose a 48dp hit area
 * even when the visual surface is smaller. We apply `defaultMinSize(48dp)`
 * to the interactive variant so taps near the edge still register.
 *
 * When `onClick` is null the row is non-interactive but still announces its
 * `contentDescription` so TalkBack users can read the value.
 */
@Composable
internal fun ActivityStatPill(
    icon: ImageVector,
    value: Int,
    contentDescription: String,
    onClick: (() -> Unit)?,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val shape = RoundedCornerShape(50)
    val base = Modifier.clip(shape)
    // Width is intentionally intrinsic so the pill does not push the
    // adjacent "last reply" pill into ellipsis on narrow cards. Height is
    // pinned to 48dp to keep an M3-compliant touch target.
    val interactive = if (onClick != null) {
        base
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    } else {
        base
            .defaultMinSize(minHeight = 40.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    }

    Row(
        modifier = interactive,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = formatStatPillValue(value),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * Compact 3-dot overflow control with a single Delete entry guarded by an
 * AlertDialog confirmation. Caller is responsible for only rendering this
 * when the activity is the viewer's own.
 */
@Composable
internal fun ActivityOverflowMenu(
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // Default IconButton size is 48dp which satisfies M3 minimum touch
        // target. Letting it be intrinsic avoids accidentally shrinking it.
        IconButton(onClick = { menuExpanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        Menu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            item(
                text = "Delete",
                leadingIcon = Icons.Default.Delete,
                destructive = true,
                onClick = {
                    menuExpanded = false
                    confirmDelete = true
                }
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete activity?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteClick()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatStatPillValue(value: Int): String {
    return when {
        value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format("%.1fk", value / 1_000.0)
        else -> value.toString()
    }
}
