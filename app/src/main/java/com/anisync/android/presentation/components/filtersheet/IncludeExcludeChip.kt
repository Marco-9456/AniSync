package com.anisync.android.presentation.components.filtersheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.anisync.android.R

enum class TriState { OFF, INCLUDED, EXCLUDED }

/** Where a tap moves a chip: off, included, excluded, then off again. */
fun TriState.next(): TriState = when (this) {
    TriState.OFF -> TriState.INCLUDED
    TriState.INCLUDED -> TriState.EXCLUDED
    TriState.EXCLUDED -> TriState.OFF
}

/** Where a long press moves a chip: straight to excluded, or back off when it already is. */
fun TriState.longPressed(): TriState = if (this == TriState.EXCLUDED) TriState.OFF else TriState.EXCLUDED

/**
 * Tri-state filter chip for genres and tags.
 *
 * Visual matches the Material 3 filter-chip spec:
 *   - `OFF`: outlined chip, transparent surface (unselected).
 *   - `INCLUDED`: filled tonal with leading check (selected).
 *   - `EXCLUDED`: filled error container with leading minus (selected, negated).
 *
 * Tap cycles `OFF → INCLUDED → EXCLUDED → OFF`. Long-press jumps to `EXCLUDED`
 * (or clears it when already excluded) — power-user shortcut for the most
 * common targeted action.
 */
@OptIn(ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun IncludeExcludeChip(
    label: String,
    state: TriState,
    onStateChange: (TriState) -> Unit,
    modifier: Modifier = Modifier
) {
    val container: Color
    val content: Color
    val border: BorderStroke?
    when (state) {
        TriState.OFF -> {
            container = Color.Transparent
            content = MaterialTheme.colorScheme.onSurfaceVariant
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        }
        TriState.INCLUDED -> {
            container = MaterialTheme.colorScheme.secondaryContainer
            content = MaterialTheme.colorScheme.onSecondaryContainer
            border = null
        }
        TriState.EXCLUDED -> {
            container = MaterialTheme.colorScheme.errorContainer
            content = MaterialTheme.colorScheme.onErrorContainer
            border = null
        }
    }
    val stateLabel = stringResource(
        when (state) {
            TriState.OFF -> R.string.a11y_filter_off
            TriState.INCLUDED -> R.string.a11y_filter_included
            TriState.EXCLUDED -> R.string.a11y_filter_excluded
        }
    )
    Surface(
        modifier = modifier
            .semantics { stateDescription = stateLabel }
            .combinedClickable(
                onClick = { onStateChange(state.next()) },
                onLongClick = { onStateChange(state.longPressed()) }
            ),
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
        border = border
    ) {
        Row(
            modifier = Modifier.padding(
                start = if (state == TriState.OFF) 12.dp else 8.dp,
                end = 12.dp,
                top = 6.dp,
                bottom = 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (state) {
                TriState.INCLUDED -> Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                TriState.EXCLUDED -> Icon(
                    Icons.Default.Remove,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                TriState.OFF -> Unit
            }
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * The line over every set of [IncludeExcludeChip]s, saying how the chips cycle. Where something is
 * already picked, [counts] leads it.
 */
@Composable
fun IncludeExcludeHint(counts: String?, modifier: Modifier = Modifier) {
    val hint = stringResource(R.string.library_filter_exclude_hint)
    Text(
        text = if (counts == null) hint else stringResource(R.string.filter_hint_with_counts, counts, hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
