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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
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
import com.anisync.android.presentation.components.ConnectedToggleSegment
import com.anisync.android.presentation.forum.ForumScope
import com.anisync.android.presentation.forum.YoursTab
import com.anisync.android.presentation.forum.forumCategoryTabs
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.rememberHapticFeedback

private val RailHeight = ConnectedToggleDefaults.Height

/** Chip inset used until the pinned toggle has been measured. */
private val PinnedToggleInsetEstimate = 120.dp

/**
 * One pinned row carrying both "which body of threads" and "which slice of it".
 *
 * The shipped screen spent three strips on this — a search bar, a five-way feed group and a tab row
 * of seventeen categories — and only the search bar ever scrolled away. They answer the same
 * question at different granularities, so they share a row here exactly as Library's do: the scope
 * toggle is pinned at the start and the chips scroll under it.
 *
 * In [ForumScope.BROWSE] the chips are the forum categories. In [ForumScope.YOURS] they are the two
 * personal collections, which is what the old Subscribed and Saved feeds became.
 */
@Composable
fun ForumRail(
    scope: ForumScope,
    yoursTab: YoursTab,
    selectedCategoryId: Int?,
    subscribedCount: Int?,
    savedCount: Int?,
    onScopeChange: (ForumScope) -> Unit,
    onCategoryChange: (Int?) -> Unit,
    onYoursTabChange: (YoursTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val background = MaterialTheme.colorScheme.background
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val haptic = rememberHapticFeedback()
    // The pinned toggle is as wide as its translated labels make it, so the chips are inset by what
    // it measures rather than by a constant that only holds in English.
    var pinnedInset by remember { mutableStateOf(PinnedToggleInsetEstimate) }

    val selectedIndex = when (scope) {
        ForumScope.BROWSE -> forumCategoryTabs.indexOfFirst { it.id == selectedCategoryId }
            .coerceAtLeast(0)

        ForumScope.YOURS -> YoursTab.entries.indexOf(yoursTab)
    }

    // Landing on a chip that sits off screen reads as the rail being out of sync with the list.
    LaunchedEffect(scope, selectedIndex) {
        listState.animateScrollToItem(selectedIndex)
    }

    Box(modifier = modifier.fillMaxWidth().height(RailHeight)) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = pinnedInset, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (scope) {
                ForumScope.BROWSE -> itemsIndexed(
                    items = forumCategoryTabs,
                    key = { _, tab -> "category_${tab.id ?: -1}" }
                ) { _, tab ->
                    ForumRailChip(
                        label = stringResource(tab.labelRes),
                        count = null,
                        selected = tab.id == selectedCategoryId,
                        onClick = { onCategoryChange(tab.id) }
                    )
                }

                ForumScope.YOURS -> itemsIndexed(
                    items = YoursTab.entries,
                    key = { _, tab -> "yours_${tab.name}" }
                ) { _, tab ->
                    ForumRailChip(
                        label = stringResource(
                            when (tab) {
                                YoursTab.SUBSCRIBED -> R.string.forum_tab_subscribed
                                YoursTab.SAVED -> R.string.forum_tab_saved
                            }
                        ),
                        count = when (tab) {
                            YoursTab.SUBSCRIBED -> subscribedCount
                            YoursTab.SAVED -> savedCount
                        },
                        selected = tab == yoursTab,
                        onClick = { onYoursTabChange(tab) }
                    )
                }
            }
        }

        // Pinned scope toggle. The plate has to be opaque across the whole pinned width, including
        // the leading inset and the seam, or chips show through the gaps as they scroll past.
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
                horizontalArrangement = Arrangement.spacedBy(ConnectedToggleDefaults.Spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val showLabels = !LocalAdaptiveInfo.current.isCompact
                val segmentModifier = if (showLabels) {
                    Modifier
                } else {
                    Modifier.width(ConnectedToggleDefaults.IconOnlyWidth)
                }
                ConnectedToggleSegment(
                    icon = Icons.Default.Forum,
                    label = stringResource(R.string.forum_scope_browse),
                    selected = scope == ForumScope.BROWSE,
                    leading = true,
                    showLabel = showLabels,
                    modifier = segmentModifier,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onScopeChange(ForumScope.BROWSE)
                    }
                )
                ConnectedToggleSegment(
                    icon = Icons.Default.Bookmark,
                    label = stringResource(R.string.forum_scope_yours),
                    selected = scope == ForumScope.YOURS,
                    leading = false,
                    showLabel = showLabels,
                    modifier = segmentModifier,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onScopeChange(ForumScope.YOURS)
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
private fun ForumRailChip(
    label: String,
    count: Int?,
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
                contentDescription = buildString {
                    append(label)
                    if (count != null) append(", $count")
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = if (count != null) 8.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = content,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (count != null) {
                Surface(shape = CircleShape, color = content.copy(alpha = 0.16f)) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = content,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

/**
 * The row under the rail: how the list is ordered, and how many filters narrow it. Both open the
 * same sheet, which is the shape Library's "Airing soon / Filters" pair already established.
 */
@Composable
fun ForumToolbar(
    sortLabel: String,
    filterCount: Int,
    onOpenSortAndFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ForumToolChip(
            label = sortLabel,
            icon = Icons.Default.SwapVert,
            count = null,
            onClick = onOpenSortAndFilter
        )
        ForumToolChip(
            label = stringResource(R.string.forum_filters),
            icon = Icons.Default.Tune,
            count = filterCount.takeIf { it > 0 },
            onClick = onOpenSortAndFilter
        )
    }
}

@Composable
private fun ForumToolChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int?,
    onClick: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = CircleShape,
        modifier = Modifier
            .height(32.dp)
            .bouncyClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                role = Role.Button,
                onClickLabel = label,
                clipShape = CircleShape
            )
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = if (count != null) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
            if (count != null) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
