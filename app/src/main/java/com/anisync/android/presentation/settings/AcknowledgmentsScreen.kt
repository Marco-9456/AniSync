package com.anisync.android.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R

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
 * Displays credits to data providers, libraries, and community members.
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

    val coreLibraries = listOf(
        AcknowledgmentItem(
            nameResId = R.string.acknowledgments_android_jetpack,
            descriptionResId = R.string.acknowledgments_android_jetpack_desc,
            url = "https://developer.android.com/jetpack"
        ),
        AcknowledgmentItem(
            nameResId = R.string.acknowledgments_kotlin,
            descriptionResId = R.string.acknowledgments_kotlin_desc,
            url = "https://kotlinlang.org"
        ),
        AcknowledgmentItem(
            nameResId = R.string.acknowledgments_materialkolor,
            descriptionResId = R.string.acknowledgments_materialkolor_desc,
            url = "https://github.com/jordond/materialkolor"
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

        SectionHeader(stringResource(R.string.acknowledgments_section_data))
        Spacer(modifier = Modifier.height(8.dp))
        SettingsGroup {
            dataProviders.forEach { item ->
                SettingsItem(
                    title = stringResource(item.nameResId),
                    subtitle = stringResource(item.descriptionResId),
                    onClick = {
                        item.url?.let { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(stringResource(R.string.acknowledgments_section_libraries))
        Spacer(modifier = Modifier.height(8.dp))
        SettingsGroup {
            coreLibraries.forEach { item ->
                SettingsItem(
                    title = stringResource(item.nameResId),
                    subtitle = stringResource(item.descriptionResId),
                    onClick = {
                        item.url?.let { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(stringResource(R.string.acknowledgments_section_community))
        Spacer(modifier = Modifier.height(8.dp))
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

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}