package com.anisync.android.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.data.AppLocale
import com.anisync.android.data.WhatsNewItem
import com.anisync.android.presentation.components.LocalWhatsNew
import com.anisync.android.presentation.components.rememberActiveWhatsNew
import com.anisync.android.presentation.settings.components.InfoNotice
import com.anisync.android.data.StartScreen
import com.anisync.android.data.StreamingService
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.settings.components.SettingsPickerSheet
import com.anisync.android.presentation.util.LocalAppSettings

/**
 * Settings that change how the app behaves rather than how it looks: where it opens, the lock,
 * tab gestures, haptics, language, and which streaming service links point to.
 */
@Composable
fun GeneralScreen(
    onBackClick: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Read straight off AppSettings: the ViewModel's uiState is assembled from combines that
    // are already at their arity ceiling, and this is a single value with a single writer.
    val startScreen by LocalAppSettings.current.startScreen.collectAsStateWithLifecycle()

    var showStartScreenSheet by rememberSaveable { mutableStateOf(false) }
    var showStreamingServiceSheet by rememberSaveable { mutableStateOf(false) }

    if (showStartScreenSheet) {
        SettingsPickerSheet(
            title = stringResource(R.string.setting_start_screen),
            items = StartScreen.entries,
            selected = startScreen,
            itemLabel = { startScreenLabel(it) },
            onSelect = {
                viewModel.onAction(SettingsAction.SetStartScreen(it))
                showStartScreenSheet = false
            },
            onDismiss = { showStartScreenSheet = false }
        )
    }

    if (showStreamingServiceSheet) {
        StreamingServiceSheet(
            currentService = uiState.preferredStreamingService,
            onServiceSelected = {
                viewModel.onAction(SettingsAction.SetPreferredStreamingService(it))
                showStreamingServiceSheet = false
            },
            onDismiss = { showStreamingServiceSheet = false }
        )
    }

    val whatsNewStore = LocalWhatsNew.current
    val whatsNew = rememberActiveWhatsNew()
    LaunchedEffect(Unit) { whatsNewStore.markSeen(WhatsNewItem.SETTINGS_GENERAL) }

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_general),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        if (WhatsNewItem.GENERAL_MOVED_NOTICE in whatsNew) {
            InfoNotice(
                text = stringResource(R.string.whats_new_general_notice),
                onDismiss = { whatsNewStore.markSeen(WhatsNewItem.GENERAL_MOVED_NOTICE) }
            )
        }
        SettingsSectionLabel(stringResource(R.string.settings_section_opening))
        SettingsGroup {
            SelectionSettingsItem(
                icon = Icons.Outlined.Home,
                title = stringResource(R.string.setting_start_screen),
                currentValue = startScreenLabel(startScreen),
                onClick = { showStartScreenSheet = true }
            )
            // The toggle is refused in the ViewModel when no screen lock is set up.
            SwitchSettingsItem(
                icon = Icons.Default.Lock,
                title = stringResource(R.string.settings_app_lock),
                subtitle = stringResource(R.string.settings_app_lock_desc),
                checked = uiState.appLockEnabled,
                onCheckedChange = { viewModel.onAction(SettingsAction.SetAppLockEnabled(it)) }
            )
        }

        SettingsSectionLabel(stringResource(R.string.settings_section_tabs_touch))
        SettingsGroup {
            SwitchSettingsItem(
                icon = Icons.Default.Search,
                title = stringResource(R.string.settings_nav_double_tap_search),
                subtitle = stringResource(R.string.settings_nav_double_tap_search_desc),
                checked = uiState.navBarDoubleTapSearch,
                onCheckedChange = { viewModel.onAction(SettingsAction.SetNavBarDoubleTapSearch(it)) }
            )
            SwitchSettingsItem(
                icon = Icons.Default.Vibration,
                title = stringResource(R.string.setting_haptic_feedback),
                checked = uiState.hapticEnabled,
                onCheckedChange = { viewModel.onAction(SettingsAction.SetHapticEnabled(it)) }
            )
        }

        SettingsSectionLabel(stringResource(R.string.settings_section_language))
        SettingsGroup {
            SelectionSettingsItem(
                icon = Icons.Default.Translate,
                title = stringResource(R.string.settings_app_language),
                currentValue = if (uiState.appLocale == AppLocale.SYSTEM) {
                    stringResource(R.string.language_system_default)
                } else {
                    uiState.appLocale.displayName
                },
                onClick = onNavigateToLanguage,
                showArrow = true
            )
        }

        SettingsSectionLabel(stringResource(R.string.settings_section_watching))
        SettingsGroup {
            SelectionSettingsItem(
                icon = Icons.Default.PlayCircle,
                title = stringResource(R.string.setting_streaming_service),
                currentValue = uiState.preferredStreamingService.displayName,
                onClick = { showStreamingServiceSheet = true }
            )
            SwitchSettingsItem(
                icon = Icons.Default.Star,
                title = stringResource(R.string.setting_score_on_complete),
                subtitle = stringResource(R.string.setting_score_on_complete_desc),
                checked = uiState.scoreOnComplete,
                onCheckedChange = { viewModel.onAction(SettingsAction.SetScoreOnComplete(it)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamingServiceSheet(
    currentService: StreamingService,
    onServiceSelected: (StreamingService) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    AppModalBottomSheet(
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
                text = stringResource(R.string.setting_streaming_service_sheet_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(16.dp))
            SettingsGroup(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .selectableGroup()
            ) {
                StreamingService.entries.forEach { service ->
                    RadioSettingsItem(
                        title = service.displayName,
                        subtitle = if (service == StreamingService.NONE) {
                            stringResource(R.string.streaming_service_none_desc)
                        } else null,
                        selected = service == currentService,
                        onClick = { onServiceSelected(service) },
                        leadingContent = { StreamingServiceLogo(service) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamingServiceLogo(service: StreamingService) {
    val brandColor = runCatching { Color(android.graphics.Color.parseColor(service.brandColor)) }.getOrNull()
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
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
                imageVector = Icons.Default.PlayCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Human label for a [StartScreen] option, shared by the row subtitle and the picker. */
@Composable
private fun startScreenLabel(screen: StartScreen): String = stringResource(
    when (screen) {
        StartScreen.LAST_VISITED -> R.string.start_screen_last_visited
        StartScreen.LIBRARY -> R.string.nav_library
        StartScreen.DISCOVER -> R.string.nav_discover
        StartScreen.FEED -> R.string.nav_feed
        StartScreen.FORUM -> R.string.nav_forum
    }
)
