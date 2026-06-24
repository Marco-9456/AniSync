package com.anisync.android.presentation.forum.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.anisync.android.presentation.components.SegmentedTabGroup
import com.anisync.android.presentation.forum.ForumFeed

/**
 * Forum feed switcher (Overview / Recent / New / Subscribed / Saved). Thin wrapper over the
 * shared [SegmentedTabGroup] that maps each [ForumFeed] to its icon and label.
 */
@Composable
fun ForumFeedSelector(
    selected: ForumFeed,
    onSelect: (ForumFeed) -> Unit,
    modifier: Modifier = Modifier
) {
    SegmentedTabGroup(
        options = ForumFeed.entries,
        selected = selected,
        onSelect = onSelect,
        label = { it.label },
        modifier = modifier,
        icon = ::feedIcon
    )
}

private fun feedIcon(feed: ForumFeed): ImageVector = when (feed) {
    ForumFeed.OVERVIEW -> Icons.AutoMirrored.Filled.ViewList
    ForumFeed.RECENT -> Icons.Default.Schedule
    ForumFeed.NEW -> Icons.Default.NewReleases
    ForumFeed.SUBSCRIBED -> Icons.Default.Notifications
    ForumFeed.SAVED -> Icons.Default.Bookmark
}
