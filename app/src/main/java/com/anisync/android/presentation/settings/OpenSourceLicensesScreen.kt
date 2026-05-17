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
import com.anisync.android.presentation.settings.components.InfoNotice
import com.anisync.android.util.launchUrl

/**
 * Data class representing an open source library.
 */
private data class OpenSourceLibrary(
    val name: String,
    val version: String,
    val license: String,
    val url: String? = null
)

// Keep versions in sync with gradle/libs.versions.toml when bumping dependencies.
private val libraries = listOf(
    OpenSourceLibrary(
        name = "Kotlin",
        version = "2.2.21",
        license = "Apache License 2.0",
        url = "https://kotlinlang.org"
    ),
    OpenSourceLibrary(
        name = "Jetpack Compose (BOM)",
        version = "2026.04.01",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/compose"
    ),
    OpenSourceLibrary(
        name = "Material 3",
        version = "1.5.0-alpha18",
        license = "Apache License 2.0",
        url = "https://m3.material.io"
    ),
    OpenSourceLibrary(
        name = "Apollo Kotlin",
        version = "4.4.3",
        license = "MIT License",
        url = "https://www.apollographql.com/docs/kotlin"
    ),
    OpenSourceLibrary(
        name = "Hilt (Dagger)",
        version = "2.59.2",
        license = "Apache License 2.0",
        url = "https://dagger.dev/hilt"
    ),
    OpenSourceLibrary(
        name = "Room",
        version = "2.8.4",
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
        version = "2.9.8",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/compose/navigation"
    ),
    OpenSourceLibrary(
        name = "WorkManager",
        version = "2.11.2",
        license = "Apache License 2.0",
        url = "https://developer.android.com/topic/libraries/architecture/workmanager"
    ),
    OpenSourceLibrary(
        name = "Glance (AppWidgets)",
        version = "1.2.0-rc01",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/compose/glance"
    ),
    OpenSourceLibrary(
        name = "kotlinx.serialization",
        version = "1.11.0",
        license = "Apache License 2.0",
        url = "https://github.com/Kotlin/kotlinx.serialization"
    ),
    OpenSourceLibrary(
        name = "kotlinx-collections-immutable",
        version = "0.4.0",
        license = "Apache License 2.0",
        url = "https://github.com/Kotlin/kotlinx.collections.immutable"
    ),
    OpenSourceLibrary(
        name = "MaterialKolor",
        version = "4.1.1",
        license = "Apache License 2.0",
        url = "https://github.com/jordond/materialkolor"
    ),
    OpenSourceLibrary(
        name = "Media3 (ExoPlayer)",
        version = "1.6.1",
        license = "Apache License 2.0",
        url = "https://developer.android.com/media/media3"
    ),
    OpenSourceLibrary(
        name = "JSoup",
        version = "1.22.2",
        license = "MIT License",
        url = "https://jsoup.org"
    ),
    OpenSourceLibrary(
        name = "OkHttp",
        version = "4.12.0",
        license = "Apache License 2.0",
        url = "https://square.github.io/okhttp"
    ),
    OpenSourceLibrary(
        name = "Reorderable",
        version = "3.1.0",
        license = "Apache License 2.0",
        url = "https://github.com/Calvin-LL/Reorderable"
    ),
    OpenSourceLibrary(
        name = "LeakCanary",
        version = "2.14",
        license = "Apache License 2.0",
        url = "https://square.github.io/leakcanary"
    ),
    OpenSourceLibrary(
        name = "Security Crypto",
        version = "1.1.0",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/androidx/releases/security"
    ),
    OpenSourceLibrary(
        name = "KSP",
        version = "2.3.2",
        license = "Apache License 2.0",
        url = "https://github.com/google/ksp"
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
            libraries.forEach { library ->
                SettingsItem(
                    title = library.name,
                    subtitle = "${library.version} - ${library.license}",
                    onClick = { library.url?.let(context::launchUrl) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        InfoNotice()
    }
}
