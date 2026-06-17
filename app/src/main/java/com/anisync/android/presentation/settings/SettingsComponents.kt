package com.anisync.android.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import com.anisync.android.presentation.util.adaptiveReadingWidth

/**
 * Scaffold for settings screens. A thin wrapper around the app-wide [CollapsingTopBarScaffold]
 * that supplies the settings-specific scrolling container: a single [LazyColumn] whose items are
 * stacked with an 8dp gap and inset 16dp horizontally.
 */
@Composable
fun SettingsScreenScaffold(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    val lazyListState = rememberLazyListState()

    CollapsingTopBarScaffold(
        title = title,
        onBackClick = onBackClick,
        modifier = modifier,
        scrollableState = lazyListState,
        enableEnterAnimation = true,
        actions = actions
    ) { topContentPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = topContentPadding + 8.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp
            )
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .adaptiveReadingWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Small section label shown above a [SettingsGroup] or a bespoke control block. Kept subtle
 * (labelLarge / primary) so groups still carry the visual weight.
 */
@Composable
fun SettingsSectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

/**
 * A grouped container for settings items using transparent layout and 2dp item gaps.
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Transparent),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

/**
 * Base unified generic settings item that supports leading icons, titles, subtitles, and trailing elements.
 */
@Composable
fun SettingsItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    trailingContent: @Composable () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier.padding(end = 16.dp).size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Box(contentAlignment = Alignment.Center) {
                trailingContent()
            }
        }
    }
}

/**
 * Shared switch palette so every toggle in Settings reads the same: primary track when on, a muted
 * container track with an outlined thumb when off.
 */
@Composable
fun appSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
)

/**
 * A settings item with a trailing switch toggle.
 */
@Composable
fun SwitchSettingsItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    SettingsItem(
        title = title,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = appSwitchColors()
            )
        }
    )
}

/**
 * A row that does two things: tapping the label area opens a sub-screen, while a trailing switch
 * (separated by a vertical pipe) flips a value inline. The pipe hints the row is more than a toggle.
 */
@Composable
fun SplitToggleSettingsItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
                    .padding(16.dp),
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier.padding(end = 16.dp).size(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .padding(vertical = 14.dp)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Box(
                modifier = Modifier.padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Switch(checked = checked, onCheckedChange = onCheckedChange, colors = appSwitchColors())
            }
        }
    }
}

/**
 * A settings item with a trailing radio button.
 */
@Composable
fun RadioSettingsItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    SettingsItem(
        title = title,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
        trailingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled
            )
        }
    )
}

/**
 * A clickable settings item showing the current value as its subtitle. Set [showArrow] when the row
 * opens a sub-screen — only those rows get the trailing chevron. Rows that open a bottom-sheet
 * picker leave it off, since the sheet (not a screen) is what appears.
 */
@Composable
fun SelectionSettingsItem(
    title: String,
    currentValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    showArrow: Boolean = false
) {
    SettingsItem(
        title = title,
        subtitle = currentValue,
        icon = icon,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
        trailingContent = {
            if (showArrow) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
    )
}

/**
 * Empty divider bridging legacy gap structure; spacing is now handled via Arrangement.spacedBy(2.dp).
 */
@Composable
fun SettingsDivider(
    startPadding: Dp = 56.dp,
    endPadding: Dp = 20.dp
) {
    // Left intentionally empty to retain original gap spacing
}
