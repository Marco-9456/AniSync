package com.anisync.android.presentation.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import de.mrxxxxx.anisyncplus.calendar.domain.MATCHER_VERSION
import de.mrxxxxx.anisyncplus.calendar.domain.PARSER_VERSION
import de.mrxxxxx.anisyncplus.calendar.domain.SOURCE_ZONE_ID

@Composable
fun AniSyncPlusSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AniSyncPlusSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sync = state.sync
    SettingsScreenScaffold(
        title = stringResource(R.string.settings_anisync_plus),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        SettingsGroup {
            SwitchSettingsItem(
                title = stringResource(R.string.anisync_plus_calendar_enabled),
                subtitle = stringResource(R.string.anisync_plus_calendar_enabled_desc),
                checked = state.calendarEnabled,
                onCheckedChange = {
                    viewModel.onAction(AniSyncPlusSettingsAction.SetCalendarEnabled(it))
                }
            )
            SwitchSettingsItem(
                title = stringResource(R.string.anisync_plus_remember_filter),
                subtitle = stringResource(R.string.anisync_plus_remember_filter_desc),
                checked = state.rememberFilter,
                onCheckedChange = {
                    viewModel.onAction(AniSyncPlusSettingsAction.SetRememberFilter(it))
                }
            )
        }

        SettingsSectionLabel(stringResource(R.string.anisync_plus_sync))
        SettingsGroup {
            SettingsItem(
                title = stringResource(R.string.anisync_plus_refresh),
                subtitle = if (state.refreshing) {
                    stringResource(R.string.anisync_plus_refreshing)
                } else {
                    sync.lastAttemptAt?.toString() ?: stringResource(R.string.anisync_plus_never)
                },
                icon = Icons.Outlined.Refresh,
                enabled = !state.refreshing,
                onClick = { viewModel.onAction(AniSyncPlusSettingsAction.Refresh) }
            )
            DiagnosticItem(
                stringResource(R.string.anisync_plus_last_success),
                sync.lastSuccessAt?.toString() ?: stringResource(R.string.anisync_plus_never)
            )
            DiagnosticItem(
                stringResource(R.string.anisync_plus_available_range),
                if (sync.rangeStart != null && sync.rangeEnd != null) {
                    "${sync.rangeStart} – ${sync.rangeEnd}"
                } else {
                    stringResource(R.string.anisync_plus_no_cache)
                }
            )
            DiagnosticItem(
                stringResource(R.string.anisync_plus_counts),
                "${sync.visibleGermanCount} / ${sync.matchedCount} / ${sync.ambiguousCount} / ${sync.unmatchedCount}"
            )
            DiagnosticItem(
                stringResource(R.string.anisync_plus_last_error),
                sync.lastErrorMessage ?: stringResource(R.string.anisync_plus_none)
            )
        }

        SettingsSectionLabel(stringResource(R.string.anisync_plus_technical))
        SettingsGroup {
            DiagnosticItem(stringResource(R.string.anisync_plus_source_zone), SOURCE_ZONE_ID)
            DiagnosticItem(stringResource(R.string.anisync_plus_parser_version), PARSER_VERSION)
            DiagnosticItem(stringResource(R.string.anisync_plus_matcher_version), MATCHER_VERSION)
        }
    }
}

@Composable
private fun DiagnosticItem(title: String, value: String) {
    SettingsItem(
        title = title,
        subtitle = value,
        enabled = false,
        onClick = {}
    )
}
