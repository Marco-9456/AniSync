package com.anisync.android.presentation.profile

import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.EditProfileDialog
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle()
    
    // PERF: Track recomposition count for performance monitoring
    val recomposeCount = remember { mutableIntStateOf(0) }
    SideEffect {
        recomposeCount.intValue++
        Log.d("ProfilePerf", "ProfileScreen recomposed: ${recomposeCount.intValue} times")
    }
    
    // Settings Sheet State
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    // PERF: Stabilize callbacks with remember to prevent unnecessary recompositions
    // When lambdas capture state, wrapping them prevents child composables from recomposing
    val onShowSettings = remember { { showSettingsSheet = true } }
    val onShowEditProfile = remember { { showEditProfileDialog = true } }
    val onHideEditProfile = remember { { showEditProfileDialog = false } }

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
                        // PERF: Use remembered callbacks instead of inline lambdas
                        onSettingsClick = onShowSettings,
                        onEditProfileClick = onShowEditProfile,
                        onMediaClick = onMediaClick,
                        onFavoritesClick = onFavoritesClick,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        titleLanguage = titleLanguage
                    )

                    if (showEditProfileDialog) {
                        EditProfileDialog(
                            initialAbout = state.profile.about ?: "",
                            // PERF: Use remembered callback
                            onDismiss = onHideEditProfile,
                            onSave = { about ->
                                viewModel.updateAbout(about) { _ ->
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
    animatedVisibilityScope: AnimatedVisibilityScope,
    titleLanguage: com.anisync.android.data.TitleLanguage = com.anisync.android.data.TitleLanguage.ROMAJI
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // --- Header Section ---
        // PERF: contentType helps Compose optimize item recycling by grouping similar items
        item(key = "profile_header", contentType = "header") {
            ProfileTopSection(
                profile = profile,
                onSettingsClick = onSettingsClick,
                onEditProfileClick = onEditProfileClick
            )
        }

        // --- Stats Row ---
        // PERF: contentType for stats section
        item(key = "profile_stats", contentType = "stats") {
            ProfileStatsRow(profile = profile)
        }

        // --- Favorites ---
        // PERF: contentType for favorites section
        item(key = "profile_favorites", contentType = "favorites") {
            if (profile.favoriteAnime.isNotEmpty()) {
                FavoritesSection(
                    favorites = profile.favoriteAnime,
                    onMediaClick = onMediaClick,
                    onFavoritesClick = onFavoritesClick,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    titleLanguage = titleLanguage,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }

        // --- Recent Updates ---
        // PERF: contentType for activities section
        item(key = "profile_activities", contentType = "activities") {
            if (profile.activities.isNotEmpty()) {
                RecentUpdatesSection(
                    activities = profile.activities,
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp)
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
    val context = LocalContext.current
    
    Box(modifier = Modifier.fillMaxWidth()) {
        // User Banner with Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            if (profile.bannerUrl != null) {
                AsyncImage(
                    model = profile.bannerUrl,
                    contentDescription = stringResource(R.string.content_description_cover, profile.name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
            }

            // Gradient Overlay from bottom for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.background
                            ),
                            startY = 100f
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 140.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                    .padding(3.dp)
            ) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = stringResource(R.string.content_description_profile_avatar),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(17.dp))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Username
            Text(
                text = profile.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Level & Active time - memoized
            Row(verticalAlignment = Alignment.CenterVertically) {
                val level = remember(profile.animeCount, profile.mangaCount) {
                    (profile.animeCount + profile.mangaCount) / 10
                }
                Text(
                    text = stringResource(R.string.profile_level, level),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                
                val activeTime = remember(profile.activeAt) {
                    formatRelativeTime(profile.activeAt, context)
                }
                Text(
                    text = " · " + stringResource(R.string.profile_active, activeTime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Edit Profile & Settings Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit Profile Button (takes most of the width)
                Button(
                    onClick = onEditProfileClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.profile_edit),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                // Settings Icon Button
                FilledTonalIconButton(
                    onClick = onSettingsClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bio with hashtag styling - using memoized composable
            if (!profile.about.isNullOrBlank()) {
                StyledBioText(bio = profile.about)
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
    animatedVisibilityScope: AnimatedVisibilityScope,
    titleLanguage: com.anisync.android.data.TitleLanguage = com.anisync.android.data.TitleLanguage.ROMAJI
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
            // PERF: contentType for poster cards enables efficient item recycling
            items(favorites, key = { it.mediaId }, contentType = { "poster" }) { entry ->
                // PERF: Stabilize onClick lambda to prevent PosterCard recomposition
                // By remembering the lambda keyed on mediaId, we ensure the same instance is passed
                val onClick = remember(entry.mediaId) { { onMediaClick(entry.mediaId) } }
                
                PosterCard(
                    item = entry,
                    titleLanguage = titleLanguage,
                    onClick = onClick,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    transitionPrefix = "profile_fav",
                    aspectRatio = 120f / 180f,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .width(120.dp)
                        .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(0.2f))
                )
            }
        }
    }
}
