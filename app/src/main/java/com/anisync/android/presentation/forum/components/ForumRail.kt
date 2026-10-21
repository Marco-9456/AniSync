package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.components.ConnectedToggleDefaults
import com.anisync.android.presentation.components.PinnedRail
import com.anisync.android.presentation.components.RailChip
import com.anisync.android.presentation.forum.ForumFeed
import com.anisync.android.presentation.forum.forumCategoryTabs
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.rememberHapticFeedback

/**
 * One pinned row carrying both "which feed" and "which category".
 *
 * The shipped screen spent three strips on this — a search bar, a five-way feed group and a tab row
 * of seventeen categories — and only the search bar ever scrolled away. They answer the same
 * question at different granularities, so they share a row here as Library's do: the feed is pinned
 * at the start and the categories scroll under it.
 *
 * The feed is a button rather than a connected toggle because there are five of them, and a
 * connected group is a two-way switch everywhere else in the app.
 */
@Composable
fun ForumRail(
    feed: ForumFeed,
    selectedCategoryId: Int?,
    onOpenFeedPicker: () -> Unit,
    onCategoryChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    /** Anchored to the feed button, so the menu opens over the control that names the feed. */
    feedMenu: @Composable () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val haptic = rememberHapticFeedback()

    val selectedIndex = forumCategoryTabs.indexOfFirst { it.id == selectedCategoryId }
        .coerceAtLeast(0)

    // Landing on a chip that sits off screen reads as the rail being out of sync with the list.
    LaunchedEffect(selectedIndex) { listState.animateScrollToItem(selectedIndex) }

    PinnedRail(
        pinned = {
            Box {
                FeedButton(
                    feed = feed,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenFeedPicker()
                    }
                )
                feedMenu()
            }
        },
        listState = listState,
        modifier = modifier
    ) {
        itemsIndexed(
            items = forumCategoryTabs,
            key = { _, tab -> "category_${tab.id ?: -1}" }
        ) { _, tab ->
            RailChip(
                label = stringResource(tab.labelRes),
                icon = tab.icon,
                selected = tab.id == selectedCategoryId,
                onClick = { onCategoryChange(tab.id) }
            )
        }
    }
}

@Composable
private fun FeedButton(feed: ForumFeed, onClick: () -> Unit) {
    val label = stringResource(feed.shortLabelRes)
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
        modifier = Modifier
            .height(ConnectedToggleDefaults.Height)
            .bouncyClickable(onClick = onClick, role = Role.Button, clipShape = CircleShape)
            .clearAndSetSemantics {
                role = Role.Button
                contentDescription = label
            }
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = feed.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
