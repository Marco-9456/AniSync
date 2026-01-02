package com.anisync.android.presentation.util

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Default shimmer animation duration in milliseconds.
 * 1200ms provides a smooth, premium feel.
 */
private const val DEFAULT_SHIMMER_DURATION = 1200

/**
 * Applies a shimmer loading effect to the composable.
 * Uses Material 3 theme-aware colors for consistent appearance.
 * 
 * @param durationMillis Animation duration in milliseconds (default: 1200ms)
 */
@Composable
fun Modifier.shimmerEffect(
    durationMillis: Int = DEFAULT_SHIMMER_DURATION
): Modifier {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.surfaceContainerLow,
        MaterialTheme.colorScheme.surfaceContainerHighest,
    )
    
    return shimmerEffect(
        colors = shimmerColors,
        durationMillis = durationMillis
    )
}

/**
 * Applies a shimmer loading effect with custom colors.
 * Use this variant for custom color schemes.
 * 
 * @param colors List of colors for the shimmer gradient (should have at least 3 colors)
 * @param durationMillis Animation duration in milliseconds
 */
@Composable
fun Modifier.shimmerEffect(
    colors: List<Color>,
    durationMillis: Int = DEFAULT_SHIMMER_DURATION
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    return this.drawWithCache {
        val brush = Brush.linearGradient(
            colors = colors,
            start = Offset(translateAnimation - 500f, translateAnimation - 500f),
            end = Offset(translateAnimation, translateAnimation)
        )
        onDrawBehind {
            drawRect(brush)
        }
    }
}

/**
 * Applies a shimmer effect with light gray colors.
 * Use this for overlays on images or dark backgrounds.
 */
@Composable
fun Modifier.shimmerEffectLight(): Modifier {
    val shimmerColors = remember {
        listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        )
    }
    
    return shimmerEffect(
        colors = shimmerColors
    )
}
