package com.anisync.android.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.library.LibrarySort

/**
 * Bottom sheet component for displaying library sorting options with direction toggle.
 * Shows arrow icons for selected option to indicate sort direction (ascending/descending).
 * Clicking the same option toggles direction; clicking a new option defaults to ascending.
 *
 * @param visible Whether the bottom sheet is visible
 * @param onDismiss Callback when the bottom sheet is dismissed
 * @param options List of available sort options
 * @param selectedOption The currently selected sort option
 * @param isAscending Current sort direction (true = ascending, false = descending)
 * @param onOptionSelected Callback when an option is selected with direction
 * @param sheetState Optional sheet state for controlling the bottom sheet
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
    if (visible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = null  // Remove drag handle as specified
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Title
                Text(
                    text = stringResource(R.string.sort_by),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                // Divider
                HorizontalDivider(
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Options
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
                    
                    SortOptionItem(
                        label = label,
                        isSelected = isSelected,
                        isAscending = isAscending,
                        onClick = {
                            val newDirection = if (isSelected) {
                                // Same option: toggle direction
                                !isAscending
                            } else {
                                // New option: default to ascending
                                true
                            }
                            onOptionSelected(option, newDirection)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortOptionItem(
    label: String,
    isSelected: Boolean,
    isAscending: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Arrow icon for selected item
            if (isSelected) {
                Icon(
                    imageVector = if (isAscending) {
                        Icons.Rounded.ArrowUpward
                    } else {
                        Icons.Rounded.ArrowDownward
                    },
                    contentDescription = if (isAscending) {
                        stringResource(R.string.ascending)
                    } else {
                        stringResource(R.string.descending)
                    },
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            } else {
                // Spacer to maintain alignment
                Spacer(modifier = Modifier.width(36.dp))
            }

            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}
