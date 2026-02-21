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
            Text(text = "Enable App Links")
        },
        text = {
            Text(
                text = "To open AniList links directly in AniSync, please enable supported web links for this app in your Android Settings.\n\n" +
                        "1. Tap 'Open Settings'\n" +
                        "2. Tap 'Add link'\n" +
                        "3. Select 'anilist.co' and save."
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    AppLinksUtil.openAppLinksSettings(context)
                    onDismissRequest()
                }
            ) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
