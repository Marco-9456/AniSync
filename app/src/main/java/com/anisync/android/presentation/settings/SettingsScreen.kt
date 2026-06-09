package com.anisync.android.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.anisync.android.BuildConfig
import com.anisync.android.R
import com.anisync.android.util.launchUrl
import com.anisync.android.data.ThemeMode
import com.anisync.android.presentation.components.AppLinksPromptDialog
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.ui.theme.decorativeAvatarShape

/** AniSync's Weblate project — community translation. Opened from the Settings top bar. */
private const val WEBLATE_URL = "https://hosted.weblate.org/engage/anisync/"

private data class CategoryData(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

/**
 * Main Settings hub screen featuring modern expressive card layouts with search.
 */
@Composable
fun SettingsScreen(
    onNavigateToLookAndFeel: () -> Unit,
    onNavigateToAniList: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToSponsors: () -> Unit,
    onNavigateToUpdates: () -> Unit,
    onNavigateToDeveloperTools: () -> Unit,
    onNavigateToMediaUpload: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Developer Tools is visible in debug builds, or once unlocked via the hidden tap gesture
    // on the About screen's version label (see AboutScreen).
    val devToolsUnlocked by LocalAppSettings.current.devToolsUnlocked
        .collectAsStateWithLifecycle(initialValue = false)
    var showAppLinksDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    if (showAppLinksDialog) {
        AppLinksPromptDialog(onDismissRequest = { showAppLinksDialog = false })
    }

    LaunchedEffect(Unit) {
        viewModel.onAction(SettingsAction.RefreshCacheSize)
    }

    SettingsScreenScaffold(
        title = stringResource(R.string.settings),
        onBackClick = onBackClick,
        modifier = modifier,
        actions = {
            val context = LocalContext.current
            IconButton(onClick = onNavigateToSponsors) {
                Icon(
                    imageVector = Icons.Rounded.VolunteerActivism,
                    contentDescription = stringResource(R.string.settings_sponsors)
                )
            }
            IconButton(onClick = { context.launchUrl(WEBLATE_URL) }) {
                Icon(
                    imageVector = Icons.Outlined.Translate,
                    contentDescription = stringResource(R.string.translate)
                )
            }
        }
    ) {
        val isDark = isSystemInDarkTheme() || uiState.themeMode == ThemeMode.DARK

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.search_settings)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear_search)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
            ),
            singleLine = true
        )

        val allCategories = listOfNotNull(
            CategoryData(
                key = "lookandfeel",
                title = stringResource(R.string.settings_look_and_feel),
                subtitle = stringResource(R.string.settings_look_and_feel_desc),
                icon = Icons.Outlined.Palette,
                onClick = onNavigateToLookAndFeel
            ),
            CategoryData(
                key = "anilist",
                title = stringResource(R.string.settings_anilist_account),
                subtitle = stringResource(R.string.settings_anilist_account_desc),
                icon = Icons.Outlined.Tune,
                onClick = onNavigateToAniList
            ),
            CategoryData(
                key = "notifications",
                title = stringResource(R.string.settings_notifications),
                subtitle = stringResource(R.string.settings_notifications_desc),
                icon = Icons.Outlined.Notifications,
                onClick = onNavigateToNotifications
            ),
            CategoryData(
                key = "storage",
                title = stringResource(R.string.settings_storage),
                subtitle = stringResource(R.string.settings_storage_subtitle, uiState.cacheSize),
                icon = Icons.Outlined.Storage,
                onClick = onNavigateToStorage
            ),
            CategoryData(
                key = "media_upload",
                title = stringResource(R.string.settings_media_upload),
                subtitle = stringResource(R.string.settings_media_upload_desc),
                icon = Icons.Outlined.CloudUpload,
                onClick = onNavigateToMediaUpload
            ),
            CategoryData(
                key = "links",
                title = stringResource(R.string.settings_app_links),
                subtitle = stringResource(R.string.settings_app_links_desc),
                icon = Icons.Rounded.Link,
                onClick = { showAppLinksDialog = true }
            ),
            CategoryData(
                key = "updates",
                title = stringResource(R.string.settings_updates),
                subtitle = stringResource(R.string.settings_updates_desc),
                icon = Icons.Outlined.Update,
                onClick = onNavigateToUpdates
            ),
            CategoryData(
                key = "sponsors",
                title = stringResource(R.string.settings_sponsors),
                subtitle = stringResource(R.string.settings_sponsors_desc),
                icon = Icons.Rounded.VolunteerActivism,
                onClick = onNavigateToSponsors
            ),
            CategoryData(
                key = "about",
                title = stringResource(R.string.settings_about),
                subtitle = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                icon = Icons.Outlined.Info,
                onClick = onNavigateToAbout
            ),
            if (BuildConfig.DEBUG || devToolsUnlocked) {
                CategoryData(
                    key = "dev_tools",
                    title = stringResource(R.string.settings_developer_tools),
                    subtitle = stringResource(R.string.settings_developer_tools_desc),
                    icon = Icons.Outlined.Build,
                    onClick = onNavigateToDeveloperTools
                )
            } else null
        )

        val filteredCategories = if (searchQuery.isBlank()) {
            allCategories
        } else {
            allCategories.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.subtitle.contains(searchQuery, ignoreCase = true)
            }
        }

        if (filteredCategories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_settings_found),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            SettingsGroup {
                filteredCategories.forEachIndexed { index, category ->
                    val shape = when {
                        filteredCategories.size == 1 -> RoundedCornerShape(24.dp)
                        index == 0 -> RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        )

                        index == filteredCategories.size - 1 -> RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp,
                            bottomStart = 24.dp,
                            bottomEnd = 24.dp
                        )
                        else -> RoundedCornerShape(4.dp)
                    }

                    ExpressiveCategoryItem(
                        title = category.title,
                        subtitle = category.subtitle,
                        icon = category.icon,
                        customColors = getCategoryColors(category.key, isDark),
                        onClick = category.onClick,
                        shape = shape
                    )
                }
            }
        }
    }
}

@Composable
fun ExpressiveCategoryItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    customColors: Pair<Color, Color>,
    onClick: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(decorativeAvatarShape())
                    .background(customColors.first)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = customColors.second,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    maxLines = 2
                )
            }
        }
    }
}

private fun getCategoryColors(key: String, isDark: Boolean): Pair<Color, Color> {
    return if (isDark) {
        when (key) {
            "lookandfeel" -> Color(0xFF7D5260) to Color(0xFFFFD8E4)
            "anilist" -> Color(0xFF2E4B5F) to Color(0xFFB8E6FF)
            "notifications" -> Color(0xFF3E4C63) to Color(0xFFD7E3FF)
            "storage" -> Color(0xFF3B4869) to Color(0xFFD9E2FF)
            "media_upload" -> Color(0xFF004F58) to Color(0xFF88FAFF)
            "account" -> Color(0xFF37474F) to Color(0xFFBBD9E8)
            "links" -> Color(0xFF324F34) to Color(0xFFCBEFD0)
            "updates" -> Color(0xFF004D61) to Color(0xFFACEFEE)
            "about" -> Color(0xFF3F474D) to Color(0xFFDEE3EB)
            "dev_tools" -> Color(0xFF6E4E13) to Color(0xFFFFDEAC)
            else -> Color(0xFF37474F) to Color(0xFFBBD9E8)
        }
    } else {
        when (key) {
            "lookandfeel" -> Color(0xFFFFD8E4) to Color(0xFF631835)
            "anilist" -> Color(0xFFB8E6FF) to Color(0xFF0E3346)
            "notifications" -> Color(0xFFD7E3FF) to Color(0xFF253347)
            "storage" -> Color(0xFFD9E2FF) to Color(0xFF27304E)
            "media_upload" -> Color(0xFFCCE8EA) to Color(0xFF004F58)
            "account" -> Color(0xFFD6EAF5) to Color(0xFF103548)
            "links" -> Color(0xFFCBEFD0) to Color(0xFF042106)
            "updates" -> Color(0xFFACEFEE) to Color(0xFF002022)
            "about" -> Color(0xFFEFF1F7) to Color(0xFF44474F)
            "dev_tools" -> Color(0xFFFFDEAC) to Color(0xFF281900)
            else -> Color(0xFFD6EAF5) to Color(0xFF103548)
        }
    }
}