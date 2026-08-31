package com.anisync.android.presentation.discover.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import com.anisync.android.presentation.components.menu.Menu
import com.anisync.android.type.MediaType

/**
 * The Discover search bar's overflow.
 *
 * It takes the place of a filter button that duplicated the chip already inside the search
 * overlay, and it is where rail reordering lives: a per-screen preference has no business
 * occupying a permanent control on a browse surface.
 *
 * The airing calendar entry is anime-only, for the same reason the timeline is.
 */
@Composable
fun DiscoverOverflowMenu(
    expanded: Boolean,
    mediaType: MediaType,
    onDismiss: () -> Unit,
    onReorderSections: () -> Unit,
    onOpenCalendar: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Menu(expanded = expanded, onDismissRequest = onDismiss) {
        item(
            text = stringResource(R.string.discover_reorder_title),
            leadingIcon = Icons.AutoMirrored.Filled.Sort,
            onClick = {
                onDismiss()
                onReorderSections()
            }
        )
        if (mediaType == MediaType.ANIME) {
            item(
                text = stringResource(R.string.calendar_open),
                leadingIcon = Icons.Default.CalendarMonth,
                onClick = {
                    onDismiss()
                    onOpenCalendar()
                }
            )
        }
        gap()
        item(
            text = stringResource(R.string.discover_refresh),
            leadingIcon = Icons.Default.Refresh,
            onClick = {
                onDismiss()
                onRefresh()
            }
        )
        item(
            text = stringResource(R.string.section_settings),
            leadingIcon = Icons.Default.Settings,
            onClick = {
                onDismiss()
                onOpenSettings()
            }
        )
    }
}
