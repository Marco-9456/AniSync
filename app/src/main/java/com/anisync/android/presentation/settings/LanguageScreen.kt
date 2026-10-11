package com.anisync.android.presentation.settings

import android.content.res.Resources
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.data.AppLocale
import com.anisync.android.util.launchUrl
import java.util.Locale

/**
 * App language picker. System default sits on its own above the list, which is ordered by each
 * language's name in the current language, with the native name leading so a reader can always find
 * their own. Selecting one applies the locale immediately via the view model.
 */
@Composable
fun LanguageScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uiLocale = LocalConfiguration.current.locales[0]
    val deviceLocale = remember { Resources.getSystem().configuration.locales[0] }

    val languages = remember(uiLocale) {
        AppLocale.entries
            .filter { it != AppLocale.SYSTEM }
            .map { it to Locale.forLanguageTag(it.tag).getDisplayName(uiLocale) }
            .sortedBy { (_, name) -> name.lowercase(uiLocale) }
    }

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_app_language),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        SettingsGroup(modifier = Modifier.selectableGroup()) {
            val systemSelected = uiState.appLocale == AppLocale.SYSTEM
            RadioSettingsItem(
                title = stringResource(R.string.language_system_default),
                subtitle = stringResource(
                    R.string.language_system_default_desc,
                    deviceLocale.getDisplayLanguage(uiLocale)
                ),
                selected = systemSelected,
                onClick = { viewModel.onAction(SettingsAction.SetAppLocale(AppLocale.SYSTEM)) },
                containerColor = if (systemSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
            )
        }

        SettingsSectionLabel(stringResource(R.string.language_all))
        SettingsGroup(modifier = Modifier.selectableGroup()) {
            languages.forEach { (locale, localizedName) ->
                RadioSettingsItem(
                    title = locale.displayName,
                    subtitle = localizedName.takeUnless { it.equals(locale.displayName, ignoreCase = true) },
                    selected = uiState.appLocale == locale,
                    onClick = { viewModel.onAction(SettingsAction.SetAppLocale(locale)) }
                )
            }
        }

        SettingsGroup {
            SettingsItem(
                icon = Icons.Default.Translate,
                title = stringResource(R.string.language_help_translate),
                subtitle = stringResource(R.string.language_help_translate_desc),
                onClick = { context.launchUrl(WEBLATE_URL) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}
