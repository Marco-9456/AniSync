package com.anisync.android.presentation.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.anisync.android.R
import com.anisync.android.data.ThemeMode
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader

/**
 * Settings section displayed in the Profile screen's bottom sheet.
 * Contains Look & Feel and Account settings.
 */
@Composable
fun SettingsSection(
    viewModel: ProfileViewModel,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.toggleNotifications(true)
    }

    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    
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

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        Text(
             stringResource(R.string.section_settings),
             style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
             modifier = Modifier.padding(bottom = 8.dp)
        )

        // Look and Feel Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SectionHeader(
                    title = stringResource(R.string.section_look_and_feel),
                    level = HeaderLevel.Subsection,
                    padding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )

                // Theme Row
                Row(
                    modifier = Modifier
                        .clickable { showThemeDialog = true }
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary)
                         Spacer(Modifier.width(16.dp))
                         Text(stringResource(R.string.setting_theme))
                    }
                    Text(getThemeLabel(themeMode), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                // Haptic
                 Row(
                    modifier = Modifier
                        .clickable { viewModel.setHapticEnabled(!hapticEnabled) }
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Icon(Icons.Default.Vibration, null, tint = MaterialTheme.colorScheme.primary)
                         Spacer(Modifier.width(16.dp))
                         Text(stringResource(R.string.setting_haptic_feedback))
                    }
                    Switch(checked = hapticEnabled, onCheckedChange = null)
                }
            }
        }

        // Account Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SectionHeader(
                   title = stringResource(R.string.section_account),
                   level = HeaderLevel.Subsection,
                   padding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )

                // Notification Row
                Row(
                    modifier = Modifier
                        .clickable {
                            if (!isNotificationsEnabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.toggleNotifications(true)
                                }
                            } else {
                                viewModel.toggleNotifications(false)
                            }
                        }
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                         Spacer(Modifier.width(16.dp))
                         Text(stringResource(R.string.control_notifications))
                    }
                    Switch(checked = isNotificationsEnabled, onCheckedChange = null)
                }
                
                 // Logout Row
                Row(
                    modifier = Modifier
                        .clickable { viewModel.logout { onLogoutClick() } }
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.control_log_out), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/**
 * Theme selection dialog following Material Design 3 guidelines.
 * Uses Dialog + Card for precise control over size and shape.
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
            shape = RoundedCornerShape(28.dp), // M3 dialog corner radius
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Title - left aligned
                Text(
                    text = stringResource(R.string.setting_theme),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.padding(top = 16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(modifier = Modifier.padding(top = 16.dp))

                // Radio options
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

                Spacer(modifier = Modifier.padding(top = 24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.padding(top = 24.dp))
                
                // Dismiss button aligned to end
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
 * Returns the localized label for a [ThemeMode].
 */
@Composable
fun getThemeLabel(mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    }
}
