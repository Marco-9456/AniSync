package com.anisync.android.presentation.forum

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.ContentLimits
import com.anisync.android.presentation.components.richtext.RichTextInputScreen

/**
 * Full-screen editor for an existing thread's body (the `EditThreadBody` route). Loads the thread,
 * then shows the shared [RichTextInputScreen]; on submit it publishes the edit to the thread bus and
 * pops. Being a real destination (not an inline overlay), it always opens full-screen — even when
 * the thread detail that launched it lives in a list-detail pane on a large screen.
 */
@Composable
fun EditThreadBodyScreen(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditThreadBodyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EditThreadBodyEvent.Saved -> onSaved()
                EditThreadBodyEvent.LoadFailed -> onBackClick()
            }
        }
    }

    if (uiState.isLoading) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppCircularProgressIndicator()
            }
        }
    } else {
        RichTextInputScreen(
            title = stringResource(R.string.forum_edit_thread_title),
            placeholder = stringResource(R.string.forum_thread_body_hint),
            initialBody = uiState.initialBody,
            isSubmitting = uiState.isSubmitting,
            minLength = ContentLimits.ThreadBody.min,
            maxLength = ContentLimits.ThreadBody.max,
            onSubmit = { viewModel.submit(it) },
            onDismiss = onBackClick
        )
    }
}
