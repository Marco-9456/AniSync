package com.anisync.android.presentation.settings

import android.os.Build
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.data.ThemeMode
import com.anisync.android.presentation.settings.components.ColorPickerSheet
import com.anisync.android.presentation.settings.components.ColorSchemeSelector
import com.anisync.android.presentation.settings.components.PaletteStyleSelector
import com.anisync.android.presentation.settings.components.PhonePreview
import com.anisync.android.ui.theme.PresetPalettes
import com.anisync.android.ui.theme.ThemePalette
import com.anisync.android.ui.theme.resolveDarkTheme

/**
 * Theme subscreen — the app's full theming hub: live preview, color scheme + palette style, custom
 * seed color, light/dark/system mode, AMOLED toggle, and the "use profile colors" preference.
 */
@Composable
fun ThemeScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode = uiState.themeMode
    val amoledEnabled = uiState.amoledEnabled
    val respectUserProfileColors = uiState.respectUserProfileColors
    val selectedPaletteId = uiState.selectedPaletteId
    val customSeedColor = uiState.customSeedColor
    val paletteStyle = uiState.paletteStyle

    val isDarkMode = themeMode.resolveDarkTheme()

    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val palettes = remember(customSeedColor, supportsDynamicColor) {
        val presets = if (supportsDynamicColor) {
            PresetPalettes.all
        } else {
            // Pre-Android-12 "dynamic" resolves to the static brand scheme; keep it selectable as
            // "Default" — hiding it made the fallback effect below rewrite the persisted setting.
            listOf(PresetPalettes.Dynamic.copy(name = "Default")) +
                PresetPalettes.all.filter { !it.isDynamic }
        }
        if (customSeedColor != null) {
            listOf(ThemePalette("custom", "Custom", customSeedColor!!)) + presets
        } else {
            presets
        }
    }

    LaunchedEffect(selectedPaletteId, palettes, uiState.isLoaded) {
        if (uiState.isLoaded && palettes.none { it.id == selectedPaletteId }) {
            viewModel.onAction(SettingsAction.SetSelectedPalette(palettes.firstOrNull()?.id ?: "dynamic"))
        }
    }

    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    if (showColorPicker) {
        ColorPickerSheet(
            currentColor = customSeedColor,
            onColorSelected = { color ->
                viewModel.onAction(SettingsAction.SetCustomSeedColor(color))
                viewModel.onAction(SettingsAction.SetSelectedPalette("custom"))
            },
            onDismiss = { showColorPicker = false }
        )
    }

    SettingsScreenScaffold(
        title = stringResource(R.string.setting_theme),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        SettingsSectionLabel(stringResource(R.string.preview))

        val selectedPalette = remember(palettes, selectedPaletteId) {
            palettes.find { it.id == selectedPaletteId }
        }
        val seedColor = if (selectedPalette?.isDynamic == true) null else selectedPalette?.seedColor

        if (uiState.isLoaded) {
            PhonePreview(
                seedColor = seedColor,
                isDarkMode = isDarkMode,
                paletteStyle = paletteStyle,
                amoled = amoledEnabled
            )
        }

        SettingsSectionLabel(stringResource(R.string.color_scheme))

        ColorSchemeSelector(
            palettes = palettes,
            selectedPaletteId = selectedPaletteId,
            isDarkMode = isDarkMode,
            paletteStyle = paletteStyle,
            onPaletteSelected = { viewModel.onAction(SettingsAction.SetSelectedPalette(it.id)) }
        )

        PaletteStyleSelector(
            selectedStyle = paletteStyle,
            onStyleSelected = { viewModel.onAction(SettingsAction.SetPaletteStyle(it)) },
            enabled = selectedPaletteId != "dynamic",
            modifier = Modifier.fillMaxWidth()
        )

        SettingsGroup {
            SettingsItem(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.custom_color),
                subtitle = stringResource(R.string.custom_color_description),
                onClick = { showColorPicker = true }
            )
        }

        SettingsGroup(modifier = Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                RadioSettingsItem(
                    title = stringResource(mode.labelRes()),
                    subtitle = stringResource(mode.subtitleRes()),
                    selected = themeMode == mode,
                    onClick = { viewModel.onAction(SettingsAction.SetThemeMode(mode)) }
                )
            }
        }

        SettingsGroup {
            SwitchSettingsItem(
                icon = Icons.Default.Contrast,
                title = stringResource(R.string.setting_amoled),
                subtitle = stringResource(R.string.setting_amoled_desc),
                checked = amoledEnabled,
                onCheckedChange = { viewModel.onAction(SettingsAction.SetAmoledEnabled(it)) }
            )
        }

        SettingsGroup {
            SwitchSettingsItem(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.setting_respect_profile_colors),
                subtitle = stringResource(R.string.setting_respect_profile_colors_desc),
                checked = respectUserProfileColors,
                onCheckedChange = {
                    viewModel.onAction(SettingsAction.SetRespectUserProfileColors(it))
                }
            )
        }
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
    ThemeMode.SYSTEM -> R.string.theme_system
}

private fun ThemeMode.subtitleRes(): Int = when (this) {
    ThemeMode.LIGHT -> R.string.theme_light_desc
    ThemeMode.DARK -> R.string.theme_dark_desc
    ThemeMode.SYSTEM -> R.string.theme_system_desc
}
