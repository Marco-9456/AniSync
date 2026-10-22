package com.anisync.android.presentation.forum.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.ThreadSortField
import com.anisync.android.domain.ThreadSortOption
import com.anisync.android.presentation.components.filtersheet.DirectionToggle
import com.anisync.android.presentation.components.filtersheet.OptionPillGrid

/** The column label. Direction is the toggle's job, so these name the column and nothing else. */
@StringRes
fun ThreadSortField.labelRes(): Int = when (this) {
    ThreadSortField.LAST_REPLY -> R.string.forum_sort_field_last_reply
    ThreadSortField.CREATED -> R.string.forum_sort_field_created
    ThreadSortField.REPLIES -> R.string.forum_sort_field_replies
    ThreadSortField.VIEWS -> R.string.forum_sort_field_views
    ThreadSortField.TITLE -> R.string.forum_sort_field_title
    ThreadSortField.RELEVANCE -> R.string.forum_sort_field_relevance
}

/**
 * "SORT BY" as Library draws it: an uppercase label with the direction toggle on the same line,
 * then a two-column grid of pills naming the column.
 *
 * Splitting direction out of the option list is what lets every column carry both directions.
 * The old list had seven entries that between them covered one direction per column and no way to
 * ask for the other.
 */
@Composable
fun ForumSortSection(
    sort: ThreadSortOption,
    onSortChange: (ThreadSortOption) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: androidx.compose.ui.unit.Dp = 24.dp,
    /** The search overlay's scaffold already titles the sheet, so it hides this one. */
    showLabel: Boolean = true
) {
    val selectedField = sort.sortField
    val ascending = sort.isAscending

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLabel) {
            Text(
                text = stringResource(R.string.forum_sort_by).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        DirectionToggle(
            isAscending = ascending,
            enabled = selectedField.hasDirection,
            onChange = { asc -> onSortChange(selectedField.toOption(asc)) }
        )
    }

    Spacer(Modifier.height(10.dp))

    OptionPillGrid(
        options = ThreadSortField.entries,
        label = { stringResource(it.labelRes()) },
        isSelected = { it == selectedField },
        onClick = { field -> onSortChange(field.toOption(ascending)) },
        horizontalPadding = horizontalPadding
    )
}
