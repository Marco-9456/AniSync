package com.anisync.android.presentation.forum.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.ThreadSortField
import com.anisync.android.domain.ThreadSortOption
import com.anisync.android.presentation.util.bouncyClickable

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

    ThreadSortField.entries.toList().chunked(2).forEach { pair ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            pair.forEach { field ->
                SortPill(
                    label = stringResource(field.labelRes()),
                    selected = field == selectedField,
                    onClick = { onSortChange(field.toOption(ascending)) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
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
            MaterialTheme.colorScheme.surfaceContainer
        },
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .height(48.dp)
            .bouncyClickable(
                onClick = onClick,
                role = Role.RadioButton,
                clipShape = RoundedCornerShape(22.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
private fun DirectionToggle(
    isAscending: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        DirectionSegment(
            label = stringResource(R.string.ascending),
            selected = isAscending && enabled,
            enabled = enabled,
            shape = RoundedCornerShape(
                topStart = 14.dp,
                bottomStart = 14.dp,
                topEnd = 6.dp,
                bottomEnd = 6.dp
            ),
            onClick = { onChange(true) }
        )
        DirectionSegment(
            label = stringResource(R.string.descending),
            selected = !isAscending || !enabled,
            enabled = enabled,
            shape = RoundedCornerShape(
                topEnd = 14.dp,
                bottomEnd = 14.dp,
                topStart = 6.dp,
                bottomStart = 6.dp
            ),
            onClick = { onChange(false) }
        )
    }
}

@Composable
private fun DirectionSegment(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    shape: Shape,
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
            .bouncyClickable(
                onClick = onClick,
                enabled = enabled,
                role = Role.RadioButton,
                clipShape = resolved
            )
            .then(if (enabled) Modifier else Modifier.alpha(0.38f))
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
