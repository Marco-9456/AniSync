package com.anisync.android.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.filtersheet.FilterSheetScaffold
import com.anisync.android.presentation.library.LibrarySort
import com.anisync.android.presentation.util.bouncyClickable

/**
 * The library's sort, opened from the bar and from the Sort chip inside library search.
 *
 * Sort is a grid of pills rather than one option per row, and direction is an explicit Asc/Desc
 * control. An older sheet flipped direction when you tapped the selected option again, which
 * nothing on screen said.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySortSheet(
    visible: Boolean,
    sort: LibrarySort,
    isAscending: Boolean,
    onSortChange: (LibrarySort, Boolean) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
) {
    if (!visible) return

    FilterSheetScaffold(
        title = stringResource(R.string.library_sort_by_label),
        onDismiss = onDismiss,
        sheetState = sheetState,
        onReset = { onSortChange(LibrarySort.AIRING_SOON, true) },
        resetEnabled = sort != LibrarySort.AIRING_SOON || !isAscending
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                DirectionToggle(
                    isAscending = isAscending,
                    onChange = { onSortChange(sort, it) }
                )
            }

            val options = remember { LibrarySort.entries.toList() }
            options.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { option ->
                        SortPill(
                            label = option.label(),
                            selected = option == sort,
                            onClick = { onSortChange(option, isAscending) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SortPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .height(44.dp)
            .bouncyClickable(
                onClick = onClick,
                role = Role.RadioButton,
                onClickLabel = label,
                clipShape = RoundedCornerShape(22.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The label takes the slack so the tick lands on the pill's trailing edge. Sharing a
            // weight with a spacer split the slack in two and left the tick mid-pill, which only
            // showed up once the pill got wide enough to notice.
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DirectionToggle(isAscending: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        DirectionSegment(
            label = stringResource(R.string.ascending),
            selected = isAscending,
            shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp, topEnd = 6.dp, bottomEnd = 6.dp),
            onClick = { onChange(true) }
        )
        DirectionSegment(
            label = stringResource(R.string.descending),
            selected = !isAscending,
            shape = RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp, topStart = 6.dp, bottomStart = 6.dp),
            onClick = { onChange(false) }
        )
    }
}

@Composable
private fun DirectionSegment(
    label: String,
    selected: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit
) {
    val resolved = if (selected) CircleShape else shape
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        shape = resolved,
        modifier = Modifier
            .height(28.dp)
            .bouncyClickable(onClick = onClick, role = Role.RadioButton, clipShape = resolved)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
