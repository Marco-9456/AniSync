package com.anisync.android.presentation.settings

import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explicit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.data.CardSize
import com.anisync.android.data.RecapDelivery
import com.anisync.android.presentation.settings.components.InfoNotice
import com.anisync.android.presentation.settings.components.SettingsPickerSheet
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.ui.theme.ProvideDisplaySize
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

@Composable
fun RecapsScreen(
    onBackClick: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecapsSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onAction = viewModel::onAction

    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showCardSizeSheet by rememberSaveable { mutableStateOf(false) }

    if (showTimePicker) {
        RecapTimeDialog(
            time = uiState.time,
            onConfirm = {
                onAction(RecapsSettingsAction.SetTime(it))
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }

    if (showCardSizeSheet) {
        SettingsPickerSheet(
            title = stringResource(R.string.recap_settings_card_size),
            items = CardSize.entries,
            selected = uiState.defaultCardSize,
            itemLabel = { stringResource(it.labelRes()) },
            itemSubtitle = { stringResource(it.hintRes()) },
            onSelect = {
                onAction(RecapsSettingsAction.SetDefaultCardSize(it))
                showCardSizeSheet = false
            },
            onDismiss = { showCardSizeSheet = false },
        )
    }

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_recaps),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        SettingsSectionLabel(stringResource(R.string.settings_recaps))
        SettingsGroup {
            SwitchSettingsItem(
                icon = Icons.Default.CalendarMonth,
                title = stringResource(R.string.recap_monthly),
                subtitle = stringResource(R.string.recap_settings_monthly_desc),
                checked = uiState.monthly,
                onCheckedChange = { onAction(RecapsSettingsAction.SetMonthly(it)) },
            )
            SwitchSettingsItem(
                icon = Icons.Default.AutoAwesomeMotion,
                title = stringResource(R.string.recap_yearly),
                subtitle = stringResource(R.string.recap_settings_yearly_desc),
                checked = uiState.yearly,
                onCheckedChange = { onAction(RecapsSettingsAction.SetYearly(it)) },
            )
            SwitchSettingsItem(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = stringResource(R.string.recap_settings_include_manga),
                subtitle = stringResource(R.string.recap_settings_include_manga_desc),
                checked = uiState.includeManga,
                onCheckedChange = { onAction(RecapsSettingsAction.SetIncludeManga(it)) },
            )
            SwitchSettingsItem(
                icon = Icons.Default.Repeat,
                title = stringResource(R.string.recap_settings_count_repeats),
                subtitle = stringResource(R.string.recap_settings_count_repeats_desc),
                checked = uiState.countRepeats,
                onCheckedChange = { onAction(RecapsSettingsAction.SetCountRepeats(it)) },
            )
            SwitchSettingsItem(
                icon = Icons.Default.Explicit,
                title = stringResource(R.string.recap_settings_show_adult),
                subtitle = stringResource(
                    if (uiState.adultContent) R.string.recap_settings_show_adult_on else R.string.recap_settings_show_adult_desc
                ),
                checked = uiState.adultContent || uiState.showAdult,
                enabled = !uiState.adultContent,
                onCheckedChange = { onAction(RecapsSettingsAction.SetShowAdult(it)) },
            )
        }

        SettingsSectionLabel(stringResource(R.string.recap_settings_theme_song))
        SettingsGroup {
            SwitchSettingsItem(
                icon = Icons.Default.MusicNote,
                title = stringResource(R.string.recap_settings_theme_song),
                subtitle = stringResource(R.string.recap_settings_theme_song_desc),
                checked = uiState.themeSong,
                onCheckedChange = { onAction(RecapsSettingsAction.SetThemeSong(it)) },
            )
            SwitchSettingsItem(
                icon = Icons.Outlined.Cloud,
                title = stringResource(R.string.recap_settings_mobile_data),
                subtitle = stringResource(R.string.recap_settings_mobile_data_desc),
                checked = uiState.songsOnMobileData,
                enabled = uiState.themeSong,
                onCheckedChange = { onAction(RecapsSettingsAction.SetSongsOnMobileData(it)) },
            )
        }

        SettingsSectionLabel(stringResource(R.string.recap_settings_delivery))
        if (!uiState.notificationsAllowed) {
            InfoNotice(
                text = stringResource(R.string.recap_settings_notifications_off),
                onClick = onOpenNotifications,
            )
        }
        SettingsGroup(modifier = Modifier.selectableGroup()) {
            RecapDelivery.entries.forEach { delivery ->
                DeliveryChoice(
                    delivery = delivery,
                    selected = uiState.effectiveDelivery == delivery,
                    enabled = delivery != RecapDelivery.NOTIFICATION_AND_BANNER || uiState.notificationsAllowed,
                    onClick = { onAction(RecapsSettingsAction.SetDelivery(delivery)) },
                )
            }
            SettingsItem(
                icon = Icons.Default.Schedule,
                title = stringResource(R.string.recap_settings_time),
                onClick = { showTimePicker = true },
                trailingContent = { SettingsValue(formatTime(uiState.time)) },
            )
        }

        SettingsSectionLabel(stringResource(R.string.recap_settings_sharing))
        SettingsGroup {
            SwitchSettingsItem(
                icon = Icons.Default.VisibilityOff,
                title = stringResource(R.string.recap_settings_hide_scores),
                subtitle = stringResource(R.string.recap_settings_hide_scores_desc),
                checked = uiState.hideScoresOnCards,
                onCheckedChange = { onAction(RecapsSettingsAction.SetHideScoresOnCards(it)) },
            )
            SettingsItem(
                icon = Icons.Default.Image,
                title = stringResource(R.string.recap_settings_card_size),
                onClick = { showCardSizeSheet = true },
                trailingContent = { SettingsValue(stringResource(uiState.defaultCardSize.labelRes())) },
            )
        }

        SettingsSectionLabel(stringResource(R.string.recap_settings_past))
        SettingsGroup {
            StoredRecapsItem(
                months = uiState.storedMonths,
                bytes = uiState.storedBytes,
                onClear = { onAction(RecapsSettingsAction.ClearStored) },
            )
        }
    }
}

@Composable
private fun DeliveryChoice(
    delivery: RecapDelivery,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val (title, subtitle) = when (delivery) {
        RecapDelivery.NOTIFICATION_AND_BANNER ->
            R.string.recap_settings_delivery_both to R.string.recap_settings_delivery_both_desc
        RecapDelivery.BANNER_ONLY -> R.string.recap_settings_delivery_banner to R.string.recap_settings_delivery_banner_desc
        RecapDelivery.NOWHERE -> R.string.recap_settings_delivery_nowhere to R.string.recap_settings_delivery_nowhere_desc
    }
    SettingsItem(
        title = stringResource(title),
        subtitle = stringResource(subtitle),
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.semantics {
            this.selected = selected
            role = Role.RadioButton
        },
        leadingContent = { RadioButton(selected = selected, onClick = null, enabled = enabled, modifier = Modifier.size(20.dp)) },
    )
}

@Composable
private fun StoredRecapsItem(months: Int?, bytes: Long, onClear: () -> Unit) {
    val context = LocalContext.current
    val hasStored = bytes > 0
    SettingsItem(
        icon = Icons.Outlined.Storage,
        title = stringResource(R.string.recap_settings_stored),
        subtitle = when {
            months == null -> null
            !hasStored -> stringResource(R.string.recap_settings_stored_empty)
            else -> pluralStringResourceOrDefault(
                R.plurals.recap_settings_stored_desc,
                months,
                months.toString(),
                Formatter.formatShortFileSize(context, bytes),
            )
        },
        enabled = hasStored,
        onClick = onClear,
        trailingContent = { SettingsValue(stringResource(R.string.clear), enabled = hasStored) },
    )
}

/** A row's value or action at its end. */
@Composable
private fun SettingsValue(text: String, enabled: Boolean = true) {
    val color = MaterialTheme.colorScheme.primary
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (enabled) color else color.copy(alpha = 0.38f),
        maxLines = 1,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecapTimeDialog(time: LocalTime, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = time.hour,
        initialMinute = time.minute,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    var typing by rememberSaveable { mutableStateOf(false) }
    val displayMode = if (typing) TimePickerDisplayMode.Input else TimePickerDisplayMode.Picker
    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = { ProvideDisplaySize { TimePickerDialogDefaults.Title(displayMode) } },
        modeToggleButton = {
            ProvideDisplaySize {
                TimePickerDialogDefaults.DisplayModeToggle(onDisplayModeChange = { typing = !typing }, displayMode = displayMode)
            }
        },
        dismissButton = {
            ProvideDisplaySize { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
        },
        confirmButton = {
            ProvideDisplaySize {
                TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                    Text(stringResource(R.string.ok))
                }
            }
        },
    ) {
        ProvideDisplaySize {
            if (typing) TimeInput(state = state) else TimePicker(state = state)
        }
    }
}

@Composable
private fun formatTime(time: LocalTime): String {
    val context = LocalContext.current
    val instant = time.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant()
    return DateFormat.getTimeFormat(context).format(Date.from(instant))
}

private fun CardSize.labelRes(): Int = when (this) {
    CardSize.CHAT -> R.string.share_template_standard
    CardSize.SQUARE -> R.string.share_format_square
    CardSize.PORTRAIT -> R.string.share_size_portrait
    CardSize.STORY -> R.string.share_format_story
}

private fun CardSize.hintRes(): Int = when (this) {
    CardSize.CHAT -> R.string.share_size_chat_hint
    CardSize.SQUARE -> R.string.share_size_ratio_square
    CardSize.PORTRAIT -> R.string.share_size_ratio_portrait
    CardSize.STORY -> R.string.share_size_ratio_story
}
