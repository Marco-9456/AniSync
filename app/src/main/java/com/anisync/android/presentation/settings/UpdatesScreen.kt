package com.anisync.android.presentation.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.BuildConfig
import com.anisync.android.R
import com.anisync.android.data.update.UpdateState
import com.anisync.android.presentation.components.AsyncRichTextRenderer

@Composable
fun UpdatesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Launcher for navigating to "Install Unknown Apps" system settings.
    // On return, we attempt installation since the user may have granted permission.
    val installSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.onAction(SettingsAction.InstallUpdate)
        }

    /**
     * Checks canRequestPackageInstalls() on API 26+ and either installs directly
     * or redirects the user to the system settings page for this app.
     */
    fun requestInstall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (context.packageManager.canRequestPackageInstalls()) {
                viewModel.onAction(SettingsAction.InstallUpdate)
            } else {
                installSettingsLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        "package:${context.packageName}".toUri()
                    )
                )
            }
        } else {
            viewModel.onAction(SettingsAction.InstallUpdate)
        }
    }

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_updates),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        SettingsGroup {
            SwitchSettingsItem(
                title = stringResource(R.string.auto_update),
                subtitle = stringResource(R.string.enable_auto_update),
                checked = uiState.isAutoUpdateEnabled,
                onCheckedChange = { viewModel.onAction(SettingsAction.SetAutoUpdateEnabled(it)) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.update_channel),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
            )

            SettingsGroup {
                RadioSettingsItem(
                    title = stringResource(R.string.channel_stable),
                    subtitle = stringResource(R.string.channel_stable_desc),
                    selected = !uiState.isPrereleaseAllowed,
                    onClick = { viewModel.onAction(SettingsAction.SetPrereleaseAllowed(false)) }
                )
                SettingsDivider(startPadding = 20.dp)
                RadioSettingsItem(
                    title = stringResource(R.string.channel_prerelease),
                    subtitle = stringResource(R.string.channel_prerelease_desc),
                    selected = uiState.isPrereleaseAllowed,
                    onClick = { viewModel.onAction(SettingsAction.SetPrereleaseAllowed(true)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsGroup {
                Box(modifier = Modifier.fillMaxWidth()) {
                    SettingsItem(
                        title = stringResource(R.string.check_for_updates),
                        subtitle = stringResource(
                            R.string.settings_version,
                            BuildConfig.VERSION_NAME
                        ),
                        icon = Icons.Outlined.Refresh,
                        onClick = {
                            if (updateState !is UpdateState.Checking) {
                                viewModel.onAction(SettingsAction.CheckForUpdate)
                            }
                        }
                    )
                    if (updateState is UpdateState.Checking) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 20.dp)
                        )
                    }
                }
            }

            // Update dialog — shown for UpdateAvailable, Downloading, and ReadyToInstall states
            val dialogRelease = when (val state = updateState) {
                is UpdateState.UpdateAvailable -> state.release
                is UpdateState.Downloading -> state.release
                is UpdateState.ReadyToInstall -> state.release
                else -> null
            }

            if (dialogRelease != null) {
                UpdateDialog(
                    updateState = updateState,
                    tagName = dialogRelease.tagName,
                    releaseBody = dialogRelease.body,
                    onDismiss = { viewModel.onAction(SettingsAction.DismissUpdate) },
                    onDownload = { viewModel.onAction(SettingsAction.StartDownload(dialogRelease)) },
                    onCancel = { viewModel.onAction(SettingsAction.CancelDownload) },
                    onInstall = { requestInstall() }
                )
            }
        }
    }
}

// =============================================================================
// Reusable Update Dialog
// =============================================================================

/**
 * Dialog that shows release notes, download progress, and install actions.
 * Used from both the UpdatesScreen (manual check) and MainActivity (auto-check on launch).
 */
@Composable
fun UpdateDialog(
    updateState: UpdateState,
    tagName: String,
    releaseBody: String,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit
) {
    val isDownloading = updateState is UpdateState.Downloading
    val isReadyToInstall = updateState is UpdateState.ReadyToInstall

    AlertDialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        title = {
            Text(text = "${stringResource(R.string.new_update_available)}: $tagName")
        },
        text = {
            Column {
                // Render release notes as Markdown via AsyncRichTextRenderer
                AsyncRichTextRenderer(
                    html = releaseBody,
                    modifier = Modifier
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState())
                )

                if (isDownloading) {
                    val progress = (updateState as UpdateState.Downloading).progress
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(R.string.downloading_update, progress)
                    )
                }
            }
        },
        confirmButton = {
            when {
                isReadyToInstall -> {
                    Button(onClick = onInstall) {
                        Text(stringResource(R.string.install_update))
                    }
                }

                isDownloading -> {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.cancel))
                    }
                }

                else -> {
                    Button(onClick = onDownload) {
                        Text(stringResource(R.string.download_update))
                    }
                }
            }
        },
        dismissButton = {
            if (!isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}
