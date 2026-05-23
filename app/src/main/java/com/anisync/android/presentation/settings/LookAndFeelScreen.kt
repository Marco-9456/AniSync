package com.anisync.android.presentation.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.data.AppLocale
import com.anisync.android.data.AppSettings
import com.anisync.android.data.CoverQuality
import com.anisync.android.data.NavBarStyle
import com.anisync.android.data.StreamingService
import com.anisync.android.data.ThemeMode
import com.anisync.android.data.TitleLanguage
import com.anisync.android.presentation.settings.components.ColorPickerSheet
import com.anisync.android.presentation.settings.components.ColorSchemeSelector
import com.anisync.android.presentation.settings.components.PaletteStyleSelector
import com.anisync.android.presentation.settings.components.PhonePreview
import com.anisync.android.ui.theme.PresetPalettes
import com.anisync.android.ui.theme.ThemePalette

private val TitleLanguages = TitleLanguage.entries
private val StreamingServices = StreamingService.entries
private val ThemeModes = ThemeMode.entries
private val AppLocales = AppLocale.entries
private val CoverQualities = CoverQuality.entries
private val NavBarStyles = NavBarStyle.entries
private val AvatarShapes = com.anisync.android.data.AvatarShape.entries

@Composable
fun LookAndFeelScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode = uiState.themeMode
    val titleLanguage = uiState.titleLanguage
    val preferredStreamingService = uiState.preferredStreamingService
    val hapticEnabled = uiState.hapticEnabled
    val showAdultContent = uiState.showAdultContent
    val appLocale = uiState.appLocale
    val coverQuality = uiState.coverQuality
    val navBarStyle = uiState.navBarStyle
    val navBarShowLabels = uiState.navBarShowLabels
    val navBarCornerRadius = uiState.navBarCornerRadius
    val avatarShape = uiState.avatarShape
    val avatarBackgroundEnabled = uiState.avatarBackgroundEnabled

    val selectedPaletteId = uiState.selectedPaletteId
    val customSeedColor = uiState.customSeedColor
    val paletteStyle = uiState.paletteStyle

    var showTitleLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showStreamingServiceDialog by rememberSaveable { mutableStateOf(false) }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    var showThemeModeDialog by rememberSaveable { mutableStateOf(false) }
    var showAppLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showCoverQualityDialog by rememberSaveable { mutableStateOf(false) }
    var showNavBarStyleDialog by rememberSaveable { mutableStateOf(false) }
    var showAvatarShapeDialog by rememberSaveable { mutableStateOf(false) }

    val isSystemDark = isSystemInDarkTheme()
    val isDarkMode = remember(themeMode, isSystemDark) {
        when (themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> isSystemDark
        }
    }

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

    LaunchedEffect(selectedPaletteId, palettes, uiState.isLoaded) {
        if (uiState.isLoaded && palettes.none { it.id == selectedPaletteId }) {
            viewModel.onAction(SettingsAction.SetSelectedPalette(palettes.firstOrNull()?.id ?: "dynamic"))
        }
    }

    val onColorSelected = remember(viewModel) {
        { color: Color ->
            viewModel.onAction(SettingsAction.SetCustomSeedColor(color))
            viewModel.onAction(SettingsAction.SetSelectedPalette("custom"))
        }
    }
    val onPaletteSelected = remember(viewModel) {
        { palette: ThemePalette -> viewModel.onAction(SettingsAction.SetSelectedPalette(palette.id)) }
    }
    val onStyleSelected = remember(viewModel) {
        { style: com.materialkolor.PaletteStyle -> viewModel.onAction(SettingsAction.SetPaletteStyle(style)) }
    }

    if (showColorPicker) {
        ColorPickerSheet(
            currentColor = customSeedColor,
            onColorSelected = onColorSelected,
            onDismiss = { showColorPicker = false }
        )
    }

    if (showTitleLanguageDialog) {
        TitleLanguageSelectionDialog(
            currentLanguage = titleLanguage,
            onLanguageSelected = {
                viewModel.onAction(SettingsAction.SetTitleLanguage(it))
                showTitleLanguageDialog = false
            },
            onDismiss = { showTitleLanguageDialog = false }
        )
    }

    if (showStreamingServiceDialog) {
        StreamingServiceSelectionDialog(
            currentService = preferredStreamingService,
            onServiceSelected = {
                viewModel.onAction(SettingsAction.SetPreferredStreamingService(it))
                showStreamingServiceDialog = false
            },
            onDismiss = { showStreamingServiceDialog = false }
        )
    }

    if (showThemeModeDialog) {
        ThemeModeSelectionDialog(
            currentMode = themeMode,
            onModeSelected = {
                viewModel.onAction(SettingsAction.SetThemeMode(it))
                showThemeModeDialog = false
            },
            onDismiss = { showThemeModeDialog = false }
        )
    }

    if (showAppLanguageDialog) {
        AppLanguageSelectionDialog(
            currentLocale = appLocale,
            onLocaleSelected = {
                viewModel.onAction(SettingsAction.SetAppLocale(it))
                showAppLanguageDialog = false
            },
            onDismiss = { showAppLanguageDialog = false }
        )
    }

    if (showCoverQualityDialog) {
        CoverQualitySelectionDialog(
            currentQuality = coverQuality,
            onQualitySelected = {
                viewModel.onAction(SettingsAction.SetCoverQuality(it))
                showCoverQualityDialog = false
            },
            onDismiss = { showCoverQualityDialog = false }
        )
    }

    if (showNavBarStyleDialog) {
        NavBarStyleSelectionDialog(
            currentStyle = navBarStyle,
            onStyleSelected = {
                viewModel.onAction(SettingsAction.SetNavBarStyle(it))
                showNavBarStyleDialog = false
            },
            onDismiss = { showNavBarStyleDialog = false }
        )
    }

    if (showAvatarShapeDialog) {
        AvatarShapeSelectionDialog(
            currentShape = avatarShape,
            backgroundEnabled = avatarBackgroundEnabled,
            onShapeSelected = {
                viewModel.onAction(SettingsAction.SetAvatarShape(it))
            },
            onBackgroundEnabledChange = {
                viewModel.onAction(SettingsAction.SetAvatarBackgroundEnabled(it))
            },
            onDismiss = { showAvatarShapeDialog = false }
        )
    }

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_look_and_feel),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.preview),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        val selectedPalette = remember(palettes, selectedPaletteId) {
            palettes.find { it.id == selectedPaletteId }
        }
        val seedColor = if (selectedPalette?.isDynamic == true) null else selectedPalette?.seedColor

        if (uiState.isLoaded) {
            PhonePreview(
                seedColor = seedColor,
                isDarkMode = isDarkMode,
                paletteStyle = paletteStyle,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.color_scheme),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        ColorSchemeSelector(
            palettes = palettes,
            selectedPaletteId = selectedPaletteId,
            isDarkMode = isDarkMode,
            paletteStyle = paletteStyle,
            onPaletteSelected = onPaletteSelected
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

        PaletteStyleSelector(
            selectedStyle = paletteStyle,
            onStyleSelected = onStyleSelected,
            enabled = selectedPaletteId != "dynamic",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsGroup {
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

        SettingsGroup {
            SelectionSettingsItem(
                icon = Icons.Default.Translate,
                title = stringResource(R.string.settings_app_language),
                currentValue = appLocale.displayName,
                onClick = { showAppLanguageDialog = true }
            )
            SettingsDivider()
            SelectionSettingsItem(
                icon = Icons.Default.Language,
                title = stringResource(R.string.settings_title_language),
                currentValue = getTitleLanguageLabel(titleLanguage),
                onClick = { showTitleLanguageDialog = true }
            )
            SettingsDivider()
            SelectionSettingsItem(
                icon = Icons.Default.HighQuality,
                title = stringResource(R.string.settings_cover_quality),
                currentValue = coverQualityLabel(coverQuality),
                onClick = { showCoverQualityDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup {
            SelectionSettingsItem(
                icon = Icons.Default.Navigation,
                title = stringResource(R.string.setting_nav_bar_style),
                currentValue = navBarStyleLabel(navBarStyle),
                onClick = { showNavBarStyleDialog = true }
            )
            SettingsDivider()
            SwitchSettingsItem(
                icon = Icons.AutoMirrored.Filled.Label,
                title = stringResource(R.string.setting_nav_bar_show_labels),
                checked = navBarShowLabels,
                onCheckedChange = {
                    viewModel.onAction(SettingsAction.SetNavBarShowLabels(it))
                }
            )
            SettingsDivider()
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                NavBarCornerRadiusSlider(
                    value = navBarCornerRadius,
                    onValueChange = {
                        viewModel.onAction(SettingsAction.SetNavBarCornerRadius(it))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup {
            SelectionSettingsItem(
                icon = Icons.Default.Face,
                title = stringResource(R.string.setting_avatar_shape),
                currentValue = avatarShapeLabel(avatarShape),
                onClick = { showAvatarShapeDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                onCheckedChange = { viewModel.onAction(SettingsAction.SetHapticEnabled(it)) }
            )
            SettingsDivider()
            SwitchSettingsItem(
                icon = Icons.Default.Visibility,
                title = "Show adult content",
                checked = showAdultContent,
                onCheckedChange = { viewModel.onAction(SettingsAction.SetShowAdultContent(it)) }
            )
        }
    }
}

@Composable
fun TitleLanguageSelectionDialog(
    currentLanguage: TitleLanguage,
    onLanguageSelected: (TitleLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                item {
                    Text(
                        text = stringResource(R.string.settings_title_language),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(
                    items = TitleLanguages,
                    key = { it.name },
                ) { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onLanguageSelected(language) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentLanguage == language,
                            onClick = { onLanguageSelected(language) },
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = getTitleLanguageLabel(language),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = getTitleLanguageExample(language),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamingServiceSelectionDialog(
    currentService: StreamingService,
    onServiceSelected: (StreamingService) -> Unit,
    onDismiss: () -> Unit,
) {
    val brandColors = rememberBrandColors()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                item {
                    Text(
                        text = stringResource(R.string.setting_streaming_service),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.setting_streaming_service_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(
                    items = StreamingServices,
                    key = { it.name },
                ) { service ->
                    val brandColor = brandColors[service]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onServiceSelected(service) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentService == service,
                            onClick = { onServiceSelected(service) },
                        )
                        Spacer(Modifier.width(12.dp))

                        if (service.iconUrl != null) {
                            AsyncImage(
                                model = service.iconUrl,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                contentScale = ContentScale.Fit,
                                colorFilter = brandColor?.let { ColorFilter.tint(it) },
                            )
                        } else {
                            Icon(
                                Icons.Default.PlayCircle,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Text(
                            text = service.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberBrandColors(): Map<StreamingService, Color?> {
    return remember {
        StreamingServices.associateWith { service ->
            try {
                Color(android.graphics.Color.parseColor(service.brandColor))
            } catch (e: Exception) {
                null
            }
        }
    }
}

@Composable
fun ThemeModeSelectionDialog(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                item {
                    Text(
                        text = stringResource(R.string.theme_mode_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(
                    items = ThemeModes,
                    key = { it.name },
                ) { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = { onModeSelected(mode) },
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = when (mode) {
                                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppLanguageSelectionDialog(
    currentLocale: AppLocale,
    onLocaleSelected: (AppLocale) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                item {
                    Text(
                        text = stringResource(R.string.settings_app_language),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(
                    items = AppLocales,
                    key = { it.name },
                ) { locale ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onLocaleSelected(locale) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentLocale == locale,
                            onClick = { onLocaleSelected(locale) },
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = locale.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (locale == AppLocale.SYSTEM) {
                                Text(
                                    text = stringResource(R.string.settings_app_language_system_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
    }
}

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

@Composable
private fun coverQualityLabel(quality: CoverQuality): String = stringResource(
    when (quality) {
        CoverQuality.MEDIUM -> R.string.cover_quality_medium
        CoverQuality.LARGE -> R.string.cover_quality_large
        CoverQuality.EXTRA_LARGE -> R.string.cover_quality_extra_large
    }
)

@Composable
private fun coverQualityDescription(quality: CoverQuality): String = stringResource(
    when (quality) {
        CoverQuality.MEDIUM -> R.string.cover_quality_medium_desc
        CoverQuality.LARGE -> R.string.cover_quality_large_desc
        CoverQuality.EXTRA_LARGE -> R.string.cover_quality_extra_large_desc
    }
)

@Composable
private fun navBarStyleLabel(style: NavBarStyle): String = stringResource(
    when (style) {
        NavBarStyle.ANCHORED -> R.string.setting_nav_bar_style_anchored
        NavBarStyle.FLOATING -> R.string.setting_nav_bar_style_floating
    }
)

@Composable
private fun navBarStyleDescription(style: NavBarStyle): String = stringResource(
    when (style) {
        NavBarStyle.ANCHORED -> R.string.setting_nav_bar_style_anchored_desc
        NavBarStyle.FLOATING -> R.string.setting_nav_bar_style_floating_desc
    }
)

@Composable
private fun NavBarCornerRadiusSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.RoundedCorner,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.setting_nav_bar_corner_radius),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${value.toInt()}dp",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = AppSettings.MIN_NAV_BAR_CORNER_RADIUS..AppSettings.MAX_NAV_BAR_CORNER_RADIUS,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun NavBarStyleSelectionDialog(
    currentStyle: NavBarStyle,
    onStyleSelected: (NavBarStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                item {
                    Text(
                        text = stringResource(R.string.nav_bar_style_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(items = NavBarStyles, key = { it.name }) { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onStyleSelected(style) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentStyle == style,
                            onClick = { onStyleSelected(style) },
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = navBarStyleLabel(style),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = navBarStyleDescription(style),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoverQualitySelectionDialog(
    currentQuality: CoverQuality,
    onQualitySelected: (CoverQuality) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                item {
                    Text(
                        text = stringResource(R.string.settings_cover_quality),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_cover_quality_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(items = CoverQualities, key = { it.name }) { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onQualitySelected(quality) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentQuality == quality,
                            onClick = { onQualitySelected(quality) },
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = coverQualityLabel(quality),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = coverQualityDescription(quality),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun avatarShapeLabel(shape: com.anisync.android.data.AvatarShape): String = stringResource(
    when (shape) {
        com.anisync.android.data.AvatarShape.CLOVER -> R.string.avatar_shape_clover
        com.anisync.android.data.AvatarShape.CIRCLE -> R.string.avatar_shape_circle
        com.anisync.android.data.AvatarShape.CLOVER_4_LEAF -> R.string.avatar_shape_clover_4_leaf
        com.anisync.android.data.AvatarShape.GHOSTISH -> R.string.avatar_shape_ghostish
    }
)

@Composable
private fun avatarShapeDescription(shape: com.anisync.android.data.AvatarShape): String = stringResource(
    when (shape) {
        com.anisync.android.data.AvatarShape.CLOVER -> R.string.avatar_shape_clover_desc
        com.anisync.android.data.AvatarShape.CIRCLE -> R.string.avatar_shape_circle_desc
        com.anisync.android.data.AvatarShape.CLOVER_4_LEAF -> R.string.avatar_shape_clover_4_leaf_desc
        com.anisync.android.data.AvatarShape.GHOSTISH -> R.string.avatar_shape_ghostish_desc
    }
)

@Composable
fun AvatarShapeSelectionDialog(
    currentShape: com.anisync.android.data.AvatarShape,
    backgroundEnabled: Boolean,
    onShapeSelected: (com.anisync.android.data.AvatarShape) -> Unit,
    onBackgroundEnabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                item {
                    Text(
                        text = stringResource(R.string.avatar_shape_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(items = AvatarShapes, key = { it.name }) { shape ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onShapeSelected(shape) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentShape == shape,
                            onClick = { onShapeSelected(shape) },
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = avatarShapeLabel(shape),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = avatarShapeDescription(shape),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        
                        // Preview
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(shape.toComposeShape())
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBackgroundEnabledChange(!backgroundEnabled) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Avatar Background",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.Switch(
                            checked = backgroundEnabled,
                            onCheckedChange = onBackgroundEnabledChange
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
    }
}
