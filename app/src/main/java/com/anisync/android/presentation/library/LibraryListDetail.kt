package com.anisync.android.presentation.library

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.anisync.android.presentation.navigation.MediaListDetailScaffold
import com.anisync.android.presentation.util.LocalAdaptiveInfo

/**
 * The Library tab. Compact/medium widths show the plain [LibraryScreen] and push the full-screen
 * detail (keeping the card→page shared-element morph). Expanded widths use the shared Material 3
 * two-pane [MediaListDetailScaffold] — a permanent library list with an on-demand, resizable detail
 * pane.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LibraryListDetail(
    navController: NavHostController,
    onMediaClickFullScreen: (Int) -> Unit,
    onNavigateToCalendar: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    if (!LocalAdaptiveInfo.current.supportsTwoPane) {
        LibraryScreen(
            onMediaClick = onMediaClickFullScreen,
            onNavigateToCalendar = onNavigateToCalendar,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )
        return
    }

    MediaListDetailScaffold(navController = navController) { onMediaClick ->
        LibraryScreen(
            onMediaClick = onMediaClick,
            onNavigateToCalendar = onNavigateToCalendar,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
}
