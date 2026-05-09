package com.anisync.android.widget.config

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.anisync.android.R
import com.anisync.android.ui.theme.AppTheme
import com.anisync.android.widget.UpNextWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Configuration activity for the UpNext widget.
 *
 * This activity is launched when the user adds a new UpNext widget or
 * long-presses an existing widget and selects "Configure" (if supported).
 */
@AndroidEntryPoint
class UpNextWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Load existing configuration if reconfiguring an existing widget
        lifecycleScope.launch {
            val initialConfig = try {
                val glanceId = GlanceAppWidgetManager(applicationContext)
                    .getGlanceIdBy(appWidgetId)
                val prefs = getAppWidgetState<Preferences>(
                    applicationContext,
                    PreferencesGlanceStateDefinition,
                    glanceId
                )
                WidgetConfigState(
                    showCountdown = prefs[UpNextWidgetConfig.ShowCountdownKey]
                        ?: UpNextWidgetConfig.DEFAULT_SHOW_COUNTDOWN,
                    maxItems = prefs[UpNextWidgetConfig.MaxItemsKey]
                        ?: UpNextWidgetConfig.DEFAULT_MAX_ITEMS,
                    includePlanning = prefs[UpNextWidgetConfig.IncludePlanningKey]
                        ?: UpNextWidgetConfig.DEFAULT_INCLUDE_PLANNING,
                    showAvailableNow = prefs[UpNextWidgetConfig.ShowAvailableNowKey]
                        ?: UpNextWidgetConfig.DEFAULT_SHOW_AVAILABLE_NOW
                )
            } catch (_: Exception) {
                WidgetConfigState() // Use defaults for new widgets
            }

            setContent {
                AppTheme {
                    WidgetConfigScreen(
                        initialConfig = initialConfig,
                        onSave = { config -> saveConfiguration(config) },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }

    private fun saveConfiguration(config: WidgetConfigState) {
        lifecycleScope.launch {
            try {
                val glanceId = GlanceAppWidgetManager(applicationContext)
                    .getGlanceIdBy(appWidgetId)

                updateAppWidgetState(applicationContext, glanceId) { prefs ->
                    prefs[UpNextWidgetConfig.ShowCountdownKey] = config.showCountdown
                    prefs[UpNextWidgetConfig.MaxItemsKey] = config.maxItems
                    prefs[UpNextWidgetConfig.IncludePlanningKey] = config.includePlanning
                    prefs[UpNextWidgetConfig.ShowAvailableNowKey] = config.showAvailableNow
                }

                UpNextWidget().update(applicationContext, glanceId)

                val resultIntent = Intent().apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } catch (e: Exception) {
                finish()
            }
        }
    }
}

data class WidgetConfigState(
    val showCountdown: Boolean = UpNextWidgetConfig.DEFAULT_SHOW_COUNTDOWN,
    val maxItems: Int = UpNextWidgetConfig.DEFAULT_MAX_ITEMS,
    val includePlanning: Boolean = UpNextWidgetConfig.DEFAULT_INCLUDE_PLANNING,
    val showAvailableNow: Boolean = UpNextWidgetConfig.DEFAULT_SHOW_AVAILABLE_NOW
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    initialConfig: WidgetConfigState = WidgetConfigState(),
    onSave: (WidgetConfigState) -> Unit,
    onCancel: () -> Unit
) {
    // State - initialized from existing config values
    var showCountdown by remember { mutableStateOf(initialConfig.showCountdown) }
    var maxItems by remember { mutableFloatStateOf(initialConfig.maxItems.toFloat()) }
    var includePlanning by remember { mutableStateOf(initialConfig.includePlanning) }
    var showAvailableNow by remember { mutableStateOf(initialConfig.showAvailableNow) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.widget_config_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            ConfigBottomBar(
                onSave = {
                    onSave(
                        WidgetConfigState(
                            showCountdown = showCountdown,
                            maxItems = maxItems.roundToInt(),
                            includePlanning = includePlanning,
                            showAvailableNow = showAvailableNow
                        )
                    )
                },
                onCancel = onCancel
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Hero Section: Item Count
                ItemCountSelector(
                    value = maxItems,
                    onValueChange = { maxItems = it }
                )
            }

            item {
                // Section: Content Rules
                ConfigGroup(title = stringResource(R.string.widget_config_section_content)) {
                    ConfigOption(
                        icon = Icons.Default.CalendarMonth,
                        title = stringResource(R.string.widget_config_include_planning),
                        description = stringResource(R.string.widget_config_include_planning_desc),
                        checked = includePlanning,
                        onCheckedChange = { includePlanning = it }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    ConfigOption(
                        icon = Icons.Default.Visibility,
                        title = stringResource(R.string.widget_config_show_available),
                        description = stringResource(R.string.widget_config_show_available_desc),
                        checked = showAvailableNow,
                        onCheckedChange = { showAvailableNow = it }
                    )
                }
            }

            item {
                // Section: Display Options
                ConfigGroup(title = stringResource(R.string.widget_config_section_display)) {
                    ConfigOption(
                        icon = Icons.Default.Schedule,
                        title = stringResource(R.string.widget_config_show_countdown),
                        description = stringResource(R.string.widget_config_show_countdown_desc),
                        checked = showCountdown,
                        onCheckedChange = { showCountdown = it }
                    )
                }
            }

            item {
                // Bottom padding to ensure content isn't hidden by navigation bars if bottomBar is transparent
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun ConfigGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun ConfigOption(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (checked) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ItemCountSelector(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        // Hero card using Secondary Container for Tonal emphasis
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = stringResource(R.string.widget_config_max_items).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = value.roundToInt().toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Text(
                    text = stringResource(R.string.widget_config_items_visible),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.height(24.dp))

                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = UpNextWidgetConfig.MIN_ITEMS.toFloat()..UpNextWidgetConfig.MAX_ITEMS.toFloat(),
                    steps = UpNextWidgetConfig.MAX_ITEMS - UpNextWidgetConfig.MIN_ITEMS - 1,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${UpNextWidgetConfig.MIN_ITEMS}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "${UpNextWidgetConfig.MAX_ITEMS}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigBottomBar(
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing), // Handle nav bar insets
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.cancel))
            }

            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.widget_config_add_widget))
            }
        }
    }
}