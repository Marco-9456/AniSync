package com.anisync.android.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.ui.theme.PresetPalettes
import com.anisync.android.ui.theme.ThemePalette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

/**
 * Horizontal scrollable color scheme selector.
 * 
 * Displays available color palettes as circular icons with 4 color segments,
 * allowing users to select their preferred theme color scheme.
 */
@Composable
fun ColorSchemeSelector(
    palettes: List<ThemePalette>,
    selectedPaletteId: String,
    isDarkMode: Boolean,
    paletteStyle: PaletteStyle,
    onPaletteSelected: (ThemePalette) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = palettes,
            key = { it.id }
        ) { palette ->
            ColorSchemeItem(
                palette = palette,
                isSelected = palette.id == selectedPaletteId,
                isDarkMode = isDarkMode,
                paletteStyle = paletteStyle,
                onClick = { onPaletteSelected(palette) }
            )
        }
    }
}

/**
 * Individual color scheme item with a 4-segment circle and label.
 */
@Composable
private fun ColorSchemeItem(
    palette: ThemePalette,
    isSelected: Boolean,
    isDarkMode: Boolean,
    paletteStyle: PaletteStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Generate the color scheme for this palette to extract the 4 colors
    val paletteColors = remember(palette, isDarkMode, paletteStyle) {
        if (palette.isDynamic) {
            // For dynamic, use a neutral color scheme preview
            PaletteColors(
                topLeft = Color(0xFFE8DEF8),
                topRight = Color(0xFFF3EDF7),
                bottomLeft = Color(0xFFD0BCFF),
                bottomRight = Color(0xFFCCC2DC)
            )
        } else {
            try {
                val colorScheme = dynamicColorScheme(
                    seedColor = palette.seedColor,
                    isDark = isDarkMode,
                    isAmoled = false,
                    style = paletteStyle
                )
                PaletteColors(
                    topLeft = colorScheme.primaryContainer,
                    topRight = colorScheme.secondaryContainer,
                    bottomLeft = colorScheme.primary,
                    bottomRight = colorScheme.tertiary
                )
            } catch (e: Exception) {
                // Fallback colors
                PaletteColors(
                    topLeft = palette.seedColor.copy(alpha = 0.3f),
                    topRight = palette.seedColor.copy(alpha = 0.5f),
                    bottomLeft = palette.seedColor,
                    bottomRight = palette.seedColor.copy(alpha = 0.7f)
                )
            }
        }
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Circular palette indicator inside a rounded square container
        Surface(
            modifier = Modifier
                .size(64.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else {
                        Modifier
                    }
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
                // 4-segment circle
                FourSegmentCircle(
                    colors = paletteColors,
                    modifier = Modifier.size(48.dp)
                )
                
                // Checkmark overlay for selected item
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
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        // Palette name label
        Text(
            text = palette.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/**
 * Data class for the 4 colors in the palette preview circle.
 */
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
                val halfWidth = size.width / 2
                val halfHeight = size.height / 2
                
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

// =============================================================================
// PREVIEWS
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun ColorSchemeSelectorDarkPreview() {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
        ColorSchemeSelector(
            palettes = PresetPalettes.all.take(5),
            selectedPaletteId = "dynamic",
            isDarkMode = true,
            paletteStyle = PaletteStyle.TonalSpot,
            onPaletteSelected = {},
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ColorSchemeSelectorLightPreview() {
    MaterialTheme {
        ColorSchemeSelector(
            palettes = PresetPalettes.all.take(5),
            selectedPaletteId = "pink",
            isDarkMode = false,
            paletteStyle = PaletteStyle.TonalSpot,
            onPaletteSelected = {},
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}
