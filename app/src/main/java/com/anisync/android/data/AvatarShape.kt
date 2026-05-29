package com.anisync.android.data

import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

/**
 * Avatar shape options for the app.
 */
enum class AvatarShape {
    CLOVER_8_LEAF,
    CIRCLE,
    CLOVER_4_LEAF,
    GHOSTISH,

    /** No shape applied — the avatar image renders in its natural rectangle. */
    NONE;

    @Composable
    fun toComposeShape(): Shape = when (this) {
        CLOVER_8_LEAF -> MaterialShapes.Clover8Leaf.toShape()
        CIRCLE -> MaterialShapes.Circle.toShape()
        CLOVER_4_LEAF -> MaterialShapes.Clover4Leaf.toShape()
        GHOSTISH -> MaterialShapes.Ghostish.toShape()
        NONE -> RectangleShape
    }
}
