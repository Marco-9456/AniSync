package com.anisync.android.presentation.forum.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import com.anisync.android.presentation.components.menu.Menu
import com.anisync.android.presentation.forum.ForumFeed

/**
 * The five feeds, as a menu off the pinned rail button. This is the shape the media details screen
 * uses to move a title between lists: a short set of mutually exclusive choices, the current one
 * marked, anchored to the control that names it.
 */
@Composable
fun ForumFeedMenu(
    expanded: Boolean,
    selected: ForumFeed,
    onDismiss: () -> Unit,
    onSelect: (ForumFeed) -> Unit
) {
    Menu(expanded = expanded, onDismissRequest = onDismiss) {
        ForumFeed.entries.forEach { feed ->
            item(
                text = stringResource(feed.labelRes),
                leadingIcon = feed.icon,
                selected = feed == selected,
                onClick = {
                    onDismiss()
                    onSelect(feed)
                }
            )
        }
    }
}

/**
 * The Forum search bar's overflow, in the same place and shape as Discover's.
 *
 * Ordering the Overview is a per-screen preference, so it lives here rather than occupying a
 * permanent control on a browse surface.
 */
@Composable
fun ForumOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onReorderSections: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Menu(expanded = expanded, onDismissRequest = onDismiss) {
        item(
            text = stringResource(R.string.forum_reorder_title),
            leadingIcon = Icons.AutoMirrored.Filled.Sort,
            onClick = {
                onDismiss()
                onReorderSections()
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
