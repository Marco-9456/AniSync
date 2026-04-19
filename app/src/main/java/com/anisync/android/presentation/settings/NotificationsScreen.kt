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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.util.NotificationPermissionHelper

/**
 * Notification settings screen.
 * Contains master toggle and granular notification type controls grouped by category.
 */
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isNotificationsEnabled = uiState.isNotificationsEnabled
    val watchingEnabled = uiState.watchingNotificationsEnabled
    val planningEnabled = uiState.planningNotificationsEnabled
    val upcomingEnabled = uiState.upcomingNotificationsEnabled
    val threadCommentReplyEnabled = uiState.threadCommentReplyEnabled
    val threadSubscribedEnabled = uiState.threadSubscribedEnabled
    val threadCommentMentionEnabled = uiState.threadCommentMentionEnabled
    val threadLikeEnabled = uiState.threadLikeEnabled
    val threadCommentLikeEnabled = uiState.threadCommentLikeEnabled
    val activityReplyEnabled = uiState.activityReplyEnabled
    val activityMentionEnabled = uiState.activityMentionEnabled
    val activityLikeEnabled = uiState.activityLikeEnabled
    val activityMessageEnabled = uiState.activityMessageEnabled

    // Track actual system permission status
    var hasSystemPermission by rememberSaveable { mutableStateOf(true) }

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onAction(SettingsAction.ToggleNotifications(true))
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
                    viewModel.onAction(SettingsAction.ToggleNotifications(false))
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
                                viewModel.onAction(SettingsAction.ToggleNotifications(true))
                            }
                        } else {
                            viewModel.onAction(SettingsAction.ToggleNotifications(false))
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
                                viewModel.onAction(SettingsAction.ToggleNotifications(true))
                            }
                        } else {
                            viewModel.onAction(SettingsAction.ToggleNotifications(false))
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

                // Airing group header
                NotificationGroupHeader(
                    title = stringResource(R.string.notification_group_airing)
                )

                SettingsGroup {
                    NotificationTypeItem(
                        title = stringResource(R.string.notification_watching),
                        description = stringResource(R.string.notification_watching_desc),
                        isEnabled = watchingEnabled,
                        onToggle = { viewModel.onAction(SettingsAction.SetWatchingNotificationsEnabled(it)) }
                    )
                    SettingsDivider(startPadding = 20.dp)
                    NotificationTypeItem(
                        title = stringResource(R.string.notification_planning),
                        description = stringResource(R.string.notification_planning_desc),
                        isEnabled = planningEnabled,
                        onToggle = { viewModel.onAction(SettingsAction.SetPlanningNotificationsEnabled(it)) }
                    )
                    SettingsDivider(startPadding = 20.dp)
                    NotificationTypeItem(
                        title = stringResource(R.string.notification_upcoming),
                        description = stringResource(R.string.notification_upcoming_desc),
                        isEnabled = upcomingEnabled,
                        onToggle = { viewModel.onAction(SettingsAction.SetUpcomingNotificationsEnabled(it)) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Forum group header
                NotificationGroupHeader(
                    title = stringResource(R.string.notification_group_forum)
                )

                SettingsGroup {
                    NotificationTypeItem(
                        title = stringResource(R.string.notification_thread_comment_reply),
                        description = stringResource(R.string.notification_thread_comment_reply_desc),
                        isEnabled = threadCommentReplyEnabled,
                        onToggle = { viewModel.onAction(SettingsAction.SetThreadCommentReplyEnabled(it)) }
                    )
                    SettingsDivider(startPadding = 20.dp)
                    NotificationTypeItem(
                        title = stringResource(R.string.notification_thread_subscribed),
                        description = stringResource(R.string.notification_thread_subscribed_desc),
                        isEnabled = threadSubscribedEnabled,
                        onToggle = { viewModel.onAction(SettingsAction.SetThreadSubscribedEnabled(it)) }
                    )
                    SettingsDivider(startPadding = 20.dp)
                    NotificationTypeItem(
                        title = stringResource(R.string.notification_thread_comment_mention),
                        description = stringResource(R.string.notification_thread_comment_mention_desc),
                        isEnabled = threadCommentMentionEnabled,
                        onToggle = { viewModel.onAction(SettingsAction.SetThreadCommentMentionEnabled(it)) }
                    )
                    SettingsDivider(startPadding = 20.dp)
                    NotificationTypeItem(
                        title = stringResource(R.string.notification_thread_like),
                        description = stringResource(R.string.notification_thread_like_desc),
                        isEnabled = threadLikeEnabled,
                        onToggle = { viewModel.onAction(SettingsAction.SetThreadLikeEnabled(it)) }
                    )
                    SettingsDivider(startPadding = 20.dp)
                    NotificationTypeItem(
                        title = stringResource(R.string.notification_thread_comment_like),
                        description = stringResource(R.string.notification_thread_comment_like_desc),
                        isEnabled = threadCommentLikeEnabled,
                        onToggle = { viewModel.onAction(SettingsAction.SetThreadCommentLikeEnabled(it)) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                NotificationGroupHeader(
                    title = stringResource(R.string.notification_group_activity)
                )

                SettingsGroup {
                    NotificationTypeItem(
                        title = stringResource(R.string.notification_channel_activity_reply),
                        description = stringResource(R.string.notification_channel_activity_reply_desc),
                        isEnabled = activityReplyEnabled,
                        onToggle = { viewModel.onAction(SettingsAction.SetActivityReplyEnabled(it)) }
                    )
                    SettingsDivider(startPadding = 20.dp)
                    NotificationTypeItem(
                        title = stringResource(R.string.notification_channel_activity_mention),
                        description = stringResource(R.string.notification_channel_activity_mention_desc),
                        isEnabled = activityMentionEnabled,
                        onToggle = { viewModel.onAction(SettingsAction.SetActivityMentionEnabled(it)) }
                    )
                    SettingsDivider(startPadding = 20.dp)
                    NotificationTypeItem(
                        title = stringResource(R.string.notification_channel_activity_like),
                        description = stringResource(R.string.notification_channel_activity_like_desc),
                        isEnabled = activityLikeEnabled,
                        onToggle = { viewModel.onAction(SettingsAction.SetActivityLikeEnabled(it)) }
                    )
                    SettingsDivider(startPadding = 20.dp)
                    NotificationTypeItem(
                        title = stringResource(R.string.notification_channel_activity_message),
                        description = stringResource(R.string.notification_channel_activity_message_desc),
                        isEnabled = activityMessageEnabled,
                        onToggle = { viewModel.onAction(SettingsAction.SetActivityMessageEnabled(it)) }
                    )
                }
            }
        }
    }
}

/**
 * Section header for notification groups.
 */
@Composable
private fun NotificationGroupHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
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
