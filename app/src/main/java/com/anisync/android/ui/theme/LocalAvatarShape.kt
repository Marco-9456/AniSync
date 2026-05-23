package com.anisync.android.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape

/**
 * CompositionLocal providing the user's preferred avatar shape down the hierarchy.
 * Default is the Clover8Leaf shape.
 */
val LocalAvatarShape =
    staticCompositionLocalOf<Shape> { androidx.compose.foundation.shape.CircleShape }

/**
 * CompositionLocal providing whether the user wants avatar backgrounds enabled.
 */
val LocalAvatarBackgroundEnabled = staticCompositionLocalOf<Boolean> { true }
