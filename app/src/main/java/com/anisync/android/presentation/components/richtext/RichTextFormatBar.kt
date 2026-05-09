package com.anisync.android.presentation.components.richtext

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.R

/**
 * Scrollable row of formatting buttons (bold, italic, strike, code, link, spoiler).
 * Pure content — caller owns insets, surface, layout.
 */
@Composable
fun RichTextFormatBar(
    textFieldState: TextFieldState,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    onAttachClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onAttachClick != null) {
            FormatButton(Icons.Outlined.AttachFile, R.string.media_attach, buttonSize, iconSize) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onAttachClick()
            }
        }
        FormatButton(Icons.Default.FormatBold, R.string.richtext_format_bold, buttonSize, iconSize) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            textFieldState.wrapSelection("__", "__")
        }
        FormatButton(Icons.Default.FormatItalic, R.string.richtext_format_italic, buttonSize, iconSize) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            textFieldState.wrapSelection("_", "_")
        }
        FormatButton(Icons.Default.FormatStrikethrough, R.string.richtext_format_strikethrough, buttonSize, iconSize) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            textFieldState.wrapSelection("~~", "~~")
        }
        FormatButton(Icons.Default.Code, R.string.richtext_format_code, buttonSize, iconSize) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            textFieldState.wrapSelection("`", "`")
        }
        FormatButton(Icons.Default.Link, R.string.richtext_format_link, buttonSize, iconSize) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            textFieldState.toggleLinkSyntax()
        }
        FormatButton(Icons.Default.VisibilityOff, R.string.richtext_format_spoiler, buttonSize, iconSize) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            textFieldState.wrapSelection("~!", "!~")
        }
    }
}

@Composable
fun RichTextCharCounter(
    length: Int,
    maxLength: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelMedium
) {
    Text(
        text = stringResource(R.string.richtext_char_count, length, maxLength),
        style = style,
        color = if (length > maxLength * 0.9) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/**
 * Full-screen docked toolbar — Surface wrapper, scrollable buttons, char counter,
 * and `windowInsetsPadding` so it floats above the IME and gesture bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichTextDockedFormatBar(
    textFieldState: TextFieldState,
    maxLength: Int,
    modifier: Modifier = Modifier,
    onAttachClick: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(BottomAppBarDefaults.windowInsets.union(WindowInsets.ime))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RichTextFormatBar(
                textFieldState = textFieldState,
                modifier = Modifier.weight(1f),
                onAttachClick = onAttachClick
            )
            RichTextCharCounter(
                length = textFieldState.text.length,
                maxLength = maxLength,
                modifier = Modifier.padding(start = 12.dp, end = 8.dp)
            )
        }
    }
}

@Composable
private fun FormatButton(
    icon: ImageVector,
    contentDescriptionRes: Int,
    buttonSize: Dp,
    iconSize: Dp,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(buttonSize),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(contentDescriptionRes),
            modifier = Modifier.size(iconSize)
        )
    }
}