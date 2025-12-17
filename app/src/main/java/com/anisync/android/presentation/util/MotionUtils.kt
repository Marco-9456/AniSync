@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.anisync.android.presentation.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * A modifier that handles click interactions with a "bouncy" scale effect.
 * Uses [MaterialTheme.motionScheme] to ensure physics match the rest of the app.
 *
 * @param onClick The callback when the item is clicked.
 * @param pressedScale The scale factor when pressed (default 0.97f).
 * @param enabled Whether the click is enabled.
 */
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Use the slow spatial spec for a "heavy", premium feel suitable for cards
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "BouncyScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Disable default ripple if the scale is the primary feedback, or keep it.
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * A modifier that handles click and long-click interactions with a "bouncy" scale effect.
 * Uses [MaterialTheme.motionScheme] to ensure physics match the rest of the app.
 *
 * @param onClick The callback when the item is clicked.
 * @param onLongClick The callback when the item is long-clicked.
 * @param pressedScale The scale factor when pressed (default 0.95f).
 * @param enabled Whether the click is enabled.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bouncyCombinedClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "BouncyScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick
        )
}
