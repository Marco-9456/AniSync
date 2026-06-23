package com.anisync.android.presentation.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.components.richtext.RichTextInputScreen

/**
 * Full-screen composer for a new feed status (the `CreateStatus` route). Reuses the shared
 * [RichTextInputScreen]; on submit it posts the status, signals the feed to reload, and pops. Being
 * a real destination (not an inline overlay), it always opens full-screen — even when launched from
 * the Feed list pane on a large screen, where an open detail pane would otherwise crush the editor.
 */
@Composable
fun CreateStatusScreen(
    onBackClick: () -> Unit,
    onPosted: () -> Unit,
    viewModel: CreateStatusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CreateStatusEvent.Posted -> onPosted()
            }
        }
    }

    RichTextInputScreen(
        title = stringResource(R.string.feed_compose_title),
        placeholder = stringResource(R.string.feed_compose_placeholder),
        initialBody = "",
        submitLabel = stringResource(R.string.feed_compose_submit),
        isSubmitting = uiState.isSubmitting,
        minLength = uiState.minLength,
        maxLength = uiState.maxLength,
        onSubmit = { viewModel.submit(it) },
        onDismiss = onBackClick
    )
}
