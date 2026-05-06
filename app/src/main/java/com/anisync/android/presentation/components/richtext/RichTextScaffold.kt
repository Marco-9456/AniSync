package com.anisync.android.presentation.components.richtext

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.AsyncRichTextRenderer

/**
 * Full-screen scaffold for rich-text authoring (status edits, thread create/edit, etc.).
 *
 * Owns body state, preview toggle, and the discard-changes flow. Callers that have
 * additional state (title, categories, etc.) pass [headerSlot] for content above the
 * body field, [hasExternalUnsavedChanges] to fold their state into the discard guard,
 * and [isExternallyValid] to gate the submit button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichTextScaffold(
    title: String,
    initialBody: String,
    placeholder: String,
    submitLabel: String,
    isSubmitting: Boolean,
    onSubmit: (body: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onBodyChange: (String) -> Unit = {},
    isExternallyValid: Boolean = true,
    hasExternalUnsavedChanges: Boolean = false,
    autoFocusBody: Boolean = true,
    maxLength: Int = 10_000,
    insertController: RichTextInsertController? = null,
    headerSlot: (@Composable ColumnScope.() -> Unit)? = null,
) {
    // `initialBody` is consumed once. Callers wanting to reset state for a different
    // entity should remount via `key(entityId) { RichTextScaffold(...) }`.
    var bodyValue by remember {
        mutableStateOf(TextFieldValue(initialBody, selection = TextRange(initialBody.length)))
    }
    val initialBodySnapshot = remember { initialBody }
    var isPreviewMode by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    if (insertController != null) {
        androidx.compose.runtime.SideEffect {
            insertController.bind { text ->
                bodyValue = bodyValue.insertAtCursor(text)
                onBodyChange(bodyValue.text)
            }
        }
    }

    val isBlank by remember { derivedStateOf { bodyValue.text.isBlank() } }
    val hasBodyChanges by remember {
        derivedStateOf { bodyValue.text != initialBodySnapshot }
    }
    val hasUnsavedChanges = hasBodyChanges || hasExternalUnsavedChanges
    val canSubmit = !isBlank && isExternallyValid

    val focusRequester = remember { FocusRequester() }

    if (autoFocusBody) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    BackHandler(enabled = hasUnsavedChanges) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            onConfirm = {
                showDiscardDialog = false
                onDismiss()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(text = title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (hasUnsavedChanges) showDiscardDialog = true else onDismiss()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { isPreviewMode = !isPreviewMode },
                            enabled = !isBlank
                        ) {
                            Text(
                                text = stringResource(
                                    if (isPreviewMode) R.string.write else R.string.preview
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = if (isBlank) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                else MaterialTheme.colorScheme.primary
                            )
                        }

                        if (isSubmitting) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        } else {
                            TextButton(
                                onClick = { onSubmit(bodyValue.text) },
                                enabled = canSubmit
                            ) {
                                Text(
                                    text = submitLabel,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!canSubmit) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            },
            bottomBar = {
                if (!isPreviewMode) {
                    RichTextDockedFormatBar(
                        textFieldValue = bodyValue,
                        onValueChange = {
                            bodyValue = it
                            onBodyChange(it.text)
                        },
                        maxLength = maxLength
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                headerSlot?.invoke(this)

                if (isPreviewMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        AsyncRichTextRenderer(
                            html = bodyValue.text,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    TextField(
                        value = bodyValue,
                        onValueChange = {
                            if (it.text.length <= maxLength) {
                                bodyValue = it
                                onBodyChange(it.text)
                            }
                        },
                        placeholder = { Text(placeholder) },
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(focusRequester),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscardChangesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.editor_discard_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = { Text(stringResource(R.string.editor_discard_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.editor_discard_confirm),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.editor_discard_keep),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}
