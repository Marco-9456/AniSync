package com.anisync.android.presentation.discover.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Vertical wheel picker reminiscent of the platform date/time pickers.
 * Items snap to center; the centered item is emphasised. Drives the
 * year From/To inputs and the score/episode/chapter numeric inputs.
 */
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemCount: Int = 5,
    itemHeight: Dp = 40.dp
) {
    val safeSelected = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val state = rememberLazyListState(initialFirstVisibleItemIndex = safeSelected)
    val snap = rememberSnapFlingBehavior(state)
    val halfCount = visibleItemCount / 2

    val centerIndex by remember {
        derivedStateOf {
            val info = state.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) {
                safeSelected
            } else {
                val viewportCenter = info.viewportStartOffset + info.viewportSize.height / 2
                info.visibleItemsInfo.minByOrNull {
                    abs((it.offset + it.size / 2) - viewportCenter)
                }?.index ?: safeSelected
            }
        }
    }

    LaunchedEffect(centerIndex, state.isScrollInProgress) {
        if (!state.isScrollInProgress && centerIndex != safeSelected) {
            onSelectedIndexChange(centerIndex)
        }
    }

    LaunchedEffect(safeSelected) {
        if (safeSelected != centerIndex && !state.isScrollInProgress) {
            state.scrollToItem(safeSelected)
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemCount)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(itemHeight)
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    RoundedCornerShape(10.dp)
                )
        )

        val density = LocalDensity.current
        val fadeHeightPx = with(density) { (itemHeight * 1.5f).toPx() }

        LazyColumn(
            state = state,
            flingBehavior = snap,
            contentPadding = PaddingValues(vertical = itemHeight * halfCount),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            startY = 0f,
                            endY = fadeHeightPx
                        ),
                        topLeft = Offset.Zero,
                        size = Size(size.width, fadeHeightPx),
                        blendMode = BlendMode.DstIn
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Black, Color.Transparent),
                            startY = size.height - fadeHeightPx,
                            endY = size.height
                        ),
                        topLeft = Offset(0f, size.height - fadeHeightPx),
                        size = Size(size.width, fadeHeightPx),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            itemsIndexed(items) { idx, item ->
                val distance = abs(idx - centerIndex)
                val alpha = (1f - distance * 0.25f).coerceAtLeast(0.3f)
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        style = if (distance == 0) MaterialTheme.typography.titleMedium
                        else MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
