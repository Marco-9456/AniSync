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
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = WavyProgressIndicatorDefaults.indicatorColor,
    trackColor: Color = WavyProgressIndicatorDefaults.trackColor,
    strokeWidth: Dp = Dp.Unspecified,
) {
    val stroke = if (strokeWidth.isSpecified) {
        with(LocalDensity.current) {
            Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        }
    } else {
        WavyProgressIndicatorDefaults.circularIndicatorStroke
    }
    CircularWavyProgressIndicator(
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        stroke = stroke,
    )
}
