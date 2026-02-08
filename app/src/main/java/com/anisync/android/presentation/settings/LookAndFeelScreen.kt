package com.anisync.android.presentation.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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

/**
 * Look and Feel settings screen.
 * Contains theme, title language, streaming service, and haptic feedback settings.
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

    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showTitleLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showStreamingServiceDialog by rememberSaveable { mutableStateOf(false) }

    // Theme selection dialog
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = themeMode,
            onThemeSelected = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
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

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_look_and_feel),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        // Theme & Display Group
        SettingsGroup {
            SelectionSettingsItem(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.setting_theme),
                currentValue = getThemeLabel(themeMode),
                onClick = { showThemeDialog = true }
            )
            SettingsDivider()
            SelectionSettingsItem(
                icon = Icons.Default.Language,
                title = stringResource(R.string.settings_title_language),
                currentValue = getTitleLanguageLabel(titleLanguage),
                onClick = { showTitleLanguageDialog = true }
            )
        }

        // Streaming & Interaction Group
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
    }
}

// =============================================================================
// SELECTION DIALOGS
// =============================================================================

/**
 * Theme selection dialog following Material Design 3 guidelines.
 */
@Composable
fun ThemeSelectionDialog(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
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
                    text = stringResource(R.string.setting_theme),
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
                            .clickable { onThemeSelected(mode) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == mode,
                            onClick = { onThemeSelected(mode) }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = getThemeLabel(mode),
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

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

@Composable
fun getThemeLabel(mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    }
}

@Composable
fun getTitleLanguageLabel(language: TitleLanguage): String {
    return when (language) {
        TitleLanguage.ROMAJI -> stringResource(R.string.title_language_romaji)
        TitleLanguage.ENGLISH -> stringResource(R.string.title_language_english)
        TitleLanguage.NATIVE -> stringResource(R.string.title_language_native)
    }
}

@Composable
fun getTitleLanguageExample(language: TitleLanguage): String {
    return when (language) {
        TitleLanguage.ROMAJI -> stringResource(R.string.title_language_romaji_example)
        TitleLanguage.ENGLISH -> stringResource(R.string.title_language_english_example)
        TitleLanguage.NATIVE -> stringResource(R.string.title_language_native_example)
    }
}
