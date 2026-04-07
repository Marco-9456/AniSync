package com.anisync.android.presentation.profile.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.sin

class ExpressiveBadgeShape(
    private val waves: Int = 16,
    private val amplitude: Float = 0.04f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.width / 2f
        val waveAmplitude = radius * amplitude

        for (angle in 0..360) {
            val radians = Math.toRadians(angle.toDouble())
            val currentRadius = radius + (waveAmplitude * cos(waves * radians))

            val x = centerX + (currentRadius * cos(radians)).toFloat()
            val y = centerY + (currentRadius * sin(radians)).toFloat()

            if (angle == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        path.close()
        return Outline.Generic(path)
    }
}
