package com.anisync.android.presentation.components.filtersheet

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R

/** One chip in a search screen's filter bar. Tapping it opens that filter's sheet. */
@Composable
fun FilterBarChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = active,
        onClick = onClick,
        label = { Text(label) },
        trailingIcon = {
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (active) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

/**
 * Leads the filter bar once anything is filtered and clears every filter in one tap. The sort is
 * left alone: it says how to read the results rather than which to show. It is the bar chip's
 * shape with a leading close in place of the arrow, so it reads as an action on the bar.
 */
@Composable
fun ClearFiltersChip(onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(stringResource(R.string.clear)) },
        leadingIcon = {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

/** "Genres", or "Genres · 3" once something is picked. */
@Composable
fun countChipLabel(name: String, count: Int): String =
    if (count == 0) name else stringResource(R.string.library_filter_chip_count, name, count)

/** "Format", "Format · TV" or "Format · TV +1": a closed list names what is picked. */
@Composable
fun pickChipLabel(name: String, picked: List<String>): String = when (picked.size) {
    0 -> name
    1 -> stringResource(R.string.filter_chip_value, name, picked.first())
    else -> stringResource(R.string.filter_chip_value_more, name, picked.first(), picked.size - 1)
}

/** "Airing status", or "Publishing status" where the titles are manga. */
@Composable
fun airingStatusTitle(manga: Boolean): String = stringResource(
    if (manga) R.string.filter_publishing_status else R.string.library_filter_airing_status
)
