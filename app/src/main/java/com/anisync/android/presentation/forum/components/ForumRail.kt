package com.anisync.android.presentation.forum.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.ConnectedToggleDefaults
import com.anisync.android.presentation.forum.ForumFeed
import com.anisync.android.presentation.forum.forumCategoryTabs
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.rememberHapticFeedback

private val RailHeight = ConnectedToggleDefaults.Height

/** Chip inset used until the pinned feed button has been measured. */
private val PinnedInsetEstimate = 140.dp

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
    modifier: Modifier = Modifier
) {
    val background = MaterialTheme.colorScheme.background
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val haptic = rememberHapticFeedback()
    // The pinned button is as wide as its translated label makes it, so the chips are inset by what
    // it measures rather than by a constant that only holds in English.
    var pinnedInset by remember { mutableStateOf(PinnedInsetEstimate) }

    val selectedIndex = forumCategoryTabs.indexOfFirst { it.id == selectedCategoryId }
        .coerceAtLeast(0)

    // Landing on a chip that sits off screen reads as the rail being out of sync with the list.
    LaunchedEffect(selectedIndex) { listState.animateScrollToItem(selectedIndex) }

    Box(modifier = modifier.fillMaxWidth().height(RailHeight)) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = pinnedInset, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(
                items = forumCategoryTabs,
                key = { _, tab -> "category_${tab.id ?: -1}" }
            ) { _, tab ->
                ForumRailChip(
                    label = stringResource(tab.labelRes),
                    icon = tab.icon,
                    selected = tab.id == selectedCategoryId,
                    onClick = { onCategoryChange(tab.id) }
                )
            }
        }

        // Pinned feed button. The plate has to be opaque across the whole pinned width, including
        // the leading inset, or chips show through as they scroll past.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.onSizeChanged { size ->
                pinnedInset = with(density) { size.width.toDp() }
            }
        ) {
            Row(
                modifier = Modifier
                    .background(background)
                    .height(RailHeight)
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeedButton(
                    feed = feed,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenFeedPicker()
                    }
                )
            }
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(RailHeight)
                    .background(Brush.horizontalGradient(listOf(background, Color.Transparent)))
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(24.dp)
                .height(RailHeight)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, background)))
        )
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
            .height(RailHeight)
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

@Composable
private fun ForumRailChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val container by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "ForumRailChipContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "ForumRailChipContent"
    )

    Surface(
        color = container,
        shape = CircleShape,
        modifier = Modifier
            .height(RailHeight)
            .bouncyClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                role = Role.Tab,
                clipShape = CircleShape
            )
            .clearAndSetSemantics {
                role = Role.Tab
                this.selected = selected
                contentDescription = label
            }
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 12.dp),
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
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = content,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
