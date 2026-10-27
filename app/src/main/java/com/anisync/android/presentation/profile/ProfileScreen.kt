package com.anisync.android.presentation.profile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.anisync.android.presentation.components.richtext.RichTextInputScreen
import com.anisync.android.presentation.login.AniListAuth
import com.anisync.android.presentation.profile.components.AccountSwitcherSheet

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
    isOwnProfile: Boolean = true,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollToTopRequest by viewModel.scrollToTopRequest.collectAsStateWithLifecycle()
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AppCircularProgressIndicator()
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
                        onAccountSwitchClick = { showAccountSwitcherSheet = true }
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
        RichTextInputScreen(
            title = stringResource(R.string.edit_profile_title),
            placeholder = stringResource(R.string.edit_profile_about_label),
            // Edit the raw markdown source; only fall back to the rendered HTML when no raw bio is
            // cached yet (e.g. a pre-v19 row before its first refresh) so we never save HTML back.
            initialBody = profile.aboutMarkdown?.takeUnless { it.isBlank() }
                ?: profile.about.orEmpty(),
            isSubmitting = false,
            submitLabel = stringResource(R.string.save),
            minLength = 1,
            maxLength = 65_000,
            onSubmit = { about ->
                viewModel.onAction(ProfileAction.UpdateAbout(about))
                viewModel.onAction(ProfileAction.SetEditProfileDialogVisible(false))
            },
            onDismiss = {
                viewModel.onAction(ProfileAction.SetEditProfileDialogVisible(false))
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
            onDismiss = { showAccountSwitcherSheet = false }
        )
    }
}
