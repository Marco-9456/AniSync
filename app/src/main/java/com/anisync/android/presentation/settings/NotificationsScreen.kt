package com.anisync.android.presentation.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.BuildConfig
import com.anisync.android.R
import com.anisync.android.util.NotificationPermissionHelper

/**
 * Notification settings screen.
 * Contains master toggle and granular notification type controls.
 */
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsStateWithLifecycle()
    val watchingEnabled by viewModel.watchingNotificationsEnabled.collectAsStateWithLifecycle()
    val planningEnabled by viewModel.planningNotificationsEnabled.collectAsStateWithLifecycle()
    val upcomingEnabled by viewModel.upcomingNotificationsEnabled.collectAsStateWithLifecycle()

    // Track actual system permission status
    var hasSystemPermission by rememberSaveable { mutableStateOf(true) }

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleNotifications(true)
        }
        hasSystemPermission = isGranted
    }

    // Check permission when screen becomes visible (handles return from settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasSystemPermission = NotificationPermissionHelper.hasNotificationPermission(context)
                // Auto-disable if permission was revoked
                if (!hasSystemPermission && isNotificationsEnabled) {
                    viewModel.toggleNotifications(false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_notifications),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        // Permission revoked warning banner
        AnimatedVisibility(
            visible = !hasSystemPermission && isNotificationsEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = stringResource(R.string.a11y_settings_notification_warning),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.notification_permission_revoked),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Master Toggle
        SettingsGroup {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = stringResource(R.string.control_notifications),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_allow_notifications),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(R.string.settings_allow_notifications_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isNotificationsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.toggleNotifications(true)
                            }
                        } else {
                            viewModel.toggleNotifications(false)
                        }
                    }
                )
            }
        }

        // Notification Type Options (visible when master toggle is enabled)
        AnimatedVisibility(
            visible = isNotificationsEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(modifier = Modifier.height(8.dp))

                SettingsGroup {
                    NotificationTypeItem(
                        title = stringResource(R.string.notification_watching),
                        description = stringResource(R.string.notification_watching_desc),
                        isEnabled = watchingEnabled,
                        onToggle = { viewModel.setWatchingNotificationsEnabled(it) }
                    )
                    SettingsDivider(startPadding = 20.dp)
                    NotificationTypeItem(
                        title = stringResource(R.string.notification_planning),
                        description = stringResource(R.string.notification_planning_desc),
                        isEnabled = planningEnabled,
                        onToggle = { viewModel.setPlanningNotificationsEnabled(it) }
                    )
                    SettingsDivider(startPadding = 20.dp)
                    NotificationTypeItem(
                        title = stringResource(R.string.notification_upcoming),
                        description = stringResource(R.string.notification_upcoming_desc),
                        isEnabled = upcomingEnabled,
                        onToggle = { viewModel.setUpcomingNotificationsEnabled(it) }
                    )
                }
            }
        }

        // Debug section (debug builds only)
        if (BuildConfig.DEBUG) {
            Spacer(modifier = Modifier.height(16.dp))
            DebugNotificationsSection(viewModel = viewModel)
        }
    }
}

/**
 * Individual notification type toggle row.
 */
@Composable
private fun NotificationTypeItem(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isEnabled) }
            .padding(vertical = 12.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle
        )
    }
}

/**
 * Debug notifications section for testing (debug builds only).
 */
@Composable
private fun DebugNotificationsSection(
    viewModel: SettingsViewModel
) {
    SettingsGroup {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.debug_notifications),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // First row: Watching and Planning
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { viewModel.sendTestWatchingNotification() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.debug_test_watching))
                }
                FilledTonalButton(
                    onClick = { viewModel.sendTestPlanningNotification() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.debug_test_planning))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Second row: Advance and Imminent
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { viewModel.sendTestAdvanceNotification() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.debug_test_advance))
                }
                FilledTonalButton(
                    onClick = { viewModel.sendTestImminentNotification() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.debug_test_imminent))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Third row: Clear All
            FilledTonalButton(
                onClick = { viewModel.clearAllNotifications() },
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
