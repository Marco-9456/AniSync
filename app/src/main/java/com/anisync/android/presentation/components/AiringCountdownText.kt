package com.anisync.android.presentation.components

import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FadeEdgeWidth = 12.dp

/**
 * Scrolls the content sideways when it is wider than the space it was given, and leaves it alone
 * when it fits. Library cards use it to keep the "episodes behind" badge and the next-episode
 * countdown both readable on a narrow grid card instead of truncating either one.
 *
 * The trailing fade is drawn only while scrolling, so content that fits keeps its last few pixels
 * at full opacity.
 */
fun Modifier.scrollWhenTooWide(): Modifier = composed {
    // Written during measure and read only from the draw lambdas below, so a change invalidates
    // drawing rather than recomposing the caller.
    var tooWide by remember { mutableStateOf(false) }

    Modifier
        // Offscreen so the fade masks the content alone, not whatever sits beneath it.
        .graphicsLayer {
            compositingStrategy =
                if (tooWide) CompositingStrategy.Offscreen else CompositingStrategy.Auto
        }
        .drawWithContent {
            drawContent()
            if (!tooWide) return@drawWithContent
            val edge = FadeEdgeWidth.toPx()
            drawRect(
                topLeft = Offset(size.width - edge, 0f),
                size = Size(edge, size.height),
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startX = size.width,
                    endX = size.width - edge
                ),
                blendMode = BlendMode.DstIn
            )
        }
        .layout { measurable, constraints ->
            tooWide = measurable.maxIntrinsicWidth(constraints.maxHeight) > constraints.maxWidth
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
        .basicMarquee(
            iterations = Int.MAX_VALUE,
            repeatDelayMillis = 2000,
            initialDelayMillis = 1500,
            spacing = MarqueeSpacing(16.dp)
        )
}

/**
 * The "Ep 12 in 3d" countdown, sized to sit on one line beside the "episodes behind" badge. It
 * scrolls rather than truncates when a many-column grid, a long locale or a large font scale leaves
 * it too little room, and stays still in the common case where it fits.
 */
@Composable
fun AiringCountdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontSize = 10.sp,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier.scrollWhenTooWide()
    )
}
