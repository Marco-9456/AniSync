package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R

@Composable
fun ReplyBottomSheetContent(
    replyingToAuthor: String?,
    isSubmitting: Boolean,
    onSubmit: (body: String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val isBlank = text.isBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header
        if (replyingToAuthor != null) {
            Text(
                text = "Replying to $replyingToAuthor",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
        }

        Text(
            text = stringResource(R.string.forum_write_reply),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(R.string.forum_reply_hint)) },
            minLines = 4,
            maxLines = 10,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { onSubmit(text) },
                enabled = !isBlank && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text(stringResource(R.string.forum_post_reply))
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
