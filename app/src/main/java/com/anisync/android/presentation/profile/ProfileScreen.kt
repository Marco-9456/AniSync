package com.anisync.android.presentation.profile

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.EditProfileDialog
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.PosterCard
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.ui.theme.StarGold
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    onMediaClick: (Int) -> Unit,
    onLogoutClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onFavoritesClick: () -> Unit = {},
    onStatisticsClick: (userId: Int) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh profile on resume to ensure favorites are up to date
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAction(ProfileAction.Refresh)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }

    // Stabilized callbacks
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
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = 80.dp) // Bottom navigation bar padding
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ProfileUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.onAction(ProfileAction.Refresh) }
                    )
                }

                is ProfileUiState.Success -> {
                    ProfileScreenContent(
                        profile = state.profile,
                        onSettingsClick = onNavigateToSettings,
                        onEditProfileClick = onShowEditProfile,
                        onMediaClick = onMediaClick,
                        onFavoritesClick = onFavoritesClick,
                        onStatisticsClick = { onStatisticsClick(state.profile.id) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        titleLanguage = titleLanguage
                    )

                    if (showEditProfileDialog) {
                        EditProfileDialog(
                            initialAbout = state.profile.about ?: "",
                            onDismiss = onHideEditProfile,
                            onSave = { about ->
                                viewModel.onAction(ProfileAction.UpdateAbout(about))
                                showEditProfileDialog = false
                            }
                        )
                    }
                }
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
    onStatisticsClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    titleLanguage: TitleLanguage = TitleLanguage.ROMAJI
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // --- Header Section ---
        item(key = "profile_header", contentType = "header") {
            ProfileTopSection(
                profile = profile,
                onSettingsClick = onSettingsClick,
                onEditProfileClick = onEditProfileClick,
                onStatisticsClick = onStatisticsClick
            )
        }

        // --- Stats Row ---
        item(key = "profile_stats", contentType = "stats") {
            ProfileStatsRow(profile = profile)
        }

        // --- Favorites ---
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

/**
 * Redesigned Top Section featuring "Expressive" Material Design 3 concepts.
 * - Scalloped/Wavy Avatar shape
 * - Large rounded corners on the banner
 * - Centered, symmetrical layout
 */
@Composable
fun ProfileTopSection(
    profile: UserProfile,
    onSettingsClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onStatisticsClick: () -> Unit
) {
    val context = LocalContext.current
    val surfaceColor = MaterialTheme.colorScheme.background

    // Dimensions
    val bannerHeight = 260.dp
    val avatarSize = 140.dp

    // Shapes
    val bannerShape = remember {
        RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
    }
    val avatarShape = remember {
        ScallopedProfileShape(waves = 10, amplitude = 0.05f)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        // 1. The Banner (Backdrop)
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerHeight)
                    .clip(bannerShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (profile.bannerUrl != null) {
                    AsyncImage(
                        model = profile.bannerUrl,
                        contentDescription = stringResource(
                            R.string.content_description_cover,
                            profile.name
                        ),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient Scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.3f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.15f)
                                    )
                                )
                            )
                    )
                }
            }

            // Spacer to push content down: Half avatar size + padding
            Spacer(modifier = Modifier.height((avatarSize / 2) + 12.dp))

            // 2. User Info (Text & Buttons)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Username
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Meta Info (Level & Active)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                ) {
                    val level = remember(profile.animeCount, profile.mangaCount) {
                        (profile.animeCount + profile.mangaCount) / 10
                    }
                    Text(
                        text = stringResource(R.string.profile_level, level),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    val activeTime = remember(profile.activeAt) {
                        formatRelativeTime(profile.activeAt, context)
                    }
                    Text(
                        text = activeTime,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action Buttons Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    // Statistics
                    FilledTonalIconButton(
                        onClick = onStatisticsClick,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = stringResource(R.string.statistics_view),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Edit Profile
                    Button(
                        onClick = onEditProfileClick,
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        modifier = Modifier.height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
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
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Settings
                    FilledTonalIconButton(
                        onClick = onSettingsClick,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Bio
                if (!profile.about.isNullOrBlank()) {
                    StyledBioText(
                        bio = profile.about,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 3. The Floating Avatar
        Box(
            modifier = Modifier
                .offset(y = bannerHeight - (avatarSize / 2))
                .size(avatarSize)
                .background(surfaceColor, avatarShape) // Border effect
                .padding(6.dp) // Border thickness
                .shadow(
                    elevation = 4.dp,
                    shape = avatarShape,
                    spotColor = Color.Black.copy(alpha = 0.25f)
                )
                .clip(avatarShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = profile.avatarUrl,
                contentDescription = stringResource(R.string.content_description_profile_avatar),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FavoritesSection(
    favorites: List<LibraryEntry>,
    onMediaClick: (Int) -> Unit,
    onFavoritesClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    titleLanguage: TitleLanguage = TitleLanguage.ROMAJI
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
            items(
                items = favorites,
                key = { it.mediaId },
                contentType = { "poster" }
            ) { entry ->
                // Stable click listener
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

/**
 * A custom shape that creates a wavy/scalloped circle.
 * Ideal for "Expressive" Material Design avatars.
 */
class ScallopedProfileShape(
    private val waves: Int = 10,
    private val amplitude: Float = 0.05f // Percentage of radius
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.width / 2f
        val waveAmplitude = radius * amplitude

        // Create 360 points for a smooth circle
        for (angle in 0..360) {
            val radians = Math.toRadians(angle.toDouble())

            // Calculate current radius based on cosine wave for peak alignment
            val currentRadius = radius + (waveAmplitude * cos(waves * radians))

            val x = centerX + (currentRadius * cos(radians)).toFloat()
            val y = centerY + (currentRadius * sin(radians)).toFloat()

            if (angle == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        path.close()
        return Outline.Generic(path)
    }
}