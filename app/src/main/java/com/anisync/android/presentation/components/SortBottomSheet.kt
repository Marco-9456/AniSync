package com.anisync.android.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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

/**
 * A reusable bottom sheet component for displaying sorting options.
 * Follows the Material Design 3 pattern with a title, divider, and selectable options.
 *
 * @param T The type of the sort option (e.g., an enum)
 * @param visible Whether the bottom sheet is visible
 * @param onDismiss Callback when the bottom sheet is dismissed
 * @param options List of available sort options
 * @param selectedOption The currently selected sort option
 * @param onOptionSelected Callback when an option is selected
 * @param optionLabel Lambda to convert an option to its display label
 * @param sheetState Optional sheet state for controlling the bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SortBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    optionLabel: @Composable (T) -> String,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    if (visible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState
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
                    SortOptionItem(
                        label = optionLabel(option),
                        isSelected = isSelected,
                        onClick = {
                            onOptionSelected(option)
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
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
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
            // Checkmark for selected item
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.selected),
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
