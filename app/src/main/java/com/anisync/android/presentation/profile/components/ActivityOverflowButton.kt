package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.UserActivity
import com.anisync.android.presentation.components.AppAlertDialog
import com.anisync.android.presentation.components.menu.Menu
import com.anisync.android.presentation.util.openUrl
import com.anisync.android.presentation.util.shareActivity

/**
 * The card's overflow: share, the AniList link, and the owner's own edit and delete.
 *
 * Every activity type gets the same one, so a list update is as shareable and as deletable as a
 * status. Edit is absent on a list update, which AniList derives from the list itself.
 */
@Composable
internal fun ActivityOverflowButton(
    activity: UserActivity,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 36.dp,
    iconSize: Dp = 20.dp,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(buttonSize)) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(iconSize)
            )
        }
        Menu(expanded = expanded, onDismissRequest = { expanded = false }) {
            item(
                text = stringResource(R.string.cd_share),
                leadingIcon = Icons.Default.Share,
                onClick = {
                    expanded = false
                    shareActivity(context, activity.id)
                }
            )
            item(
                text = stringResource(R.string.activity_open_on_anilist),
                leadingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                onClick = {
                    expanded = false
                    openUrl(context, "https://anilist.co/activity/${activity.id}")
                }
            )
            if (onEditClick != null || onDeleteClick != null) gap()
            if (onEditClick != null) {
                item(
                    text = stringResource(R.string.edit),
                    leadingIcon = Icons.Default.Edit,
                    onClick = {
                        expanded = false
                        onEditClick()
                    }
                )
            }
            if (onDeleteClick != null) {
                item(
                    text = stringResource(R.string.delete),
                    leadingIcon = Icons.Default.Delete,
                    destructive = true,
                    onClick = {
                        expanded = false
                        confirmDelete = true
                    }
                )
            }
        }
    }

    if (confirmDelete && onDeleteClick != null) {
        AppAlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.activity_delete_confirm_title)) },
            text = { Text(stringResource(R.string.activity_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteClick()
                }) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
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
