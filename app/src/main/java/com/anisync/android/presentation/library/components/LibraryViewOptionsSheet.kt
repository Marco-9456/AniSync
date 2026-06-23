package com.anisync.android.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.data.AppSettings
import kotlin.math.roundToInt

/**
 * Bottom sheet for the Library's view button. Replaces the old plain grid/list toggle with:
 *  - a List / Grid choice (Material 3 segmented buttons),
 *  - an "Automatic" switch that sizes grid columns to the window width, and
 *  - a columns slider (2..8) used when Automatic is off.
 *
 * The column controls only apply to the grid layout, so they are disabled in list mode. The column
 * preference is app-wide (see AppSettings / posterGridColumns); the list/grid choice is the Library's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryViewOptionsSheet(
    visible: Boolean,
    isGridView: Boolean,
    autoColumns: Boolean,
    columnCount: Int,
    onSetGridView: (Boolean) -> Unit,
    onSetAutoColumns: (Boolean) -> Unit,
    onSetColumnCount: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.library_view_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(Modifier.height(16.dp))

            // List / Grid choice.
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !isGridView,
                    onClick = { onSetGridView(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.ViewAgenda,
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    }
                ) {
                    Text(stringResource(R.string.library_view_list))
                }
                SegmentedButton(
                    selected = isGridView,
                    onClick = { onSetGridView(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.GridView,
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    }
                ) {
                    Text(stringResource(R.string.library_view_grid))
                }
            }

            Spacer(Modifier.height(24.dp))

            // Automatic columns — only meaningful for the grid layout.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.library_view_automatic),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.library_view_automatic_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoColumns,
                    onCheckedChange = onSetAutoColumns,
                    enabled = isGridView
                )
            }

            Spacer(Modifier.height(8.dp))

            // Manual column count (2..8), used when Automatic is off.
            val sliderEnabled = isGridView && !autoColumns
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.library_view_columns),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (sliderEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = columnCount.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = columnCount.toFloat(),
                onValueChange = { onSetColumnCount(it.roundToInt()) },
                valueRange = AppSettings.MIN_GRID_COLUMNS.toFloat()..AppSettings.MAX_GRID_COLUMNS.toFloat(),
                steps = AppSettings.MAX_GRID_COLUMNS - AppSettings.MIN_GRID_COLUMNS - 1,
                enabled = sliderEnabled
            )
        }
    }
}
