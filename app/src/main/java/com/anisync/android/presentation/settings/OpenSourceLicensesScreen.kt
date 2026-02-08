package com.anisync.android.presentation.settings

import android.content.Intent
import android.net.Uri
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

/**
 * Data class representing an open source library.
 */
private data class OpenSourceLibrary(
    val name: String,
    val version: String,
    val license: String,
    val url: String? = null
)

/**
 * List of open source libraries used in AniSync.
 */
private val libraries = listOf(
    OpenSourceLibrary(
        name = "Kotlin",
        version = "2.0.0",
        license = "Apache License 2.0",
        url = "https://kotlinlang.org"
    ),
    OpenSourceLibrary(
        name = "Jetpack Compose",
        version = "2024.12.01",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/compose"
    ),
    OpenSourceLibrary(
        name = "Material 3",
        version = "1.4.0-alpha05",
        license = "Apache License 2.0",
        url = "https://m3.material.io"
    ),
    OpenSourceLibrary(
        name = "Apollo Kotlin",
        version = "4.1.1",
        license = "MIT License",
        url = "https://www.apollographql.com/docs/kotlin"
    ),
    OpenSourceLibrary(
        name = "Hilt",
        version = "2.55",
        license = "Apache License 2.0",
        url = "https://dagger.dev/hilt"
    ),
    OpenSourceLibrary(
        name = "Room",
        version = "2.6.1",
        license = "Apache License 2.0",
        url = "https://developer.android.com/training/data-storage/room"
    ),
    OpenSourceLibrary(
        name = "Coil",
        version = "2.7.0",
        license = "Apache License 2.0",
        url = "https://coil-kt.github.io/coil"
    ),
    OpenSourceLibrary(
        name = "Navigation Compose",
        version = "2.8.6",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/compose/navigation"
    ),
    OpenSourceLibrary(
        name = "WorkManager",
        version = "2.10.0",
        license = "Apache License 2.0",
        url = "https://developer.android.com/topic/libraries/architecture/workmanager"
    ),
    OpenSourceLibrary(
        name = "Glance (AppWidgets)",
        version = "1.1.1",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/compose/glance"
    ),
    OpenSourceLibrary(
        name = "kotlinx.serialization",
        version = "1.7.3",
        license = "Apache License 2.0",
        url = "https://github.com/Kotlin/kotlinx.serialization"
    ),
    OpenSourceLibrary(
        name = "kotlinx.coroutines",
        version = "1.8.1",
        license = "Apache License 2.0",
        url = "https://github.com/Kotlin/kotlinx.coroutines"
    )
)

/**
 * Open Source Licenses screen.
 * Displays a list of third-party libraries and their licenses.
 */
@Composable
fun OpenSourceLicensesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_open_source_licenses),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.settings_oss_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup {
            libraries.forEachIndexed { index, library ->
                SettingsItem(
                    title = library.name,
                    subtitle = "${library.version} - ${library.license}",
                    onClick = {
                        library.url?.let { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    }
                )
                if (index < libraries.lastIndex) {
                    SettingsDivider(startPadding = 20.dp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.settings_oss_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
