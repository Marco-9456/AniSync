package com.anisync.android.presentation.components

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import com.anisync.android.util.AppLinksUtil

@Composable
fun AppLinksPromptDialog(
    onDismissRequest: () -> Unit
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return
    }

    val context = LocalContext.current
    val instructions = if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
        stringResource(R.string.samsung_app_links_instructions)
    } else {
        stringResource(R.string.default_app_links_instructions)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(text = stringResource(R.string.enable_app_links_title))
        },
        text = {
            Text(
                instructions
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    AppLinksUtil.openAppLinksSettings(context)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
