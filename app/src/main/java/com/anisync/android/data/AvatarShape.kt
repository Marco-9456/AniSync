package com.anisync.android.data

import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape

/**
 * Avatar shape options for the app.
 */
enum class AvatarShape {
    CLOVER,
    CIRCLE,
    CLOVER_4_LEAF,
    GHOSTISH;

    @Composable
    fun toComposeShape(): Shape = when (this) {
        CLOVER -> MaterialShapes.Clover8Leaf.toShape()
        CIRCLE -> MaterialShapes.Circle.toShape()
        CLOVER_4_LEAF -> MaterialShapes.Clover4Leaf.toShape()
        GHOSTISH -> MaterialShapes.Ghostish.toShape()
    }
}
