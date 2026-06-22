package com.anisync.android.presentation.forum

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.anisync.android.R
import com.anisync.android.presentation.navigation.CreateThread
import com.anisync.android.presentation.navigation.DetailPanePlaceholder
import com.anisync.android.presentation.navigation.EditThreadBody
import com.anisync.android.presentation.navigation.ForumThreadDetail
import com.anisync.android.presentation.navigation.TwoPaneListDetailScaffold
import com.anisync.android.presentation.navigation.UserProfile
import com.anisync.android.presentation.navigation.navigateSafely
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.LocalPaneIsRoot

/**
 * The Forum tab. Compact/medium widths show the plain [ForumScreen] and push the full-screen thread.
 * Expanded widths use the shared two-pane [TwoPaneListDetailScaffold] — the thread list as the
 * permanent list pane, the tapped thread in the on-demand resizable detail pane (closable ✕).
 */
@Composable
fun ForumListDetail(
    navController: NavHostController,
    onThreadClickFullScreen: (threadId: Int, threadTitle: String) -> Unit,
) {
    val forum: @Composable (selectedThreadId: Int?, onThreadOpen: (Int, String) -> Unit) -> Unit = { selectedThreadId, onThreadOpen ->
        ForumScreen(
            onThreadClick = onThreadOpen,
            selectedThreadId = selectedThreadId,
            onThreadCommentClick = { threadId, commentId ->
                navController.navigate(ForumThreadDetail(threadId, "", commentId))
            },
            onCreateThreadClick = { navController.navigate(CreateThread()) },
            onCreateThreadForMedia = { mediaId, title, coverUrl ->
                navController.navigate(CreateThread(mediaId, title, coverUrl.orEmpty()))
            },
            onUserClick = { navController.navigateSafely(UserProfile(it)) },
        )
    }

    if (!LocalAdaptiveInfo.current.supportsTwoPane) {
        forum(null, onThreadClickFullScreen)
        return
    }

    TwoPaneListDetailScaffold(
        placeholderPane = {
            DetailPanePlaceholder(
                icon = Icons.Outlined.Forum,
                text = stringResource(R.string.pane_placeholder_thread),
            )
        },
        listPane = { selectedThreadId, onItemClick ->
            forum(selectedThreadId) { threadId, _ -> onItemClick(threadId) }
        },
        detailPane = { threadId, onClose ->
            // Single-screen pane (no in-pane drilling) → always the root: the detail shows the trailing
            // close (✕) via LocalPaneIsRoot.
            CompositionLocalProvider(LocalPaneIsRoot provides true) {
                ThreadDetailScreen(
                    threadId = threadId,
                    threadTitle = "",
                    onBackClick = onClose,
                    onUserClick = { navController.navigateSafely(UserProfile(it)) },
                    onEditThread = { navController.navigate(EditThreadBody(it)) },
                    // Fill the resizable pane instead of the centered reading column, so collapsing the
                    // list gives the thread the gained width (§6.4 follow-up).
                    capContentWidth = false,
                )
            }
        },
    )
}
