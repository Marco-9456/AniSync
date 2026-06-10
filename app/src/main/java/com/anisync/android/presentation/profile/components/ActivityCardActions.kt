package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.menu.Menu

/**
 * Pill-shaped engagement metric (like/comment count) for activity cards.
 *
 * Designed using M3 aesthetics, it features an explicit background tint
 * and bolded content colors.
 */
@Composable
internal fun ActivityStatPill(
    icon: ImageVector,
    value: Int,
    contentDescription: String,
    onClick: (() -> Unit)?,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = Color.Transparent
) {
    val shape = RoundedCornerShape(50)
    val base = Modifier
        .clip(shape)
        .background(containerColor)

    val interactive = if (onClick != null) {
        base
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 40.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    } else {
        base
            .defaultMinSize(minHeight = 40.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    }

    Row(
        modifier = interactive,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = formatStatPillValue(value),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1
        )
    }
}

/**
 * Compact 3-dot overflow control. Renders Edit (when [onEditClick] non-null) and
 * Delete entries; Delete is guarded by an AlertDialog. Caller is responsible for
 * only rendering this when the activity is the viewer's own.
 */
@Composable
internal fun ActivityOverflowMenu(
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEditClick: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Menu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            if (onEditClick != null) {
                item(
                    text = stringResource(R.string.edit),
                    leadingIcon = Icons.Default.Edit,
                    onClick = {
                        menuExpanded = false
                        onEditClick()
                    }
                )
                gap()
            }
            item(
                text = stringResource(R.string.delete),
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
            title = { Text(stringResource(R.string.dialog_delete_activity_title)) },
            text = { Text(stringResource(R.string.dialog_delete_activity_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteClick()
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
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