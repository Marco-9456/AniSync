package com.anisync.android.presentation.settings.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.ui.theme.ThemePalette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme

/**
 * The color palettes as a wrapping grid of the four-segment swatches, followed by a Custom tile.
 *
 * Each row holds as many 64dp tiles as fit with at least 8dp between them, and the leftover width is
 * shared out as the gap so every full row lines up with both edges.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSchemeGrid(
    palettes: List<ThemePalette>,
    selectedPaletteId: String,
    isDarkMode: Boolean,
    paletteStyle: PaletteStyle,
    customSeedColor: Color?,
    onPaletteSelected: (ThemePalette) -> Unit,
    onCustomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val systemScheme = remember(isDarkMode, context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDarkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            null
        }
    }
    val custom = remember(customSeedColor) {
        customSeedColor?.let { ThemePalette(CUSTOM_PALETTE_ID, "Custom", it) }
    }
    val customColors = remember(custom, isDarkMode, paletteStyle, systemScheme) {
        custom?.let { resolvePaletteColors(it, isDarkMode, paletteStyle, systemScheme) }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val perRow = ((maxWidth + MinSwatchGap) / (SwatchSize + MinSwatchGap)).toInt().coerceAtLeast(1)
        val gap = if (perRow > 1) (maxWidth - SwatchSize * perRow) / (perRow - 1) else 0.dp
        FlowRow(
            maxItemsInEachRow = perRow,
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            palettes.forEach { palette ->
                val colors = remember(palette, isDarkMode, paletteStyle, systemScheme) {
                    resolvePaletteColors(palette, isDarkMode, paletteStyle, systemScheme)
                }
                ColorSchemeItem(
                    label = palette.name,
                    colors = colors,
                    isSelected = palette.id == selectedPaletteId,
                    onClick = { onPaletteSelected(palette) }
                )
            }
            ColorSchemeItem(
                label = stringResource(R.string.color_custom),
                colors = customColors,
                isSelected = selectedPaletteId == CUSTOM_PALETTE_ID,
                onClick = onCustomClick
            )
        }
    }
}

/** The same swatches in one horizontally scrolling row, for onboarding where height is scarce. */
@Composable
fun ColorSchemeSelector(
    palettes: List<ThemePalette>,
    selectedPaletteId: String,
    isDarkMode: Boolean,
    paletteStyle: PaletteStyle,
    onPaletteSelected: (ThemePalette) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val systemScheme = remember(isDarkMode, context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDarkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            null
        }
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items = palettes, key = { it.id }) { palette ->
            val colors = remember(palette, isDarkMode, paletteStyle, systemScheme) {
                resolvePaletteColors(palette, isDarkMode, paletteStyle, systemScheme)
            }
            ColorSchemeItem(
                label = palette.name,
                colors = colors,
                isSelected = palette.id == selectedPaletteId,
                onClick = { onPaletteSelected(palette) }
            )
        }
    }
}

/** Id the custom seed color is stored under as the selected palette. */
const val CUSTOM_PALETTE_ID = "custom"

private val SwatchSize = 64.dp
private val MinSwatchGap = 8.dp

/**
 * A 64dp rounded-square tile holding the four-segment circle and its label. With no [colors] it is
 * the empty Custom tile, a dashed ring around a plus.
 */
@Composable
private fun ColorSchemeItem(
    label: String,
    colors: PaletteColors?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(SwatchSize),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(SwatchSize)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else Modifier
                )
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier.padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (colors != null) {
                    FourSegmentCircle(colors = colors, modifier = Modifier.size(48.dp))
                } else {
                    val outline = MaterialTheme.colorScheme.outline
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .drawBehind {
                                drawCircle(
                                    color = outline,
                                    style = Stroke(
                                        width = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(4.dp.toPx(), 4.dp.toPx())
                                        )
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.a11y_settings_selected),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/**
 * Helper to resolve the preview colors. Extracted to keep the Composable clean.
 */
private fun resolvePaletteColors(
    palette: ThemePalette,
    isDarkMode: Boolean,
    paletteStyle: PaletteStyle,
    systemScheme: ColorScheme?
): PaletteColors {
    if (palette.isDynamic) {
        // Use system colors if available (Android 12+)
        if (systemScheme != null) {
            return PaletteColors(
                topLeft = systemScheme.primaryContainer,
                topRight = systemScheme.secondaryContainer,
                bottomLeft = systemScheme.primary,
                bottomRight = systemScheme.tertiary
            )
        } else if (isDarkMode) {
            // Fallback Dark
            return PaletteColors(
                topLeft = Color(0xFF4F378B),
                topRight = Color(0xFF332D41),
                bottomLeft = Color(0xFFD0BCFF),
                bottomRight = Color(0xFFCCC2DC)
            )
        } else {
            // Fallback Light
            return PaletteColors(
                topLeft = Color(0xFFE8DEF8),
                topRight = Color(0xFFF3EDF7),
                bottomLeft = Color(0xFF6750A4),
                bottomRight = Color(0xFF7D5260)
            )
        }
    } else {
        // Generate from seed using Material Kolor
        return try {
            val colorScheme = dynamicColorScheme(
                seedColor = palette.seedColor,
                isDark = isDarkMode,
                isAmoled = false,
                style = paletteStyle,
                specVersion = ColorSpec.SpecVersion.SPEC_2026
            )
            PaletteColors(
                topLeft = colorScheme.primaryContainer,
                topRight = colorScheme.secondaryContainer,
                bottomLeft = colorScheme.primary,
                bottomRight = colorScheme.tertiary
            )
        } catch (e: Exception) {
            // Safety fallback
            PaletteColors(
                topLeft = palette.seedColor.copy(alpha = 0.3f),
                topRight = palette.seedColor.copy(alpha = 0.5f),
                bottomLeft = palette.seedColor,
                bottomRight = palette.seedColor.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Data class for the 4 colors in the palette preview circle.
 */
@Immutable
private data class PaletteColors(
    val topLeft: Color,
    val topRight: Color,
    val bottomLeft: Color,
    val bottomRight: Color
)

/**
 * Draws a circle with 4 color segments (quadrants).
 */
@Composable
private fun FourSegmentCircle(
    colors: PaletteColors,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .drawBehind {
                // Top-left quadrant
                drawArc(
                    color = colors.topLeft,
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = size
                )

                // Top-right quadrant
                drawArc(
                    color = colors.topRight,
                    startAngle = 270f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = size
                )

                // Bottom-right quadrant
                drawArc(
                    color = colors.bottomRight,
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = size
                )

                // Bottom-left quadrant
                drawArc(
                    color = colors.bottomLeft,
                    startAngle = 90f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = size
                )
            }
    )
}
