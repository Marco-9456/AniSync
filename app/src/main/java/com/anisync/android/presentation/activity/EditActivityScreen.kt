package com.anisync.android.presentation.activity

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
import com.anisync.android.presentation.components.richtext.RichTextInputScreen

/**
 * Full-screen editor for the viewer's own status/message body (the `EditActivity` route). Loads the
 * activity, then shows the shared [RichTextInputScreen]; on submit it publishes the edit to the
 * activity bus and pops. Being a real destination (not an inline overlay), it always opens
 * full-screen — even when the activity detail that launched it lives in a list-detail pane.
 */
@Composable
fun EditActivityScreen(
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EditActivityEvent.Saved -> onSaved()
                EditActivityEvent.LoadFailed -> onBackClick()
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
            title = stringResource(R.string.activity_edit_status_title),
            placeholder = stringResource(R.string.feed_compose_placeholder),
            initialBody = uiState.initialBody,
            isSubmitting = uiState.isSubmitting,
            minLength = uiState.minLength,
            maxLength = uiState.maxLength,
            onSubmit = { viewModel.submit(it) },
            onDismiss = onBackClick
        )
    }
}
