package com.anisync.android.presentation.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.presentation.login.AniListAuth
import com.anisync.android.presentation.onboarding.components.AllSetStep
import com.anisync.android.presentation.onboarding.components.PermissionRow
import com.anisync.android.presentation.onboarding.components.PermissionsStep
import com.anisync.android.presentation.onboarding.components.PersonaliseStep
import com.anisync.android.presentation.onboarding.components.SignInSheet
import com.anisync.android.presentation.onboarding.components.SyncingStep
import com.anisync.android.presentation.onboarding.components.WelcomeStep
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.presentation.util.LocalStatusBarColor
import com.anisync.android.ui.theme.resolveDarkTheme
import com.anisync.android.util.AppLinksUtil
import com.anisync.android.util.BackgroundWorkUtil
import com.anisync.android.widget.core.WidgetPin

private const val ANILIST_REGISTER_URL = "https://anilist.co/signup"

/**
 * Host for the first-run flow. Owns everything that needs an Activity — the browser handoff, the
 * runtime permission launcher, and the system settings screens — and leaves the step order and the
 * account import to [OnboardingViewModel].
 */
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val appSettings = LocalAppSettings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val paletteStyle by appSettings.paletteStyle.collectAsStateWithLifecycle()

    // The welcome hero runs to the top edge, so the status-bar strip has to be the page background
    // rather than the default surfaceContainer tone.
    val statusBarColorHolder = LocalStatusBarColor.current
    val backgroundColor = MaterialTheme.colorScheme.background
    LaunchedEffect(backgroundColor) { statusBarColorHolder.value = backgroundColor }

    // All four set-up rows are granted on a system screen, so the answer only arrives on resume.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(OnboardingAction.RefreshPermissions)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onAction(OnboardingAction.EnableNotifications)
        viewModel.onAction(OnboardingAction.RefreshPermissions)
    }

    val requestPermission: (PermissionRow) -> Unit = remember(context) {
        { row ->
            when (row) {
                PermissionRow.Notifications ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }

                PermissionRow.Battery -> BackgroundWorkUtil.requestIgnoreBatteryOptimizations(context)

                PermissionRow.Links ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        AppLinksUtil.openAppLinksSettings(context)
                    }

                PermissionRow.Hibernation -> BackgroundWorkUtil.openHibernationSettings(context)
            }
        }
    }

    // Back is a no-op past the welcome step: the flow is forward-only once an account is attached,
    // and there is nothing behind it to return to.
    BackHandler(enabled = uiState.step != OnboardingStep.WELCOME) {}

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = uiState.step,
            transitionSpec = {
                (slideInHorizontally(tween(320)) { it / 6 } + fadeIn(tween(220)))
                    .togetherWith(slideOutHorizontally(tween(320)) { -it / 6 } + fadeOut(tween(180)))
            },
            label = "OnboardingStep"
        ) { step ->
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep(
                    covers = uiState.heroCovers,
                    onContinue = { viewModel.onAction(OnboardingAction.ContinueWithAniList) },
                    onCreateAccount = {
                        AppLinksUtil.openInBrowser(context, ANILIST_REGISTER_URL)
                    },
                    modifier = Modifier.navigationBarsOnly()
                )

                OnboardingStep.SYNCING -> SyncingStep(
                    username = uiState.username,
                    avatarUrl = uiState.avatarUrl,
                    bannerUrl = uiState.bannerUrl,
                    progress = uiState.sync,
                    onContinue = { viewModel.onAction(OnboardingAction.Next) },
                    modifier = Modifier.navigationBarsOnly()
                )

                OnboardingStep.PERMISSIONS -> PermissionsStep(
                    permissions = uiState.permissions,
                    onRequest = requestPermission,
                    onSkip = { viewModel.onAction(OnboardingAction.Skip) },
                    onContinue = { viewModel.onAction(OnboardingAction.Next) },
                    modifier = Modifier.navigationBarsOnly()
                )

                OnboardingStep.PERSONALISE -> PersonaliseStep(
                    personalise = uiState.personalise,
                    isDarkMode = uiState.personalise.themeMode.resolveDarkTheme(),
                    paletteStyle = paletteStyle,
                    previewEntry = uiState.previewEntry,
                    onPaletteSelected = { viewModel.onAction(OnboardingAction.SetPalette(it)) },
                    onThemeModeSelected = { viewModel.onAction(OnboardingAction.SetThemeMode(it)) },
                    onTitleLanguageSelected = {
                        viewModel.onAction(OnboardingAction.SetTitleLanguage(it))
                    },
                    onStartTabSelected = { viewModel.onAction(OnboardingAction.SetStartTab(it)) },
                    onSkip = { viewModel.onAction(OnboardingAction.Skip) },
                    onContinue = { viewModel.onAction(OnboardingAction.Next) },
                    modifier = Modifier.navigationBarsOnly()
                )

                OnboardingStep.DONE -> AllSetStep(
                    libraryEntries = uiState.sync.libraryEntries,
                    alertsOn = uiState.permissions.notifications,
                    linksOn = uiState.permissions.linksVerified,
                    widgetPinSupported = uiState.widgetPinSupported,
                    onAddWidget = { WidgetPin.requestUpNext(context) },
                    onFinish = { viewModel.onAction(OnboardingAction.Finish) },
                    modifier = Modifier.navigationBarsOnly()
                )
            }
        }

        if (uiState.showSignInSheet) {
            SignInSheet(
                onOpenAniList = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, AniListAuth.AUTH_URL.toUri())
                        )
                    }
                },
                onDismiss = { viewModel.onAction(OnboardingAction.DismissSignInSheet) }
            )
        }
    }
}

/**
 * The activity already consumes the status-bar inset for every screen; onboarding only has to keep
 * its own bottom action clear of the gesture bar.
 */
@Composable
private fun Modifier.navigationBarsOnly(): Modifier =
    windowInsetsPadding(WindowInsets.navigationBars)
