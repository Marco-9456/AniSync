package com.anisync.android.presentation.util

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import kotlinx.coroutines.launch

/**
 * Single entry point for "long-press to copy" interactions.
 *
 * Uses Compose's [LocalClipboard] (suspend API, recommended over the deprecated
 * [androidx.compose.ui.platform.LocalClipboardManager]) and centralizes haptic
 * feedback so callers using a plain `combinedClickable` get a consistent press
 * vibration without having to wire it themselves.
 *
 * Android 13+ shows an OS-level clipboard confirmation, so the in-app toast is
 * suppressed there to avoid duplicate UI.
 */
class CopyToClipboard internal constructor(
    private val onCopy: (label: String, text: String, message: String?, isSensitive: Boolean) -> Unit
) {
    operator fun invoke(
        label: String,
        text: String,
        message: String? = null,
        isSensitive: Boolean = false
    ) = onCopy(label, text, message, isSensitive)
}

@Composable
fun rememberCopyToClipboard(): CopyToClipboard {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    val defaultMessage = stringResource(R.string.copied_to_clipboard)
    return remember(context, clipboard, haptic, defaultMessage) {
        CopyToClipboard { label, text, message, isSensitive ->
            if (text.isEmpty()) return@CopyToClipboard
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val clip = ClipData.newPlainText(label, text).apply {
                if (isSensitive) {
                    description.extras = PersistableBundle().apply {
                        // ClipDescription.EXTRA_IS_SENSITIVE on API 33+;
                        // legacy string key for older OEM clipboard UIs.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                        } else {
                            putBoolean("android.content.extra.IS_SENSITIVE", true)
                        }
                    }
                }
            }
            scope.launch { clipboard.setClipEntry(ClipEntry(clip)) }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(context, message ?: defaultMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
