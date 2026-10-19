package com.anisync.android.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties
import com.anisync.android.ui.theme.ProvideDisplaySize

/**
 * AniSync's alert dialog, a drop-in for the Material 3 [AlertDialog] call sites with the same
 * parameters. The dialog is a window of its own, which Compose draws at the phone's density, so
 * every slot takes the app's display size back. The container around the slots keeps the phone's.
 */
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { ProvideDisplaySize(content = confirmButton) },
        modifier = modifier,
        dismissButton = dismissButton?.let { slot -> { ProvideDisplaySize(content = slot) } },
        icon = icon?.let { slot -> { ProvideDisplaySize(content = slot) } },
        title = title?.let { slot -> { ProvideDisplaySize(content = slot) } },
        text = text?.let { slot -> { ProvideDisplaySize(content = slot) } },
        shape = shape,
        containerColor = containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties,
    )
}
