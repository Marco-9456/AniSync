package com.anisync.android.presentation.share.studio

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.anisync.android.presentation.share.cards.CardWidth
import kotlin.math.roundToInt

/** Every card leaves the studio this many pixels wide, however dense the screen it was made on. */
private const val ExportWidthPx = 1080f

/**
 * Lays [card] out exactly as it will be exported and shows it scaled down to fit. The card measures
 * at 360dp under a density that makes that 1080px, with the font scale pinned so large system text
 * cannot push it out of its frame, and [layer] records that full size node for the capture.
 */
@Composable
fun ShareCaptureArea(layer: GraphicsLayer?, modifier: Modifier = Modifier, card: @Composable () -> Unit) {
    val display = LocalDensity.current
    val captureDensity = ExportWidthPx / CardWidth.value
    val natural = display.density / captureDensity
    Box(modifier) {
        CompositionLocalProvider(LocalDensity provides Density(captureDensity, 1f)) {
            Box(
                Modifier
                    .scaledToFit(natural)
                    .then(
                        if (layer != null) {
                            Modifier.drawWithContent {
                                layer.record { this@drawWithContent.drawContent() }
                                drawLayer(layer)
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                card()
            }
        }
    }
}

/** Measures the child at export size and places it scaled to fit the space it was given, never past [natural]. */
private fun Modifier.scaledToFit(natural: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(Constraints())
    val fit = minOf(
        natural,
        if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat() / placeable.width.coerceAtLeast(1) else natural,
        if (constraints.hasBoundedHeight) constraints.maxHeight.toFloat() / placeable.height.coerceAtLeast(1) else natural,
    )
    layout((placeable.width * fit).roundToInt(), (placeable.height * fit).roundToInt()) {
        placeable.placeWithLayer(0, 0) {
            transformOrigin = TransformOrigin(0f, 0f)
            scaleX = fit
            scaleY = fit
        }
    }
}
