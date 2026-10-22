package com.anisync.android.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first

/**
 * A row of chips that scroll under a control pinned at its start.
 *
 * Library, Forum and both searches carry two levels of one question in a single 40dp row: the
 * pinned control answers the broad one (which library, which feed, list or grid) and the chips the
 * narrow one. The pinned control is as wide as its translated labels make it, so the chips are
 * inset by what it measures rather than by a constant that only holds in English.
 */
@Composable
fun PinnedRail(
    pinned: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    /** Chip inset used until the pinned control has been measured. */
    pinnedInsetEstimate: Dp = 140.dp,
    content: LazyListScope.() -> Unit
) {
    val background = MaterialTheme.colorScheme.background
    val density = LocalDensity.current
    var pinnedInset by remember { mutableStateOf(pinnedInsetEstimate) }
    val height = ConnectedToggleDefaults.Height

    Box(modifier = modifier.fillMaxWidth().height(height)) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = pinnedInset, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        // The plate has to be opaque across the whole pinned width, including the leading inset
        // and any seam inside the control, or chips show through the gaps as they scroll past.
        // The short fade after it keeps a chip scrolling under it from colliding.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.onSizeChanged { size ->
                pinnedInset = with(density) { size.width.toDp() }
            }
        ) {
            Row(
                modifier = Modifier
                    .background(background)
                    .height(height)
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pinned()
            }
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(height)
                    .background(Brush.horizontalGradient(listOf(background, Color.Transparent)))
            )
        }

        // A fade rather than a button: the last chip should read as scrollable.
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(24.dp)
                .height(height)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, background)))
        )
    }
}

/**
 * Scrolls the chip at [index] fully into view, by as little as it takes. A chip already in sight
 * leaves the rail alone, so the chips before it stay under the viewer's eye instead of sliding
 * behind the pinned control.
 */
suspend fun LazyListState.revealChip(index: Int) {
    // The first pick can arrive before the rail has been laid out.
    val info = snapshotFlow { layoutInfo }.first { it.visibleItemsInfo.isNotEmpty() }
    val chip = info.visibleItemsInfo.firstOrNull { it.index == index }
    val end = info.viewportEndOffset - info.afterContentPadding
    when {
        chip == null -> animateScrollToItem(index)
        chip.offset < 0 -> animateScrollBy(chip.offset.toFloat())
        chip.offset + chip.size > end -> animateScrollBy((chip.offset + chip.size - end).toFloat())
    }
}
