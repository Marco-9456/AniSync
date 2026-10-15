package com.anisync.android.presentation.components.richtext

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.AppBarRow
import androidx.compose.material3.AppBarScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.menu.Menu

/**
 * Row of formatting buttons (bold, italic, strike, code, link, spoiler). Buttons that don't fit
 * the width move into an overflow [Menu]. Pure content — caller owns insets, surface, layout.
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
    val actions = buildList {
        if (onAttachClick != null) {
            add(FormatAction(Icons.Outlined.AttachFile, stringResource(R.string.media_attach)) { onAttachClick() })
        }
        add(FormatAction(Icons.Default.FormatBold, stringResource(R.string.richtext_format_bold)) {
            textFieldState.wrapSelection("__", "__")
        })
        add(FormatAction(Icons.Default.FormatItalic, stringResource(R.string.richtext_format_italic)) {
            textFieldState.wrapSelection("_", "_")
        })
        add(FormatAction(Icons.Default.FormatStrikethrough, stringResource(R.string.richtext_format_strikethrough)) {
            textFieldState.wrapSelection("~~", "~~")
        })
        add(FormatAction(Icons.Default.Code, stringResource(R.string.richtext_format_code)) {
            textFieldState.wrapSelection("`", "`")
        })
        add(FormatAction(Icons.Default.Link, stringResource(R.string.richtext_format_link)) {
            textFieldState.toggleLinkSyntax()
        })
        add(FormatAction(Icons.Default.VisibilityOff, stringResource(R.string.richtext_format_spoiler)) {
            textFieldState.wrapSelection("~!", "!~")
        })
    }
    val moreLabel = stringResource(R.string.more_options)
    var menuExpanded by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier) {
        // Every button is the same width, so the visible count is plain arithmetic. When the row
        // overflows, one slot goes to the overflow button.
        val slot = buttonSize + FormatButtonGap
        val fitting = (maxWidth / slot).toInt()
        val visibleCount = if (fitting >= actions.size) actions.size else (fitting - 1).coerceAtLeast(0)

        Row(verticalAlignment = Alignment.CenterVertically) {
            actions.take(visibleCount).forEach { action ->
                FormatButton(action.icon, action.label, buttonSize, iconSize) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    action.onClick()
                }
            }
            if (visibleCount < actions.size) {
                Box {
                    FormatButton(Icons.Default.MoreVert, moreLabel, buttonSize, iconSize) {
                        menuExpanded = true
                    }
                    Menu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        actions.drop(visibleCount).forEach { action ->
                            item(
                                text = action.label,
                                leadingIcon = action.icon,
                                onClick = {
                                    menuExpanded = false
                                    action.onClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private class FormatAction(val icon: ImageVector, val label: String, val onClick: () -> Unit)

private val FormatButtonGap = 4.dp

/**
 * Expressive Material 3 [HorizontalFloatingToolbar] carrying the same markdown formatting actions as
 * [RichTextFormatBar], styled with the vibrant (primaryContainer) toolbar colors. This is the editor's
 * format toolbar everywhere: docked below the app bar on wide screens, and above the IME on phones.
 *
 * Actions that don't fit the available width collapse into an overflow menu via [AppBarRow], so the
 * pill never overflows a narrow phone (a plain [HorizontalFloatingToolbar] does not scroll its
 * content).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditorFormatToolbar(
    textFieldState: TextFieldState,
    modifier: Modifier = Modifier,
    onAttachClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    // The AppBarRow DSL builder lambda is not composable, so the labels (reused as the overflow-menu
    // item text) are resolved here, in composition, and captured by the builder.
    val attachLabel = stringResource(R.string.media_attach)
    val boldLabel = stringResource(R.string.richtext_format_bold)
    val italicLabel = stringResource(R.string.richtext_format_italic)
    val strikeLabel = stringResource(R.string.richtext_format_strikethrough)
    val codeLabel = stringResource(R.string.richtext_format_code)
    val linkLabel = stringResource(R.string.richtext_format_link)
    val spoilerLabel = stringResource(R.string.richtext_format_spoiler)

    HorizontalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        modifier = modifier
    ) {
        AppBarRow {
            if (onAttachClick != null) {
                formatItem(Icons.Outlined.AttachFile, attachLabel) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAttachClick()
                }
            }
            formatItem(Icons.Default.FormatBold, boldLabel) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                textFieldState.wrapSelection("__", "__")
            }
            formatItem(Icons.Default.FormatItalic, italicLabel) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                textFieldState.wrapSelection("_", "_")
            }
            formatItem(Icons.Default.FormatStrikethrough, strikeLabel) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                textFieldState.wrapSelection("~~", "~~")
            }
            formatItem(Icons.Default.Code, codeLabel) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                textFieldState.wrapSelection("`", "`")
            }
            formatItem(Icons.Default.Link, linkLabel) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                textFieldState.toggleLinkSyntax()
            }
            formatItem(Icons.Default.VisibilityOff, spoilerLabel) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                textFieldState.wrapSelection("~!", "!~")
            }
        }
    }
}

// One markdown action in [EditorFormatToolbar]: an icon button in the pill that collapses to a
// labeled row in the overflow menu when the row runs out of width.
private fun AppBarScope.formatItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    clickableItem(
        onClick = onClick,
        icon = { Icon(imageVector = icon, contentDescription = label) },
        label = label
    )
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
 * Compact upload-progress strip rendered inside the composer when an IME-source
 * upload is in flight. Sheet-source uploads use the full [MediaAttachSheet] UI
 * instead, so this is only ever shown for [MediaAttachState.Source.Ime].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImeUploadStrip(
    state: MediaAttachState.Uploading,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = state.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp)
        )
        if (state.total > 0) {
            val target = (state.uploaded.toFloat() / state.total).coerceIn(0f, 1f)
            val animated by animateFloatAsState(
                targetValue = target,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                label = "ImeUploadProgress"
            )
            LinearWavyProgressIndicator(
                progress = { animated },
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(target * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            LinearWavyProgressIndicator(modifier = Modifier.weight(1f))
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.media_attach_cancel),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FormatButton(
    icon: ImageVector,
    contentDescription: String,
    buttonSize: Dp,
    iconSize: Dp,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = FormatButtonGap / 2)
            .size(buttonSize),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize)
        )
    }
}