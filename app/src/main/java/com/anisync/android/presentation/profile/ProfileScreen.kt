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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.components.EditProfileDialog
import com.anisync.android.presentation.components.ErrorState

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    onMediaClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onLogoutClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigateToSettings: () -> Unit = {},
    isOwnProfile: Boolean = true,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = if (isOwnProfile) 80.dp else 0.dp)
        ) {
            val profile = uiState.profile
            when {
                uiState.isLoading && profile == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null && profile == null -> {
                    ErrorState(
                        message = uiState.errorMessage ?: if (isOwnProfile) {
                            stringResource(R.string.profile_unknown_error)
                        } else {
                            stringResource(R.string.profile_user_load_error)
                        },
                        onRetry = { viewModel.onAction(ProfileAction.Refresh) }
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
                        onMediaClick = onMediaClick,
                        onCharacterClick = onCharacterClick,
                        onStaffClick = onStaffClick,
                        onUserClick = onUserClick
                    )
                }

                else -> {
                    ErrorState(
                        message = if (isOwnProfile) {
                            stringResource(R.string.profile_unknown_error)
                        } else {
                            stringResource(R.string.profile_user_load_error)
                        },
                        onRetry = { viewModel.onAction(ProfileAction.Refresh) }
                    )
                }
            }
        }
    }

    val profile = uiState.profile
    if (profile != null && uiState.isEditProfileDialogVisible && isOwnProfile) {
        EditProfileDialog(
            initialAbout = profile.about.orEmpty(),
            onDismiss = {
                viewModel.onAction(ProfileAction.SetEditProfileDialogVisible(false))
            },
            onSave = { about ->
                viewModel.onAction(ProfileAction.UpdateAbout(about))
                viewModel.onAction(ProfileAction.SetEditProfileDialogVisible(false))
            }
        )
    }
}
