package com.anisync.android.presentation.components.richtext

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.components.AsyncRichTextRenderer
import com.anisync.android.presentation.util.WindowModalSheetScope
import kotlinx.coroutines.flow.first

private const val DEFAULT_MAX_LENGTH = 10_000

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RichTextInputSheet(
    title: String,
    placeholder: String,
    submitLabel: String,
    isSubmitting: Boolean,
    onSubmit: (body: String) -> Unit,
    onDismiss: () -> Unit,
    replyingToLabel: String? = null,
    prefillBody: String? = null,
    minLength: Int = 1,
    maxLength: Int = DEFAULT_MAX_LENGTH,
    minLines: Int = 4,
    maxLines: Int = 10,
    enablePreview: Boolean = true,
    enableMediaAttach: Boolean = true,
    fullScreen: Boolean = false,
    isSubmitEnabled: (body: String) -> Boolean = { it.trim().length in minLength..maxLength },
    bottomBarLeading: (@Composable RowScope.() -> Unit)? = null
) {
    val bodyState = remember(prefillBody) { TextFieldState(prefillBody.orEmpty()) }
    val maxLengthTransform = remember(maxLength) { MaxLengthInputTransformation(maxLength) }
    var isPreviewMode by remember { mutableStateOf(false) }
    val canSubmit = isSubmitEnabled(bodyState.text.toString()) && !isSubmitting
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current
    val attachViewModel: MediaAttachViewModel = hiltViewModel()
    val attachState by attachViewModel.state.collectAsStateWithLifecycle()
    var showAttachSheet by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val hasDraft = bodyState.text.isNotBlank() && bodyState.text.toString() != prefillBody.orEmpty()
    val hasDraftState = androidx.compose.runtime.rememberUpdatedState(hasDraft)

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = fullScreen,
        confirmValueChange = { targetValue ->
            if (targetValue == SheetValue.Hidden && hasDraftState.value) {
                showDiscardDialog = true
                false
            } else {
                true
            }
        }
    )

    val dismissWithCheck = {
        if (hasDraft) showDiscardDialog = true else onDismiss()
    }
    val insertMarkdown: (String) -> Unit = { md ->
        bodyState.insertAtCursor(if (bodyState.text.isEmpty()) md else "\n$md\n")
    }

    if (enableMediaAttach && showAttachSheet) {
        WindowModalSheetScope {
            MediaAttachSheet(
                viewModel = attachViewModel,
                onDismiss = { showAttachSheet = false },
                onMarkdownReady = insertMarkdown
            )
        }
    }

    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.currentValue }
            .first { it == SheetValue.Expanded || it == SheetValue.PartiallyExpanded }
        focusRequester.requestFocus()
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.editor_discard_title)) },
            text = { Text(stringResource(R.string.editor_discard_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.editor_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.editor_discard_keep))
                }
            }
        )
    }

    // Writing is a focused task: even when launched from a pane, the composer (and the attach sheet
    // stacked over it) stays a WINDOW-modal sheet — the user's attention is on the text, not on
    // pane ownership, and the IME needs the full window anyway.
    WindowModalSheetScope {
    AppModalBottomSheet(
        onDismissRequest = dismissWithCheck,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
            ) {
                HeaderRow(
                    title = title,
                    isPreviewMode = isPreviewMode,
                    isPreviewEnabled = enablePreview && bodyState.text.isNotBlank(),
                    showPreviewToggle = enablePreview,
                    onCancel = dismissWithCheck,
                    onTogglePreview = { isPreviewMode = !isPreviewMode }
                )

                if (replyingToLabel != null) {
                    ReplyingToChip(replyingToLabel)
                }

                Spacer(Modifier.height(8.dp))

                val boxModifier = if (fullScreen) Modifier.weight(1f) else Modifier.weight(1f, fill = false)
                Box(modifier = boxModifier) {
                    if (isPreviewMode) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            AsyncRichTextRenderer(
                                html = bodyState.text.toString(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        val receiveContentListener = remember {
                            object : ReceiveContentListener {
                                override fun onReceive(transferableContent: TransferableContent): TransferableContent? {
                                    return handleImeContent(transferableContent, attachViewModel, insertMarkdown)
                                }
                            }
                        }

                        val textFieldModifier = Modifier
                            .fillMaxWidth()
                            .then(if (fullScreen) Modifier.fillMaxHeight() else Modifier)
                            .padding(horizontal = 16.dp)
                            .focusRequester(focusRequester)
                            .let { base ->
                                if (enableMediaAttach) base.contentReceiver(receiveContentListener) else base
                            }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = textFieldModifier
                        ) {
                            BasicTextField(
                                state = bodyState,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                lineLimits = TextFieldLineLimits.MultiLine(minLines, maxLines),
                                inputTransformation = maxLengthTransform,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
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

                // Toolbar + IME upload strip share an inset-aware Surface so they
                // dock against the IME / nav bar regardless of body length.
                val imeUpload = (attachState as? MediaAttachState.Uploading)
                    ?.takeIf { it.source == MediaAttachState.Source.Ime }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Each modifier consumes only its own inset, so the
                            // toolbar stays docked even when the activity is
                            // `adjustResize` and the system has already
                            // shrunk the window for the IME.
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        if (imeUpload != null) {
                            ImeUploadStrip(state = imeUpload, onCancel = { attachViewModel.cancel() })
                        }
                        BottomBarRow(
                            textFieldState = bodyState,
                            maxLength = maxLength,
                            submitLabel = submitLabel,
                            isSubmitting = isSubmitting,
                            canSubmit = canSubmit,
                            onSubmit = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSubmit(bodyState.text.toString())
                            },
                            bottomBarLeading = bottomBarLeading,
                            onAttachClick = if (enableMediaAttach) ({
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showAttachSheet = true
                            }) else null
                        )
                    }
                }
            }
        }
    }
    }
}


@Composable
private fun HeaderRow(
    title: String,
    isPreviewMode: Boolean,
    isPreviewEnabled: Boolean,
    showPreviewToggle: Boolean,
    onCancel: () -> Unit,
    onTogglePreview: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onCancel) {
            Text(
                text = stringResource(R.string.cancel),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (showPreviewToggle) {
            TextButton(
                onClick = onTogglePreview,
                enabled = isPreviewEnabled,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isPreviewMode) R.string.write else R.string.preview
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = if (!isPreviewEnabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Spacer(Modifier.size(64.dp))
        }
    }
}

@Composable
private fun ReplyingToChip(label: String) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun BottomBarRow(
    textFieldState: TextFieldState,
    maxLength: Int,
    submitLabel: String,
    isSubmitting: Boolean,
    canSubmit: Boolean,
    onSubmit: () -> Unit,
    bottomBarLeading: (@Composable RowScope.() -> Unit)?,
    onAttachClick: (() -> Unit)? = null
) {
    if (bottomBarLeading != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RichTextFormatBar(
                textFieldState = textFieldState,
                buttonSize = 36.dp,
                iconSize = 18.dp,
                modifier = Modifier.fillMaxWidth(),
                onAttachClick = onAttachClick
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomBarLeading()
            Spacer(modifier = Modifier.weight(1f))
            RichTextCharCounter(
                length = textFieldState.text.length,
                maxLength = maxLength,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(end = 12.dp)
            )
            SubmitButton(
                label = submitLabel,
                isSubmitting = isSubmitting,
                enabled = canSubmit,
                onClick = onSubmit
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RichTextFormatBar(
                textFieldState = textFieldState,
                buttonSize = 36.dp,
                iconSize = 18.dp,
                modifier = Modifier.weight(1f),
                onAttachClick = onAttachClick
            )
            RichTextCharCounter(
                length = textFieldState.text.length,
                maxLength = maxLength,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            SubmitButton(
                label = submitLabel,
                isSubmitting = isSubmitting,
                enabled = canSubmit,
                onClick = onSubmit
            )
        }
    }
}

@Composable
private fun SubmitButton(
    label: String,
    isSubmitting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(percent = 50),
        modifier = Modifier.height(40.dp)
    ) {
        if (isSubmitting) {
            AppCircularProgressIndicator(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(16.dp)
            )
        }
        Text(text = label, fontWeight = FontWeight.Bold)
    }
}
