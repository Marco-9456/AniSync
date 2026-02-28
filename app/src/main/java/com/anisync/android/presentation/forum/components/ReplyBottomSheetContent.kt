package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.util.rememberHapticFeedback

private const val MAX_REPLY_LENGTH = 2000

@Composable
fun ReplyBottomSheetContent(
    replyingToAuthor: String?,
    isSubmitting: Boolean,
    onSubmit: (body: String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val isBlank = text.isBlank()
    val focusRequester = remember { FocusRequester() }
    val haptic = rememberHapticFeedback()

    // Auto-focus the text field when the sheet opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Header
        if (replyingToAuthor != null) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "Replying to $replyingToAuthor",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        Text(
            text = stringResource(R.string.forum_write_reply),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= MAX_REPLY_LENGTH) text = it },
            placeholder = { Text(stringResource(R.string.forum_reply_hint)) },
            minLines = 4,
            maxLines = 10,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )

        // Character counter
        Text(
            text = "${text.length} / $MAX_REPLY_LENGTH",
            style = MaterialTheme.typography.labelSmall,
            color = if (text.length > MAX_REPLY_LENGTH * 0.9)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 4.dp)
        )

        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(percent = 50)
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSubmit(text)
                },
                enabled = !isBlank && !isSubmitting,
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier.height(48.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp).size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp).size(18.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.forum_post_reply),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
