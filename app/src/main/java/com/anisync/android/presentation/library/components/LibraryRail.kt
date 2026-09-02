package com.anisync.android.presentation.library.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.library.LibraryTab
import com.anisync.android.presentation.components.MediaTypeToggle
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.presentation.util.toListIcon
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.ListIndicatorColor
import com.anisync.android.ui.theme.ListIndicatorKind
import com.anisync.android.ui.theme.listIndicatorColor

private val RailHeight = 34.dp

/**
 * One pinned row carrying both "which library" and "which list".
 *
 * The shipped screen spent two full rows on these — a full-width Anime/Manga group and a tab row —
 * for 114dp of chrome that never scrolled away. They answer the same question at different
 * granularities, so they share a row here: the type toggle is pinned at the start, the lists scroll
 * under it, and the overflow button pinned at the end opens the full list manager.
 *
 * The selected chip is tinted with its own list colour, the same palette the corner indicators and
 * the list-manager badges use, so a list is recognisable by hue anywhere in the app.
 */
@Composable
fun LibraryRail(
    tabs: List<LibraryTab>,
    selectedIndex: Int,
    mediaType: MediaType,
    counts: Map<String, Int>,
    onTabClick: (Int) -> Unit,
    onMediaTypeChange: (MediaType) -> Unit,
    modifier: Modifier = Modifier
) {
    val background = MaterialTheme.colorScheme.background
    val listState = rememberLazyListState()

    // Swiping the pager changes the list; the rail has to follow, or the chip you just landed on
    // stays off screen and the rail reads as out of sync with the content.
    LaunchedEffect(selectedIndex, tabs.size) {
        if (selectedIndex in tabs.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Box(modifier = modifier.fillMaxWidth().height(RailHeight)) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 100.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(tabs, key = { _, tab -> tab.toId() }) { index, tab ->
                LibraryRailChip(
                    tab = tab,
                    mediaType = mediaType,
                    count = counts[tab.toId()],
                    selected = index == selectedIndex,
                    onClick = { onTabClick(index) }
                )
            }
        }

        // Pinned type toggle, with a short fade so a chip scrolling under it does not collide.
        // The plate has to be opaque across the whole pinned width, including the leading inset and
        // the seam between the two segments, or chips show through the gaps as they scroll past.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .background(background)
                    .height(RailHeight)
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaTypeToggle(selected = mediaType, onSelect = onMediaTypeChange)
            }
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(RailHeight)
                    .background(
                        Brush.horizontalGradient(listOf(background, Color.Transparent))
                    )
            )
        }

        // A fade rather than a button: the last chip should read as scrollable, and managing the
        // lists is one entry point in the overflow, not two.
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
private fun LibraryRailChip(
    tab: LibraryTab,
    mediaType: MediaType,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val palette = tab.railColors()
    val container by animateColorAsState(
        targetValue = if (selected) palette.container else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "RailChipContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) palette.content else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "RailChipContent"
    )
    val label = tab.getLabel(mediaType)

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
            modifier = Modifier.padding(start = 14.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = tab.railIcon(),
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

@Composable
private fun LibraryTab.railColors(): ListIndicatorColor = when (this) {
    is LibraryTab.Standard -> listIndicatorColor(status.toIndicatorKindForRail())
    is LibraryTab.Favorites -> ListIndicatorColor(
        container = MaterialTheme.colorScheme.errorContainer,
        content = MaterialTheme.colorScheme.onErrorContainer
    )

    else -> listIndicatorColor(ListIndicatorKind.CUSTOM)
}

@Composable
private fun LibraryTab.railIcon(): ImageVector = when (this) {
    is LibraryTab.All -> Icons.Default.AllInclusive
    is LibraryTab.Standard -> status.toListIcon()
    is LibraryTab.Favorites -> Icons.Default.Favorite
    is LibraryTab.Custom -> Icons.AutoMirrored.Filled.List
}

private fun LibraryStatus.toIndicatorKindForRail(): ListIndicatorKind = when (this) {
    LibraryStatus.CURRENT -> ListIndicatorKind.WATCHING
    LibraryStatus.REPEATING -> ListIndicatorKind.REPEATING
    LibraryStatus.PLANNING -> ListIndicatorKind.PLANNING
    LibraryStatus.PAUSED -> ListIndicatorKind.PAUSED
    LibraryStatus.COMPLETED -> ListIndicatorKind.COMPLETED
    LibraryStatus.DROPPED -> ListIndicatorKind.DROPPED
    LibraryStatus.UNKNOWN -> ListIndicatorKind.CUSTOM
}
