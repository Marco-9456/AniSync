package com.anisync.android.presentation.library.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.anisync.android.presentation.components.filtersheet.SortSheet
import com.anisync.android.presentation.library.LibrarySort

/** The order the sheet lists the columns in, the default first. */
private val SortColumns = listOf(
    LibrarySort.AIRING_SOON,
    LibrarySort.TITLE,
    LibrarySort.PROGRESS,
    LibrarySort.SCORE,
    LibrarySort.LAST_UPDATED,
    LibrarySort.LAST_ADDED,
    LibrarySort.START_DATE,
    LibrarySort.RELEASE_DATE,
    LibrarySort.PRIORITY
)

/**
 * The library's sort, opened from the bar and from the Sort chip inside library search. It is the
 * sort sheet Discover uses: picking a column starts it in its natural direction, and the toggle
 * says which way the list really runs, so Score reads Descending while it lists the highest first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySortSheet(
    visible: Boolean,
    sort: LibrarySort,
    isAscending: Boolean,
    onSortChange: (LibrarySort, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    SortSheet(
        columns = SortColumns,
        label = { it.label() },
        selected = sort,
        isAscending = sort.flipDirection(isAscending),
        hasDirection = true,
        onColumnPick = { onSortChange(it, true) },
        onDirectionChange = { ascending -> onSortChange(sort, sort.flipDirection(ascending)) },
        onReset = { onSortChange(LibrarySort.AIRING_SOON, true) },
        resetEnabled = sort != LibrarySort.AIRING_SOON || !isAscending,
        onDismiss = onDismiss
    )
}
