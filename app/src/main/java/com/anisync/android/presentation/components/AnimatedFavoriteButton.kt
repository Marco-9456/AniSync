package com.anisync.android.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.util.rememberHapticFeedback
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * A highly polished favorite button with a "pop" spring animation, color transitions,
 * haptic feedback, and a particle explosion effect.
 *
 * Fixes:
 * - Prevents animation triggering on initial composition.
 * - Fixes race conditions causing stuck particles.
 */
@Composable
fun AnimatedFavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    activeColor: Color = Color(0xFFFF1744), // Material Red A400
    inactiveColor: Color = LocalContentColor.current
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = rememberHapticFeedback()

    // Animations
    val scale = remember { Animatable(1f) }
    val sparklesAlpha = remember { Animatable(0f) }
    val sparklesRadius = remember { Animatable(0f) }

    // Track previous state to skip initial animation on screen load
    var previousState by remember { mutableStateOf(isFavorite) }

    // Trigger animations only when state actually changes
    LaunchedEffect(isFavorite) {
        val wasFavorite = previousState
        previousState = isFavorite

        // Skip if this is the first composition (wasFavorite == isFavorite)
        if (wasFavorite != isFavorite) {
            if (isFavorite) {
                // Haptic feedback
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                // Reset values for new animation
                scale.snapTo(0.7f)
                sparklesRadius.snapTo(0f)
                sparklesAlpha.snapTo(1f)

                // Run animations in parallel using child coroutines
                launch {
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioHighBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }

                launch {
                    sparklesRadius.animateTo(
                        targetValue = 1.5f,
                        animationSpec = keyframes {
                            durationMillis = 400
                            0.0f at 0 using FastOutSlowInEasing
                            1.5f at 300
                        }
                    )
                }

                launch {
                    sparklesAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = keyframes {
                            durationMillis = 400
                            1f at 0
                            1f at 150 // Keep visible for a bit
                            0f at 400 // Fade out completely
                        }
                    )
                }
            } else {
                // Unlike animation (just smooth reset)
                scale.animateTo(1f)
                sparklesAlpha.snapTo(0f) // Hide immediately
            }
        }
    }

    val tint by animateColorAsState(
        targetValue = if (isFavorite) activeColor else inactiveColor,
        label = "ColorAnimation"
    )

    Box(
        modifier = modifier
            .size(iconSize + 8.dp) // Fixed size prevents layout shift
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Sparkles Canvas — always present to avoid layout shift, visibility via alpha
        Canvas(
            modifier = Modifier
                .size(iconSize * 2.0f)
                .graphicsLayer { alpha = sparklesAlpha.value }
        ) {
            val currentAlpha = sparklesAlpha.value
            if (currentAlpha > 0f) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxTravelDistance = size.width / 2
                val currentRadius = maxTravelDistance * (sparklesRadius.value * 0.5f)
                val dotRadius = (size.width / 20) * currentAlpha

                for (i in 0 until 8) {
                    val angle = Math.toRadians((i * 45).toDouble())
                    val x = center.x + cos(angle).toFloat() * currentRadius
                    val y = center.y + sin(angle).toFloat() * currentRadius

                    drawCircle(
                        color = activeColor.copy(alpha = currentAlpha),
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                }
            }
        }

        // The Icon
        Crossfade(
            targetState = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            label = "IconCrossfade",
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
        ) { icon ->
            Icon(
                imageVector = icon,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}