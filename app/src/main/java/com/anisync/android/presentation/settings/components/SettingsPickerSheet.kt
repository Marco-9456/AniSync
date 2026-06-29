package com.anisync.android.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.settings.RadioSettingsItem
import com.anisync.android.presentation.settings.SettingsGroup

/**
 * A single-choice bottom-sheet picker built from the shared settings vocabulary: a heading followed
 * by a [SettingsGroup] of [RadioSettingsItem]s. Use this for plain enumerations (theme mode, app
 * language, …) so every picker reads the same. Richer pickers that need previews or grids keep their
 * bespoke sheet.
 *
 * The caller owns dismissal: have [onSelect] apply the value and close the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SettingsPickerSheet(
    title: String,
    items: List<T>,
    selected: T?,
    itemLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    itemSubtitle: @Composable (T) -> String? = { null },
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsGroup(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .selectableGroup()
            ) {
                items.forEach { item ->
                    RadioSettingsItem(
                        title = itemLabel(item),
                        subtitle = itemSubtitle(item),
                        selected = item == selected,
                        onClick = { onSelect(item) },
                    )
                }
            }
        }
    }
}
