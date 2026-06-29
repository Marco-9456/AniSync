package com.anisync.android.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.util.bouncyClickable
import kotlin.math.abs

/**
 * Quick pick colors for the color picker.
 * These are popular seed colors that generate nice M3 palettes.
 */
private val QuickPickColors = listOf(
    Color(0xFFF44336), // Red
    Color(0xFFE91E63), // Pink
    Color(0xFF9C27B0), // Purple
    Color(0xFF673AB7), // Deep Purple
    Color(0xFF3F51B5), // Indigo
    Color(0xFF2196F3), // Blue
    Color(0xFF03A9F4), // Light Blue
    Color(0xFF00BCD4), // Cyan
    Color(0xFF009688), // Teal
    Color(0xFF4CAF50), // Green
    Color(0xFF8BC34A), // Light Green
    Color(0xFFCDDC39), // Lime
    Color(0xFFFFEB3B), // Yellow
    Color(0xFFFFC107), // Amber
    Color(0xFFFF9800), // Orange
    Color(0xFFFF5722), // Deep Orange
)

/**
 * Helper class to cache pre-calculated hue for quick pick colors.
 * Eliminates repeated math during slider drag.
 */
@Immutable
private data class ColorCandidate(val color: Color, val hue: Float)

/**
 * Bottom sheet for custom color selection.
 *
 * Provides two ways to pick a color:
 * 1. Quick pick from preset colors
 * 2. Hue slider for fine-tuned selection
 *
 * @param currentColor The currently selected custom color (if any)
 * @param onColorSelected Callback when a color is confirmed
 * @param onDismiss Callback when the sheet is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    currentColor: Color?,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Track the current hue (0-360), survives configuration changes
    var hue by rememberSaveable {
        mutableFloatStateOf(
            currentColor?.let { extractHue(it) } ?: 0f
        )
    }

    val quickPickCandidates = remember {
        QuickPickColors.map { ColorCandidate(it, extractHue(it)) }
    }

    // Saturation and lightness are fixed for consistent, vibrant colors
    val saturation = 0.7f
    val lightness = 0.5f

    // The color derived from current slider position
    val selectedColor = remember(hue) {
        Color.hsl(hue, saturation, lightness)
    }

    val rainbowBrush = remember {
        Brush.horizontalGradient(
            colors = (0..360 step 30).map { h ->
                Color.hsl(h.toFloat(), 0.7f, 0.5f)
            }
        )
    }

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.custom_color),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.custom_color_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Quick pick colors
            Text(
                text = stringResource(R.string.quick_pick),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = quickPickCandidates,
                    key = { it.color.toArgb() }
                ) { candidate ->
                    val isSelected = remember(candidate, hue) {
                        // Compare hues (within 5 degrees tolerance)
                        val diff = abs(candidate.hue - hue)
                        diff < 5f || diff > 355f
                    }

                    ColorSwatch(
                        color = candidate.color,
                        isSelected = isSelected,
                        onClick = {
                            hue = candidate.hue
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hue slider
            Text(
                text = stringResource(R.string.hue_slider),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rainbow gradient background for slider track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(rainbowBrush),
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = selectedColor,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Preview swatch
            Text(
                text = stringResource(R.string.preview),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(selectedColor)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(16.dp)
                    )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onColorSelected(selectedColor)
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.apply))
                }
            }
        }
    }
}

/**
 * A circular color swatch for quick selection.
 */
@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val borderWidth = if (isSelected) 3.dp else 1.dp

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = CircleShape
            )
            .bouncyClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = stringResource(R.string.custom_color)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.a11y_settings_selected),
                tint = if (isLightColor(color)) Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Extract hue (0-360) from a Color.
 */
private fun extractHue(color: Color): Float {
    val r = color.red
    val g = color.green
    val b = color.blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    if (delta == 0f) return 0f

    val hue = when (max) {
        r -> 60f * (((g - b) / delta) % 6f)
        g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }

    return if (hue < 0) hue + 360f else hue
}

/**
 * Determine if a color is "light" (for choosing icon tint).
 */
private fun isLightColor(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance > 0.5
}

// =============================================================================
// PREVIEWS
// =============================================================================

@Preview(showBackground = true)
@Composable
private fun ColorSwatchPreview() {
    MaterialTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            ColorSwatch(
                color = Color(0xFFE91E63),
                isSelected = true,
                onClick = {}
            )
            ColorSwatch(
                color = Color(0xFF2196F3),
                isSelected = false,
                onClick = {}
            )
        }
    }
}
