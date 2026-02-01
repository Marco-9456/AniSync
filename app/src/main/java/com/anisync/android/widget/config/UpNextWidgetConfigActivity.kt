package com.anisync.android.widget.config

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
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
 * 
 * The user can configure:
 * - Maximum number of items to display
 * - Whether to show countdown timers
 * - Whether to include planning entries
 * - Whether to show "Available Now" entries
 */
@AndroidEntryPoint
class UpNextWidgetConfigActivity : ComponentActivity() {
    
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Set the result to CANCELED initially. This way if the user backs out,
        // the widget host is notified that the configuration was cancelled.
        setResult(RESULT_CANCELED)
        
        // Get the widget ID from the intent extras
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        
        // If no valid widget ID, finish immediately
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WidgetConfigScreen(
                        onSave = { config -> saveConfiguration(config) },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }
    
    private fun saveConfiguration(config: WidgetConfigState) {
        val scope = kotlinx.coroutines.MainScope()
        scope.launch {
            try {
                // Get the GlanceId for this widget
                val glanceId = GlanceAppWidgetManager(this@UpNextWidgetConfigActivity)
                    .getGlanceIdBy(appWidgetId)
                
                // Update the widget state with the configuration
                updateAppWidgetState(this@UpNextWidgetConfigActivity, glanceId) { prefs ->
                    prefs[UpNextWidgetConfig.ShowCountdownKey] = config.showCountdown
                    prefs[UpNextWidgetConfig.MaxItemsKey] = config.maxItems
                    prefs[UpNextWidgetConfig.IncludePlanningKey] = config.includePlanning
                    prefs[UpNextWidgetConfig.ShowAvailableNowKey] = config.showAvailableNow
                }
                
                // Update the widget
                UpNextWidget().update(this@UpNextWidgetConfigActivity, glanceId)
                
                // Return success
                val resultIntent = Intent().apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } catch (e: Exception) {
                // If something goes wrong, just finish with cancel
                finish()
            }
        }
    }
}

/**
 * State holder for widget configuration
 */
data class WidgetConfigState(
    val showCountdown: Boolean = UpNextWidgetConfig.DEFAULT_SHOW_COUNTDOWN,
    val maxItems: Int = UpNextWidgetConfig.DEFAULT_MAX_ITEMS,
    val includePlanning: Boolean = UpNextWidgetConfig.DEFAULT_INCLUDE_PLANNING,
    val showAvailableNow: Boolean = UpNextWidgetConfig.DEFAULT_SHOW_AVAILABLE_NOW
)

@Composable
private fun WidgetConfigScreen(
    onSave: (WidgetConfigState) -> Unit,
    onCancel: () -> Unit
) {
    var showCountdown by remember { mutableStateOf(UpNextWidgetConfig.DEFAULT_SHOW_COUNTDOWN) }
    var maxItems by remember { mutableFloatStateOf(UpNextWidgetConfig.DEFAULT_MAX_ITEMS.toFloat()) }
    var includePlanning by remember { mutableStateOf(UpNextWidgetConfig.DEFAULT_INCLUDE_PLANNING) }
    var showAvailableNow by remember { mutableStateOf(UpNextWidgetConfig.DEFAULT_SHOW_AVAILABLE_NOW) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = stringResource(R.string.widget_config_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.widget_config_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Configuration Options
        ConfigSection(title = stringResource(R.string.widget_config_section_display)) {
            // Max Items Slider
            MaxItemsSlider(
                value = maxItems,
                onValueChange = { maxItems = it }
            )
            
            // Show Countdown Toggle
            ConfigToggle(
                icon = Icons.Default.Schedule,
                title = stringResource(R.string.widget_config_show_countdown),
                description = stringResource(R.string.widget_config_show_countdown_desc),
                checked = showCountdown,
                onCheckedChange = { showCountdown = it }
            )
            
            // Show Available Now Toggle
            ConfigToggle(
                icon = Icons.Default.Visibility,
                title = stringResource(R.string.widget_config_show_available),
                description = stringResource(R.string.widget_config_show_available_desc),
                checked = showAvailableNow,
                onCheckedChange = { showAvailableNow = it }
            )
        }
        
        ConfigSection(title = stringResource(R.string.widget_config_section_content)) {
            // Include Planning Toggle
            ConfigToggle(
                icon = Icons.Default.CalendarMonth,
                title = stringResource(R.string.widget_config_include_planning),
                description = stringResource(R.string.widget_config_include_planning_desc),
                checked = includePlanning,
                onCheckedChange = { includePlanning = it }
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp)
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
                onClick = {
                    onSave(
                        WidgetConfigState(
                            showCountdown = showCountdown,
                            maxItems = maxItems.roundToInt(),
                            includePlanning = includePlanning,
                            showAvailableNow = showAvailableNow
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp)
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

@Composable
private fun ConfigSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun MaxItemsSlider(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Numbers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.widget_config_max_items),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.widget_config_max_items_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Current value badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value.roundToInt().toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${UpNextWidgetConfig.MAX_ITEMS}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConfigToggle(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val iconBackground by animateColorAsState(
        targetValue = if (checked) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "iconBackground"
    )
    val iconTint by animateColorAsState(
        targetValue = if (checked) 
            MaterialTheme.colorScheme.onPrimaryContainer 
        else 
            MaterialTheme.colorScheme.onSurfaceVariant,
        label = "iconTint"
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
