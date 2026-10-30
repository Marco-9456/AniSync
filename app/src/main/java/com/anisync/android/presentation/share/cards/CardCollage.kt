package com.anisync.android.presentation.share.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The shapes covers are cut into, as PixelPlayer's home sets them. */
enum class CollageShape { COOKIE, CIRCLE, PILL, ARCH, CLOVER, SUNNY }

/**
 * One cover in a collage, placed in dp from the collage's top start corner. [bottomCorner] overrides
 * an arch's bottom corners where a frame draws them rounder than its width suggests.
 */
data class CollageSlot(
    val shape: CollageShape,
    val x: Dp,
    val y: Dp,
    val width: Dp,
    val height: Dp,
    val bottomCorner: Dp? = null,
)

/** Covers in their shapes. Slots past the last cover stay empty, so fewer favourites never leave a hole. */
@Composable
fun CardCollage(covers: List<CardImage>, slots: List<CollageSlot>, modifier: Modifier = Modifier) {
    Box(modifier) {
        slots.zip(covers).forEach { (slot, cover) ->
            CardPicture(
                url = cover.url,
                shape = slot.shape.toComposeShape(slot.width, slot.bottomCorner ?: slot.width * ArchBottom),
                modifier = Modifier
                    .offset(slot.x, slot.y)
                    .size(slot.width, slot.height),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CollageShape.toComposeShape(width: Dp, archBottom: Dp): Shape = when (this) {
    CollageShape.COOKIE -> MaterialShapes.Cookie9Sided.toShape()
    CollageShape.CIRCLE -> CircleShape
    CollageShape.PILL -> RoundedCornerShape(percent = 50)
    CollageShape.ARCH -> RoundedCornerShape(topStart = width / 2, topEnd = width / 2, bottomEnd = archBottom, bottomStart = archBottom)
    CollageShape.CLOVER -> MaterialShapes.Clover8Leaf.toShape()
    CollageShape.SUNNY -> MaterialShapes.Sunny.toShape()
}

/** The arch's bottom corners against its width, 20 on 104 in the frames. */
private const val ArchBottom = 0.19f

/** Five favourites in a 312 x 96 strip, the Stats story cards' collage. */
val StripCollage = listOf(
    CollageSlot(CollageShape.COOKIE, 0.dp, 0.dp, 94.8.dp, 93.6.dp),
    CollageSlot(CollageShape.CIRCLE, 80.dp, 8.dp, 80.dp, 80.dp),
    CollageSlot(CollageShape.PILL, 146.dp, 0.dp, 56.dp, 96.dp),
    CollageSlot(CollageShape.SUNNY, 192.dp, 6.dp, 82.6.dp, 82.6.dp),
    CollageSlot(CollageShape.ARCH, 262.dp, 10.dp, 50.dp, 80.dp, bottomCorner = 12.dp),
)
