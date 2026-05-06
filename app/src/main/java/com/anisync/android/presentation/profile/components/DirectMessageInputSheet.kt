package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.ContentLimits
import com.anisync.android.presentation.components.richtext.RichTextInputSheet

private val MessageBounds = ContentLimits.MessageActivity

/**
 * Direct-message composer. Wraps [RichTextInputSheet] with a `private` toggle that
 * occupies the bottom-bar leading slot. Preview is enabled — AniList renders DMs as
 * rich text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectMessageInputSheet(
    recipientName: String,
    isSending: Boolean,
    errorMessage: String?,
    onDismissRequest: () -> Unit,
    onSend: (text: String, isPrivate: Boolean) -> Unit,
    initialBody: String? = null,
    submitLabel: String = stringResource(R.string.message_composer_send),
    title: String = stringResource(R.string.message_composer_title, recipientName)
) {
    var isPrivate by rememberSaveable { mutableStateOf(false) }

    RichTextInputSheet(
        title = title,
        placeholder = stringResource(R.string.message_composer_hint),
        submitLabel = submitLabel,
        isSubmitting = isSending,
        onSubmit = { text -> onSend(text.trim(), isPrivate) },
        onDismiss = { if (!isSending) onDismissRequest() },
        prefillBody = initialBody,
        minLength = MessageBounds.min,
        maxLength = MessageBounds.max,
        isSubmitEnabled = { MessageBounds.isValid(it.trim().length) && !isSending },
        bottomBarLeading = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = isPrivate,
                    onCheckedChange = { isPrivate = it },
                    enabled = !isSending,
                    modifier = Modifier.scale(0.85f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.message_composer_private),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    )
}
