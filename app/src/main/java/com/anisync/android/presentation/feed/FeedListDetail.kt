package com.anisync.android.presentation.feed

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.anisync.android.R
import com.anisync.android.presentation.activity.ActivityDetailScreen
import com.anisync.android.presentation.navigation.ActivityDetail
import com.anisync.android.presentation.navigation.CreateStatus
import com.anisync.android.presentation.navigation.DetailPanePlaceholder
import com.anisync.android.presentation.navigation.EditActivity
import com.anisync.android.presentation.navigation.MediaDetails
import com.anisync.android.presentation.navigation.TwoPaneListDetailScaffold
import com.anisync.android.presentation.navigation.UserProfile
import com.anisync.android.presentation.navigation.navigateSafely
import com.anisync.android.presentation.util.LocalAdaptiveInfo

/**
 * The Feed tab. Compact/medium widths show the plain [FeedScreen] and push the full-screen activity
 * detail. Expanded widths use the shared two-pane [TwoPaneListDetailScaffold] — the activity feed as
 * the permanent list pane, the tapped activity in the on-demand resizable detail pane (closable ✕).
 * Media taps and reply taps still escalate to the app [navController].
 */
@Composable
fun FeedListDetail(
    navController: NavHostController,
    onLoginClick: () -> Unit,
    onActivityClickFullScreen: (Int) -> Unit,
) {
    val feed: @Composable (onActivityClick: (Int) -> Unit) -> Unit = { onActivityClick ->
        FeedScreen(
            onActivityClick = onActivityClick,
            onUserClick = { navController.navigateSafely(UserProfile(it)) },
            onMediaClick = { navController.navigate(MediaDetails(it, "feed")) },
            onLastReplyClick = { activityId, replyId ->
                navController.navigate(ActivityDetail(activityId, replyId))
            },
            onLoginClick = onLoginClick,
            onComposeStatus = { navController.navigate(CreateStatus) },
        )
    }

    if (!LocalAdaptiveInfo.current.supportsTwoPane) {
        feed(onActivityClickFullScreen)
        return
    }

    TwoPaneListDetailScaffold(
        placeholderPane = {
            DetailPanePlaceholder(
                icon = Icons.Outlined.DynamicFeed,
                text = stringResource(R.string.pane_placeholder_activity),
            )
        },
        listPane = { onItemClick -> feed(onItemClick) },
        detailPane = { activityId, onClose ->
            ActivityDetailScreen(
                activityId = activityId,
                onBackClick = onClose,
                onUserClick = { navController.navigateSafely(UserProfile(it)) },
                navigationIcon = Icons.Default.Close,
                onEditActivity = { navController.navigate(EditActivity(it)) },
            )
        },
    )
}
