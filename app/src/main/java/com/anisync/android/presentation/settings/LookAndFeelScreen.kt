package com.anisync.android.presentation.settings

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
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
    val disableAvatarShapeProfile = uiState.disableAvatarShapeProfile

    val selectedPaletteId = uiState.selectedPaletteId
    val customSeedColor = uiState.customSeedColor
    val paletteStyle = uiState.paletteStyle

    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    var showThemeModeDialog by rememberSaveable { mutableStateOf(false) }
    var showTitleLanguageSheet by rememberSaveable { mutableStateOf(false) }
    var showAppLanguageSheet by rememberSaveable { mutableStateOf(false) }
    var showStreamingServiceSheet by rememberSaveable { mutableStateOf(false) }
    var showCoverQualitySheet by rememberSaveable { mutableStateOf(false) }
    var showNavBarStyleSheet by rememberSaveable { mutableStateOf(false) }
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

    if (showTitleLanguageSheet) {
        TitleLanguageSelectionSheet(
            currentLanguage = titleLanguage,
            onLanguageSelected = {
                viewModel.onAction(SettingsAction.SetTitleLanguage(it))
                showTitleLanguageSheet = false
            },
            onDismiss = { showTitleLanguageSheet = false }
        )
    }

    if (showStreamingServiceSheet) {
        StreamingServiceSelectionSheet(
            currentService = preferredStreamingService,
            onServiceSelected = {
                viewModel.onAction(SettingsAction.SetPreferredStreamingService(it))
                showStreamingServiceSheet = false
            },
            onDismiss = { showStreamingServiceSheet = false }
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

    if (showAppLanguageSheet) {
        AppLanguageSelectionSheet(
            currentLocale = appLocale,
            onLocaleSelected = {
                viewModel.onAction(SettingsAction.SetAppLocale(it))
                showAppLanguageSheet = false
            },
            onDismiss = { showAppLanguageSheet = false }
        )
    }

    if (showCoverQualitySheet) {
        CoverQualitySelectionSheet(
            currentQuality = coverQuality,
            onQualitySelected = {
                viewModel.onAction(SettingsAction.SetCoverQuality(it))
            },
            onDismiss = { showCoverQualitySheet = false }
        )
    }

    if (showNavBarStyleSheet) {
        NavBarStyleSelectionSheet(
            currentStyle = navBarStyle,
            showLabels = navBarShowLabels,
            cornerRadius = navBarCornerRadius,
            onStyleSelected = {
                viewModel.onAction(SettingsAction.SetNavBarStyle(it))
            },
            onShowLabelsChange = {
                viewModel.onAction(SettingsAction.SetNavBarShowLabels(it))
            },
            onCornerRadiusChange = {
                viewModel.onAction(SettingsAction.SetNavBarCornerRadius(it))
            },
            onDismiss = { showNavBarStyleSheet = false }
        )
    }

    if (showAvatarShapeDialog) {
        AvatarShapeSelectionSheet(
            currentShape = avatarShape,
            frameEnabled = avatarBackgroundEnabled,
            onShapeSelected = {
                viewModel.onAction(SettingsAction.SetAvatarShape(it))
            },
            onFrameEnabledChange = {
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
                onClick = { showAppLanguageSheet = true }
            )
            SettingsDivider()
            SelectionSettingsItem(
                icon = Icons.Default.Language,
                title = stringResource(R.string.settings_title_language),
                currentValue = getTitleLanguageLabel(titleLanguage),
                onClick = { showTitleLanguageSheet = true }
            )
            SettingsDivider()
            SelectionSettingsItem(
                icon = Icons.Default.HighQuality,
                title = stringResource(R.string.settings_cover_quality),
                currentValue = coverQualityLabel(coverQuality),
                onClick = { showCoverQualitySheet = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup {
            SelectionSettingsItem(
                icon = Icons.Default.Navigation,
                title = stringResource(R.string.setting_nav_bar_style),
                currentValue = navBarStyleLabel(navBarStyle),
                onClick = { showNavBarStyleSheet = true }
            )
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
                onClick = { showStreamingServiceSheet = true }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleLanguageSelectionSheet(
    currentLanguage: TitleLanguage,
    onLanguageSelected: (TitleLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title_language),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = TitleLanguages,
                    key = { it.name },
                ) { language ->
                    val isSelected = currentLanguage == language

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onLanguageSelected(language) }
                            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                            .padding(vertical = 16.dp, horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = getTitleLanguageLabel(language),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = getTitleLanguageExample(language),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (isSelected) {
                            Spacer(Modifier.width(16.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLanguageSelectionSheet(
    currentLocale: AppLocale,
    onLocaleSelected: (AppLocale) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_app_language),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = AppLocales,
                    key = { it.name },
                ) { locale ->
                    val isSelected = currentLocale == locale

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onLocaleSelected(locale) }
                            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                            .padding(vertical = 16.dp, horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = locale.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                            )
                            if (locale == AppLocale.SYSTEM) {
                                Text(
                                    text = stringResource(R.string.settings_app_language_system_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (isSelected) {
                            Spacer(Modifier.width(16.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamingServiceSelectionSheet(
    currentService: StreamingService,
    onServiceSelected: (StreamingService) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val brandColors = rememberBrandColors()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.setting_streaming_service),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Text(
                text = stringResource(R.string.setting_streaming_service_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(StreamingServices) { service ->
                    val isSelected = currentService == service
                    val brandColor = brandColors[service]

                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        label = "card_bg"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        label = "card_border"
                    )

                    Card(
                        modifier = Modifier
                            .height(110.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onServiceSelected(service) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        border = BorderStroke(2.dp, borderColor)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (service.iconUrl != null) {
                                AsyncImage(
                                    model = service.iconUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    contentScale = ContentScale.Fit,
                                    colorFilter = brandColor?.let { ColorFilter.tint(it) },
                                )
                            } else {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = brandColor ?: if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = service.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverQualitySelectionSheet(
    currentQuality: CoverQuality,
    onQualitySelected: (CoverQuality) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_cover_quality),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Text(
                text = stringResource(R.string.settings_cover_quality_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Central Fixed-Size Preview Image
            val imageUrl = when(currentQuality) {
                CoverQuality.MEDIUM -> "https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/bx105333-GybuoSoOZfpH.jpg"
                CoverQuality.LARGE -> "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx105333-GybuoSoOZfpH.jpg"
                CoverQuality.EXTRA_LARGE -> "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx105333-GybuoSoOZfpH.jpg"
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Cover Quality Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .height(280.dp)
                        .width(190.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Quality Options List
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CoverQualities.forEach { quality ->
                    val isSelected = currentQuality == quality
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        label = "card_bg"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        label = "card_border"
                    )

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onQualitySelected(quality) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        border = BorderStroke(2.dp, borderColor)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = coverQualityLabel(quality),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
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

// -----------------------------------------------------------------------------------------
// Original Dialogs (Theme Mode)
// -----------------------------------------------------------------------------------------

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

// -----------------------------------------------------------------------------------------
// Helper Components
// -----------------------------------------------------------------------------------------

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
private fun navBarStyleLabel(style: NavBarStyle): String = stringResource(
    when (style) {
        NavBarStyle.ANCHORED -> R.string.setting_nav_bar_style_anchored
        NavBarStyle.FLOATING -> R.string.setting_nav_bar_style_floating
    }
)

@Composable
private fun NavBarCornerRadiusSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding)
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
            modifier = Modifier.fillMaxWidth(),
            valueRange = AppSettings.MIN_NAV_BAR_CORNER_RADIUS..AppSettings.MAX_NAV_BAR_CORNER_RADIUS,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavBarStyleSelectionSheet(
    currentStyle: NavBarStyle,
    showLabels: Boolean,
    cornerRadius: Float,
    onStyleSelected: (NavBarStyle) -> Unit,
    onShowLabelsChange: (Boolean) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.nav_bar_style_dialog_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Live Preview Area
            val shape = if (currentStyle == NavBarStyle.FLOATING) {
                RoundedCornerShape(cornerRadius.dp)
            } else {
                RoundedCornerShape(topStart = cornerRadius.dp, topEnd = cornerRadius.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
            }

            val padding = if (currentStyle == NavBarStyle.FLOATING) {
                PaddingValues(horizontal = 16.dp, vertical = 16.dp)
            } else {
                PaddingValues(0.dp)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding),
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = if (currentStyle == NavBarStyle.FLOATING) 8.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (showLabels) 80.dp else 64.dp)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MockNavItem(Icons.Filled.VideoLibrary, "Library", selected = true, showLabel = showLabels, modifier = Modifier.weight(1f))
                        MockNavItem(Icons.Outlined.Explore, "Discover", selected = false, showLabel = showLabels, modifier = Modifier.weight(1f))
                        MockNavItem(Icons.Outlined.DynamicFeed, "Feed", selected = false, showLabel = showLabels, modifier = Modifier.weight(1f))
                        MockNavItem(Icons.Outlined.Forum, "Forum", selected = false, showLabel = showLabels, modifier = Modifier.weight(1f))
                        MockNavItem(Icons.Outlined.Person, "Profile", selected = false, showLabel = showLabels, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Style Selection Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NavBarStyles.forEach { style ->
                    val isSelected = currentStyle == style
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        label = "card_bg_color"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        label = "card_border_color"
                    )

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onStyleSelected(style) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        border = BorderStroke(2.dp, borderColor)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (style == NavBarStyle.FLOATING) Icons.Default.RoundedCorner else Icons.Default.Navigation,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = navBarStyleLabel(style),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Settings Block (Labels + Corner Radius)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShowLabelsChange(!showLabels) }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Label,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setting_nav_bar_show_labels),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Show text labels under icons",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Switch(
                            checked = showLabels,
                            onCheckedChange = onShowLabelsChange
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    NavBarCornerRadiusSlider(
                        value = cornerRadius,
                        onValueChange = onCornerRadiusChange,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MockNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    showLabel: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun avatarShapeLabel(shape: com.anisync.android.data.AvatarShape): String = stringResource(
    when (shape) {
        com.anisync.android.data.AvatarShape.CLOVER_8_LEAF -> R.string.avatar_shape_clover
        com.anisync.android.data.AvatarShape.CIRCLE -> R.string.avatar_shape_circle
        com.anisync.android.data.AvatarShape.CLOVER_4_LEAF -> R.string.avatar_shape_clover_4_leaf
        com.anisync.android.data.AvatarShape.GHOSTISH -> R.string.avatar_shape_ghostish
        com.anisync.android.data.AvatarShape.NONE -> R.string.avatar_shape_none
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarShapeSelectionSheet(
    currentShape: com.anisync.android.data.AvatarShape,
    frameEnabled: Boolean,
    onShapeSelected: (com.anisync.android.data.AvatarShape) -> Unit,
    onFrameEnabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.avatar_shape_dialog_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AvatarShapes) { shape ->
                    val isSelected = currentShape == shape
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        label = "card_bg_color"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        label = "card_border_color"
                    )

                    Card(
                        modifier = Modifier
                            .width(120.dp)
                            .height(140.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onShapeSelected(shape) },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        border = BorderStroke(2.dp, borderColor)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(shape.toComposeShape())
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = avatarShapeLabel(shape),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFrameEnabledChange(!frameEnabled) }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.avatar_shape_show_frame),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.avatar_shape_show_frame_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Switch(
                        checked = frameEnabled,
                        onCheckedChange = onFrameEnabledChange
                    )
                }
            }
        }
    }
}