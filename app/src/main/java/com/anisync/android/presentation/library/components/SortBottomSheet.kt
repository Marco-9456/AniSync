package com.anisync.android.presentation.library.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.filtersheet.FilterOptionRow
import com.anisync.android.presentation.components.filtersheet.FilterSheetScaffold
import com.anisync.android.presentation.library.LibrarySort

/**
 * Library sort sheet. Tapping the same option toggles direction; tapping a
 * different option resets to ascending. Built on the shared filter-sheet
 * primitives so library + discover share visual identity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    options: List<LibrarySort>,
    selectedOption: LibrarySort,
    isAscending: Boolean,
    onOptionSelected: (LibrarySort, Boolean) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    if (!visible) return
    FilterSheetScaffold(
        title = stringResource(R.string.sort_by),
        onDismiss = onDismiss,
        sheetState = sheetState
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            val label = when (option) {
                LibrarySort.TITLE -> stringResource(R.string.sort_title_az)
                LibrarySort.PROGRESS -> stringResource(R.string.sort_progress)
                LibrarySort.AIRING_SOON -> stringResource(R.string.sort_airing_soon)
                LibrarySort.SCORE -> stringResource(R.string.sort_score)
                LibrarySort.LAST_UPDATED -> stringResource(R.string.sort_last_updated)
                LibrarySort.LAST_ADDED -> stringResource(R.string.sort_last_added)
                LibrarySort.START_DATE -> stringResource(R.string.sort_start_date)
                LibrarySort.RELEASE_DATE -> stringResource(R.string.sort_release_date)
            }
            FilterOptionRow(
                label = label,
                selected = isSelected,
                leading = if (isSelected) {
                    {
                        Icon(
                            imageVector = if (isAscending) Icons.Rounded.ArrowUpward
                            else Icons.Rounded.ArrowDownward,
                            contentDescription = if (isAscending) stringResource(R.string.ascending)
                            else stringResource(R.string.descending),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else null,
                onClick = {
                    val newDirection = if (isSelected) !isAscending else true
                    onOptionSelected(option, newDirection)
                    onDismiss()
                }
            )
        }
    }
}
