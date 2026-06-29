package com.anisync.android.presentation.details.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.components.AppModalBottomSheet

enum class MediaSort {
    POPULARITY,
    AVERAGE_SCORE,
    FAVORITES,
    NEWEST,
    OLDEST,
    TITLE;

    val label: String
        get() = when (this) {
            POPULARITY -> "Popularity"
            AVERAGE_SCORE -> "Average Score"
            FAVORITES -> "Favorites"
            NEWEST -> "Newest"
            OLDEST -> "Oldest"
            TITLE -> "Title"
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSortBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    selectedSort: MediaSort,
    isAscending: Boolean,
    onSortSelected: (MediaSort, Boolean) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    if (visible) {
        AppModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.sort_by),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                MediaSort.entries.forEach { sort ->
                    val isSelected = sort == selectedSort
                    MediaSortOptionItem(
                        label = sort.label,
                        isSelected = isSelected,
                        isAscending = isAscending,
                        onClick = {
                            val newDirection = if (isSelected) !isAscending else true
                            onSortSelected(sort, newDirection)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaSortOptionItem(
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
            if (isSelected) {
                Icon(
                    imageVector = if (isAscending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                    contentDescription = if (isAscending) "Ascending" else "Descending",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            } else {
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
