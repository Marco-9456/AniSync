package com.anisync.android.presentation.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.data.StreamingService
import com.anisync.android.data.ThemeMode
import com.anisync.android.data.TitleLanguage
import com.anisync.android.presentation.settings.components.ColorPickerSheet
import com.anisync.android.presentation.settings.components.ColorSchemeSelector
import com.anisync.android.presentation.settings.components.PaletteStyleSelector
import com.anisync.android.presentation.settings.components.PhonePreview
import com.anisync.android.ui.theme.PresetPalettes
import com.anisync.android.ui.theme.ThemePalette

/**
 * Look and Feel settings screen.
 * Contains visual theme selector with mini-app previews, title language,
 * streaming service, and haptic feedback settings.
 */
@Composable
fun LookAndFeelScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle()
    val preferredStreamingService by viewModel.preferredStreamingService.collectAsStateWithLifecycle()
    val hapticEnabled by viewModel.hapticEnabled.collectAsStateWithLifecycle()
    
    // Theme palette settings
    val selectedPaletteId by viewModel.selectedPaletteId.collectAsStateWithLifecycle()
    val customSeedColor by viewModel.customSeedColor.collectAsStateWithLifecycle()
    val paletteStyle by viewModel.paletteStyle.collectAsStateWithLifecycle()

    var showTitleLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showStreamingServiceDialog by rememberSaveable { mutableStateOf(false) }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    var showThemeModeDialog by rememberSaveable { mutableStateOf(false) }
    
    // Determine dark mode for preview rendering
    val isDarkMode = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    // Build palette list: filter out Dynamic on pre-Android 12, include custom if set
    val supportseDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val palettes = remember(customSeedColor, supportseDynamicColor) {
        val presets = if (supportseDynamicColor) {
            PresetPalettes.all
        } else {
            PresetPalettes.all.filter { !it.isDynamic }
        }
        if (customSeedColor != null) {
            listOf(ThemePalette("custom", "Custom", customSeedColor!!)) + presets
        } else {
            presets
        }
    }
    
    // Auto-reset orphaned "custom" palette state: if selected palette ID doesn't
    // exist in the current palette list (e.g., custom cleared, or dynamic on pre-12),
    // reset to the first available palette.
    LaunchedEffect(selectedPaletteId, palettes) {
        if (palettes.none { it.id == selectedPaletteId }) {
            viewModel.setSelectedPalette(palettes.firstOrNull()?.id ?: "dynamic")
        }
    }
    
    // Color picker bottom sheet
    if (showColorPicker) {
        ColorPickerSheet(
            currentColor = customSeedColor,
            onColorSelected = { color ->
                viewModel.setCustomSeedColor(color)
                viewModel.setSelectedPalette("custom")
            },
            onDismiss = { showColorPicker = false }
        )
    }

    // Title Language selection dialog
    if (showTitleLanguageDialog) {
        TitleLanguageSelectionDialog(
            currentLanguage = titleLanguage,
            onLanguageSelected = {
                viewModel.setTitleLanguage(it)
                showTitleLanguageDialog = false
            },
            onDismiss = { showTitleLanguageDialog = false }
        )
    }

    // Streaming Service selection dialog
    if (showStreamingServiceDialog) {
        StreamingServiceSelectionDialog(
            currentService = preferredStreamingService,
            onServiceSelected = {
                viewModel.setPreferredStreamingService(it)
                showStreamingServiceDialog = false
            },
            onDismiss = { showStreamingServiceDialog = false }
        )
    }

    // Theme Mode selection dialog
    if (showThemeModeDialog) {
        ThemeModeSelectionDialog(
            currentMode = themeMode,
            onModeSelected = {
                viewModel.setThemeMode(it)
                showThemeModeDialog = false
            },
            onDismiss = { showThemeModeDialog = false }
        )
    }

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_look_and_feel),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        // Note: SettingsScreenScaffold already provides vertical scroll
        
        // =================================================================
        // PREVIEW SECTION
        // =================================================================
        
        Text(
            text = stringResource(R.string.preview),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Get the seed color for the currently selected palette
        val selectedPalette = palettes.find { it.id == selectedPaletteId }
        val seedColor = if (selectedPalette?.isDynamic == true) null else selectedPalette?.seedColor
        
        // Phone mockup preview
        PhonePreview(
            seedColor = seedColor,
            isDarkMode = isDarkMode,
            paletteStyle = paletteStyle,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // =================================================================
        // COLOR SCHEME SECTION
        // =================================================================
        
        Text(
            text = stringResource(R.string.color_scheme),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Color scheme selector with palette circles
        ColorSchemeSelector(
            palettes = palettes,
            selectedPaletteId = selectedPaletteId,
            isDarkMode = isDarkMode,
            paletteStyle = paletteStyle,
            onPaletteSelected = { palette ->
                viewModel.setSelectedPalette(palette.id)
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Custom Color & Palette Style Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Custom Color Button
            OutlinedButton(
                onClick = { showColorPicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = stringResource(R.string.custom_color),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.custom_color))
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Palette Style Selector (disabled when Dynamic palette is selected,
        // since dynamic colors ignore PaletteStyle)
        PaletteStyleSelector(
            selectedStyle = paletteStyle,
            onStyleSelected = { viewModel.setPaletteStyle(it) },
            enabled = selectedPaletteId != "dynamic",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // =================================================================
        // APPEARANCE SECTION
        // =================================================================
        
        SettingsGroup {
            // Theme Mode Selection
            SelectionSettingsItem(
                icon = Icons.Default.DarkMode,
                title = stringResource(R.string.setting_theme),
                currentValue = when (themeMode) {
                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                },
                onClick = { showThemeModeDialog = true }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // =================================================================
        // DISPLAY & CONTENT SECTION
        // =================================================================
        
        SettingsGroup {
            SelectionSettingsItem(
                icon = Icons.Default.Language,
                title = stringResource(R.string.settings_title_language),
                currentValue = getTitleLanguageLabel(titleLanguage),
                onClick = { showTitleLanguageDialog = true }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // =================================================================
        // STREAMING & INTERACTION SECTION
        // =================================================================
        
        SettingsGroup {
            SelectionSettingsItem(
                icon = Icons.Default.PlayCircle,
                title = stringResource(R.string.setting_streaming_service),
                currentValue = preferredStreamingService.displayName,
                onClick = { showStreamingServiceDialog = true }
            )
            SettingsDivider()
            SwitchSettingsItem(
                icon = Icons.Default.Vibration,
                title = stringResource(R.string.setting_haptic_feedback),
                checked = hapticEnabled,
                onCheckedChange = { viewModel.setHapticEnabled(it) }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// =============================================================================
// SELECTION DIALOGS
// =============================================================================

/**
 * Title Language selection dialog.
 */
@Composable
fun TitleLanguageSelectionDialog(
    currentLanguage: TitleLanguage,
    onLanguageSelected: (TitleLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.settings_title_language),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                TitleLanguage.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onLanguageSelected(language) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == language,
                            onClick = { onLanguageSelected(language) }
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = getTitleLanguageLabel(language),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = getTitleLanguageExample(language),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

/**
 * Streaming Service selection dialog with service icons.
 */
@Composable
fun StreamingServiceSelectionDialog(
    currentService: StreamingService,
    onServiceSelected: (StreamingService) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.setting_streaming_service),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.setting_streaming_service_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                StreamingService.entries.forEach { service ->
                    val brandColor = remember(service) {
                        try {
                            Color(android.graphics.Color.parseColor(service.brandColor))
                        } catch (e: Exception) {
                            null
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onServiceSelected(service) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentService == service,
                            onClick = { onServiceSelected(service) }
                        )
                        Spacer(Modifier.width(12.dp))

                        if (service.iconUrl != null) {
                            AsyncImage(
                                model = service.iconUrl,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                contentScale = ContentScale.Fit,
                                colorFilter = brandColor?.let { ColorFilter.tint(it) }
                            )
                        } else {
                            Icon(
                                Icons.Default.PlayCircle,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Text(
                            text = service.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

/**
 * Theme Mode selection dialog.
 */
@Composable
fun ThemeModeSelectionDialog(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.theme_mode_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = { onModeSelected(mode) }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = when (mode) {
                                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

@Composable
private fun getTitleLanguageLabel(language: TitleLanguage): String {
    return when (language) {
        TitleLanguage.ROMAJI -> stringResource(R.string.title_language_romaji)
        TitleLanguage.ENGLISH -> stringResource(R.string.title_language_english)
        TitleLanguage.NATIVE -> stringResource(R.string.title_language_native)
    }
}

@Composable
private fun getTitleLanguageExample(language: TitleLanguage): String {
    return when (language) {
        TitleLanguage.ROMAJI -> stringResource(R.string.title_language_romaji_example)
        TitleLanguage.ENGLISH -> stringResource(R.string.title_language_english_example)
        TitleLanguage.NATIVE -> stringResource(R.string.title_language_native_example)
    }
}
