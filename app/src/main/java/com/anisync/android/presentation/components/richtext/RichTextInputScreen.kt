package com.anisync.android.presentation.components.richtext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import com.anisync.android.presentation.util.LocalMainNavBarSuppressor

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
    minLength: Int = 1,
    maxLength: Int = 10_000
) {
    // Hide main bottom nav bar while overlay visible — covers the Edit Profile case
    // where the parent Profile route stays in the bottom-bar whitelist.
    val navBarSuppressor = LocalMainNavBarSuppressor.current
    DisposableEffect(navBarSuppressor) {
        navBarSuppressor?.acquire()
        onDispose { navBarSuppressor?.release() }
    }

    RichTextScaffold(
        title = title,
        initialBody = initialBody,
        placeholder = placeholder,
        submitLabel = submitLabel,
        isSubmitting = isSubmitting,
        onSubmit = onSubmit,
        onDismiss = onDismiss,
        minLength = minLength,
        maxLength = maxLength
    )
}
