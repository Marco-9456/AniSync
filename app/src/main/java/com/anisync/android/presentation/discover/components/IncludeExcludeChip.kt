package com.anisync.android.presentation.discover.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

enum class TriState { OFF, INCLUDED, EXCLUDED }

/**
 * Tri-state chip used for genres and tags in advanced search.
 * Tap cycles `OFF → INCLUDED → EXCLUDED → OFF`. Long-press jumps to `EXCLUDED`.
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
    when (state) {
        TriState.OFF -> {
            container = MaterialTheme.colorScheme.surfaceContainerHigh
            content = MaterialTheme.colorScheme.onSurfaceVariant
        }
        TriState.INCLUDED -> {
            container = MaterialTheme.colorScheme.primary
            content = MaterialTheme.colorScheme.onPrimary
        }
        TriState.EXCLUDED -> {
            container = MaterialTheme.colorScheme.errorContainer
            content = MaterialTheme.colorScheme.onErrorContainer
        }
    }
    val stateLabel = when (state) {
        TriState.OFF -> "off"
        TriState.INCLUDED -> "included"
        TriState.EXCLUDED -> "excluded"
    }
    Surface(
        modifier = modifier
            .semantics { stateDescription = stateLabel }
            .combinedClickable(
                onClick = {
                    onStateChange(
                        when (state) {
                            TriState.OFF -> TriState.INCLUDED
                            TriState.INCLUDED -> TriState.EXCLUDED
                            TriState.EXCLUDED -> TriState.OFF
                        }
                    )
                },
                onLongClick = {
                    onStateChange(
                        if (state == TriState.EXCLUDED) TriState.OFF else TriState.EXCLUDED
                    )
                }
            ),
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (state) {
                TriState.INCLUDED -> Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                TriState.EXCLUDED -> Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                TriState.OFF -> Unit
            }
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
