package com.anisync.android.presentation.profile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.richtext.RichTextInputScreen
import com.anisync.android.presentation.login.AniListAuth
import com.anisync.android.presentation.profile.components.AccountSwitcherSheet
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.ui.theme.AppTheme
import com.anisync.android.ui.theme.aniListProfileSeedColor
import com.anisync.android.ui.theme.resolveDarkTheme

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    onMediaClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onVoiceActorClick: (Int) -> Unit = {},
    onStudioClick: (Int) -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onThreadClick: (threadId: Int, threadTitle: String) -> Unit = { _, _ -> },
    onCommentClick: (threadId: Int, commentId: Int, threadTitle: String) -> Unit = { _, _, _ -> },
    onActivityClick: (Int) -> Unit = {},
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit = { _, _ -> },
    onLogoutClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    isOwnProfile: Boolean = true,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val activeAccountId by viewModel.activeAccountId.collectAsStateWithLifecycle()
    var showAccountSwitcherSheet by remember { mutableStateOf(false) }

    // Cooldown-gated ON_RESUME refresh. Notification badge no longer needs a
    // separate refresh — it rides on GetUserProfile via @include directive.
    // The 60s floor prevents quick app-switch from re-firing a refresh; the
    // ViewModel's profileCooldown (15s) is the second line of defence.
    // rememberSaveable (not remember): survives this screen's composition being
    // disposed on navigation. Otherwise the 60s gate resets to 0 on every back-nav
    // and the ON_RESUME refresh re-fires, re-fetching the active tab and resetting
    // scroll position.
    val lastResumeAtMs = rememberSaveable { mutableLongStateOf(0L) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastResumeAtMs.longValue > 60_000L) {
            viewModel.onAction(ProfileAction.Refresh(forceNetwork = false))
        }
        lastResumeAtMs.longValue = now
    }

    ProfileColorThemeOverride(
        isOwnProfile = isOwnProfile,
        profileColor = uiState.profile?.profileColor,
    ) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            val profile = uiState.profile
            when {
                uiState.isLoading && profile == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AppCircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null && profile == null -> {
                    ErrorState(
                        message = uiState.errorMessage ?: if (isOwnProfile) {
                            stringResource(R.string.profile_unknown_error)
                        } else {
                            stringResource(R.string.profile_user_load_error)
                        },
                        onRetry = { viewModel.onAction(ProfileAction.Refresh()) }
                    )
                }

                profile != null -> {
                    ProfileContent(
                        profile = profile,
                        uiState = uiState,
                        isOwnProfile = isOwnProfile,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onAction = viewModel::onAction,
                        onSettingsClick = onNavigateToSettings,
                        onNotificationsClick = {
                            viewModel.onNotificationsOpened()
                            onNavigateToNotifications()
                        },
                        unreadNotificationCount = uiState.unreadNotificationCount,
                        onMediaClick = onMediaClick,
                        onCharacterClick = onCharacterClick,
                        onStaffClick = onStaffClick,
                        onVoiceActorClick = onVoiceActorClick,
                        onStudioClick = onStudioClick,
                        onUserClick = onUserClick,
                        onThreadClick = onThreadClick,
                        onCommentClick = onCommentClick,
                        onActivityClick = onActivityClick,
                        onLastReplyClick = onLastReplyClick,
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

/**
 * Wraps a visited user's profile in a MaterialKolor theme seeded from their AniList profile color,
 * when the "Use profile colors" appearance preference is on. The viewer's own profile, profiles on
 * the default/unset color, and the toggle being off all fall through to the app's normal theme.
 * Only the seed hue changes — the viewer's palette style and light/dark choice are preserved.
 */
@Composable
private fun ProfileColorThemeOverride(
    isOwnProfile: Boolean,
    profileColor: String?,
    content: @Composable () -> Unit,
) {
    val appSettings = LocalAppSettings.current
    val respect by appSettings.respectUserProfileColors.collectAsStateWithLifecycle()
    val seed = if (!isOwnProfile && respect) aniListProfileSeedColor(profileColor) else null
    if (seed == null) {
        content()
        return
    }
    val themeMode by appSettings.themeMode.collectAsStateWithLifecycle()
    val paletteStyle by appSettings.paletteStyle.collectAsStateWithLifecycle()
    val amoledEnabled by appSettings.amoledEnabled.collectAsStateWithLifecycle()
    val darkTheme = themeMode.resolveDarkTheme()
    AppTheme(
        darkTheme = darkTheme,
        dynamicColor = false,
        amoled = amoledEnabled,
        seedColor = seed,
        paletteStyle = paletteStyle,
        content = content,
    )
}
