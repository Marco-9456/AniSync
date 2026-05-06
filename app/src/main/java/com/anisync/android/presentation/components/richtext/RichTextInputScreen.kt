package com.anisync.android.presentation.components.richtext

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.anisync.android.R

/**
 * Header-less full-screen rich-text input. Use for edits that have no extra metadata
 * (status edits, thread-body edits). For thread creation/edits with title + categories,
 * use [com.anisync.android.presentation.forum.ForumThreadInputScreen] instead.
 */
@Composable
fun RichTextInputScreen(
    title: String,
    placeholder: String,
    initialBody: String,
    isSubmitting: Boolean,
    onSubmit: (body: String) -> Unit,
    onDismiss: () -> Unit,
    submitLabel: String = stringResource(R.string.activity_edit_save),
    maxLength: Int = 10_000
) {
    RichTextScaffold(
        title = title,
        initialBody = initialBody,
        placeholder = placeholder,
        submitLabel = submitLabel,
        isSubmitting = isSubmitting,
        onSubmit = onSubmit,
        onDismiss = onDismiss,
        maxLength = maxLength
    )
}
