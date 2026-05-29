package com.anisync.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import com.anisync.android.data.AvatarShape

/**
 * CompositionLocal providing the user's preferred avatar shape down the hierarchy.
 * Default is the Clover8Leaf shape.
 */
val LocalAvatarShape =
    staticCompositionLocalOf<Shape> { androidx.compose.foundation.shape.CircleShape }

/**
 * The selected avatar shape as the enum, so consumers can special-case [AvatarShape.NONE].
 */
val LocalAvatarShapeId = staticCompositionLocalOf { AvatarShape.CLOVER_8_LEAF }

/**
 * Shape for decorative, non-avatar UI that borrows the avatar shape (e.g. settings
 * item icons). Falls back to the 8-leaf clover when the shape is [AvatarShape.NONE]:
 * NONE is meant to leave real avatar images unclipped, not to square these elements.
 */
@Composable
fun decorativeAvatarShape(): Shape {
    val id = LocalAvatarShapeId.current
    return (if (id == AvatarShape.NONE) AvatarShape.CLOVER_8_LEAF else id).toComposeShape()
}

/**
 * CompositionLocal providing whether the user wants avatar backgrounds enabled.
 */
val LocalAvatarBackgroundEnabled = staticCompositionLocalOf<Boolean> { true }

/**
 * CompositionLocal providing whether the user wants to disable the avatar shape on their own profile.
 */
val LocalDisableAvatarShapeProfile = staticCompositionLocalOf<Boolean> { false }
