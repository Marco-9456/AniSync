package com.anisync.android.presentation.profile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.anisync.android.presentation.navigation.ShareStudio
import com.anisync.android.presentation.components.AppAlertDialog
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.ProfileColorThemeOverride
import com.anisync.android.presentation.login.AniListAuth
import com.anisync.android.presentation.profile.components.AccountSwitcherSheet
import com.anisync.android.presentation.profile.components.EditProfileSheet
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.components.EmptyState
import com.anisync.android.presentation.components.navigation.BackTrailButton
import com.anisync.android.presentation.profile.components.ProfileLoadingSkeleton
import com.anisync.android.presentation.util.openUrl
import com.anisync.android.ui.theme.emphasis
import com.anisync.android.util.AniListUrls

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    onMediaClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onVoiceActorClick: (Int) -> Unit = {},
    onStudioClick: (Int) -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onThreadClick: (threadId: Int, threadTitle: String, profileColor: String?) -> Unit,
    onCommentClick: (threadId: Int, commentId: Int, threadTitle: String, profileColor: String?) -> Unit,
    onActivityClick: (activityId: Int, profileColor: String?) -> Unit,
    onLastReplyClick: (activityId: Int, replyId: Int, profileColor: String?) -> Unit,
    onLogoutClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onManageAccounts: (() -> Unit)? = null,
    isOwnProfile: Boolean = true,
    onShareCard: (ShareStudio) -> Unit = {},
    onOpenRecap: (String) -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollToTopRequest by viewModel.scrollToTopRequest.collectAsStateWithLifecycle()
    val dockRequest by viewModel.dockRequest.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val activeAccountId by viewModel.activeAccountId.collectAsStateWithLifecycle()
    var showAccountSwitcherSheet by remember { mutableStateOf(false) }

    // Cooldown-gated ON_RESUME refresh. Notification badge no longer needs a
    // separate refresh — it rides on GetUserProfile via @include directive.
    // The 60s floor prevents quick app-switch from re-firing a refresh; the
    // ViewModel's profileCooldown (15s) is the second line of defence.
    // The floor counts from when the page was left. Counted from the last resume,
    // it took in the time spent reading here as well (#164).
    // rememberSaveable (not remember): survives this screen's composition being
    // disposed on navigation. Otherwise the 60s gate resets to 0 on every back-nav
    // and the ON_RESUME refresh re-fires, re-fetching the active tab and resetting
    // scroll position.
    val leftAtMs = rememberSaveable { mutableLongStateOf(0L) }
    LifecycleResumeEffect(viewModel) {
        if (SystemClock.elapsedRealtime() - leftAtMs.longValue > 60_000L) {
            viewModel.onAction(ProfileAction.Refresh(forceNetwork = false))
        }
        onPauseOrDispose { leftAtMs.longValue = SystemClock.elapsedRealtime() }
    }

    val profileColor = uiState.profile?.profileColor.takeUnless { isOwnProfile }

    ProfileColorThemeOverride(profileColor = profileColor) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val profile = uiState.profile
            val errorText = uiState.errorMessage
                ?: uiState.loadErrorRes?.let { stringResource(it) }
            when {
                uiState.isLoading && profile == null -> {
                    ProfileLoadingSkeleton(name = viewModel.targetName, onBackClick = onBackClick)
                }

                uiState.loadErrorRes == R.string.profile_user_unavailable && profile == null -> {
                    ProfileUnavailable(name = viewModel.targetName, onBackClick = onBackClick)
                }

                uiState.loadErrorRes == R.string.profile_offline_never_saved && profile == null -> {
                    ProfileStateFrame(name = viewModel.targetName, onBackClick = onBackClick) {
                        EmptyState(
                            icon = Icons.Default.CloudOff,
                            title = stringResource(R.string.profile_offline_title),
                            description = viewModel.targetName?.let { stringResource(R.string.profile_offline_never_saved_named, it) }
                                ?: stringResource(R.string.profile_offline_never_saved),
                            actionLabel = stringResource(R.string.profile_try_again),
                            actionIcon = Icons.Default.Refresh,
                            onAction = { viewModel.onAction(ProfileAction.Refresh()) }
                        )
                    }
                }

                errorText != null && profile == null -> {
                    ErrorState(
                        message = errorText,
                        onRetry = { viewModel.onAction(ProfileAction.Refresh()) }
                    )
                }

                profile != null -> {
                    ProfileContent(
                        profile = profile,
                        uiState = uiState,
                        scrollToTopRequest = scrollToTopRequest,
                        dockRequest = dockRequest,
                        isOwnProfile = isOwnProfile,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onAction = viewModel::onAction,
                        onSettingsClick = onNavigateToSettings,
                        onNotificationsClick = onNavigateToNotifications,
                        unreadNotificationCount = uiState.unreadNotificationCount,
                        onMediaClick = onMediaClick,
                        onCharacterClick = onCharacterClick,
                        onStaffClick = onStaffClick,
                        onVoiceActorClick = onVoiceActorClick,
                        onStudioClick = onStudioClick,
                        onUserClick = onUserClick,
                        onThreadClick = { threadId, threadTitle ->
                            onThreadClick(threadId, threadTitle, profileColor)
                        },
                        onCommentClick = { threadId, commentId, threadTitle ->
                            onCommentClick(threadId, commentId, threadTitle, profileColor)
                        },
                        onActivityClick = { onActivityClick(it, profileColor) },
                        onLastReplyClick = { activityId, replyId ->
                            onLastReplyClick(activityId, replyId, profileColor)
                        },
                        showAccountSwitcher = isOwnProfile && accounts.size > 1,
                        onAccountSwitchClick = { showAccountSwitcherSheet = true },
                        onShareCard = onShareCard,
                        onOpenRecap = onOpenRecap,
                        onBackClick = onBackClick
                    )
                }

                else -> {
                    ErrorState(
                        message = if (isOwnProfile) {
                            stringResource(R.string.profile_unknown_error)
                        } else {
                            stringResource(R.string.profile_user_load_error)
                        },
                        onRetry = { viewModel.onAction(ProfileAction.Refresh()) }
                    )
                }
            }
        }
    }
    }

    val profile = uiState.profile
    if (profile != null && uiState.isEditProfileDialogVisible && isOwnProfile) {
        EditProfileSheet(
            // Edit the raw markdown source; only fall back to the rendered HTML when no raw bio is
            // cached yet (e.g. a pre-v19 row before its first refresh) so we never save HTML back.
            about = profile.aboutMarkdown?.takeUnless { it.isBlank() } ?: profile.about.orEmpty(),
            isSaving = uiState.isSavingAbout,
            onSave = { about -> viewModel.onAction(ProfileAction.UpdateAbout(about)) },
            onDismiss = { viewModel.onAction(ProfileAction.SetEditProfileDialogVisible(false)) }
        )
    }

    if (uiState.isUnfollowConfirmVisible && profile != null) {
        AppAlertDialog(
            onDismissRequest = { viewModel.onAction(ProfileAction.DismissUnfollow) },
            title = { Text(stringResource(R.string.profile_unfollow_title, profile.name)) },
            text = { Text(stringResource(R.string.profile_unfollow_body, profile.name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(ProfileAction.ConfirmUnfollow) }) {
                    Text(stringResource(R.string.profile_unfollow_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(ProfileAction.DismissUnfollow) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    uiState.followerToRemove?.let { follower ->
        AppAlertDialog(
            onDismissRequest = { viewModel.onAction(ProfileAction.DismissRemoveFollower) },
            title = { Text(stringResource(R.string.profile_remove_follower_title)) },
            text = { Text(stringResource(R.string.profile_remove_follower_body, follower.name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(ProfileAction.ConfirmRemoveFollower) }) {
                    Text(
                        text = stringResource(R.string.profile_remove_follower_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(ProfileAction.DismissRemoveFollower) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAccountSwitcherSheet) {
        AccountSwitcherSheet(
            accounts = accounts,
            activeAccountId = activeAccountId,
            onSwitch = { id ->
                showAccountSwitcherSheet = false
                viewModel.switchAccount(id)
            },
            onAddAccount = {
                showAccountSwitcherSheet = false
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AniListAuth.AUTH_URL)))
            },
            onDismiss = { showAccountSwitcherSheet = false },
            unreadNotificationCount = uiState.unreadNotificationCount,
            onManageAccounts = onManageAccounts?.let { manage ->
                {
                    showAccountSwitcherSheet = false
                    manage()
                }
            }
        )
    }
}

/** AniList answers a private profile and a deleted one with the same 404, so the copy covers both. */
@Composable
private fun ProfileUnavailable(name: String?, onBackClick: (() -> Unit)?) {
    val context = LocalContext.current
    ProfileStateFrame(name = name, onBackClick = onBackClick) {
        EmptyState(
            icon = Icons.Outlined.Lock,
            title = stringResource(R.string.profile_unavailable_title),
            description = name?.let { stringResource(R.string.profile_unavailable_body, it) }
                ?: stringResource(R.string.profile_user_unavailable),
            actionLabel = name?.let { stringResource(R.string.profile_open_on_anilist) },
            actionIcon = name?.let { Icons.AutoMirrored.Filled.OpenInNew },
            onAction = name?.let { { openUrl(context, AniListUrls.userUrl(it)) } }
        )
    }
}

/** A profile that cannot show: the bar says whose it was, the block below says why. */
@Composable
private fun ProfileStateFrame(name: String?, onBackClick: (() -> Unit)?, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .statusBarsPadding()
                    .height(64.dp)
                    .padding(start = 4.dp, end = 16.dp)
            ) {
                if (onBackClick != null) BackTrailButton(onClick = onBackClick)
                if (name != null) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.padding(start = if (onBackClick != null) 4.dp else 12.dp)
                    )
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}
