package com.anisync.android.presentation.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.BuildConfig
import com.anisync.android.R
import com.anisync.android.util.UpdateUtil
import kotlinx.coroutines.launch

@Composable
fun UpdatesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val autoUpdate = uiState.isAutoUpdateEnabled
    val allowPrerelease = uiState.isPrereleaseAllowed

    var isChecking by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var latestRelease by remember { mutableStateOf<UpdateUtil.Release?>(null) }

    var downloadStatus by remember { mutableStateOf<UpdateUtil.DownloadStatus>(UpdateUtil.DownloadStatus.NotYet) }

    val installSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            UpdateUtil.installApk(context)
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                UpdateUtil.installApk(context)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!context.packageManager.canRequestPackageInstalls()) {
                        installSettingsLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    } else {
                        UpdateUtil.installApk(context)
                    }
                }
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
                checked = autoUpdate,
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
                    subtitle = "Recommended for most users",
                    selected = !allowPrerelease,
                    onClick = { viewModel.onAction(SettingsAction.SetPrereleaseAllowed(false)) }
                )
                SettingsDivider(startPadding = 20.dp)
                RadioSettingsItem(
                    title = stringResource(R.string.channel_prerelease),
                    subtitle = "Get early access to new features",
                    selected = allowPrerelease,
                    onClick = { viewModel.onAction(SettingsAction.SetPrereleaseAllowed(true)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsGroup {
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth()) {
                    SettingsItem(
                        title = stringResource(R.string.check_for_updates),
                        subtitle = stringResource(
                            R.string.settings_version,
                            BuildConfig.VERSION_NAME
                        ),
                        icon = Icons.Outlined.Refresh,
                        onClick = {
                            if (!isChecking) {
                                isChecking = true
                                scope.launch {
                                    val release = UpdateUtil.checkForUpdate(allowPrerelease)
                                    isChecking = false
                                    if (release != null) {
                                        latestRelease = release
                                        showUpdateDialog = true
                                    } else {
                                        Toast.makeText(
                                            context,
                                            R.string.update_is_up_to_date,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    )
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.CenterEnd)
                                .padding(end = 20.dp)
                        )
                    }
                }
            }

            if (showUpdateDialog && latestRelease != null) {
                AlertDialog(
                    onDismissRequest = {
                        if (downloadStatus !is UpdateUtil.DownloadStatus.Progress) {
                            showUpdateDialog = false
                        }
                    },
                    title = { Text(text = "${stringResource(R.string.new_update_available)}: ${latestRelease!!.tagName}") },
                    text = {
                        Column {
                            Text(
                                text = latestRelease!!.body,
                                modifier = Modifier
                                    .height(200.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                            if (downloadStatus is UpdateUtil.DownloadStatus.Progress) {
                                Spacer(modifier = Modifier.height(16.dp))
                                val percent =
                                    (downloadStatus as UpdateUtil.DownloadStatus.Progress).percent
                                LinearProgressIndicator(
                                    progress = { percent / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(text = stringResource(R.string.downloading_update, percent))
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (downloadStatus is UpdateUtil.DownloadStatus.Finished) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        permissionLauncher.launch(Manifest.permission.REQUEST_INSTALL_PACKAGES)
                                    } else {
                                        UpdateUtil.installApk(context)
                                    }
                                } else {
                                    scope.launch {
                                        try {
                                            UpdateUtil.downloadApk(context, latestRelease!!)
                                                .collect { status ->
                                                    downloadStatus = status
                                                    if (status is UpdateUtil.DownloadStatus.Finished) {
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                            permissionLauncher.launch(Manifest.permission.REQUEST_INSTALL_PACKAGES)
                                                        } else {
                                                            UpdateUtil.installApk(context)
                                                        }
                                                        showUpdateDialog = false
                                                    }
                                                }
                                        } catch (e: Exception) {
                                            downloadStatus = UpdateUtil.DownloadStatus.NotYet
                                            Toast.makeText(
                                                context,
                                                R.string.app_update_failed,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            },
                            enabled = downloadStatus !is UpdateUtil.DownloadStatus.Progress
                        ) {
                            Text(
                                if (downloadStatus is UpdateUtil.DownloadStatus.Finished) stringResource(
                                    R.string.install_update
                                )
                                else stringResource(R.string.check_for_updates).replace(
                                    "Check for",
                                    "Download"
                                )
                            )
                        }
                    },
                    dismissButton = {
                        if (downloadStatus !is UpdateUtil.DownloadStatus.Progress) {
                            TextButton(onClick = { showUpdateDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    }
                )
            }
        }
    }
}