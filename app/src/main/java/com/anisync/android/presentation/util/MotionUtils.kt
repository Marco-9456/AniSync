@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.anisync.android.presentation.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * A modifier that handles click interactions with a "bouncy" scale effect and full accessibility support.
 * Uses [MaterialTheme.motionScheme] to ensure physics match the rest of the app.
 *
 * This modifier provides:
 * - Visual feedback via ripple indication that is properly clipped to the shape
 * - Semantic role for screen readers (defaults to [Role.Button])
 * - Custom click action label for accessibility services
 *
 * @param enabled Whether the click is enabled.
 * @param pressedScale The scale factor when pressed (default 0.95f).
 * @param role The semantic role for accessibility services (default [Role.Button]).
 *             Set to null if the parent composable already defines a role.
 * @param onClickLabel The accessibility label describing what happens when clicked.
 *                     This is announced by screen readers (e.g., "Open details for Attack on Titan").
 * @param clipShape Optional shape to clip the ripple effect to. If null, no clipping is applied
 *                  (useful when parent Surface already handles clipping).
 * @param onClick The callback when the item is clicked.
 */
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    clipShape: Shape? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Use the default spatial spec for a balanced, expressive feel
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "BouncyScale"
    )

    this
        .semantics(mergeDescendants = false) {
            role?.let { this.role = it }
            onClickLabel?.let { label ->
                onClick(label = label, action = null)
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(if (clipShape != null) Modifier.clip(clipShape) else Modifier)
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * A modifier that handles click and long-click interactions with a "bouncy" scale effect
 * and full accessibility support.
 * Uses [MaterialTheme.motionScheme] to ensure physics match the rest of the app.
 *
 * This modifier provides:
 * - Visual feedback via ripple indication that is properly clipped to the shape
 * - Semantic role for screen readers (defaults to [Role.Button])
 * - Custom click and long-click action labels for accessibility services
 *
 * @param enabled Whether the click is enabled.
 * @param pressedScale The scale factor when pressed (default 0.95f).
 * @param role The semantic role for accessibility services (default [Role.Button]).
 *             Set to null if the parent composable already defines a role.
 * @param onClickLabel The accessibility label describing what happens when clicked.
 * @param onLongClickLabel The accessibility label describing what happens when long-clicked.
 * @param clipShape Optional shape to clip the ripple effect to. If null, no clipping is applied
 *                  (useful when parent Surface already handles clipping).
 * @param onClick The callback when the item is clicked.
 * @param onLongClick The callback when the item is long-clicked.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bouncyCombinedClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    onLongClickLabel: String? = null,
    clipShape: Shape? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "BouncyScale"
    )

    this
        .semantics(mergeDescendants = false) {
            role?.let { this.role = it }
            onClickLabel?.let { label ->
                onClick(label = label, action = null)
            }
            if (onLongClick != null && onLongClickLabel != null) {
                onLongClick(label = onLongClickLabel, action = null)
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(if (clipShape != null) Modifier.clip(clipShape) else Modifier)
        .combinedClickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick
        )
}
