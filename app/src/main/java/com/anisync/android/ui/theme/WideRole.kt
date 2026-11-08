package com.anisync.android.ui.theme

import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.layout
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
    private val drawsWidth: Boolean = WidthAxisDrawn,
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
                preset = preset.copy(opticalSize = opticalSize.toFloat(), width = preset.width.takeIf { drawsWidth }),
                weights = listOf(weight.weight),
                overrides = if (drawsWidth) overrides else overrides.copy(width = null),
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

/**
 * Lays wide text out in the box Figma gives it. Figma centres the font's ascent and descent in the
 * line height, while Android grows a line to them, so an 80/84 hero is 100dp tall on a device and
 * sits 8dp low. This keeps every line at [style]'s line height with Figma's baseline.
 */
fun Modifier.lineBox(style: TextStyle): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val first = placeable[FirstBaseline]
    val last = placeable[LastBaseline]
    if (first == AlignmentLine.Unspecified || !style.lineHeight.isSp || !style.fontSize.isSp) {
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    val lineHeight = style.lineHeight.toPx()
    val fontSize = style.fontSize.toPx()
    val baseline = (lineHeight - (FontAscent + FontDescent) * fontSize) / 2f + FontAscent * fontSize
    val height = (last - first + lineHeight).roundToInt()
    layout(placeable.width, height) { placeable.place(0, (baseline - first).roundToInt()) }
}

/**
 * Android 8.0 and 8.1 space glyphs by the width axis but draw them without it, so wide lines
 * spread their letters apart and get clipped. Those releases keep the regular width.
 */
private val WidthAxisDrawn = Build.VERSION.SDK_INT !in Build.VERSION_CODES.O..Build.VERSION_CODES.O_MR1

/** Google Sans Flex's hhea ascent and descent, 1932 and 572 on a 2000 unit em. */
private const val FontAscent = 0.966f
private const val FontDescent = 0.286f
