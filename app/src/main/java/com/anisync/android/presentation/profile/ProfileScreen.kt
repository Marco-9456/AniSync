package com.anisync.android.presentation.profile

import android.Manifest
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.UserActivity
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.PosterCard
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.ui.theme.StarGold
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onMediaClick: (Int) -> Unit,
    onLogoutClick: () -> Unit,
    onFavoritesClick: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Settings Sheet State
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ProfileUiState.Error -> {
                    ErrorState(message = state.message, onRetry = { viewModel.refresh() })
                }
                is ProfileUiState.Success -> {
                    ProfileScreenContent(
                        profile = state.profile,
                        onSettingsClick = { showSettingsSheet = true },
                        onEditProfileClick = { showEditProfileDialog = true },
                        onMediaClick = onMediaClick,
                        onFavoritesClick = onFavoritesClick,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )

                    if (showEditProfileDialog) {
                        com.anisync.android.presentation.components.EditProfileDialog(
                            initialAbout = state.profile.about ?: "",
                            onDismiss = { showEditProfileDialog = false },
                            onSave = { about ->
                                viewModel.updateAbout(about) { error ->
                                    // TODO: Show error snackbar
                                }
                                showEditProfileDialog = false
                            }
                        )
                    }
                }
            }
        }
        
        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                sheetState = sheetState
            ) {
                 SettingsSection(
                    viewModel = viewModel,
                    onLogoutClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                             onLogoutClick()
                             showSettingsSheet = false
                        }
                    },
                    modifier = Modifier.padding(bottom = 32.dp).padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreenContent(
    profile: UserProfile,
    onSettingsClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onMediaClick: (Int) -> Unit,
    onFavoritesClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // --- Header Section ---
        item {
            ProfileTopSection(
                profile = profile,
                onSettingsClick = onSettingsClick,
                onEditProfileClick = onEditProfileClick
            )
        }

        // --- Stats Row ---
        item {
            ProfileStatsRow(profile = profile)
        }

        // --- Recent Updates ---
        if (profile.activities.isNotEmpty()) {
            item {
                RecentUpdatesSection(
                    activities = profile.activities,
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp)
                )
            }
        }

        // --- Favorites ---
        item {
            if (profile.favoriteAnime.isNotEmpty()) {
                FavoritesSection(
                    favorites = profile.favoriteAnime,
                    onMediaClick = onMediaClick,
                    onFavoritesClick = onFavoritesClick,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileTopSection(
    profile: UserProfile,
    onSettingsClick: () -> Unit,
    onEditProfileClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // User Banner with Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            if (profile.bannerUrl != null) {
                AsyncImage(
                    model = profile.bannerUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                 Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer))
            }

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Spacer(modifier = Modifier.height(20.dp))

            // Avatar & Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .shadow(8.dp, RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                        .padding(4.dp)
                ) {
                   AsyncImage(
                        model = profile.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text Info
            Text(
                text = profile.name,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Level & Active time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val level = (profile.animeCount + profile.mangaCount) / 10
                    Text(
                        text = "Level $level",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    val activeTime = formatRelativeTime(profile.activeAt, LocalContext.current)
                    Text(
                        text = " • Active $activeTime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Edit & Settings Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(
                        onClick = onEditProfileClick,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!profile.about.isNullOrBlank()) {
                // Strip HTML tags from AniList bio
                val cleanBio = profile.about?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""
                Text(
                    text = cleanBio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ProfileStatsRow(profile: UserProfile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Anime
        StatPill(
            icon = Icons.Default.Movie,
            count = profile.animeCount.toString(),
            label = "ANIME",
            color = MaterialTheme.colorScheme.primaryContainer,
            iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )

        // Manga
        StatPill(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            count = profile.mangaCount.toString(),
            label = "MANGA",
            color = MaterialTheme.colorScheme.secondaryContainer,
            iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )

        // Time
        StatPill(
            icon = Icons.Default.AccessTime,
            count = "${profile.daysWatched.toInt()}d",
            label = "TIME",
            color = MaterialTheme.colorScheme.tertiaryContainer,
            iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatPill(
    icon: ImageVector,
    count: String,
    label: String,
    color: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(56.dp),
        color = color,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = count,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = UtilColor.darken(iconColor, 0.2f)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 8.sp,
                        letterSpacing = 1.sp
                    ),
                    color = UtilColor.darken(iconColor, 0.4f)
                )
            }
        }
    }
}

// Simple Color Utility to ensure contrast if needed, or just use contentColorFor
object UtilColor {
    fun darken(color: Color, factor: Float): Color {
        return Color(
            red = color.red * (1 - factor),
            green = color.green * (1 - factor),
            blue = color.blue * (1 - factor),
            alpha = color.alpha
        )
    }
}

@Composable
fun RecentUpdatesSection(
    activities: List<UserActivity>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(
            title = "Recent Updates",
            level = HeaderLevel.Section,
            padding = PaddingValues(bottom = 16.dp)
        )

        activities.take(5).forEach { activity ->
            UpdateItem(activity)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun UpdateItem(activity: UserActivity) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline Dot
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))

        // Card Content
        Row(modifier = Modifier.fillMaxWidth()) {
             // Media Image Box
             Box(
                 modifier = Modifier
                     .size(56.dp)
                     .clip(RoundedCornerShape(12.dp))
                     .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                 contentAlignment = Alignment.Center
             ) {
                 if (activity.mediaCoverUrl != null) {
                     AsyncImage(
                         model = activity.mediaCoverUrl,
                         contentDescription = null,
                         contentScale = ContentScale.Crop,
                         modifier = Modifier.fillMaxSize()
                     )
                 } else {
                     // Fallback Initials? Or Icon
                     Text(
                         text = activity.mediaTitle.take(2).uppercase(),
                         style = MaterialTheme.typography.titleMedium,
                         color = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                 }
             }

             Spacer(modifier = Modifier.width(16.dp))

             Column {
                 val timeAgo = formatRelativeTime(activity.timestamp, LocalContext.current)
                 Text(
                     text = timeAgo,
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                 )
                 
                 // Construct styled text: "Watched Episode X of Title"
                 val statusText = activity.status ?: "Updated"
                 val progressText = activity.progress ?: ""
                 
                 // Capitalize status
                 val capStatus = statusText.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                 
                 Text(
                     text = buildAnnotatedString {
                         append("$capStatus ")
                         if (progressText.isNotEmpty()) {
                             withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
                                 append(progressText)
                             }
                             append(" of ")
                         }
                         withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                             append(activity.mediaTitle)
                         }
                     },
                     style = MaterialTheme.typography.bodyMedium,
                     color = MaterialTheme.colorScheme.onSurface,
                     lineHeight = 20.sp,
                     maxLines = 2,
                     overflow = TextOverflow.Ellipsis
                 )
                 
                 if (activity.mediaScore != null && activity.mediaScore > 0) {
                     Spacer(modifier = Modifier.height(8.dp))
                     Surface(
                         color = MaterialTheme.colorScheme.surfaceVariant,
                         shape = RoundedCornerShape(8.dp)
                     ) {
                         Text(
                             text = "Score: ${activity.mediaScore}",
                             style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                             color = MaterialTheme.colorScheme.onSurfaceVariant,
                             modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                         )
                     }
                 }
             }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FavoritesSection(
    favorites: List<LibraryEntry>,
    onMediaClick: (Int) -> Unit,
    onFavoritesClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.section_favorites),
            level = HeaderLevel.Section,
            iconColor = StarGold,
            padding = PaddingValues(start = 16.dp, bottom = 12.dp),
            onActionClick = onFavoritesClick
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(favorites) { entry ->
                PosterCard(
                    title = entry.title,
                    coverUrl = entry.coverUrl,
                    mediaId = entry.mediaId,
                    onClick = { onMediaClick(entry.mediaId) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    transitionPrefix = "profile_fav",
                    aspectRatio = 120f / 180f,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .width(120.dp) // Fixed width for scrolling
                        .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(0.2f))
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    viewModel: ProfileViewModel,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.toggleNotifications(true)
    }

    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    
    // Theme selection dialog
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = themeMode,
            onThemeSelected = { 
                viewModel.setThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        Text(
             stringResource(R.string.section_settings),
             style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
             modifier = Modifier.padding(bottom = 8.dp)
        )

        // Look and Feel Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SectionHeader(
                    title = stringResource(R.string.section_look_and_feel),
                    level = HeaderLevel.Subsection,
                    padding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )

                // Theme Row
                Row(
                    modifier = Modifier
                        .clickable { showThemeDialog = true }
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary)
                         Spacer(Modifier.width(16.dp))
                         Text(stringResource(R.string.setting_theme))
                    }
                    Text(getThemeLabel(themeMode), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                // Haptic
                 Row(
                    modifier = Modifier
                        .clickable { viewModel.setHapticEnabled(!hapticEnabled) }
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Icon(Icons.Default.Vibration, null, tint = MaterialTheme.colorScheme.primary)
                         Spacer(Modifier.width(16.dp))
                         Text(stringResource(R.string.setting_haptic_feedback))
                    }
                    Switch(checked = hapticEnabled, onCheckedChange = null)
                }
            }
        }

        // Account Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SectionHeader(
                   title = stringResource(R.string.section_account),
                   level = HeaderLevel.Subsection,
                   padding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )

                // Notification Row
                Row(
                    modifier = Modifier
                        .clickable {
                            if (!isNotificationsEnabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.toggleNotifications(true)
                                }
                            } else {
                                viewModel.toggleNotifications(false)
                            }
                        }
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                         Spacer(Modifier.width(16.dp))
                         Text(stringResource(R.string.control_notifications))
                    }
                    Switch(checked = isNotificationsEnabled, onCheckedChange = null)
                }
                
                 // Logout Row
                Row(
                    modifier = Modifier
                        .clickable { viewModel.logout { onLogoutClick() } }
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.control_log_out), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/**
 * Theme selection dialog following Material Design 3 guidelines.
 * Uses Dialog + Card for precise control over size and shape.
 */
@Composable
fun ThemeSelectionDialog(
    currentTheme: com.anisync.android.data.ThemeMode,
    onThemeSelected: (com.anisync.android.data.ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp), // M3 dialog corner radius
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Title - left aligned
                Text(
                    text = stringResource(R.string.setting_theme),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(modifier = Modifier.height(16.dp))

                // Radio options
                com.anisync.android.data.ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onThemeSelected(mode) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = currentTheme == mode,
                            onClick = { onThemeSelected(mode) }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = getThemeLabel(mode),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(24.dp))
                // Dismiss button aligned to end
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

@Composable
fun getThemeLabel(mode: com.anisync.android.data.ThemeMode): String {
    return when (mode) {
        com.anisync.android.data.ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        com.anisync.android.data.ThemeMode.DARK -> stringResource(R.string.theme_dark)
        com.anisync.android.data.ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    }
}

fun formatRelativeTime(timeMillis: Long?, context: android.content.Context): String {
    if (timeMillis == null || timeMillis == 0L) return "Unknown"
    return try {
        val now = System.currentTimeMillis()
        DateUtils.getRelativeTimeSpanString(timeMillis, now, DateUtils.MINUTE_IN_MILLIS).toString()
    } catch (e: Exception) {
        "Unknown"
    }
}
