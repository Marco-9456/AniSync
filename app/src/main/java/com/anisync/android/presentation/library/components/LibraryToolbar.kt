package com.anisync.android.presentation.library.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import com.anisync.android.presentation.components.menu.Menu
import com.anisync.android.presentation.library.LibrarySort

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
    onManageLists: () -> Unit,
    onCardOptions: () -> Unit,
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
            text = stringResource(R.string.manage_tabs),
            leadingIcon = Icons.AutoMirrored.Filled.List,
            onClick = {
                onDismiss()
                onManageLists()
            }
        )
        item(
            text = stringResource(R.string.library_card_options),
            leadingIcon = Icons.Default.Tune,
            onClick = {
                onDismiss()
                onCardOptions()
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
