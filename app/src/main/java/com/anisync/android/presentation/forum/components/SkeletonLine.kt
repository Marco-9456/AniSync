package com.anisync.android.presentation.forum.components

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Skeleton placeholder line with a subtle shimmer/pulse animation.
 * Fades between low and high alpha for a polished loading feel.
 */
@Composable
fun SkeletonLine(
    fraction: Float,
    height: Dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val transition: InfiniteTransition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth(fraction)
            .height(height)
            .clip(MaterialTheme.shapes.small)
            .drawBehind { drawRect(color.copy(alpha = alpha)) }
    )
}
