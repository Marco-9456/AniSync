package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Simple skeleton placeholder line for loading states.
 * Color must be passed as a composable-context parameter since
 * drawBehind/drawWithContent lambdas are not @Composable.
 */
@Composable
fun SkeletonLine(
    fraction: Float,
    height: Dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
) {
    Box(
        modifier = modifier
            .fillMaxWidth(fraction)
            .height(height)
            .clip(MaterialTheme.shapes.small)
            .drawBehind { drawRect(color) }
    )
}
