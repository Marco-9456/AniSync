package com.anisync.android.presentation.components.filtersheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R

/**
 * One sort sheet for every search: the direction toggle, then the columns two to a row.
 *
 * Direction is its own control rather than a second option per column, so every column sorts both
 * ways. Picking a column is left to the caller, which starts it in its natural direction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SortSheet(
    columns: List<T>,
    label: @Composable (T) -> String,
    selected: T,
    isAscending: Boolean,
    hasDirection: Boolean,
    onColumnPick: (T) -> Unit,
    onDirectionChange: (ascending: Boolean) -> Unit,
    onReset: () -> Unit,
    resetEnabled: Boolean,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
) {
    FilterSheetScaffold(
        title = stringResource(R.string.library_sort_by_label),
        onDismiss = onDismiss,
        sheetState = sheetState,
        onReset = onReset,
        resetEnabled = resetEnabled
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            DirectionToggle(
                isAscending = isAscending,
                enabled = hasDirection,
                onChange = onDirectionChange
            )
        }
        Spacer(Modifier.height(10.dp))
        OptionPillGrid(
            options = columns,
            label = label,
            isSelected = { it == selected },
            onClick = onColumnPick
        )
    }
}
