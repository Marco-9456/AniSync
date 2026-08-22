package com.anisync.android.presentation.components

import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified

/**
 * App-wide indeterminate circular progress indicator.
 *
 * Single source of truth for the loading spinner used across every screen. It currently renders a
 * Material 3 Expressive [CircularWavyProgressIndicator]; swap the implementation here to change the
 * spinner everywhere it is used.
 *
 * @param strokeWidth optional stroke width. When unspecified the expressive default stroke is used;
 *   pass a value (e.g. for compact spinners) and it is converted to the underlying [Stroke].
 * @param wavelength optional wave length, and [gapSize] the indicator-to-track gap. The defaults
 *   are drawn for the indicator's natural 48.dp container; a caller that renders it smaller has to
 *   scale these down by the same factor or the wave degenerates into a single lopsided bump.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = WavyProgressIndicatorDefaults.indicatorColor,
    trackColor: Color = WavyProgressIndicatorDefaults.trackColor,
    strokeWidth: Dp = Dp.Unspecified,
    wavelength: Dp = Dp.Unspecified,
    gapSize: Dp = Dp.Unspecified,
) {
    val stroke = if (strokeWidth.isSpecified) {
        with(LocalDensity.current) {
            Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        }
    } else {
        WavyProgressIndicatorDefaults.circularIndicatorStroke
    }
    // The track is drawn with its own stroke; leaving it at the 4.dp default while the active
    // stroke shrinks makes a compact spinner look like a ring with a thread inside it.
    val trackStroke = if (strokeWidth.isSpecified) {
        with(LocalDensity.current) {
            Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        }
    } else {
        WavyProgressIndicatorDefaults.circularTrackStroke
    }
    CircularWavyProgressIndicator(
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        stroke = stroke,
        trackStroke = trackStroke,
        gapSize = if (gapSize.isSpecified) {
            gapSize
        } else {
            WavyProgressIndicatorDefaults.CircularIndicatorTrackGapSize
        },
        wavelength = if (wavelength.isSpecified) {
            wavelength
        } else {
            WavyProgressIndicatorDefaults.CircularWavelength
        },
    )
}
