package com.anisync.android.presentation.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One discrete card/section of a dashboard. [key] is used as the lazy-list item key on compact
 * (single-column) so scroll position and item animations stay stable; [content] is the section
 * composable, which is expected to manage its own horizontal insets (the existing statistics and
 * profile sections all self-pad to 16.dp).
 */
class DashboardSection(
    val key: String,
    val content: @Composable () -> Unit,
)

/**
 * How many columns a section dashboard should flow into at the current width. Single column on a
 * phone (keeps the lazy, full-bleed stack), two on medium/expanded, three on very wide windows.
 * Charts stay readable because each column is still >= ~360dp at these breakpoints.
 */
@Composable
fun dashboardColumns(): Int = when (LocalAdaptiveInfo.current.widthClass) {
    WidthSizeClass.COMPACT -> 1
    WidthSizeClass.MEDIUM -> 2
    WidthSizeClass.EXPANDED -> 2
    WidthSizeClass.LARGE -> 3
}

/**
 * Minimum width for one dashboard stat card, used by [StatsDashboardGrid] to decide how many cards
 * flow across each row. The cards themselves reflow internally for narrower widths (e.g. the status
 * donut stacks its legend under the ring), so this only needs to keep charts comfortably legible.
 */
private val DashboardMinCardWidth = 360.dp

/**
 * Lays [sections] out as a responsive **flow grid** that reacts to the available width: each card is
 * at least [minCardWidth] wide, and as many equal-width cards as fit flow across each row, wrapping to
 * the next row when they don't — so the column count is derived from the **measured** width and is
 * never fixed (resize the pane and the cards reflow). Cards are uniform width and the final row is
 * start-aligned (no stretching). Sections keep their own internal 16.dp horizontal padding, which
 * becomes the inter-card gutter (32.dp) and outer margin (16.dp); [verticalSpacing] separates rows.
 * The hero card is laid out by the caller, full-width above this grid.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsDashboardGrid(
    sections: List<DashboardSection>,
    modifier: Modifier = Modifier,
    minCardWidth: Dp = DashboardMinCardWidth,
    verticalSpacing: Dp = 24.dp,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = (maxWidth / minCardWidth).toInt().coerceAtLeast(1)
        // Subtract a hair before dividing so `columns * cardWidth` never rounds a sub-pixel past the
        // row width — otherwise the exact-fit case (e.g. 2 × 417.5dp on an 835dp row) rounds each card
        // up and wraps the last one to its own row, collapsing the grid back to a single column.
        val cardWidth = (maxWidth - 2.dp) / columns
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = columns,
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        ) {
            sections.forEach { section ->
                Box(modifier = Modifier.width(cardWidth)) {
                    section.content()
                }
            }
        }
    }
}
