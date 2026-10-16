package com.anisync.android.presentation.settings

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.data.AppIcon
import com.anisync.android.data.AppSettings
import com.anisync.android.data.AvatarShape
import com.anisync.android.data.DisplaySize
import com.anisync.android.data.NavBarStyle
import com.anisync.android.data.ThemeMode
import com.anisync.android.data.WhatsNewItem
import com.anisync.android.presentation.components.LocalWhatsNew
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import com.anisync.android.presentation.components.rememberControlledSliderState
import com.anisync.android.presentation.settings.components.CUSTOM_PALETTE_ID
import com.anisync.android.presentation.settings.components.ColorPickerSheet
import com.anisync.android.presentation.settings.components.ColorSchemeGrid
import com.anisync.android.presentation.settings.components.LookAndFeelPreview
import com.anisync.android.presentation.settings.components.PaletteStyleSheet
import com.anisync.android.presentation.util.adaptiveReadingWidth
import com.anisync.android.ui.theme.PresetPalettes
import com.anisync.android.ui.theme.resolveDarkTheme
import com.anisync.android.ui.theme.toDescription
import com.anisync.android.ui.theme.toDisplayName
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme
import kotlin.math.abs

/** Below this width the page is one column. At or above it the preview and mode stay pinned. */
private val TwoColumnMinWidth = 720.dp

private const val DISPLAY_SIZE_SETTLE_FRAMES = 12

/**
 * Everything about how the app looks, each choice drawn as what it produces. Theme choices apply
 * app-wide as they are made, so the preview at the top is the real result.
 *
 * On wide panes the preview and Appearance are pinned in the left column and the remaining choices
 * scroll beside them, so the preview stays in view while they change.
 */
@Composable
fun LookAndFeelScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appIcon by viewModel.appIcon.collectAsStateWithLifecycle()
    val isDarkMode = uiState.themeMode.resolveDarkTheme()
    val whatsNewStore = LocalWhatsNew.current
    LaunchedEffect(Unit) { whatsNewStore.markSeen(WhatsNewItem.SETTINGS_LOOK_AND_FEEL) }

    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val palettes = remember(supportsDynamicColor) {
        if (supportsDynamicColor) {
            PresetPalettes.all
        } else {
            // Pre-Android-12 "dynamic" resolves to the static brand scheme. It stays selectable as
            // "Default", since hiding it made the fallback below rewrite the persisted setting.
            listOf(PresetPalettes.Dynamic.copy(name = "Default")) +
                PresetPalettes.all.filter { !it.isDynamic }
        }
    }
    val selectedPaletteId = uiState.selectedPaletteId
    val customSeedColor = uiState.customSeedColor

    LaunchedEffect(selectedPaletteId, palettes, customSeedColor, uiState.isLoaded) {
        val known = palettes.any { it.id == selectedPaletteId } ||
            (selectedPaletteId == CUSTOM_PALETTE_ID && customSeedColor != null)
        if (uiState.isLoaded && !known) {
            viewModel.onAction(SettingsAction.SetSelectedPalette(palettes.firstOrNull()?.id ?: "dynamic"))
        }
    }

    // The seed the current palette builds from. Null for the dynamic palette, whose style the
    // wallpaper decides.
    val seedColor: Color? = if (selectedPaletteId == CUSTOM_PALETTE_ID) {
        customSeedColor
    } else {
        palettes.find { it.id == selectedPaletteId }?.takeUnless { it.isDynamic }?.seedColor
    }

    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    var showStyleSheet by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // A new display size relayouts the whole page. Where the tiles sat before the tap is kept, so
    // the page scrolls them back under the finger instead of letting the content above push them.
    // The top bar re-clamps over the next few frames, so the tiles are re-pinned until it settles.
    var displaySizeTilesY by remember { mutableFloatStateOf(0f) }
    var displaySizeAnchorY by remember { mutableStateOf<Float?>(null) }
    val density = LocalDensity.current
    LaunchedEffect(density) {
        val anchor = displaySizeAnchorY ?: return@LaunchedEffect
        displaySizeAnchorY = null
        repeat(DISPLAY_SIZE_SETTLE_FRAMES) {
            withFrameNanos { }
            val drift = displaySizeTilesY - anchor
            if (abs(drift) >= 1f) listState.scrollBy(drift)
        }
    }

    if (showColorPicker) {
        ColorPickerSheet(
            currentColor = customSeedColor,
            isDarkMode = isDarkMode,
            paletteStyle = uiState.paletteStyle,
            onColorSelected = { color ->
                viewModel.onAction(SettingsAction.SetCustomSeedColor(color))
                viewModel.onAction(SettingsAction.SetSelectedPalette(CUSTOM_PALETTE_ID))
            },
            onDismiss = { showColorPicker = false }
        )
    }

    if (showStyleSheet && seedColor != null) {
        PaletteStyleSheet(
            seedColor = seedColor,
            isDarkMode = isDarkMode,
            selectedStyle = uiState.paletteStyle,
            onStyleSelected = {
                viewModel.onAction(SettingsAction.SetPaletteStyle(it))
                showStyleSheet = false
            },
            onDismiss = { showStyleSheet = false }
        )
    }

    val pinned: @Composable ColumnScope.() -> Unit = {
        SettingsSectionLabel(stringResource(R.string.preview))
        LookAndFeelPreview()
        SettingsSectionLabel(stringResource(R.string.settings_section_appearance))
        SettingsGroup {
            SettingsBlock(modifier = Modifier.selectableGroup()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        ThemeModeTile(
                            mode = mode,
                            selected = uiState.themeMode == mode,
                            onClick = { viewModel.onAction(SettingsAction.SetThemeMode(mode)) }
                        )
                    }
                }
            }
            SwitchSettingsItem(
                icon = Icons.Default.Contrast,
                title = stringResource(R.string.setting_pure_black),
                subtitle = stringResource(R.string.setting_pure_black_desc),
                checked = uiState.amoledEnabled,
                onCheckedChange = { viewModel.onAction(SettingsAction.SetAmoledEnabled(it)) }
            )
        }
    }

    val choices: @Composable ColumnScope.() -> Unit = {
        SettingsSectionLabel(stringResource(R.string.settings_section_display_size))
        SettingsGroup {
            SettingsBlock(
                modifier = Modifier
                    .selectableGroup()
                    .onGloballyPositioned { displaySizeTilesY = it.positionInRoot().y }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DisplaySize.entries.forEach { size ->
                        DisplaySizeTile(
                            size = size,
                            current = uiState.displaySize,
                            selected = uiState.displaySize == size,
                            onClick = {
                                if (size != uiState.displaySize) {
                                    displaySizeAnchorY = displaySizeTilesY
                                    viewModel.onAction(SettingsAction.SetDisplaySize(size))
                                }
                            }
                        )
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.setting_display_size_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        SettingsSectionLabel(stringResource(R.string.settings_section_color))
        SettingsGroup {
            SettingsBlock(contentPadding = PaddingValues(16.dp)) {
                ColorSchemeGrid(
                    palettes = palettes,
                    selectedPaletteId = selectedPaletteId,
                    isDarkMode = isDarkMode,
                    paletteStyle = uiState.paletteStyle,
                    customSeedColor = customSeedColor,
                    onPaletteSelected = { viewModel.onAction(SettingsAction.SetSelectedPalette(it.id)) },
                    onCustomClick = { showColorPicker = true }
                )
            }
            SettingsItem(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.setting_color_style),
                subtitle = if (seedColor != null) {
                    "${uiState.paletteStyle.toDisplayName()} · ${uiState.paletteStyle.toDescription()}"
                } else {
                    stringResource(R.string.setting_color_style_dynamic_desc)
                },
                enabled = seedColor != null,
                onClick = { showStyleSheet = true },
                trailingContent = {
                    if (seedColor != null) StyleSwatches(seedColor, isDarkMode, uiState.paletteStyle)
                }
            )
        }
        SettingsGroup {
            SwitchSettingsItem(
                title = stringResource(R.string.setting_respect_profile_colors),
                subtitle = stringResource(R.string.setting_respect_profile_colors_short_desc),
                checked = uiState.respectUserProfileColors,
                onCheckedChange = { viewModel.onAction(SettingsAction.SetRespectUserProfileColors(it)) }
            )
            SwitchSettingsItem(
                title = stringResource(R.string.setting_media_artwork_theming),
                subtitle = stringResource(R.string.setting_media_artwork_theming_short_desc),
                checked = uiState.mediaArtworkTheming,
                onCheckedChange = { viewModel.onAction(SettingsAction.SetMediaArtworkTheming(it)) }
            )
        }

        SettingsSectionLabel(stringResource(R.string.settings_section_bottom_bar))
        SettingsGroup {
            SettingsBlock(modifier = Modifier.selectableGroup()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NavBarStyle.entries.forEach { style ->
                        NavBarStyleTile(
                            style = style,
                            selected = uiState.navBarStyle == style,
                            onClick = { viewModel.onAction(SettingsAction.SetNavBarStyle(style)) }
                        )
                    }
                }
            }
            SwitchSettingsItem(
                icon = Icons.AutoMirrored.Filled.Label,
                title = stringResource(R.string.setting_nav_bar_show_labels),
                subtitle = stringResource(R.string.setting_nav_bar_show_labels_desc),
                checked = uiState.navBarShowLabels,
                onCheckedChange = { viewModel.onAction(SettingsAction.SetNavBarShowLabels(it)) }
            )
            NavBarCornerRadiusItem(
                value = uiState.navBarCornerRadius,
                onValueChange = { viewModel.onAction(SettingsAction.SetNavBarCornerRadius(it)) }
            )
        }

        SettingsSectionLabel(stringResource(R.string.settings_section_avatars))
        SettingsGroup {
            SettingsBlock(modifier = Modifier.selectableGroup()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AvatarShape.entries.forEach { shape ->
                        AvatarShapeTile(
                            shape = shape,
                            selected = uiState.avatarShape == shape,
                            onClick = { viewModel.onAction(SettingsAction.SetAvatarShape(shape)) }
                        )
                    }
                }
            }
            SwitchSettingsItem(
                title = stringResource(R.string.avatar_shape_show_frame),
                subtitle = stringResource(R.string.avatar_shape_show_frame_desc),
                checked = uiState.avatarBackgroundEnabled,
                onCheckedChange = { viewModel.onAction(SettingsAction.SetAvatarBackgroundEnabled(it)) }
            )
        }

        SettingsSectionLabel(stringResource(R.string.setting_app_icon))
        SettingsGroup {
            SettingsBlock(modifier = Modifier.selectableGroup()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppIcon.entries.forEach { icon ->
                        AppIconTile(
                            icon = icon,
                            selected = appIcon == icon,
                            onClick = { viewModel.onAction(SettingsAction.SetAppIcon(icon)) }
                        )
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.setting_app_icon_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.settings_look_and_feel),
        onBackClick = onBackClick,
        modifier = modifier,
        scrollableState = listState,
        enableEnterAnimation = true
    ) { topContentPadding ->
        val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (maxWidth < TwoColumnMinWidth) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = topContentPadding + 8.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = bottomPadding
                    )
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .adaptiveReadingWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pinned()
                            choices()
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 480.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(top = topContentPadding + 8.dp, bottom = bottomPadding),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        content = pinned
                    )
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 480.dp)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(top = topContentPadding + 8.dp, bottom = bottomPadding)
                    ) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = choices)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ThemeModeTile(mode: ThemeMode, selected: Boolean, onClick: () -> Unit) {
    val (icon, label) = when (mode) {
        ThemeMode.LIGHT -> Icons.Default.WbSunny to R.string.theme_light
        ThemeMode.DARK -> Icons.Default.DarkMode to R.string.theme_dark
        ThemeMode.SYSTEM -> Icons.Default.Smartphone to R.string.theme_system
    }
    SettingsChoiceTile(selected = selected, onClick = onClick, modifier = Modifier.weight(1f)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) LocalContentColor.current else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = stringResource(label), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * "Aa" as it will look at [size]. The page is already drawn at [current], so the sample is sized
 * relative to it and the selected tile always shows text at today's size.
 */
@Composable
private fun RowScope.DisplaySizeTile(
    size: DisplaySize,
    current: DisplaySize,
    selected: Boolean,
    onClick: () -> Unit
) {
    val label = when (size) {
        DisplaySize.SMALL -> R.string.display_size_small
        DisplaySize.DEFAULT -> R.string.display_size_default
        DisplaySize.LARGE -> R.string.display_size_large
        DisplaySize.LARGER -> R.string.display_size_larger
    }
    SettingsChoiceTile(selected = selected, onClick = onClick, modifier = Modifier.weight(1f)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            // Fixed height, so the four tiles stay level while their samples differ in size.
            Box(modifier = Modifier.height(44.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.display_size_sample),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = (24f * size.scale / current.scale).sp
                    ),
                    maxLines = 1,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            }
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** The four colors of the current style, overlapped, as the trailing hint on the Color style row. */
@Composable
private fun StyleSwatches(seedColor: Color, isDarkMode: Boolean, style: PaletteStyle) {
    val scheme = remember(seedColor, isDarkMode, style) {
        dynamicColorScheme(
            seedColor = seedColor,
            isDark = isDarkMode,
            isAmoled = false,
            style = style,
            specVersion = ColorSpec.SpecVersion.SPEC_2026
        )
    }
    val ring = MaterialTheme.colorScheme.surfaceContainer
    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
        listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.primaryContainer).forEach { color ->
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(ring, CircleShape)
                    .padding(2.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
private fun RowScope.NavBarStyleTile(style: NavBarStyle, selected: Boolean, onClick: () -> Unit) {
    val label = when (style) {
        NavBarStyle.ANCHORED -> R.string.setting_nav_bar_style_anchored
        NavBarStyle.FLOATING -> R.string.setting_nav_bar_style_floating_short
    }
    SettingsChoiceTile(selected = selected, onClick = onClick, modifier = Modifier.weight(1f)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            NavBarSketch(style)
            Text(text = stringResource(label), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** A screen in miniature: two lines of content and the bar in [style], first tab selected. */
@Composable
private fun NavBarSketch(style: NavBarStyle) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.fillMaxWidth(0.75f).height(8.dp).background(colors.surfaceContainerHighest, CircleShape))
            Box(Modifier.fillMaxWidth(0.5f).height(8.dp).background(colors.surfaceContainerHighest, CircleShape))
        }
        val barModifier = when (style) {
            NavBarStyle.ANCHORED -> Modifier
                .fillMaxWidth()
                .height(26.dp)
                .background(colors.surfaceContainerHigh, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            NavBarStyle.FLOATING -> Modifier
                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                .fillMaxWidth()
                .height(24.dp)
                .shadow(3.dp, RoundedCornerShape(12.dp))
                .background(colors.surfaceContainerHigh, RoundedCornerShape(12.dp))
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(barModifier),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .width(if (index == 0) 20.dp else 8.dp)
                        .height(8.dp)
                        .background(
                            if (index == 0) colors.secondaryContainer
                            else colors.onSurfaceVariant.copy(alpha = 0.5f),
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun NavBarCornerRadiusItem(value: Float, onValueChange: (Float) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.RoundedCorner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.setting_nav_bar_corner_radius),
                    style = MaterialTheme.typography.titleMedium,
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
                state = rememberControlledSliderState(
                    value = value,
                    valueRange = AppSettings.MIN_NAV_BAR_CORNER_RADIUS..AppSettings.MAX_NAV_BAR_CORNER_RADIUS
                ),
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun RowScope.AvatarShapeTile(shape: AvatarShape, selected: Boolean, onClick: () -> Unit) {
    val label = when (shape) {
        AvatarShape.CLOVER_8_LEAF -> R.string.avatar_shape_short_clover
        AvatarShape.CIRCLE -> R.string.avatar_shape_circle
        AvatarShape.CLOVER_4_LEAF -> R.string.avatar_shape_short_clover_4_leaf
        AvatarShape.GHOSTISH -> R.string.avatar_shape_short_ghostish
        AvatarShape.NONE -> R.string.avatar_shape_none
    }
    SettingsChoiceTile(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.weight(1f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(if (shape == AvatarShape.NONE) RoundedCornerShape(6.dp) else shape.toComposeShape())
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
            )
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RowScope.AppIconTile(icon: AppIcon, selected: Boolean, onClick: () -> Unit) {
    SettingsChoiceTile(selected = selected, onClick = onClick, modifier = Modifier.weight(1f)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            AppIconPreview(icon = icon, modifier = Modifier.size(48.dp))
            Text(
                text = stringResource(icon.label),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * An adaptive icon drawn from its two layers. The layers are 108dp with the visible area in the
 * middle 72dp, so they are scaled up by that ratio before the circle crops them, as a launcher does.
 */
@Composable
private fun AppIconPreview(icon: AppIcon, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(CircleShape)) {
        listOf(icon.background, icon.foreground).forEach { layer ->
            Image(
                painter = painterResource(layer),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = ADAPTIVE_ICON_SCALE
                        scaleY = ADAPTIVE_ICON_SCALE
                    }
            )
        }
    }
}

private const val ADAPTIVE_ICON_SCALE = 108f / 72f
