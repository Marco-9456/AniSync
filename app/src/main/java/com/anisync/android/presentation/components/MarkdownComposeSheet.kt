package com.anisync.android.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.util.rememberHapticFeedback

/**
 * Reusable markdown compose bottom sheet — supports replies, status posts, comments.
 *
 * Layout pattern matches `EditLibraryEntrySheet` (verticalScroll + imePadding +
 * disabled overscroll) so the keyboard never hides the input.
 *
 * Caller controls all copy via [title], [placeholder], [submitLabel], [replyingToLabel].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MarkdownComposeSheet(
    title: String,
    placeholder: String,
    submitLabel: String,
    isSubmitting: Boolean,
    onSubmit: (body: String) -> Unit,
    onDismiss: () -> Unit,
    replyingToLabel: String? = null,
    prefillBody: String? = null,
    maxLength: Int = DEFAULT_MAX_LENGTH,
    minLines: Int = 4,
    maxLines: Int = 10,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var textFieldValue by remember(prefillBody) {
        val seed = prefillBody ?: ""
        mutableStateOf(TextFieldValue(seed, selection = TextRange(seed.length)))
    }
    var isPreviewMode by remember { mutableStateOf(false) }
    val isBlank = textFieldValue.text.isBlank()
    val focusRequester = remember { FocusRequester() }
    val haptic = rememberHapticFeedback()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        val scrollState = rememberScrollState()

        // Same overscroll-disable + imePadding + verticalScroll pattern used by
        // EditLibraryEntrySheet to keep the input above the IME.
        CompositionLocalProvider(LocalOverscrollFactory provides null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                if (replyingToLabel != null) {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = replyingToLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MarkdownToolbarButton(
                        icon = Icons.Default.FormatBold,
                        contentDescription = "Bold"
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        textFieldValue = textFieldValue.wrapSelection("__", "__")
                    }
                    MarkdownToolbarButton(
                        icon = Icons.Default.FormatItalic,
                        contentDescription = "Italic"
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        textFieldValue = textFieldValue.wrapSelection("_", "_")
                    }
                    MarkdownToolbarButton(
                        icon = Icons.Default.FormatStrikethrough,
                        contentDescription = "Strikethrough"
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        textFieldValue = textFieldValue.wrapSelection("~~", "~~")
                    }
                    MarkdownToolbarButton(
                        icon = Icons.Default.Code,
                        contentDescription = "Code"
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        textFieldValue = textFieldValue.wrapSelection("`", "`")
                    }
                    MarkdownToolbarButton(
                        icon = Icons.Default.Link,
                        contentDescription = "Link"
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val selected = textFieldValue.getSelectedText()
                        textFieldValue = if (selected.isNotEmpty()) {
                            textFieldValue.wrapSelection("[", "](url)")
                        } else {
                            textFieldValue.insertAtCursor("[text](url)")
                        }
                    }
                    MarkdownToolbarButton(
                        icon = Icons.Default.VisibilityOff,
                        contentDescription = "Spoiler"
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        textFieldValue = textFieldValue.wrapSelection("~!", "!~")
                    }

                    Spacer(Modifier.weight(1f))

                    FilledTonalIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isPreviewMode = !isPreviewMode
                        },
                        enabled = !isBlank,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isPreviewMode) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Preview,
                            contentDescription = if (isPreviewMode) "Edit" else "Preview",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (isPreviewMode) {
                    AsyncRichTextRenderer(
                        html = textFieldValue.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = {
                            if (it.text.length <= maxLength) textFieldValue = it
                        },
                        placeholder = { Text(placeholder) },
                        minLines = minLines,
                        maxLines = maxLines,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }

                Text(
                    text = "${textFieldValue.text.length} / $maxLength",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (textFieldValue.text.length > maxLength * 0.9) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )

                Spacer(Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(percent = 50)
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSubmit(textFieldValue.text)
                        },
                        enabled = !isBlank && !isSubmitting,
                        shape = RoundedCornerShape(percent = 50),
                        modifier = Modifier.height(48.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(18.dp)
                            )
                        }
                        Text(text = submitLabel, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private const val DEFAULT_MAX_LENGTH = 2000

@Composable
private fun MarkdownToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun TextFieldValue.wrapSelection(prefix: String, suffix: String): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val selectedText = text.substring(start, end)
    val newText = text.substring(0, start) + prefix + selectedText + suffix + text.substring(end)
    val newCursorPos = if (selectedText.isEmpty()) {
        start + prefix.length
    } else {
        start + prefix.length + selectedText.length + suffix.length
    }
    return copy(text = newText, selection = TextRange(newCursorPos))
}

private fun TextFieldValue.getSelectedText(): String =
    text.substring(selection.min, selection.max)

private fun TextFieldValue.insertAtCursor(insertion: String): TextFieldValue {
    val pos = selection.min
    val newText = text.substring(0, pos) + insertion + text.substring(pos)
    return copy(text = newText, selection = TextRange(pos + insertion.length))
}
