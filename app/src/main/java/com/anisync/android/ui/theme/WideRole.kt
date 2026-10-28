package com.anisync.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * One of the wide expressive roles, heroWide or titleWide. The axes never change and each use
 * picks its own size, line height and tracking from the frame it implements.
 *
 * Figma sets the optical size to the font size, and the width of a line follows opsz, so every
 * size gets a family with opsz at that size. The families are cached per size.
 */
@Immutable
class WideRole internal constructor(
    private val preset: FontAxes,
    private val overrides: FontAxisOverrides,
) {
    private val families = ConcurrentHashMap<Int, FontFamily>()

    val weight: FontWeight = FontWeight((overrides.weight ?: preset.weight).roundToInt())

    fun style(
        fontSize: TextUnit,
        lineHeight: TextUnit,
        letterSpacing: TextUnit = 0.sp,
    ): TextStyle {
        val opticalSize = fontSize.value.roundToInt().coerceIn(MinOpticalSize, MaxOpticalSize)
        val family = families.getOrPut(opticalSize) {
            roleFamily(
                preset = preset.copy(opticalSize = opticalSize.toFloat()),
                weights = listOf(weight.weight),
                overrides = overrides,
            )
        }
        return TextStyle(
            fontFamily = family,
            fontWeight = weight,
            fontSize = fontSize,
            lineHeight = lineHeight,
            letterSpacing = letterSpacing,
            lineHeightStyle = DefaultLineHeightStyle,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        )
    }

    private companion object {
        const val MinOpticalSize = 8
        const val MaxOpticalSize = 144
    }
}
