package com.anisync.android.presentation.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.menu.MenuField
import com.anisync.android.ui.theme.toDescription
import com.anisync.android.ui.theme.toDisplayName
import com.materialkolor.PaletteStyle

/**
 * Dropdown selector for PaletteStyle options.
 *
 * Allows users to choose how colors are distributed in the generated palette,
 * affecting the overall mood and vibrancy of the theme.
 *
 * @param selectedStyle The currently selected palette style
 * @param onStyleSelected Callback when a style is selected
 * @param modifier Modifier for the dropdown container
 */
@Composable
fun PaletteStyleSelector(
    selectedStyle: PaletteStyle,
    onStyleSelected: (PaletteStyle) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val styles = remember { PaletteStyle.entries }

    MenuField(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        selectedLabel = selectedStyle.toDisplayName(),
        label = stringResource(R.string.palette_style),
        fieldSupportingText = selectedStyle.toDescription(),
        enabled = enabled,
        modifier = modifier
    ) {
        styles.forEach { style ->
            item(
                text = style.toDisplayName(),
                supportingText = style.toDescription(),
                selected = style == selectedStyle,
                onClick = {
                    onStyleSelected(style)
                    expanded = false
                }
            )
        }
    }
}

// =============================================================================
// PREVIEWS
// =============================================================================

@Preview(showBackground = true)
@Composable
private fun PaletteStyleSelectorPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PaletteStyleSelector(
                selectedStyle = PaletteStyle.TonalSpot,
                onStyleSelected = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PaletteStyleSelectorVibrantPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PaletteStyleSelector(
                selectedStyle = PaletteStyle.Vibrant,
                onStyleSelected = {}
            )
        }
    }
}
