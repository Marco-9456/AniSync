package com.anisync.android.presentation.library.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.filtersheet.FilterSheetScaffold
import com.anisync.android.presentation.library.LibraryFilters
import com.anisync.android.presentation.library.LibrarySort
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaFormat

/**
 * Sort and filters in one sheet.
 *
 * Sort is a grid of pills rather than the old one-per-row list: eight options fitted into four rows
 * leaves room for the filters underneath, which is the point — the library had no filtering at all,
 * while Discover has had a filter sheet for as long as it has existed.
 *
 * Direction is an explicit Asc/Desc control. The previous sheet flipped direction when you tapped
 * the already-selected option, which nothing on screen said.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryFilterSheet(
    visible: Boolean,
    sort: LibrarySort,
    isAscending: Boolean,
    filters: LibraryFilters,
    availableGenres: List<String>,
    availableFormats: List<MediaFormat>,
    availableAiringStatuses: List<String>,
    resultCount: Int,
    onSortChange: (LibrarySort, Boolean) -> Unit,
    onFiltersChange: (LibraryFilters) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (!visible) return

    FilterSheetScaffold(
        title = stringResource(R.string.library_sort_and_filter),
        onDismiss = onDismiss,
        sheetState = sheetState,
        onReset = { onFiltersChange(LibraryFilters.None) },
        resetEnabled = !filters.isEmpty
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel(stringResource(R.string.library_sort_by_label))
                Spacer(Modifier.weight(1f))
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

            if (availableAiringStatuses.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                SectionLabel(stringResource(R.string.library_filter_airing_status))
                FilterChipRow(
                    values = availableAiringStatuses,
                    label = { it.airingStatusLabel() },
                    selected = filters.airingStatuses,
                    onToggle = { value ->
                        onFiltersChange(filters.copy(airingStatuses = filters.airingStatuses.toggle(value)))
                    }
                )
            }

            if (availableFormats.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                SectionLabel(stringResource(R.string.library_filter_format))
                FilterChipRow(
                    values = availableFormats,
                    label = { it.toLabel() },
                    selected = filters.formats,
                    onToggle = { value ->
                        onFiltersChange(filters.copy(formats = filters.formats.toggle(value)))
                    }
                )
            }

            if (availableGenres.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                SectionLabel(stringResource(R.string.library_filter_genre))
                var expanded by remember { mutableStateOf(false) }
                val shown = if (expanded) availableGenres else availableGenres.take(8)
                FilterChipRow(
                    values = shown,
                    label = { it },
                    selected = filters.genres,
                    onToggle = { value ->
                        onFiltersChange(filters.copy(genres = filters.genres.toggle(value)))
                    },
                    trailing = if (!expanded && availableGenres.size > 8) {
                        {
                            FilterPill(
                                label = stringResource(
                                    R.string.library_filter_more,
                                    availableGenres.size - 8
                                ),
                                selected = false,
                                onClick = { expanded = true }
                            )
                        }
                    } else {
                        null
                    }
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    text = pluralStringResource(R.plurals.library_show_results, resultCount, resultCount),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
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
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (selected) {
                Spacer(Modifier.weight(1f))
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

@Composable
private fun <T> FilterChipRow(
    values: List<T>,
    label: @Composable (T) -> String,
    selected: Set<T>,
    onToggle: (T) -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        values.forEach { value ->
            FilterPill(
                label = label(value),
                selected = value in selected,
                onClick = { onToggle(value) }
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = CircleShape,
        modifier = Modifier
            .height(32.dp)
            .bouncyClickable(
                onClick = onClick,
                role = Role.Checkbox,
                onClickLabel = label,
                clipShape = CircleShape
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1
            )
        }
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

@Composable
private fun String.airingStatusLabel(): String = when (this) {
    "RELEASING" -> stringResource(R.string.media_status_releasing)
    "FINISHED" -> stringResource(R.string.media_status_finished)
    "NOT_YET_RELEASED" -> stringResource(R.string.media_status_not_yet_released)
    "HIATUS" -> stringResource(R.string.media_status_hiatus)
    "CANCELLED" -> stringResource(R.string.media_status_cancelled)
    else -> this
}
