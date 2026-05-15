package com.anisync.android.presentation.components.richtext

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anisync.android.R
import com.anisync.android.presentation.components.AsyncRichTextRenderer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    minLength: Int = 1,
    maxLength: Int = 10_000,
    enableMediaAttach: Boolean = true,
    insertController: RichTextInsertController? = null,
    headerSlot: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val bodyState = remember { TextFieldState(initialBody) }
    val initialBodySnapshot = remember { initialBody }
    var isPreviewMode by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val attachViewModel: MediaAttachViewModel = hiltViewModel()
    val attachState by attachViewModel.state.collectAsStateWithLifecycle()
    var showAttachSheet by remember { mutableStateOf(false) }

    val insertMarkdown: (String) -> Unit = { md ->
        bodyState.insertAtCursor(if (bodyState.text.isEmpty()) md else "\n$md\n")
    }

    if (enableMediaAttach && showAttachSheet) {
        MediaAttachSheet(
            viewModel = attachViewModel,
            onDismiss = { showAttachSheet = false },
            onMarkdownReady = insertMarkdown
        )
    }

    if (insertController != null) {
        androidx.compose.runtime.SideEffect {
            insertController.bind { text ->
                bodyState.insertAtCursor(text)
            }
        }
    }

    // Connect TextFieldState changes to the provided callback
    LaunchedEffect(bodyState) {
        snapshotFlow { bodyState.text }.collect { text ->
            onBodyChange(text.toString())
        }
    }

    val isBlank by remember { derivedStateOf { bodyState.text.isBlank() } }
    val meetsMinLength by remember(minLength) {
        derivedStateOf { bodyState.text.trim().length >= minLength }
    }
    val withinMaxLength by remember(maxLength) {
        derivedStateOf { bodyState.text.length <= maxLength }
    }
    val hasBodyChanges by remember {
        derivedStateOf { bodyState.text.toString() != initialBodySnapshot }
    }
    val hasUnsavedChanges = hasBodyChanges || hasExternalUnsavedChanges
    val canSubmit = meetsMinLength && withinMaxLength && isExternallyValid
    val maxLengthTransform = remember(maxLength) { MaxLengthInputTransformation(maxLength) }

    val focusRequester = remember { FocusRequester() }

    if (autoFocusBody) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    // Always intercept system back so the overlay dismisses itself instead of
    // letting the gesture pop the parent route off the nav stack (which would
    // drop the user back to the start destination, e.g. Library, when the
    // editor is rendered as an inline overlay on Feed / Profile).
    BackHandler {
        if (hasUnsavedChanges) showDiscardDialog = true else onDismiss()
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
                                onClick = { onSubmit(bodyState.text.toString()) },
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
                    val imeUpload = (attachState as? MediaAttachState.Uploading)
                        ?.takeIf { it.source == MediaAttachState.Source.Ime }
                    RichTextDockedFormatBar(
                        textFieldState = bodyState,
                        maxLength = maxLength,
                        onAttachClick = if (enableMediaAttach) ({ showAttachSheet = true }) else null,
                        imeUpload = imeUpload,
                        onImeUploadCancel = { attachViewModel.cancel() }
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
                            html = bodyState.text.toString(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    val receiveContentListener = remember {
                        ReceiveContentListener { transferableContent -> handleImeContent(transferableContent, attachViewModel, insertMarkdown) }
                    }

                    val textFieldModifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .padding(16.dp)
                        .let { base ->
                            if (enableMediaAttach) base.contentReceiver(receiveContentListener) else base
                        }

                    BasicTextField(
                        state = bodyState,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        inputTransformation = maxLengthTransform,
                        modifier = textFieldModifier,
                        decorator = { innerTextField ->
                            if (bodyState.text.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            innerTextField()
                        }
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