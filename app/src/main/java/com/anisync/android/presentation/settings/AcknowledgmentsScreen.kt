package com.anisync.android.presentation.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.util.launchUrl

/**
 * Data class representing an acknowledgment item.
 */
private data class AcknowledgmentItem(
    val nameResId: Int,
    val descriptionResId: Int,
    val url: String? = null
)

/**
 * Acknowledgments screen.
 * Credits non-library contributors: data providers and the community.
 * Library credits live in Open Source Licenses.
 */
@Composable
fun AcknowledgmentsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val dataProviders = listOf(
        AcknowledgmentItem(
            nameResId = R.string.acknowledgments_anilist,
            descriptionResId = R.string.acknowledgments_anilist_desc,
            url = "https://anilist.co"
        )
    )

    val community = listOf(
        AcknowledgmentItem(
            nameResId = R.string.acknowledgments_contributors,
            descriptionResId = R.string.acknowledgments_contributors_desc
        ),
        AcknowledgmentItem(
            nameResId = R.string.acknowledgments_users,
            descriptionResId = R.string.acknowledgments_users_desc
        )
    )

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_acknowledgments),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.settings_acknowledgments_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSectionLabel(stringResource(R.string.acknowledgments_section_data))
        SettingsGroup {
            dataProviders.forEach { item ->
                SettingsItem(
                    title = stringResource(item.nameResId),
                    subtitle = stringResource(item.descriptionResId),
                    onClick = { item.url?.let(context::launchUrl) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSectionLabel(stringResource(R.string.acknowledgments_section_community))
        SettingsGroup {
            community.forEach { item ->
                SettingsItem(
                    title = stringResource(item.nameResId),
                    subtitle = stringResource(item.descriptionResId),
                    onClick = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
