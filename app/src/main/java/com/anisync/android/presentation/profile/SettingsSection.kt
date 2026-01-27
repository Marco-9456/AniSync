package com.anisync.android.presentation.profile

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.anisync.android.R
import com.anisync.android.data.StreamingService
import com.anisync.android.data.ThemeMode
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.data.TitleLanguage
import com.anisync.android.util.NotificationPermissionHelper
import com.anisync.android.BuildConfig

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
    val titleLanguage by viewModel.titleLanguage.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val preferredStreamingService by viewModel.preferredStreamingService.collectAsState()
    
    // Granular notification settings
    val watchingEnabled by viewModel.watchingNotificationsEnabled.collectAsState()
    val planningEnabled by viewModel.planningNotificationsEnabled.collectAsState()
    val upcomingEnabled by viewModel.upcomingNotificationsEnabled.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.toggleNotifications(true)
    }

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

                // Title Language Row
                Row(
                    modifier = Modifier
                        .clickable { showTitleLanguageDialog = true }
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary)
                         Spacer(Modifier.width(16.dp))
                         // You might need to add this string resource if it doesn't exist, using raw for now or assume existence
                         Text("Title Language") 
                    }
                    Text(getTitleLanguageLabel(titleLanguage), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Streaming Service Row
                Row(
                    modifier = Modifier
                        .clickable { showStreamingServiceDialog = true }
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Icon(Icons.Default.PlayCircle, null, tint = MaterialTheme.colorScheme.primary)
                         Spacer(Modifier.width(16.dp))
                         Text(stringResource(R.string.setting_streaming_service))
                    }
                    Text(preferredStreamingService.displayName, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

        // Account Card with Expandable Notification Settings
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            
            // Track actual system permission status
            var hasSystemPermission by rememberSaveable { mutableStateOf(true) }
            var isNotificationExpanded by rememberSaveable { mutableStateOf(false) }
            
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

            Column {
                SectionHeader(
                   title = stringResource(R.string.section_account),
                   level = HeaderLevel.Subsection,
                   padding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )

                // Permission revoked warning banner
                AnimatedVisibility(
                    visible = !hasSystemPermission && isNotificationsEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
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
                            contentDescription = null,
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

                // Notification Row with expand indicator
                Row(
                    modifier = Modifier
                        .clickable {
                            if (!isNotificationsEnabled) {
                                // Enable notifications
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.toggleNotifications(true)
                                }
                            } else {
                                // Toggle expansion when enabled
                                isNotificationExpanded = !isNotificationExpanded
                            }
                        }
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Notifications,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.control_notifications))
                        
                        // Show expand icon when enabled
                        if (isNotificationsEnabled) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                if (isNotificationExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Vertical divider indicator for expandable section (before switch)
                        if (isNotificationsEnabled) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                            )
                            Spacer(Modifier.width(12.dp))
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
                                    isNotificationExpanded = false
                                }
                            }
                        )
                    }
                }
                
                // Expandable notification type options
                AnimatedVisibility(
                    visible = isNotificationsEnabled && isNotificationExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(start = 32.dp, end = 16.dp, bottom = 8.dp)
                    ) {
                        // Watching List toggle
                        NotificationTypeRow(
                            title = stringResource(R.string.notification_watching),
                            description = stringResource(R.string.notification_watching_desc),
                            isEnabled = watchingEnabled,
                            onToggle = { viewModel.setWatchingNotificationsEnabled(it) }
                        )
                        
                        // Planning List toggle
                        NotificationTypeRow(
                            title = stringResource(R.string.notification_planning),
                            description = stringResource(R.string.notification_planning_desc),
                            isEnabled = planningEnabled,
                            onToggle = { viewModel.setPlanningNotificationsEnabled(it) }
                        )
                        
                        // Upcoming Premieres toggle
                        NotificationTypeRow(
                            title = stringResource(R.string.notification_upcoming),
                            description = stringResource(R.string.notification_upcoming_desc),
                            isEnabled = upcomingEnabled,
                            onToggle = { viewModel.setUpcomingNotificationsEnabled(it) }
                        )
                    }
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

        // Developer Card (debug builds only)
        if (BuildConfig.DEBUG) {
            var isDebugExpanded by rememberSaveable { mutableStateOf(false) }
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SectionHeader(
                        title = stringResource(R.string.section_developer),
                        level = HeaderLevel.Subsection,
                        padding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )

                    // Notification Debug Row
                    Row(
                        modifier = Modifier
                            .clickable { isDebugExpanded = !isDebugExpanded }
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Notifications,
                                null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(stringResource(R.string.debug_notifications))
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                if (isDebugExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Expandable debug buttons
                    AnimatedVisibility(
                        visible = isDebugExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
            }
        }
    }
}

/**
 * Row for individual notification type toggle.
 */
@Composable
private fun NotificationTypeRow(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isEnabled) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium
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

/**
 * Title Language selection dialog following Material Design 3 guidelines.
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
                    text = "Preferred Title Language", // Replace with string resource
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.padding(top = 16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(modifier = Modifier.padding(top = 16.dp))

                // Radio options
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
 * Returns the localized label for a [TitleLanguage].
 */
@Composable
fun getTitleLanguageLabel(language: TitleLanguage): String {
    return when (language) {
        TitleLanguage.ROMAJI -> "Romaji"
        TitleLanguage.ENGLISH -> "English"
        TitleLanguage.NATIVE -> "Native"
    }
}

/**
 * Returns an example title for a [TitleLanguage] to help the user understand the setting.
 */
@Composable
fun getTitleLanguageExample(language: TitleLanguage): String {
    return when (language) {
        TitleLanguage.ROMAJI -> "e.g. Shingeki no Kyojin"
        TitleLanguage.ENGLISH -> "e.g. Attack on Titan"
        TitleLanguage.NATIVE -> "e.g. 進撃の巨人"
    }
}

/**
 * Streaming Service selection dialog following Material Design 3 guidelines.
 * Shows service icons loaded from AniList CDN (same as external links).
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
                // Title
                Text(
                    text = stringResource(R.string.setting_streaming_service),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.padding(top = 8.dp))
                
                // Description
                Text(
                    text = stringResource(R.string.setting_streaming_service_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.padding(top = 16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(modifier = Modifier.padding(top = 16.dp))

                // Radio options with icons
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
                        
                        // Service icon (from URL or fallback)
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
