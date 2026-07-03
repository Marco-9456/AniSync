package com.anisync.android.presentation.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.util.rememberHapticFeedback

/**
 * The app's single connected-[ToggleButton] segmented selector.
 *
 * Sibling of [SegmentedTabItem] in the app's segmented-tab family: this renders the *whole*
 * connected group itself, while [SegmentedTabItem] is a single animated pill the caller lays out in
 * a scrollable row. They share the naming, not a parent/child composition — neither uses the other.
 *
 * One Material 3 Expressive button group, parameterised over an arbitrary option type, so every
 * screen-level switcher (Anime/Manga, forum feeds, profile tabs, …) shares one implementation
 * instead of re-deriving the same Row + shapes + haptics boilerplate. Each screen keeps a thin
 * typed wrapper that maps its enum to a [label]/[icon]; this owns the look and behaviour.
 *
 * Two layouts:
 * - [fillEqually] `= false` (default): a horizontally scrollable row, for an open-ended number of
 *   options (forum feeds, profile tabs). Adds a 16.dp horizontal content inset.
 * - [fillEqually] `= true`: equal-weight buttons spanning the full width, for a small fixed set
 *   (the binary Anime/Manga toggle).
 *
 * State is hoisted: events go up via [onSelect], the [selected] value comes down.
 *
 * @param options Ordered options to render, left to right.
 * @param selected Currently selected option (compared with `==`).
 * @param onSelect Invoked with the tapped option.
 * @param label Display text for an option (composable so callers may use `stringResource`).
 * @param modifier Modifier for the row.
 * @param icon Optional leading icon for an option; `null` renders text only.
 * @param fillEqually See layout note above.
 * @param labelStyle Text style for labels (defaults to `labelLarge`).
 * @param scrollState Horizontal scroll state of the scrollable layout. Hoist it when the same
 *   logical strip is rendered twice (e.g. an in-list copy plus a pinned copy) so both stay at the
 *   same offset; ignored when [fillEqually] is true.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> SegmentedTabGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    icon: (T) -> ImageVector? = { null },
    fillEqually: Boolean = false,
    labelStyle: TextStyle = MaterialTheme.typography.labelLarge,
    scrollState: ScrollState = rememberScrollState()
) {
    val haptic = rememberHapticFeedback()

    val rowModifier = if (fillEqually) {
        modifier.fillMaxWidth()
    } else {
        modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp)
    }

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, option ->
            val shapes = when (index) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }

            ToggleButton(
                checked = selected == option,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelect(option)
                },
                modifier = if (fillEqually) Modifier.weight(1f) else Modifier,
                shapes = shapes
            ) {
                icon(option)?.let { vector ->
                    Icon(imageVector = vector, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = label(option),
                    style = labelStyle,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
