package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.ContentLimits
import com.anisync.android.presentation.components.filtersheet.OptionPill
import com.anisync.android.presentation.components.richtext.RichTextInputSheet

private val MessageBounds = ContentLimits.MessageActivity

/**
 * Direct-message composer. Who can read the message is picked above the text, private first, and
 * the send button says where it goes. Preview is enabled, AniList renders messages as rich text.
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
    title: String = stringResource(R.string.message_composer_title, recipientName)
) {
    var isPrivate by rememberSaveable { mutableStateOf(true) }

    RichTextInputSheet(
        title = title,
        placeholder = stringResource(R.string.message_composer_hint),
        submitLabel = stringResource(
            if (isPrivate) R.string.message_composer_send_private else R.string.message_composer_send_public
        ),
        isSubmitting = isSending,
        onSubmit = { text -> onSend(text.trim(), isPrivate) },
        onDismiss = { if (!isSending) onDismissRequest() },
        prefillBody = initialBody,
        minLength = MessageBounds.min,
        maxLength = MessageBounds.max,
        isSubmitEnabled = {
            MessageBounds.isValid(ContentLimits.characterCount(it.trim())) && !isSending
        },
        header = {
            Text(
                text = stringResource(R.string.message_composer_visibility),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OptionPill(
                    label = stringResource(R.string.message_composer_private_option),
                    selected = isPrivate,
                    onClick = { if (!isSending) isPrivate = true },
                    modifier = Modifier.weight(1f)
                )
                OptionPill(
                    label = stringResource(R.string.message_composer_public_option),
                    selected = !isPrivate,
                    onClick = { if (!isSending) isPrivate = false },
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = stringResource(
                    if (isPrivate) R.string.message_composer_private_explain else R.string.message_composer_public_explain,
                    recipientName
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)
            )
        },
        bottomBarLeading = errorMessage?.let { message ->
            {
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    )
}
