package com.anisync.android.presentation.forum

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.runtime.Composable
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
    val forum: @Composable (onThreadOpen: (Int, String) -> Unit) -> Unit = { onThreadOpen ->
        ForumScreen(
            onThreadClick = onThreadOpen,
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

    if (!LocalAdaptiveInfo.current.isExpandedOrWider) {
        forum(onThreadClickFullScreen)
        return
    }

    TwoPaneListDetailScaffold(
        placeholderPane = {
            DetailPanePlaceholder(
                icon = Icons.Outlined.Forum,
                text = stringResource(R.string.pane_placeholder_thread),
            )
        },
        listPane = { onItemClick -> forum { threadId, _ -> onItemClick(threadId) } },
        detailPane = { threadId, onClose ->
            ThreadDetailScreen(
                threadId = threadId,
                threadTitle = "",
                onBackClick = onClose,
                onUserClick = { navController.navigateSafely(UserProfile(it)) },
                navigationIcon = Icons.Default.Close,
                onEditThread = { navController.navigate(EditThreadBody(it)) },
            )
        },
    )
}
