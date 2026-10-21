package com.anisync.android.presentation.library.components

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.components.MediaTypeToggle
import com.anisync.android.presentation.components.PinnedRail
import com.anisync.android.presentation.components.RailChip
import com.anisync.android.presentation.library.LibraryTab
import com.anisync.android.presentation.util.toListIcon
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.ListIndicatorColor
import com.anisync.android.ui.theme.ListIndicatorKind
import com.anisync.android.ui.theme.listIndicatorColor

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
    val listState = rememberLazyListState()

    // Swiping the pager changes the list; the rail has to follow, or the chip you just landed on
    // stays off screen and the rail reads as out of sync with the content.
    LaunchedEffect(selectedIndex, tabs.size) {
        if (selectedIndex in tabs.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    PinnedRail(
        pinned = { MediaTypeToggle(selected = mediaType, onSelect = onMediaTypeChange) },
        listState = listState,
        pinnedInsetEstimate = 200.dp,
        modifier = modifier
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
}

/** A list chip in its own colour, shared by the Library rail and the Library search rail. */
@Composable
internal fun LibraryRailChip(
    tab: LibraryTab,
    mediaType: MediaType,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = tab.railColors()
    RailChip(
        label = tab.getLabel(mediaType),
        icon = tab.railIcon(),
        selected = selected,
        onClick = onClick,
        count = count,
        selectedContainer = palette.container,
        selectedContent = palette.content
    )
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
