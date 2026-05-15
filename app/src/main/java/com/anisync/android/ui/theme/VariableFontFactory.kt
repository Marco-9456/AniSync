package com.anisync.android.ui.theme

import androidx.annotation.FontRes
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * The single point that turns variable-font [FontAxes] into Compose [Font] / [FontFamily]
 * instances. Replaces the former ad-hoc private `robotoFlex(weight, opticalSize)` helper —
 * all variable-font instantiation in the app now flows through here.
 */
object VariableFontFactory {

    /** A single [Font] for [resId] at the given [axes], validated against [registry]. */
    @OptIn(ExperimentalTextApi::class)
    fun font(
        @FontRes resId: Int,
        axes: FontAxes,
        registry: VariableFontAxes = GoogleSansFlexAxes,
    ): Font = Font(
        resId = resId,
        weight = FontWeight(registry.weight.clamp(axes.weight).toInt()),
        variationSettings = axes.toVariationSettings(registry),
    )

    /**
     * A [FontFamily] covering [weights], every member sharing [axes] except its own weight.
     * Compose resolves the nearest member for a requested [FontWeight] at render time.
     */
    fun family(
        @FontRes resId: Int,
        axes: FontAxes,
        weights: List<Int>,
        registry: VariableFontAxes = GoogleSansFlexAxes,
    ): FontFamily = FontFamily(
        weights.map { w -> font(resId, axes.copy(weight = w.toFloat()), registry) },
    )
}
