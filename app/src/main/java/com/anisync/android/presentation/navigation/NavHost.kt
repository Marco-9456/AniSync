package com.anisync.android.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.anisync.android.presentation.activity.ActivityDetailScreen
import com.anisync.android.presentation.calendar.CalendarScreen
import com.anisync.android.presentation.details.CharacterDetailsScreen
import com.anisync.android.presentation.details.CharacterMediaGridScreen
import com.anisync.android.presentation.details.MediaCharactersGridScreen
import com.anisync.android.presentation.details.MediaDetailsScreen
import com.anisync.android.presentation.details.MediaRecommendationsGridScreen
import com.anisync.android.presentation.details.MediaRelationsGridScreen
import com.anisync.android.presentation.details.MediaStaffGridScreen
import com.anisync.android.presentation.details.StaffDetailsScreen
import com.anisync.android.presentation.details.StaffMediaGridScreen
import com.anisync.android.presentation.details.StaffProductionMediaGridScreen
import com.anisync.android.presentation.details.StudioDetailsScreen
import com.anisync.android.presentation.details.StudioMediaGridScreen
import com.anisync.android.presentation.discover.DiscoverScreen
import com.anisync.android.presentation.discover.FavoritesGridScreen
import com.anisync.android.presentation.discover.SectionGridScreen
import com.anisync.android.presentation.feed.FeedScreen
import com.anisync.android.presentation.forum.ForumCategoryScreen
import com.anisync.android.presentation.forum.ForumMediaThreadsScreen
import com.anisync.android.presentation.forum.ForumScreen
import com.anisync.android.presentation.forum.ForumThreadInputScreen
import com.anisync.android.presentation.forum.ThreadDetailScreen
import com.anisync.android.presentation.library.LibraryScreen
import com.anisync.android.presentation.login.LoginScreen
import com.anisync.android.presentation.notifications.NotificationsScreen
import com.anisync.android.presentation.profile.ProfileScreen
import com.anisync.android.presentation.review.RecentReviewsScreen
import com.anisync.android.presentation.review.ReviewDetailScreen
import com.anisync.android.presentation.review.WriteReviewScreen
import com.anisync.android.presentation.settings.AboutScreen
import com.anisync.android.presentation.settings.AcknowledgmentsScreen
import com.anisync.android.presentation.settings.AniListSettingsScreen
import com.anisync.android.presentation.settings.DeveloperToolsScreen
import com.anisync.android.presentation.settings.FontSettingsScreen
import com.anisync.android.presentation.settings.LinksScreen
import com.anisync.android.presentation.settings.LookAndFeelScreen
import com.anisync.android.presentation.settings.MediaUploadSettingsScreen
import com.anisync.android.presentation.settings.NotificationsScreen
import com.anisync.android.presentation.settings.OpenSourceLicensesScreen
import com.anisync.android.presentation.settings.SettingsScreen
import com.anisync.android.presentation.settings.SponsorsScreen
import com.anisync.android.presentation.settings.StorageScreen
import com.anisync.android.presentation.settings.UpdatesScreen
import com.anisync.android.presentation.util.AniLinkCallbacks
import com.anisync.android.presentation.util.LocalAniLinkCallbacks

// =============================================================================
// TAB ORDER HELPER
// =============================================================================
// Defines the order of tabs for determining slide direction during navigation.
// Lower order = further left in the navigation hierarchy.

private val tabOrder = mapOf(
    Library::class.qualifiedName to 0,
    Discover::class.qualifiedName to 1,
    Feed::class.qualifiedName to 2,
    Forum::class.qualifiedName to 3,
    Profile::class.qualifiedName to 4
)

/**
 * Determines slide direction for tab transitions based on relative position.
 * @return true if navigating forward (left to right), false if backward (right to left)
 */
private fun isForwardNavigation(fromRoute: String?, toRoute: String?): Boolean {
    val fromOrder = tabOrder[fromRoute] ?: 0
    val toOrder = tabOrder[toRoute] ?: 0
    return toOrder > fromOrder
}

// =============================================================================
// MAIN NAVIGATION HOST
// =============================================================================

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AniSyncNavHost(
    navController: NavHostController,
    onMediaClick: (mediaId: Int, sourceScreen: String) -> Unit,
    modifier: Modifier = Modifier,
    startDestination: Any = Library
) {
    val navigateToUserProfile: (String) -> Unit = { username ->
        username.trim().takeIf { it.isNotEmpty() }?.let { nonEmptyUsername ->
            navController.navigate(UserProfile(nonEmptyUsername))
        }
    }

    val navigateToActivity: (Int) -> Unit = { activityId ->
        navController.navigate(ActivityDetail(activityId))
    }

    val navigateToActivityReply: (Int, Int) -> Unit = { activityId, replyId ->
        navController.navigate(ActivityDetail(activityId = activityId, targetReplyId = replyId))
    }

    // =============================================================================
    // MATERIAL 3 MOTION SPECS (Memoized)
    // =============================================================================
    val motionScheme = MaterialTheme.motionScheme

    // Default specs — used for the deliberate detail push/pop motion.
    val spatialSpec = remember(motionScheme) { motionScheme.defaultSpatialSpec<IntOffset>() }
    val scaleSpec = remember(motionScheme) { motionScheme.defaultSpatialSpec<Float>() }
    val effectsSpec = remember(motionScheme) { motionScheme.defaultEffectsSpec<Float>() }

    // Fast specs — used for tab-to-tab switching to keep the bottom bar feeling snappy.
    val fastSpatialSpec = remember(motionScheme) { motionScheme.fastSpatialSpec<IntOffset>() }
    val fastEffectsSpec = remember(motionScheme) { motionScheme.fastEffectsSpec<Float>() }

    // Tab slide offset (small, since the X-axis is for sibling tabs not full pushes).
    val sharedAxisOffsetFraction = 0.20f

    // =============================================================================
    // TRANSITION HELPERS
    // =============================================================================

    // ---- Tab transitions (X-axis, fast) -------------------------------------
    fun sharedAxisXEnter(forward: Boolean): EnterTransition {
        val offsetMultiplier = if (forward) 1 else -1
        return slideInHorizontally(
            animationSpec = fastSpatialSpec,
            initialOffsetX = { (it * sharedAxisOffsetFraction * offsetMultiplier).toInt() }
        ) + fadeIn(animationSpec = fastEffectsSpec)
    }

    fun sharedAxisXExit(forward: Boolean): ExitTransition {
        val offsetMultiplier = if (forward) -1 else 1
        return slideOutHorizontally(
            animationSpec = fastSpatialSpec,
            targetOffsetX = { (it * sharedAxisOffsetFraction * offsetMultiplier).toInt() }
        ) + fadeOut(animationSpec = fastEffectsSpec)
    }

    // ---- Detail push/pop (parallax + scale) ---------------------------------
    // PixelPlayer-inspired motion. Names preserved (`sharedAxisZ*`) so existing
    // callsites stay; the bodies now produce parallax-slide + scale rather than
    // the prior pure Z-axis (scale-only) transition.

    fun sharedAxisZEnter(): EnterTransition {
        // Push: incoming screen slides in from the right edge.
        return slideInHorizontally(
            animationSpec = spatialSpec,
            initialOffsetX = { it }
        ) + fadeIn(animationSpec = effectsSpec)
    }

    fun sharedAxisZExit(): ExitTransition {
        // Push: outgoing screen drifts left a third of its width (parallax) and fades.
        return slideOutHorizontally(
            animationSpec = spatialSpec,
            targetOffsetX = { -it / 3 }
        ) + fadeOut(animationSpec = effectsSpec)
    }

    fun sharedAxisZPopEnter(): EnterTransition {
        // Pop: re-entering screen slides from the parallax position back to its
        // place, with a slight zoom-in for depth.
        return slideInHorizontally(
            animationSpec = spatialSpec,
            initialOffsetX = { -it / 3 }
        ) + scaleIn(
            animationSpec = scaleSpec,
            initialScale = 0.9f
        )
    }

    fun sharedAxisZPopExit(): ExitTransition {
        // Pop: outgoing detail slides off to the right and scales down for depth.
        return slideOutHorizontally(
            animationSpec = spatialSpec,
            targetOffsetX = { it }
        ) + scaleOut(
            animationSpec = scaleSpec,
            targetScale = 0.75f
        )
    }

    SharedTransitionLayout(modifier = modifier) {
        // Provide centralized link routing callbacks so any screen can navigate
        // in-app when a recognizable AniList URL is clicked (forum posts, etc.)
        val aniLinkCallbacks = remember(navController) {
            AniLinkCallbacks(
                onMediaClick = { mediaId ->
                    navController.navigateSafely(MediaDetails(mediaId, "link"))
                },
                onThreadClick = { threadId, commentId ->
                    navController.navigateSafely(
                        ForumThreadDetail(
                            threadId = threadId,
                            threadTitle = "",
                            commentId = commentId ?: 0
                        )
                    )
                },
                onCharacterClick = { characterId ->
                    navController.navigateSafely(CharacterDetails(characterId))
                },
                onStaffClick = { staffId ->
                    navController.navigateSafely(StaffDetails(staffId))
                },
                onUserClick = { username ->
                    navController.navigateSafely(UserProfile(username))
                },
                onReviewClick = { reviewId ->
                    navController.navigateSafely(ReviewDetail(reviewId))
                },
                onActivityClick = { activityId ->
                    navController.navigateSafely(ActivityDetail(activityId))
                }
            )
        }

        CompositionLocalProvider(LocalAniLinkCallbacks provides aniLinkCallbacks) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
        ) {
            // =================================================================
            // LOGIN SCREEN
            // =================================================================
            // Full slide transitions for authentication flow
            composable<Login>(
                enterTransition = { sharedAxisXEnter(forward = true) },
                exitTransition = { sharedAxisXExit(forward = true) },
                popEnterTransition = { sharedAxisXEnter(forward = false) },
                popExitTransition = { sharedAxisXExit(forward = false) }
            ) {
                LoginScreen()
            }

            // =================================================================
            // MAIN TABS - Shared Axis X (Horizontal)
            // =================================================================
            // Tab navigation uses directional awareness for natural feel

            composable<Library>(
                enterTransition = {
                    val forward = isForwardNavigation(
                        fromRoute = initialState.destination.route,
                        toRoute = Library::class.qualifiedName
                    )
                    sharedAxisXEnter(forward = !forward)
                },
                exitTransition = {
                    val forward = isForwardNavigation(
                        fromRoute = Library::class.qualifiedName,
                        toRoute = targetState.destination.route
                    )
                    sharedAxisXExit(forward = forward)
                },
                popEnterTransition = { sharedAxisXEnter(forward = false) },
                popExitTransition = { sharedAxisXExit(forward = false) }
            ) {
                // Optimization: Memoize the callback to prevent unnecessary recompositions of LibraryScreen
                val onLibraryMediaClick = remember(onMediaClick) { 
                    { mediaId: Int -> onMediaClick(mediaId, "library") } 
                }
                
                LibraryScreen(
                    onMediaClick = onLibraryMediaClick,
                    onNavigateToCalendar = { navController.navigate(Calendar) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            composable<Discover>(
                enterTransition = {
                    val forward = isForwardNavigation(
                        fromRoute = initialState.destination.route,
                        toRoute = Discover::class.qualifiedName
                    )
                    sharedAxisXEnter(forward = forward)
                },
                exitTransition = {
                    val forward = isForwardNavigation(
                        fromRoute = Discover::class.qualifiedName,
                        toRoute = targetState.destination.route
                    )
                    sharedAxisXExit(forward = forward)
                },
                popEnterTransition = { sharedAxisXEnter(forward = false) },
                popExitTransition = { sharedAxisXExit(forward = false) }
            ) {
                // Optimization: Memoize callbacks
                val onDiscoverMediaClick = remember(onMediaClick) {
                    { mediaId: Int -> onMediaClick(mediaId, "discover") }
                }
                val onSectionClick = remember(navController) {
                    { title: String, sectionType: String, mediaType: com.anisync.android.type.MediaType ->
                        navController.navigate(SectionGrid(title, sectionType, mediaType.name))
                    }
                }

                DiscoverScreen(
                    onMediaClick = onDiscoverMediaClick,
                    onCharacterClick = { characterId ->
                        navController.navigate(CharacterDetails(characterId))
                    },
                    onStaffClick = { staffId ->
                        navController.navigate(StaffDetails(staffId))
                    },
                    onStudioClick = { studioId ->
                        navController.navigate(StudioDetails(studioId))
                    },
                    onUserClick = navigateToUserProfile,
                    onSectionSeeAllClick = onSectionClick,
                    onReviewClick = { reviewId ->
                        navController.navigate(ReviewDetail(reviewId))
                    },
                    onRecentReviewsSeeAllClick = { mediaType ->
                        navController.navigate(RecentReviews(mediaType.name))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            // =================================================================
            // FEED TAB - Shared Axis X (Horizontal)
            // =================================================================

            composable<Feed>(
                enterTransition = {
                    val forward = isForwardNavigation(
                        fromRoute = initialState.destination.route,
                        toRoute = Feed::class.qualifiedName
                    )
                    sharedAxisXEnter(forward = forward)
                },
                exitTransition = {
                    val forward = isForwardNavigation(
                        fromRoute = Feed::class.qualifiedName,
                        toRoute = targetState.destination.route
                    )
                    sharedAxisXExit(forward = forward)
                },
                popEnterTransition = { sharedAxisXEnter(forward = false) },
                popExitTransition = { sharedAxisXExit(forward = false) }
            ) {
                val onLogin = remember(navController) {
                    {
                        navController.navigate(Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }

                val onFeedMediaClick = remember(onMediaClick) {
                    { mediaId: Int -> onMediaClick(mediaId, "feed") }
                }

                FeedScreen(
                    onActivityClick = navigateToActivity,
                    onUserClick = navigateToUserProfile,
                    onMediaClick = onFeedMediaClick,
                    onLastReplyClick = navigateToActivityReply,
                    onLoginClick = onLogin
                )
            }

            composable<Profile>(
                enterTransition = {
                    val forward = isForwardNavigation(
                        fromRoute = initialState.destination.route,
                        toRoute = Profile::class.qualifiedName
                    )
                    sharedAxisXEnter(forward = forward)
                },
                exitTransition = {
                    val forward = isForwardNavigation(
                        fromRoute = Profile::class.qualifiedName,
                        toRoute = targetState.destination.route
                    )
                    sharedAxisXExit(forward = forward)
                },
                popEnterTransition = { sharedAxisXEnter(forward = false) },
                popExitTransition = { sharedAxisXExit(forward = false) }
            ) {
                // Optimization: Memoize callbacks
                val onProfileMediaClick = remember(onMediaClick) { 
                    { mediaId: Int -> onMediaClick(mediaId, "profile") } 
                }
                val onLogout = remember(navController) {
                    {
                        navController.navigate(Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
                val onStatistics = remember(navController) {
                    { userId: Int -> navController.navigate(Statistics(userId)) }
                }
                val onNavigateToSettings = remember(navController) {
                    { navController.navigate(Settings) }
                }
                val onNavigateToNotifications = remember(navController) {
                    { navController.navigate(Notifications) }
                }

                ProfileScreen(
                    onMediaClick = onProfileMediaClick,
                    onCharacterClick = { characterId ->
                        navController.navigate(CharacterDetails(characterId))
                    },
                    onStaffClick = { staffId ->
                        navController.navigate(StaffDetails(staffId))
                    },
                    onVoiceActorClick = { staffId ->
                        navController.navigate(StaffDetails(staffId))
                    },
                    onStudioClick = { studioId ->
                        navController.navigate(StudioDetails(studioId))
                    },
                    onUserClick = navigateToUserProfile,
                    onThreadClick = { threadId, threadTitle ->
                        navController.navigate(ForumThreadDetail(threadId, threadTitle))
                    },
                    onCommentClick = { threadId, commentId, threadTitle ->
                        navController.navigate(
                            ForumThreadDetail(threadId, threadTitle, commentId)
                        )
                    },
                    onActivityClick = navigateToActivity,
                    onLastReplyClick = navigateToActivityReply,
                    onLogoutClick = onLogout,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToNotifications = onNavigateToNotifications,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            composable<UserProfile>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() },
                deepLinks = listOf(
                    navDeepLink { uriPattern = "anisync://user/{username}" },
                    navDeepLink { uriPattern = "https://anilist.co/user/{username}" }
                )
            ) {
                val onProfileMediaClick = remember(onMediaClick) { 
                    { mediaId: Int -> onMediaClick(mediaId, "user_profile") } 
                }
                
                ProfileScreen(
                    onMediaClick = onProfileMediaClick,
                    onCharacterClick = { characterId ->
                        navController.navigate(CharacterDetails(characterId))
                    },
                    onStaffClick = { staffId ->
                        navController.navigate(StaffDetails(staffId))
                    },
                    onVoiceActorClick = { staffId ->
                        navController.navigate(StaffDetails(staffId))
                    },
                    onStudioClick = { studioId ->
                        navController.navigate(StudioDetails(studioId))
                    },
                    onUserClick = navigateToUserProfile,
                    onThreadClick = { threadId, threadTitle ->
                        navController.navigate(ForumThreadDetail(threadId, threadTitle))
                    },
                    onCommentClick = { threadId, commentId, threadTitle ->
                        navController.navigate(
                            ForumThreadDetail(threadId, threadTitle, commentId)
                        )
                    },
                    onActivityClick = navigateToActivity,
                    onLastReplyClick = navigateToActivityReply,
                    onLogoutClick = { }, // Not used for other users
                    onNavigateToSettings = { }, // Not used for other users
                    isOwnProfile = false,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            // =================================================================
            // FORUM TAB - Shared Axis X (Horizontal)
            // =================================================================

            composable<Forum>(
                enterTransition = {
                    val forward = isForwardNavigation(
                        fromRoute = initialState.destination.route,
                        toRoute = Forum::class.qualifiedName
                    )
                    sharedAxisXEnter(forward = forward)
                },
                exitTransition = {
                    val forward = isForwardNavigation(
                        fromRoute = Forum::class.qualifiedName,
                        toRoute = targetState.destination.route
                    )
                    sharedAxisXExit(forward = forward)
                },
                popEnterTransition = { sharedAxisXEnter(forward = false) },
                popExitTransition = { sharedAxisXExit(forward = false) }
            ) {
                val onThreadClick = remember(navController) {
                    { threadId: Int, threadTitle: String ->
                        navController.navigate(ForumThreadDetail(threadId, threadTitle))
                    }
                }
                val onCreateThreadClick = remember(navController) {
                    { navController.navigate(CreateThread()) }
                }

                val onCreateThreadForMedia = remember(navController) {
                    { mediaId: Int, title: String, coverUrl: String? ->
                        navController.navigate(
                            CreateThread(
                                mediaId = mediaId,
                                mediaTitle = title,
                                mediaCoverUrl = coverUrl.orEmpty()
                            )
                        )
                    }
                }

                val onThreadCommentClick = remember(navController) {
                    { threadId: Int, commentId: Int ->
                        navController.navigate(
                            ForumThreadDetail(threadId, "", commentId)
                        )
                    }
                }

                ForumScreen(
                    onThreadClick = onThreadClick,
                    onThreadCommentClick = onThreadCommentClick,
                    onCreateThreadClick = onCreateThreadClick,
                    onCreateThreadForMedia = onCreateThreadForMedia,
                    onUserClick = navigateToUserProfile
                )
            }

            // =================================================================
            // DETAILS SCREEN - Shared Axis Z (Depth)
            // =================================================================
            // Navigating to detail view uses scale+fade for depth perception
            // =================================================================
            // MEDIA DETAILS SCREEN - Shared Axis Z (Depth)
            // =================================================================
            // Navigating to detail view uses scale+fade for depth perception
            composable<MediaDetails>(
                deepLinks = listOf(
                    // Custom app scheme (for widgets, internal links)
                    navDeepLink<MediaDetails>(basePath = "anisync://details"),
                    // AniList anime URLs (e.g., https://anilist.co/anime/16498)
                    navDeepLink { uriPattern = "https://anilist.co/anime/{mediaId}" },
                    // AniList anime URLs with slug (e.g., https://anilist.co/anime/16498/attack-on-titan)
                    navDeepLink { uriPattern = "https://anilist.co/anime/{mediaId}/{slug}" },
                    // AniList manga URLs (e.g., https://anilist.co/manga/30002)
                    navDeepLink { uriPattern = "https://anilist.co/manga/{mediaId}" },
                    // AniList manga URLs with slug (e.g., https://anilist.co/manga/30002/berserk)
                    navDeepLink { uriPattern = "https://anilist.co/manga/{mediaId}/{slug}" }
                ),
                // Fade only: the shared cover/title/container morph (card → page) carries the
                // spatial motion. A horizontal slide here competed with that morph — the page
                // arrived while the cover was still flying. Let the shared bounds own the movement.
                enterTransition = { fadeIn(animationSpec = effectsSpec) },
                exitTransition = { fadeOut(animationSpec = effectsSpec) },
                popEnterTransition = { fadeIn(animationSpec = effectsSpec) },
                popExitTransition = { fadeOut(animationSpec = effectsSpec) }
            ) { backStackEntry ->
                val details: MediaDetails = backStackEntry.toRoute()

                MediaDetailsScreen(
                    mediaId = details.mediaId,
                    sourceScreen = details.sourceScreen,
                    onBackClick = { navController.popBackStack() },
                    onRelationClick = { relationMediaId ->
                        navController.navigate(MediaDetails(relationMediaId, "media_details"))
                    },
                    onCharacterClick = { characterId ->
                        navController.navigate(CharacterDetails(characterId))
                    },
                    onStaffClick = { staffId ->
                        navController.navigate(StaffDetails(staffId))
                    },
                    onStudioClick = { studioId ->
                        navController.navigate(StudioDetails(studioId))
                    },
                    onCastSeeAllClick = { mediaId, mediaTitle ->
                        navController.navigate(MediaCharactersGrid(mediaId, mediaTitle))
                    },
                    onStaffSeeAllClick = { mediaId, mediaTitle ->
                        navController.navigate(MediaStaffGrid(mediaId, mediaTitle))
                    },
                    onRelatedSeeAllClick = { mediaId, mediaTitle ->
                        navController.navigate(MediaRelationsGrid(mediaId, mediaTitle))
                    },
                    onRecommendationsSeeAllClick = { mediaId, mediaTitle ->
                        navController.navigate(MediaRecommendationsGrid(mediaId, mediaTitle))
                    },
                    onWriteReviewClick = { mediaId, mediaTitle ->
                        navController.navigate(WriteReview(mediaId, mediaTitle))
                    },
                    onDiscussionClick = { threadId, threadTitle ->
                        navController.navigate(ForumThreadDetail(threadId, threadTitle))
                    },
                    onViewAllDiscussions = { mediaId, mediaTitle ->
                        navController.navigate(ForumMediaThreads(mediaId, mediaTitle))
                    },
                    onStartDiscussion = { mediaId, title, coverUrl ->
                        navController.navigate(
                            CreateThread(
                                mediaId = mediaId,
                                mediaTitle = title,
                                mediaCoverUrl = coverUrl.orEmpty()
                            )
                        )
                    },
                    onUserClick = navigateToUserProfile,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            // =================================================================
            // MEDIA RECOMMENDATIONS GRID - Shared Axis Z (Depth)
            // =================================================================
            composable<MediaRecommendationsGrid>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val grid: MediaRecommendationsGrid = backStackEntry.toRoute()
                MediaRecommendationsGridScreen(
                    mediaId = grid.mediaId,
                    mediaTitle = grid.mediaTitle,
                    onBackClick = { navController.popBackStack() },
                    onRecommendationClick = { recId ->
                        navController.navigate(MediaDetails(recId, "recommendations_grid"))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            // =================================================================
            // WRITE REVIEW EDITOR - Shared Axis Z (Depth)
            // =================================================================
            composable<WriteReview>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                WriteReviewScreen(
                    onBackClick = { navController.popBackStack() },
                    onReviewSaved = { navController.popBackStack() }
                )
            }

            // =================================================================
            // REVIEW DETAIL SCREEN - Shared Axis Z (Depth)
            // =================================================================
            composable<ReviewDetail>(
                deepLinks = listOf(
                    navDeepLink { uriPattern = "anisync://review/{reviewId}" },
                    navDeepLink { uriPattern = "https://anilist.co/review/{reviewId}" },
                    navDeepLink { uriPattern = "https://anilist.co/review/{reviewId}/{slug}" }
                ),
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val route: ReviewDetail = backStackEntry.toRoute()
                ReviewDetailScreen(
                    reviewId = route.reviewId,
                    onBackClick = { navController.popBackStack() },
                    onUserClick = navigateToUserProfile,
                    onMediaClick = { mediaId ->
                        navController.navigate(MediaDetails(mediaId, "review"))
                    }
                )
            }

            // =================================================================
            // RECENT REVIEWS SCREEN - Shared Axis Z (Depth)
            // =================================================================
            composable<RecentReviews>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                RecentReviewsScreen(
                    onBackClick = { navController.popBackStack() },
                    onReviewClick = { reviewId ->
                        navController.navigate(ReviewDetail(reviewId))
                    },
                    onUserClick = navigateToUserProfile
                )
            }

            // =================================================================
            // CHARACTER DETAILS SCREEN - Shared Axis Z (Depth)
            // =================================================================
            composable<CharacterDetails>(
                deepLinks = listOf(
                    // AniList character URLs (e.g., https://anilist.co/character/40882)
                    navDeepLink { uriPattern = "https://anilist.co/character/{characterId}" },
                    // AniList character URLs with slug
                    navDeepLink { uriPattern = "https://anilist.co/character/{characterId}/{slug}" }
                ),
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val character: CharacterDetails = backStackEntry.toRoute()
                CharacterDetailsScreen(
                    characterId = character.characterId,
                    onBackClick = { navController.popBackStack() },
                    onMediaClick = { mediaId ->
                        navController.navigate(MediaDetails(mediaId, com.anisync.android.presentation.util.TransitionKeys.CHARACTER))
                    },
                    onMediaSeeAllClick = { characterId, characterName ->
                        navController.navigate(CharacterMediaGrid(characterId, characterName))
                    },
                    onStaffClick = { staffId ->
                        navController.navigate(StaffDetails(staffId))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            // =================================================================
            // STAFF DETAILS SCREEN - Shared Axis Z (Depth)
            // =================================================================
            composable<StaffDetails>(
                deepLinks = listOf(
                    navDeepLink { uriPattern = "https://anilist.co/staff/{staffId}" },
                    navDeepLink { uriPattern = "https://anilist.co/staff/{staffId}/{slug}" }
                ),
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val staff: StaffDetails = backStackEntry.toRoute()
                StaffDetailsScreen(
                    staffId = staff.staffId,
                    onBackClick = { navController.popBackStack() },
                    onMediaClick = { mediaId ->
                        navController.navigate(MediaDetails(mediaId, "staff"))
                    },
                    onCharacterClick = { characterId ->
                        navController.navigate(CharacterDetails(characterId))
                    },
                    onMediaSeeAllClick = { staffId, staffName ->
                        navController.navigate(StaffMediaGrid(staffId, staffName))
                    },
                    onProductionSeeAllClick = { staffId, staffName ->
                        navController.navigate(StaffProductionMediaGrid(staffId, staffName))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            // =================================================================
            // STUDIO DETAILS SCREEN - Shared Axis Z (Depth)
            // =================================================================
            composable<StudioDetails>(
                deepLinks = listOf(
                    navDeepLink { uriPattern = "https://anilist.co/studio/{studioId}" },
                    navDeepLink { uriPattern = "https://anilist.co/studio/{studioId}/{slug}" }
                ),
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val studio: StudioDetails = backStackEntry.toRoute()
                StudioDetailsScreen(
                    studioId = studio.studioId,
                    onBackClick = { navController.popBackStack() },
                    onMediaClick = { mediaId ->
                        navController.navigate(MediaDetails(mediaId, "studio"))
                    },
                    onMediaSeeAllClick = { studioId, studioName ->
                        navController.navigate(StudioMediaGrid(studioId, studioName))
                    }
                )
            }

            // =================================================================
            // STUDIO MEDIA GRID - Shared Axis Z (Depth)
            // =================================================================
            composable<StudioMediaGrid>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val grid: StudioMediaGrid = backStackEntry.toRoute()
                StudioMediaGridScreen(
                    studioId = grid.studioId,
                    studioName = grid.studioName,
                    onBackClick = { navController.popBackStack() },
                    onMediaClick = { mediaId ->
                        navController.navigate(MediaDetails(mediaId, "studio_grid"))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            // =================================================================
            // STAFF MEDIA GRID - Shared Axis Z (Depth)
            // =================================================================
            composable<StaffMediaGrid>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val grid: StaffMediaGrid = backStackEntry.toRoute()
                StaffMediaGridScreen(
                    staffId = grid.staffId,
                    staffName = grid.staffName,
                    onBackClick = { navController.popBackStack() },
                    onMediaClick = { mediaId ->
                        navController.navigate(MediaDetails(mediaId, "staff_grid"))
                    },
                    onCharacterClick = { characterId ->
                        navController.navigate(CharacterDetails(characterId))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            // =================================================================
            // STAFF PRODUCTION MEDIA GRID - Shared Axis Z (Depth)
            // =================================================================
            composable<StaffProductionMediaGrid>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val grid: StaffProductionMediaGrid = backStackEntry.toRoute()
                StaffProductionMediaGridScreen(
                    staffId = grid.staffId,
                    staffName = grid.staffName,
                    onBackClick = { navController.popBackStack() },
                    onMediaClick = { mediaId ->
                        navController.navigate(MediaDetails(mediaId, "staff_production_grid"))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            // =================================================================
            // SECTION GRID SCREEN - Shared Axis Z (Depth)
            // =================================================================
            // Grid view for "See All" uses same depth pattern as Details
            composable<SectionGrid>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val sectionGrid: SectionGrid = backStackEntry.toRoute()

                if (sectionGrid.sectionType == "favorites") {
                    FavoritesGridScreen(
                        sectionTitle = sectionGrid.sectionTitle,
                        onBackClick = { navController.popBackStack() },
                        onMediaClick = { mediaId ->
                            navController.navigate(MediaDetails(mediaId, "sectiongrid"))
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                    )
                } else {
                    SectionGridScreen(
                        sectionTitle = sectionGrid.sectionTitle,
                        sectionType = sectionGrid.sectionType,
                        onBackClick = { navController.popBackStack() },
                        onMediaClick = { mediaId ->
                            navController.navigate(MediaDetails(mediaId, "sectiongrid"))
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                    )
                }
            }

            // =================================================================
            // MEDIA CHARACTERS GRID - Shared Axis Z (Depth)
            // =================================================================
            composable<MediaCharactersGrid>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val grid: MediaCharactersGrid = backStackEntry.toRoute()
                MediaCharactersGridScreen(
                    mediaId = grid.mediaId,
                    mediaTitle = grid.mediaTitle,
                    onBackClick = { navController.popBackStack() },
                    onCharacterClick = { characterId ->
                        navController.navigate(CharacterDetails(characterId))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            // =================================================================
            // MEDIA STAFF GRID - Shared Axis Z (Depth)
            // =================================================================
            composable<MediaStaffGrid>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val grid: MediaStaffGrid = backStackEntry.toRoute()
                MediaStaffGridScreen(
                    mediaId = grid.mediaId,
                    mediaTitle = grid.mediaTitle,
                    onBackClick = { navController.popBackStack() },
                    onStaffClick = { staffId ->
                        navController.navigate(StaffDetails(staffId))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            // =================================================================
            // MEDIA RELATIONS GRID - Shared Axis Z (Depth)
            // =================================================================
            composable<MediaRelationsGrid>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val grid: MediaRelationsGrid = backStackEntry.toRoute()
                MediaRelationsGridScreen(
                    mediaId = grid.mediaId,
                    mediaTitle = grid.mediaTitle,
                    onBackClick = { navController.popBackStack() },
                    onRelationClick = { relationMediaId ->
                        navController.navigate(MediaDetails(relationMediaId, "relations_grid"))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            // =================================================================
            // CHARACTER MEDIA GRID - Shared Axis Z (Depth)
            // =================================================================
            composable<CharacterMediaGrid>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val grid: CharacterMediaGrid = backStackEntry.toRoute()
                CharacterMediaGridScreen(
                    characterId = grid.characterId,
                    characterName = grid.characterName,
                    onBackClick = { navController.popBackStack() },
                    onMediaClick = { mediaId ->
                        navController.navigate(MediaDetails(mediaId, com.anisync.android.presentation.util.TransitionKeys.CHARACTER_GRID))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            // =================================================================
            // FORUM CATEGORY BROWSE - Shared Axis Z (Depth)
            // =================================================================
            composable<ForumCategoryBrowse>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val route: ForumCategoryBrowse = backStackEntry.toRoute()
                ForumCategoryScreen(
                    categoryId = route.categoryId,
                    categoryName = route.categoryName,
                    onBackClick = { navController.popBackStack() },
                    onThreadClick = { threadId, threadTitle ->
                        navController.navigate(ForumThreadDetail(threadId, threadTitle))
                    },
                    onThreadCommentClick = { threadId, commentId ->
                        navController.navigate(
                            ForumThreadDetail(threadId, "", commentId)
                        )
                    },
                    onUserClick = navigateToUserProfile
                )
            }

            // =================================================================
            // FORUM THREAD DETAIL - Shared Axis Z (Depth)
            // =================================================================
            composable<ForumThreadDetail>(
                deepLinks = listOf(
                    navDeepLink<ForumThreadDetail>(basePath = "anisync://forum/thread"),
                    // AniList forum thread URLs (e.g., https://anilist.co/forum/thread/12345)
                    navDeepLink { uriPattern = "https://anilist.co/forum/thread/{threadId}" },
                    // AniList forum thread URLs with slug
                    navDeepLink { uriPattern = "https://anilist.co/forum/thread/{threadId}/{slug}" },
                    // Comment-anchored URLs
                    navDeepLink { uriPattern = "https://anilist.co/forum/thread/{threadId}/comment/{commentId}" },
                ),
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val route: ForumThreadDetail = backStackEntry.toRoute()
                ThreadDetailScreen(
                    threadId = route.threadId,
                    threadTitle = route.threadTitle,
                    targetCommentId = if (route.commentId != 0) route.commentId else null,
                    onBackClick = { navController.popBackStack() },
                    onUserClick = navigateToUserProfile
                )
            }

            // =================================================================
            // ACTIVITY DETAIL - Shared Axis Z (Depth)
            // =================================================================
            composable<ActivityDetail>(
                deepLinks = listOf(
                    navDeepLink<ActivityDetail>(basePath = "anisync://activity"),
                    navDeepLink { uriPattern = "https://anilist.co/activity/{activityId}" },
                    navDeepLink { uriPattern = "https://anilist.co/activity/{activityId}/{slug}" }
                ),
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val route: ActivityDetail = backStackEntry.toRoute()
                ActivityDetailScreen(
                    activityId = route.activityId,
                    targetReplyId = if (route.targetReplyId != 0) route.targetReplyId else null,
                    onBackClick = { navController.popBackStack() },
                    onUserClick = navigateToUserProfile
                )
            }

            // =================================================================
            // NOTIFICATIONS INBOX - Shared Axis Z (Depth)
            // =================================================================
            composable<Notifications>(
                deepLinks = listOf(
                    navDeepLink { uriPattern = "anisync://notifications" }
                ),
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                NotificationsScreen(
                    onBackClick = { navController.popBackStack() },
                    onMediaClick = { mediaId ->
                        navController.navigate(MediaDetails(mediaId, "notifications"))
                    },
                    onUserClick = navigateToUserProfile,
                    onActivityClick = navigateToActivity,
                    onThreadClick = { threadId, commentId ->
                        navController.navigate(
                            ForumThreadDetail(threadId, "", commentId ?: 0)
                        )
                    },
                    onSettingsClick = { navController.navigate(SettingsNotifications) }
                )
            }

            // =================================================================
            // AIRING CALENDAR - Shared Axis Z (Depth)
            // =================================================================
            composable<Calendar>(
                deepLinks = listOf(
                    navDeepLink { uriPattern = "anisync://calendar" }
                ),
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                CalendarScreen(
                    onBackClick = { navController.popBackStack() },
                    onMediaClick = { mediaId ->
                        navController.navigate(MediaDetails(mediaId, "calendar"))
                    }
                )
            }

            // =================================================================
            // CREATE THREAD - Shared Axis Z (Depth)
            // =================================================================
            composable<CreateThread>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                ForumThreadInputScreen(
                    onBackClick = { navController.popBackStack() },
                    onThreadCreated = { navController.popBackStack() }
                )
            }

            // =================================================================
            // FORUM MEDIA THREADS - Shared Axis Z (Depth)
            // =================================================================
            composable<ForumMediaThreads>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) { backStackEntry ->
                val route: ForumMediaThreads = backStackEntry.toRoute()
                ForumMediaThreadsScreen(
                    mediaTitle = route.mediaTitle,
                    onBackClick = { navController.popBackStack() },
                    onThreadClick = { threadId, threadTitle ->
                        navController.navigate(ForumThreadDetail(threadId, threadTitle))
                    },
                    onThreadCommentClick = { threadId, commentId ->
                        navController.navigate(ForumThreadDetail(threadId, "", commentId))
                    },
                    onUserClick = navigateToUserProfile,
                    onCreateThread = {
                        navController.navigate(
                            CreateThread(mediaId = route.mediaId, mediaTitle = route.mediaTitle)
                        )
                    }
                )
            }

            // =================================================================
            // SETTINGS SCREENS - Shared Axis Z (Depth)
            // =================================================================

            // Settings Hub
            composable<Settings>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                SettingsScreen(
                    onNavigateToLookAndFeel = { navController.navigate(SettingsLookAndFeel) },
                    onNavigateToAniList = { navController.navigate(SettingsAniList) },
                    onNavigateToNotifications = { navController.navigate(SettingsNotifications) },
                    onNavigateToStorage = { navController.navigate(SettingsStorage) },
                    onNavigateToAbout = { navController.navigate(SettingsAbout) },
                    onNavigateToSponsors = { navController.navigate(SettingsSponsors) },
                    onNavigateToUpdates = { navController.navigate(SettingsUpdates) },
                    onNavigateToDeveloperTools = { navController.navigate(SettingsDeveloperTools) },
                    onNavigateToMediaUpload = { navController.navigate(SettingsMediaUpload) },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Look and Feel Settings
            composable<SettingsLookAndFeel>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                LookAndFeelScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // AniList settings (account management + AniList account options)
            composable<SettingsAniList>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                AniListSettingsScreen(
                    onLogout = {
                        navController.navigate(Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Notifications Settings
            composable<SettingsNotifications>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                NotificationsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Storage Settings
            composable<SettingsStorage>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                StorageScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // About Settings
            composable<SettingsAbout>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                AboutScreen(
                    onBackClick = { navController.popBackStack() },
                    onNavigateToOpenSourceLicenses = { navController.navigate(SettingsOpenSourceLicenses) },
                    onNavigateToAcknowledgments = { navController.navigate(SettingsAcknowledgments) },
                    onNavigateToLinks = { navController.navigate(SettingsLinks) }
                )
            }

            // Sponsors Settings
            composable<SettingsSponsors>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                SponsorsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Open Source Licenses
            composable<SettingsOpenSourceLicenses>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                OpenSourceLicensesScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Acknowledgments
            composable<SettingsAcknowledgments>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                AcknowledgmentsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Updates Screen
            composable<SettingsUpdates>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                UpdatesScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Links Screen
            composable<SettingsLinks>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                LinksScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Developer Tools (debug builds only — route is only navigated to from debug UI)
            composable<SettingsDeveloperTools>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                DeveloperToolsScreen(
                    onBackClick = { navController.popBackStack() },
                    onFontPlaygroundClick = { navController.navigate(SettingsFontPlayground) }
                )
            }

            // Font Playground (debug builds only — route is only navigated to from debug UI)
            composable<SettingsFontPlayground>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                FontSettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Media upload host config
            composable<SettingsMediaUpload>(
                enterTransition = { sharedAxisZEnter() },
                exitTransition = { sharedAxisZExit() },
                popEnterTransition = { sharedAxisZPopEnter() },
                popExitTransition = { sharedAxisZPopExit() }
            ) {
                MediaUploadSettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
        }
    }
}
