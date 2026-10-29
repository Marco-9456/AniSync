package com.anisync.android.presentation.statistics

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * The stats cards are drawn for the page background. A tablet's tab pane is already a
 * surfaceContainerLow card, so inside it every container role moves one step up and the cards keep
 * the contrast they have on a phone. Surface itself stays put, so sheets opened from here look the
 * same as anywhere else.
 */
@Composable
fun StatsPaneSurfaces(enabled: Boolean, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    val scheme = MaterialTheme.colorScheme
    val shifted = remember(scheme) {
        scheme.copy(
            surfaceContainerLow = scheme.surfaceContainer,
            surfaceContainer = scheme.surfaceContainerHigh,
            surfaceContainerHigh = scheme.surfaceContainerHighest,
        )
    }
    MaterialTheme(colorScheme = shifted, content = content)
}
