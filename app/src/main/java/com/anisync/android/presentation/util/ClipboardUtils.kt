package com.anisync.android.presentation.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.anisync.android.R

/**
 * Single entry point for "long-press to copy" interactions.
 *
 * Haptic feedback is left to the caller's [combinedClickable] / bouncyCombinedClickable
 * so it routes through the app's haptic-enabled setting. Android 13+ shows an
 * OS-level clipboard confirmation, so we skip our own toast there to avoid duplicate UI.
 */
fun interface CopyToClipboard {
    operator fun invoke(label: String, text: String, message: String?)
}

operator fun CopyToClipboard.invoke(label: String, text: String) = invoke(label, text, null)

@Composable
fun rememberCopyToClipboard(): CopyToClipboard {
    val context = LocalContext.current
    val defaultMessage = stringResource(R.string.copied_to_clipboard)
    return remember(context, defaultMessage) {
        CopyToClipboard { label, text, message ->
            if (text.isEmpty()) return@CopyToClipboard
            val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(context, message ?: defaultMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
