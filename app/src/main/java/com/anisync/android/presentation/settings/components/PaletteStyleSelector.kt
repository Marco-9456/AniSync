package com.anisync.android.presentation.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteStyleSelector(
    selectedStyle: PaletteStyle,
    onStyleSelected: (PaletteStyle) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val styles = remember { PaletteStyle.entries }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedStyle.toDisplayName(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.palette_style)) },
            supportingText = {
                Text(
                    text = selectedStyle.toDescription(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            styles.forEach { style ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = style.toDisplayName(),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = style.toDescription(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onStyleSelected(style)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
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