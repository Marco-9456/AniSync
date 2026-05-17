package com.anisync.android.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anisync.android.BuildConfig
import com.anisync.android.R
import com.anisync.android.presentation.util.LocalAppSettings

/**
 * Developer Tools settings screen (debug builds only).
 * Consolidates debug/test utilities: build info, notification debug, update debug.
 */
@Composable
fun DeveloperToolsScreen(
    onBackClick: () -> Unit,
    onFontPlaygroundClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val appSettings = LocalAppSettings.current
    var showLockDialog by remember { mutableStateOf(false) }

    if (showLockDialog) {
        AlertDialog(
            onDismissRequest = { showLockDialog = false },
            title = { Text(stringResource(R.string.dev_tools_lock_dialog_title)) },
            text = { Text(stringResource(R.string.dev_tools_lock_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    appSettings.lockDevTools()
                    showLockDialog = false
                    Toast.makeText(
                        context,
                        R.string.dev_tools_locked_toast,
                        Toast.LENGTH_SHORT,
                    ).show()
                    onBackClick()
                }) {
                    Text(stringResource(R.string.dev_tools_lock_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLockDialog = false }) {
                    Text(stringResource(R.string.dev_tools_lock_dialog_cancel))
                }
            }
        )
    }

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_developer_tools),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        // Font Playground — live variable-font axis sliders
        SettingsGroup {
            SettingsItem(
                title = stringResource(R.string.settings_font_playground),
                subtitle = stringResource(R.string.settings_font_playground_subtitle),
                onClick = onFontPlaygroundClick
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Build Information
        SettingsGroup {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.debug_build_info),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    BuildInfoRow(label = "Version", value = BuildConfig.VERSION_NAME)
                    BuildInfoRow(label = "Version Code", value = BuildConfig.VERSION_CODE.toString())
                    BuildInfoRow(label = "Build Type", value = BuildConfig.BUILD_TYPE)
                    BuildInfoRow(label = "Flavor", value = BuildConfig.FLAVOR)
                    BuildInfoRow(label = "Application ID", value = BuildConfig.APPLICATION_ID)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notification Debug
        SettingsGroup {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.debug_notifications),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.onAction(SettingsAction.SendTestWatchingNotification) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.debug_test_watching))
                        }
                        FilledTonalButton(
                            onClick = { viewModel.onAction(SettingsAction.SendTestPlanningNotification) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.debug_test_planning))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.onAction(SettingsAction.SendTestAdvanceNotification) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.debug_test_advance))
                        }
                        FilledTonalButton(
                            onClick = { viewModel.onAction(SettingsAction.SendTestImminentNotification) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.debug_test_imminent))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FilledTonalButton(
                        onClick = { viewModel.onAction(SettingsAction.BumpInboxBadge) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.debug_bump_inbox_badge))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FilledTonalButton(
                        onClick = { viewModel.onAction(SettingsAction.ClearAllNotifications) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(stringResource(R.string.debug_clear_all))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Update Debug
        SettingsGroup {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.debug_update_section),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.onAction(SettingsAction.CheckForUpdate) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.debug_force_update_check))
                        }
                        FilledTonalButton(
                            onClick = { viewModel.onAction(SettingsAction.FetchLatestRelease) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.debug_fetch_latest_release))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Crash Reporter Debug — debug builds only; never reachable in an unlocked release.
        if (BuildConfig.DEBUG) {
            SettingsGroup {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.debug_crash_section),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FilledTonalButton(
                            onClick = {
                                throw RuntimeException("Debug-triggered crash from Developer Tools")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text(stringResource(R.string.debug_trigger_crash))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Toast Debug
        SettingsGroup {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Toast Debug",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val toastCodes = listOf(400, 401, 404, 429, 500)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        toastCodes.chunked(2).forEach { rowCodes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowCodes.forEach { code ->
                                    FilledTonalButton(
                                        onClick = { viewModel.onAction(SettingsAction.ShowTestToast(code)) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.test_code, code))
                                    }
                                }
                                if (rowCodes.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lock Dev Tools. On debug builds BuildConfig.DEBUG forces the entry visible regardless,
        // so the button still flips the flag but the entry stays — verify the user-facing hide
        // effect on a release build.
        SettingsGroup {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.dev_tools_lock_section),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.dev_tools_lock_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = { showLockDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(stringResource(R.string.dev_tools_lock_button))
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}