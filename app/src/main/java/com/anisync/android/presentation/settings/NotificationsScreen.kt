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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
    val followsEnabled = uiState.followsEnabled
    val streamingDelayMinutes = uiState.streamingDelayMinutes

    var hasSystemPermission by rememberSaveable { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onAction(SettingsAction.ToggleNotifications(true))
        }
        hasSystemPermission = isGranted
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasSystemPermission = NotificationPermissionHelper.hasNotificationPermission(context)
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

        SettingsGroup {
            SwitchSettingsItem(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.settings_allow_notifications),
                subtitle = stringResource(R.string.settings_allow_notifications_desc),
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

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionLabel(stringResource(R.string.notification_group_airing))

            SettingsGroup {
                SwitchSettingsItem(
                    title = stringResource(R.string.notification_watching),
                    subtitle = stringResource(R.string.notification_watching_desc),
                    checked = watchingEnabled,
                    enabled = isNotificationsEnabled,
                    onCheckedChange = { viewModel.onAction(SettingsAction.SetWatchingNotificationsEnabled(it)) }
                )
                SettingsDivider()
                SwitchSettingsItem(
                    title = stringResource(R.string.notification_planning),
                    subtitle = stringResource(R.string.notification_planning_desc),
                    checked = planningEnabled,
                    enabled = isNotificationsEnabled,
                    onCheckedChange = { viewModel.onAction(SettingsAction.SetPlanningNotificationsEnabled(it)) }
                )
                SettingsDivider()
                SwitchSettingsItem(
                    title = stringResource(R.string.notification_upcoming),
                    subtitle = stringResource(R.string.notification_upcoming_desc),
                    checked = upcomingEnabled,
                    enabled = isNotificationsEnabled,
                    onCheckedChange = { viewModel.onAction(SettingsAction.SetUpcomingNotificationsEnabled(it)) }
                )
                SettingsDivider()
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StreamingDelayItem(
                        minutes = streamingDelayMinutes,
                        enabled = isNotificationsEnabled,
                        onValueChange = { viewModel.onAction(SettingsAction.SetStreamingDelayMinutes(it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionLabel(stringResource(R.string.notification_group_forum))

            SettingsGroup {
                SwitchSettingsItem(
                    title = stringResource(R.string.notification_thread_comment_reply),
                    subtitle = stringResource(R.string.notification_thread_comment_reply_desc),
                    checked = threadCommentReplyEnabled,
                    enabled = isNotificationsEnabled,
                    onCheckedChange = { viewModel.onAction(SettingsAction.SetThreadCommentReplyEnabled(it)) }
                )
                SettingsDivider()
                SwitchSettingsItem(
                    title = stringResource(R.string.notification_thread_subscribed),
                    subtitle = stringResource(R.string.notification_thread_subscribed_desc),
                    checked = threadSubscribedEnabled,
                    enabled = isNotificationsEnabled,
                    onCheckedChange = { viewModel.onAction(SettingsAction.SetThreadSubscribedEnabled(it)) }
                )
                SettingsDivider()
                SwitchSettingsItem(
                    title = stringResource(R.string.notification_thread_comment_mention),
                    subtitle = stringResource(R.string.notification_thread_comment_mention_desc),
                    checked = threadCommentMentionEnabled,
                    enabled = isNotificationsEnabled,
                    onCheckedChange = { viewModel.onAction(SettingsAction.SetThreadCommentMentionEnabled(it)) }
                )
                SettingsDivider()
                SwitchSettingsItem(
                    title = stringResource(R.string.notification_thread_like),
                    subtitle = stringResource(R.string.notification_thread_like_desc),
                    checked = threadLikeEnabled,
                    enabled = isNotificationsEnabled,
                    onCheckedChange = { viewModel.onAction(SettingsAction.SetThreadLikeEnabled(it)) }
                )
                SettingsDivider()
                SwitchSettingsItem(
                    title = stringResource(R.string.notification_thread_comment_like),
                    subtitle = stringResource(R.string.notification_thread_comment_like_desc),
                    checked = threadCommentLikeEnabled,
                    enabled = isNotificationsEnabled,
                    onCheckedChange = { viewModel.onAction(SettingsAction.SetThreadCommentLikeEnabled(it)) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionLabel(stringResource(R.string.notification_group_activity))

            SettingsGroup {
                SwitchSettingsItem(
                    title = stringResource(R.string.notification_channel_activity_reply),
                    subtitle = stringResource(R.string.notification_channel_activity_reply_desc),
                    checked = activityReplyEnabled,
                    enabled = isNotificationsEnabled,
                    onCheckedChange = { viewModel.onAction(SettingsAction.SetActivityReplyEnabled(it)) }
                )
                SettingsDivider()
                SwitchSettingsItem(
                    title = stringResource(R.string.notification_channel_activity_mention),
                    subtitle = stringResource(R.string.notification_channel_activity_mention_desc),
                    checked = activityMentionEnabled,
                    enabled = isNotificationsEnabled,
                    onCheckedChange = { viewModel.onAction(SettingsAction.SetActivityMentionEnabled(it)) }
                )
                SettingsDivider()
                SwitchSettingsItem(
                    title = stringResource(R.string.notification_channel_activity_like),
                    subtitle = stringResource(R.string.notification_channel_activity_like_desc),
                    checked = activityLikeEnabled,
                    enabled = isNotificationsEnabled,
                    onCheckedChange = { viewModel.onAction(SettingsAction.SetActivityLikeEnabled(it)) }
                )
                SettingsDivider()
                SwitchSettingsItem(
                    title = stringResource(R.string.notification_channel_activity_message),
                    subtitle = stringResource(R.string.notification_channel_activity_message_desc),
                    checked = activityMessageEnabled,
                    enabled = isNotificationsEnabled,
                    onCheckedChange = { viewModel.onAction(SettingsAction.SetActivityMessageEnabled(it)) }
                )
                SettingsDivider()
                SwitchSettingsItem(
                    title = stringResource(R.string.notification_follows),
                    subtitle = stringResource(R.string.notification_follows_desc),
                    checked = followsEnabled,
                    enabled = isNotificationsEnabled,
                    onCheckedChange = { viewModel.onAction(SettingsAction.SetFollowsEnabled(it)) }
                )
            }
        }
    }
}

@Composable
private fun StreamingDelayItem(
    minutes: Int,
    onValueChange: (Int) -> Unit,
    enabled: Boolean = true
) {
    val disabledAlpha = 0.38f
    val titleColor = if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = disabledAlpha)
    val valueColor = if (enabled) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primary.copy(alpha = disabledAlpha)
    val subtitleColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = disabledAlpha)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(R.string.notification_streaming_delay),
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (minutes == 0) {
                    stringResource(R.string.notification_streaming_delay_off)
                } else {
                    stringResource(R.string.notification_streaming_delay_value, minutes)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor
            )
        }
        Text(
            stringResource(R.string.notification_streaming_delay_desc),
            style = MaterialTheme.typography.bodySmall,
            color = subtitleColor,
            modifier = Modifier.padding(top = 4.dp)
        )
        Slider(
            value = minutes.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..180f,
            enabled = enabled,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}