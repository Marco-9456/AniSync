package com.anisync.android.presentation.components.richtext

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anisync.android.R
import com.anisync.android.presentation.components.AsyncRichTextRenderer
import kotlinx.coroutines.flow.first

private const val DEFAULT_MAX_LENGTH = 10_000

/**
 * Bottom-sheet rich-text input — replies, comments, status posts, direct messages.
 *
 * Layout: header (cancel / title / preview toggle) → optional replying-to chip → body
 * (or rendered preview) → docked toolbar row (formatting + leading slot + char count + send).
 *
 * @param bottomBarLeading slot rendered before char count + send (e.g. DM private toggle).
 * @param enablePreview when false, the preview toggle is hidden (preview-less surfaces).
 */
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
    isSubmitEnabled: (body: String) -> Boolean = { it.trim().length in minLength..maxLength },
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    bottomBarLeading: (@Composable RowScope.() -> Unit)? = null
) {
    var bodyValue by remember(prefillBody) {
        val seed = prefillBody.orEmpty()
        mutableStateOf(TextFieldValue(seed, selection = TextRange(seed.length)))
    }
    var isPreviewMode by remember { mutableStateOf(false) }
    val canSubmit = isSubmitEnabled(bodyValue.text) && !isSubmitting
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current
    val attachViewModel: MediaAttachViewModel = hiltViewModel()
    var showAttachSheet by remember { mutableStateOf(false) }
    val insertMarkdown: (String) -> Unit = { md ->
        bodyValue = bodyValue.insertAtCursor(if (bodyValue.text.isEmpty()) md else "\n$md\n")
    }

    if (enableMediaAttach && showAttachSheet) {
        MediaAttachSheet(
            viewModel = attachViewModel,
            onDismiss = { showAttachSheet = false },
            onMarkdownReady = insertMarkdown
        )
    }

    // Defer focus until the sheet is fully expanded — otherwise the IME opens before the
    // sheet animates in and the body sits behind the keyboard on tall layouts.
    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.currentValue }.first { it == SheetValue.Expanded }
        focusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            HeaderRow(
                title = title,
                isPreviewMode = isPreviewMode,
                isPreviewEnabled = enablePreview && bodyValue.text.isNotBlank(),
                showPreviewToggle = enablePreview,
                onCancel = onDismiss,
                onTogglePreview = { isPreviewMode = !isPreviewMode }
            )

            if (replyingToLabel != null) {
                ReplyingToChip(replyingToLabel)
            }

            Spacer(Modifier.height(8.dp))

            if (isPreviewMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    AsyncRichTextRenderer(
                        html = bodyValue.text,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                val textFieldModifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .focusRequester(focusRequester)
                    .let { base ->
                        if (enableMediaAttach) base.contentReceiver { tc ->
                            handleImeContent(tc, attachViewModel, insertMarkdown)
                        } else base
                    }
                TextField(
                    value = bodyValue,
                    onValueChange = {
                        if (it.text.length <= maxLength) bodyValue = it
                    },
                    placeholder = { Text(placeholder) },
                    minLines = minLines,
                    maxLines = maxLines,
                    shape = RoundedCornerShape(16.dp),
                    modifier = textFieldModifier,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            BottomBarRow(
                bodyValue = bodyValue,
                onValueChange = { bodyValue = it },
                maxLength = maxLength,
                submitLabel = submitLabel,
                isSubmitting = isSubmitting,
                canSubmit = canSubmit,
                onSubmit = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSubmit(bodyValue.text)
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
    bodyValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    maxLength: Int,
    submitLabel: String,
    isSubmitting: Boolean,
    canSubmit: Boolean,
    onSubmit: () -> Unit,
    bottomBarLeading: (@Composable RowScope.() -> Unit)?,
    onAttachClick: (() -> Unit)? = null
) {
    if (bottomBarLeading != null) {
        // Two-row layout: format buttons on top, leading + char count + send below.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RichTextFormatBar(
                textFieldValue = bodyValue,
                onValueChange = onValueChange,
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
                length = bodyValue.text.length,
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
        // Single-row layout: scrollable format buttons + char count + send.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RichTextFormatBar(
                textFieldValue = bodyValue,
                onValueChange = onValueChange,
                buttonSize = 36.dp,
                iconSize = 18.dp,
                modifier = Modifier.weight(1f),
                onAttachClick = onAttachClick
            )
            RichTextCharCounter(
                length = bodyValue.text.length,
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
            CircularProgressIndicator(
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
