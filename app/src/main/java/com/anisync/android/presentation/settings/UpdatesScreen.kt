package com.anisync.android.presentation.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.anisync.android.BuildConfig
import com.anisync.android.R
import com.anisync.android.data.update.Release
import com.anisync.android.data.update.UpdateState
import com.anisync.android.presentation.components.AppModalBottomSheet
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

    val installSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.onAction(SettingsAction.InstallUpdate)
        }

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
        }

        SettingsSectionLabel(stringResource(R.string.update_channel))

        SettingsGroup {
            RadioSettingsItem(
                title = stringResource(R.string.channel_stable),
                subtitle = stringResource(R.string.channel_stable_desc),
                selected = !uiState.isPrereleaseAllowed,
                onClick = { viewModel.onAction(SettingsAction.SetPrereleaseAllowed(false)) }
            )
            RadioSettingsItem(
                title = stringResource(R.string.channel_prerelease),
                subtitle = stringResource(R.string.channel_prerelease_desc),
                selected = uiState.isPrereleaseAllowed,
                onClick = { viewModel.onAction(SettingsAction.SetPrereleaseAllowed(true)) }
            )
        }

        SettingsGroup {
            SettingsItem(
                title = stringResource(R.string.check_for_updates),
                subtitle = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                icon = Icons.Outlined.Refresh,
                onClick = {
                    if (updateState !is UpdateState.Checking) {
                        viewModel.onAction(SettingsAction.CheckForUpdate)
                    }
                },
                trailingContent = {
                    if (updateState is UpdateState.Checking) {
                        AppCircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            )
        }

        val dialogRelease = when (val state = updateState) {
            is UpdateState.UpdateAvailable -> state.release
            is UpdateState.Downloading -> state.release
            is UpdateState.ReadyToInstall -> state.release
            else -> null
        }

        if (dialogRelease != null) {
            UpdateDialog(
                updateState = updateState,
                release = dialogRelease,
                onDismiss = { viewModel.onAction(SettingsAction.DismissUpdate) },
                onDownload = { viewModel.onAction(SettingsAction.StartDownload(dialogRelease)) },
                onCancel = { viewModel.onAction(SettingsAction.CancelDownload) },
                onInstall = { requestInstall() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
    updateState: UpdateState,
    release: Release,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDownloading = updateState is UpdateState.Downloading
    val isDownloadingState by rememberUpdatedState(isDownloading)

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { sheetValue ->
            if (isDownloadingState && sheetValue == SheetValue.Hidden) false else true
        }
    )

    AppModalBottomSheet(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
        // Pane-anchored counterpart of the confirmValueChange guard above.
        confirmDismiss = { !isDownloadingState }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = stringResource(R.string.new_update_available),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                text = release.tagName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        if (release.authorName != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (release.authorAvatarUrl != null) {
                                    AsyncImage(
                                        model = release.authorAvatarUrl,
                                        contentDescription = stringResource(R.string.cd_author_avatar),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = stringResource(R.string.by_author, release.authorName),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier
                    .weight(
                        1f,
                        fill = false
                    )
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                AsyncRichTextRenderer(
                    html = release.body,
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                )
            }

            Column(modifier = Modifier.animateContentSize()) {
                AnimatedVisibility(
                    visible = isDownloading,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val targetProgress = when (updateState) {
                        is UpdateState.Downloading -> updateState.progress / 100f
                        is UpdateState.ReadyToInstall -> 1f
                        else -> 0f
                    }

                    val animatedProgress by animateFloatAsState(
                        targetValue = targetProgress,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                        label = "DownloadProgressAnimation"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.downloading_update,
                                    (animatedProgress * 100).toInt()
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            strokeCap = StrokeCap.Round,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val actionButtonsState = when (updateState) {
                    is UpdateState.ReadyToInstall -> 2
                    is UpdateState.Downloading -> 1
                    else -> 0
                }

                AnimatedContent(
                    targetState = actionButtonsState,
                    label = "UpdateActionsTransition",
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    }
                ) { state ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (state) {
                            2 -> {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.cancel))
                                }
                                Button(
                                    onClick = onInstall,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.install_update))
                                }
                            }

                            1 -> {
                                Button(
                                    onClick = onCancel,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Text(stringResource(R.string.cancel))
                                }
                            }

                            else -> {
                                TextButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.cancel))
                                }
                                Button(
                                    onClick = onDownload,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.download_update))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
