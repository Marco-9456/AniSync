package com.anisync.android.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.BuildConfig
import com.anisync.android.R
import com.anisync.android.presentation.components.AppLinksPromptDialog

/**
 * Data class representing a searchable settings item.
 */
private data class SettingsItemData(
    val key: String,
    val icon: ImageVector,
    val titleResId: Int,
    val subtitleResId: Int? = null,
    val subtitleArg: String? = null,
    val searchKeywords: List<String>,
    val onClick: () -> Unit
)

/**
 * Main Settings hub screen.
 * Displays navigation items to different settings sections with search functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToLookAndFeel: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToUpdates: () -> Unit,
    onNavigateToDeveloperTools: () -> Unit,
    onNavigateToMediaUpload: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cacheSize = uiState.cacheSize
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = androidx.compose.ui.platform.LocalContext.current

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showAppLinksDialog by remember { mutableStateOf(false) }

    if (showAppLinksDialog) {
        AppLinksPromptDialog(onDismissRequest = { showAppLinksDialog = false })
    }

    // Refresh cache size when this screen is displayed
    LaunchedEffect(Unit) {
        viewModel.onAction(SettingsAction.RefreshCacheSize)
    }

    // Define all settings items with search keywords
    val settingsItems = remember(cacheSize) {
        listOf(
            SettingsItemData(
                key = "look_and_feel",
                icon = Icons.Outlined.Palette,
                titleResId = R.string.settings_look_and_feel,
                subtitleResId = R.string.settings_look_and_feel_desc,
                searchKeywords = listOf(
                    "look",
                    "feel",
                    "theme",
                    "dark",
                    "light",
                    "appearance",
                    "color",
                    "language",
                    "title",
                    "streaming",
                    "haptic",
                    "vibration"
                ),
                onClick = onNavigateToLookAndFeel
            ),
            SettingsItemData(
                key = "notifications",
                icon = Icons.Outlined.Notifications,
                titleResId = R.string.settings_notifications,
                subtitleResId = R.string.settings_notifications_desc,
                searchKeywords = listOf(
                    "notification",
                    "alert",
                    "episode",
                    "reminder",
                    "watching",
                    "planning"
                ),
                onClick = onNavigateToNotifications
            ),
            SettingsItemData(
                key = "storage",
                icon = Icons.Outlined.Storage,
                titleResId = R.string.settings_storage,
                subtitleResId = R.string.settings_storage_subtitle,
                subtitleArg = cacheSize,
                searchKeywords = listOf("storage", "cache", "clear", "data", "memory", "space"),
                onClick = onNavigateToStorage
            ),
            SettingsItemData(
                key = "media_upload",
                icon = Icons.Outlined.CloudUpload,
                titleResId = R.string.settings_media_upload,
                subtitleResId = R.string.settings_media_upload_desc,
                searchKeywords = listOf(
                    "media",
                    "upload",
                    "image",
                    "gif",
                    "video",
                    "attach",
                    "catbox",
                    "imgur",
                    "host"
                ),
                onClick = onNavigateToMediaUpload
            ),
            SettingsItemData(
                key = "account",
                icon = Icons.Outlined.AccountCircle,
                titleResId = R.string.settings_account,
                subtitleResId = R.string.settings_account_desc,
                searchKeywords = listOf(
                    "account",
                    "profile",
                    "logout",
                    "sign out",
                    "user",
                    "anilist"
                ),
                onClick = onNavigateToAccount
            ),
            SettingsItemData(
                key = "app_links",
                icon = Icons.Rounded.Link,
                titleResId = R.string.settings_app_links,
                subtitleResId = R.string.settings_app_links_desc,
                searchKeywords = listOf("links", "app links", "browser", "open", "anilist"),
                onClick = { showAppLinksDialog = true }
            ),
            SettingsItemData(
                key = "updates",
                icon = Icons.Outlined.Update,
                titleResId = R.string.settings_updates,
                subtitleResId = R.string.settings_updates_desc,
                searchKeywords = listOf("update", "version", "download", "upgrade", "new"),
                onClick = onNavigateToUpdates
            ),
            SettingsItemData(
                key = "about",
                icon = Icons.Outlined.Info,
                titleResId = R.string.settings_about,
                subtitleResId = R.string.settings_version,
                subtitleArg = BuildConfig.VERSION_NAME,
                searchKeywords = listOf(
                    "about",
                    "version",
                    "license",
                    "privacy",
                    "terms",
                    "acknowledgments"
                ),
                onClick = onNavigateToAbout
            )
        ) + if (BuildConfig.DEBUG) {
            listOf(
                SettingsItemData(
                    key = "developer_tools",
                    icon = Icons.Outlined.Build,
                    titleResId = R.string.settings_developer_tools,
                    subtitleResId = R.string.settings_developer_tools_desc,
                    searchKeywords = listOf(
                        "developer",
                        "debug",
                        "tools",
                        "test",
                        "notification",
                        "update"
                    ),
                    onClick = onNavigateToDeveloperTools
                )
            )
        } else {
            emptyList()
        }
    }

    // Filter items based on search query
    val filteredItems by remember(searchQuery, settingsItems) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                settingsItems
            } else {
                val query = searchQuery.lowercase().trim()
                settingsItems.filter { item ->
                    item.searchKeywords.any { keyword -> keyword.contains(query) }
                }
            }
        }
    }

    val isSearching = searchQuery.isNotBlank()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search Bar - pinned at top of content
            item(key = "search") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(stringResource(R.string.settings_search_placeholder))
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.a11y_settings_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Outlined.Clear,
                                    contentDescription = stringResource(R.string.clear),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { keyboardController?.hide() }
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Show filtered results or grouped items
            if (isSearching) {
                // Flat list when searching
                if (filteredItems.isEmpty()) {
                    item(key = "no_results") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.search_no_results),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    item(key = "search_results") {
                        SettingsGroup {
                            filteredItems.forEachIndexed { index, item ->
                                val subtitle =
                                    if (item.subtitleArg != null && item.subtitleResId != null) {
                                        stringResource(item.subtitleResId, item.subtitleArg)
                                    } else if (item.subtitleResId != null) {
                                        stringResource(item.subtitleResId)
                                    } else {
                                        null
                                    }

                                SettingsItem(
                                    icon = item.icon,
                                    title = stringResource(item.titleResId),
                                    subtitle = subtitle,
                                    onClick = item.onClick
                                )
                                if (index < filteredItems.lastIndex) {
                                    SettingsDivider()
                                }
                            }
                        }
                    }
                }
            } else {
                // Grouped layout when not searching

                // Look & Feel Group
                item(key = "group_look_and_feel") {
                    SettingsGroup {
                        SettingsItem(
                            icon = Icons.Outlined.Palette,
                            title = stringResource(R.string.settings_look_and_feel),
                            subtitle = stringResource(R.string.settings_look_and_feel_desc),
                            onClick = onNavigateToLookAndFeel
                        )
                    }
                }

                // Notifications & Storage Group
                item(key = "group_notifications_storage") {
                    SettingsGroup {
                        SettingsItem(
                            icon = Icons.Outlined.Notifications,
                            title = stringResource(R.string.settings_notifications),
                            subtitle = stringResource(R.string.settings_notifications_desc),
                            onClick = onNavigateToNotifications
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Outlined.Storage,
                            title = stringResource(R.string.settings_storage),
                            subtitle = stringResource(
                                R.string.settings_storage_subtitle,
                                cacheSize
                            ),
                            onClick = onNavigateToStorage
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Outlined.CloudUpload,
                            title = stringResource(R.string.settings_media_upload),
                            subtitle = stringResource(R.string.settings_media_upload_desc),
                            onClick = onNavigateToMediaUpload
                        )
                    }
                }

                // Account & About Group
                item(key = "group_account_about") {
                    SettingsGroup {
                        SettingsItem(
                            icon = Icons.Outlined.AccountCircle,
                            title = stringResource(R.string.settings_account),
                            subtitle = stringResource(R.string.settings_account_desc),
                            onClick = onNavigateToAccount
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Rounded.Link,
                            title = stringResource(R.string.settings_app_links),
                            subtitle = stringResource(R.string.settings_app_links_desc),
                            onClick = { showAppLinksDialog = true }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Outlined.Update,
                            title = stringResource(R.string.settings_updates),
                            subtitle = stringResource(R.string.settings_updates_desc),
                            onClick = onNavigateToUpdates
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Outlined.Info,
                            title = stringResource(R.string.settings_about),
                            subtitle = stringResource(
                                R.string.settings_version,
                                BuildConfig.VERSION_NAME
                            ),
                            onClick = onNavigateToAbout
                        )
                    }
                }

                // Developer Tools (debug builds only)
                if (BuildConfig.DEBUG) {
                    item(key = "group_developer_tools") {
                        SettingsGroup {
                            SettingsItem(
                                icon = Icons.Outlined.Build,
                                title = stringResource(R.string.settings_developer_tools),
                                subtitle = stringResource(R.string.settings_developer_tools_desc),
                                onClick = onNavigateToDeveloperTools
                            )
                        }
                    }
                }
            }
        }
    }
}
