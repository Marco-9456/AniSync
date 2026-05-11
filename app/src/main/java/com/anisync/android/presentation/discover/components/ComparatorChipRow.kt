package com.anisync.android.presentation.discover.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anisync.android.domain.ComparatorMode

/**
 * Single-select chip row for the four comparator modes used by score /
 * episodes / chapters filters: Any · ≥ · ≤ · = .
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComparatorChipRow(
    mode: ComparatorMode,
    onModeChange: (ComparatorMode) -> Unit,
    valueLabel: String,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ComparatorMode.entries.forEach { entry ->
            FilterChip(
                selected = entry == mode,
                onClick = { onModeChange(entry) },
                label = { Text(entry.label(valueLabel)) }
            )
        }
    }
}

private fun ComparatorMode.label(value: String): String = when (this) {
    ComparatorMode.ANY -> "Any"
    ComparatorMode.AT_LEAST -> "$value or more"
    ComparatorMode.AT_MOST -> "$value or less"
    ComparatorMode.EXACTLY -> "Exactly $value"
}
