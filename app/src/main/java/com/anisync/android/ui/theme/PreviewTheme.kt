package com.anisync.android.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

/**
 * A lightweight theme wrapper for rendering preview content with a specific color scheme.
 * 
 * Unlike [AppTheme], this composable:
 * - Does NOT modify status bar appearance
 * - Does NOT apply side effects
 * - Is safe to use inside preview cards and non-interactive contexts
 * 
 * @param seedColor The seed color to generate the color scheme from
 * @param isDark Whether to generate a dark or light color scheme
 * @param style The palette style to use for color generation
 * @param amoled Whether to use pure-black backgrounds (only applied when [isDark] is true)
 * @param content The content to render with this theme
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreviewTheme(
    seedColor: Color,
    isDark: Boolean,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    amoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val base = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        isAmoled = false,
        style = style
    )
    // Match AppTheme: apply our own cohesive AMOLED transform rather than MaterialKolor's isAmoled.
    val colorScheme = if (isDark && amoled) base.toAmoled() else base

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}

/**
 * Overload that accepts a pre-computed ColorScheme.
 * 
 * Use this when you have already generated a ColorScheme and want to
 * apply it to preview content without regenerating it.
 * 
 * @param colorScheme The pre-computed color scheme to apply
 * @param content The content to render with this theme
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreviewTheme(
    colorScheme: ColorScheme,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
