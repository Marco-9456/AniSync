package com.anisync.android.presentation.library.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.library.LibraryFilters
import com.anisync.android.presentation.library.LibrarySort
import com.anisync.android.presentation.components.menu.Menu
import com.anisync.android.presentation.util.bouncyClickable

/**
 * Sort and filters as chips that state their own value.
 *
 * The screen used to hide both behind unlabelled icons, with a tinted ring around the sort icon
 * standing in for "this list is not in its default order" — a workaround for the state being
 * invisible. A chip that reads "Airing soon" needs no such hint. This row scrolls away with the
 * content, because it is a setting you change occasionally, not a control you hold on screen.
 */
@Composable
fun LibraryToolbar(
    sort: LibrarySort,
    isAscending: Boolean,
    filters: LibraryFilters,
    onSortClick: () -> Unit,
    onFiltersClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarChip(
            icon = if (isAscending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
            label = sort.label(),
            active = false,
            onClick = onSortClick,
            contentDescription = stringResource(R.string.sort_by)
        )
        ToolbarChip(
            icon = Icons.Default.Tune,
            label = stringResource(R.string.library_filters),
            active = !filters.isEmpty,
            badge = filters.activeCount.takeIf { it > 0 }?.toString(),
            onClick = onFiltersClick,
            contentDescription = stringResource(R.string.library_filters)
        )
    }
}

@Composable
private fun ToolbarChip(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    badge: String? = null
) {
    val container = if (active) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val content = if (active) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = container,
        shape = CircleShape,
        modifier = Modifier
            .height(32.dp)
            .bouncyClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = contentDescription,
                clipShape = CircleShape
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = content,
                maxLines = 1
            )
            if (badge != null) {
                Surface(
                    shape = CircleShape,
                    color = if (active) {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                    } else {
                        Color.Transparent
                    }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = content
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LibrarySort.label(): String = when (this) {
    LibrarySort.TITLE -> stringResource(R.string.sort_title_az)
    LibrarySort.PROGRESS -> stringResource(R.string.sort_progress)
    LibrarySort.AIRING_SOON -> stringResource(R.string.sort_airing_soon)
    LibrarySort.SCORE -> stringResource(R.string.sort_score)
    LibrarySort.LAST_UPDATED -> stringResource(R.string.sort_last_updated)
    LibrarySort.LAST_ADDED -> stringResource(R.string.sort_last_added)
    LibrarySort.START_DATE -> stringResource(R.string.sort_start_date)
    LibrarySort.RELEASE_DATE -> stringResource(R.string.sort_release_date)
}

/**
 * The library overflow.
 *
 * Calendar and Notes moved here from the search field, where they sat as two unlabelled icons: both
 * navigate away from the library, so neither is a library control. "Show private entries" gets its
 * first entry point at all — the action existed in the ViewModel with nothing calling it — and
 * Refresh gives pull-to-refresh a path that does not require a gesture.
 */
@Composable
fun LibraryOverflowMenu(
    expanded: Boolean,
    showPrivateEntries: Boolean,
    onDismiss: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenNotes: () -> Unit,
    onTogglePrivate: (Boolean) -> Unit,
    onViewOptions: () -> Unit,
    onRefresh: () -> Unit
) {
    Menu(expanded = expanded, onDismissRequest = onDismiss) {
        item(
            text = stringResource(R.string.calendar_open),
            leadingIcon = Icons.Default.CalendarMonth,
            onClick = {
                onDismiss()
                onOpenCalendar()
            }
        )
        item(
            text = stringResource(R.string.a11y_open_notes_journal),
            leadingIcon = Icons.AutoMirrored.Filled.EventNote,
            onClick = {
                onDismiss()
                onOpenNotes()
            }
        )
        gap()
        item(
            text = stringResource(R.string.library_view_title),
            leadingIcon = Icons.Default.Tune,
            onClick = {
                onDismiss()
                onViewOptions()
            }
        )
        item(
            text = stringResource(R.string.library_show_private),
            leadingIcon = if (showPrivateEntries) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
            trailingLabel = stringResource(
                if (showPrivateEntries) R.string.library_on else R.string.library_off
            ),
            selected = showPrivateEntries,
            onClick = {
                onDismiss()
                onTogglePrivate(!showPrivateEntries)
            }
        )
        item(
            text = stringResource(R.string.library_refresh),
            leadingIcon = Icons.Default.Refresh,
            onClick = {
                onDismiss()
                onRefresh()
            }
        )
    }
}
