package com.anisync.android.presentation.share.cards

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * A Material shape at its own proportions, as large as its box allows and centred in it.
 * `toShape()` stretches a polygon to whatever box it gets, so a cookie in a 96 by 112 slot came out
 * tall and narrow.
 */
@Immutable
private class FittedPolygonShape(polygon: RoundedPolygon) : Shape {
    private val path = polygon.toPath(android.graphics.Path())
    private val bounds = polygon.calculateBounds()

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val width = bounds[2] - bounds[0]
        val height = bounds[3] - bounds[1]
        val scale = min(size.width / width, size.height / height)
        val matrix = android.graphics.Matrix().apply {
            setTranslate(-bounds[0], -bounds[1])
            postScale(scale, scale)
            postTranslate((size.width - width * scale) / 2f, (size.height - height * scale) / 2f)
        }
        return Outline.Generic(android.graphics.Path(path).apply { transform(matrix) }.asComposePath())
    }
}

private val fittedShapes = ConcurrentHashMap<RoundedPolygon, Shape>()

/** This polygon as a shape that keeps its proportions in any box. */
fun RoundedPolygon.fitted(): Shape = fittedShapes.getOrPut(this) { FittedPolygonShape(this) }
