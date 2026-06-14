package com.anisync.android.presentation.settings

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.data.AppLocale

/**
 * App language picker subscreen — a radio list of [AppLocale]s. Selecting one applies the locale
 * immediately via the view model (which also drives AppCompatDelegate).
 */
@Composable
fun LanguageScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_app_language),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        SettingsGroup(modifier = Modifier.selectableGroup()) {
            AppLocale.entries.forEach { locale ->
                RadioSettingsItem(
                    title = locale.displayName,
                    subtitle = if (locale == AppLocale.SYSTEM) {
                        stringResource(R.string.settings_app_language_system_desc)
                    } else null,
                    selected = uiState.appLocale == locale,
                    onClick = { viewModel.onAction(SettingsAction.SetAppLocale(locale)) }
                )
            }
        }
    }
}
