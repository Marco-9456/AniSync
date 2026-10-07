package com.anisync.android.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A flow row that stops after [maxLines] and can end its last line with an [overflowIndicator]
 * told how many items it stands in for, the "+N more" chip.
 *
 * Stands in for ContextualFlowRow, which Compose no longer maintains. [FlowRow]'s own overflow is
 * deprecated too, and it only settles the hidden count in the draw phase, too late for the
 * indicator's text. Here the count is worked out while measuring: as many items as fit in
 * [maxLines] with the indicator placed after them. Without an indicator the rest is clipped.
 */
@Composable
fun CountedFlowRow(
    itemCount: Int,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 0.dp,
    verticalSpacing: Dp = 0.dp,
    maxLines: Int = Int.MAX_VALUE,
    overflowIndicator: (@Composable (hiddenCount: Int) -> Unit)? = null,
    item: @Composable (index: Int) -> Unit,
) {
    SubcomposeLayout(modifier) { constraints ->
        val gapX = horizontalSpacing.roundToPx()
        val gapY = verticalSpacing.roundToPx()
        val maxWidth = constraints.maxWidth
        val loose = Constraints(maxWidth = maxWidth)

        val items = (0 until itemCount).map { index ->
            subcompose(index) { Box { item(index) } }.first().measure(loose)
        }

        // Greedy line breaking over the given placeables, returning the index at which each line starts.
        fun lineStarts(placeables: List<Placeable>): List<Int> {
            val starts = mutableListOf<Int>()
            var x = 0
            placeables.forEachIndexed { index, p ->
                if (index == 0 || x + gapX + p.width > maxWidth) {
                    starts += index
                    x = p.width
                } else {
                    x += gapX + p.width
                }
            }
            return starts
        }

        var shown = items
        val all = lineStarts(items)
        if (all.size > maxLines) {
            val fitting = all[maxLines]
            shown = if (overflowIndicator == null) {
                items.take(fitting)
            } else {
                // Drop items from the end until they and the indicator share maxLines. The indicator
                // is measured per hidden count, since "+9" and "+10" are not the same width.
                var kept = fitting
                var row: List<Placeable>
                while (true) {
                    val hidden = itemCount - kept
                    val indicator = subcompose("overflow-$hidden") { Box { overflowIndicator(hidden) } }
                        .first().measure(loose)
                    row = items.take(kept) + indicator
                    if (kept == 0 || lineStarts(row).size <= maxLines) break
                    kept--
                }
                row
            }
        }

        val starts = lineStarts(shown)
        val lines = starts.indices.map { line ->
            val end = if (line + 1 < starts.size) starts[line + 1] else shown.size
            shown.subList(starts[line], end)
        }
        val heights = lines.map { line -> line.maxOf { it.height } }
        val width = lines.maxOfOrNull { line -> line.sumOf { it.width } + gapX * (line.size - 1) } ?: 0
        val height = heights.sum() + gapY * (lines.size - 1).coerceAtLeast(0)

        layout(
            width.coerceIn(constraints.minWidth, constraints.maxWidth),
            height.coerceIn(constraints.minHeight, constraints.maxHeight),
        ) {
            var y = 0
            lines.forEachIndexed { index, line ->
                var x = 0
                line.forEach { p ->
                    p.placeRelative(x, y)
                    x += p.width + gapX
                }
                y += heights[index] + gapY
            }
        }
    }
}
