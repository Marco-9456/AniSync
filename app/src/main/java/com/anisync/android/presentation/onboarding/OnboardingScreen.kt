package com.anisync.android.presentation.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.ui.theme.resolveDarkTheme
import com.anisync.android.util.AppLinksUtil
import com.anisync.android.util.BackgroundWorkUtil
import com.anisync.android.util.NotificationPermissionHelper
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
                // Two separate gates sit behind this row: Android's POST_NOTIFICATIONS grant and
                // AniSync's own master switch. Whichever is missing is the one the tap addresses,
                // so the row cannot read granted while the app stays silent.
                PermissionRow.Notifications -> when {
                    NotificationPermissionHelper.hasNotificationPermission(context) ->
                        viewModel.onAction(OnboardingAction.EnableNotifications)

                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

                    else -> {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
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

    val goBack = { viewModel.onAction(OnboardingAction.Back) }

    // Back walks the steps in reverse where there is somewhere to go, and is swallowed where there
    // is not — leaving the flow by accident would drop the user onto a half-configured app.
    BackHandler(enabled = uiState.step != OnboardingStep.WELCOME) {
        if (uiState.step.previous != null) goBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Steps travel with the direction they are taken in: forward slides the new screen in
        // from the trailing edge, back reverses it, so a back press never reads as another advance.
        // The offset spring is the app's shared spatial spec, the same motion the tab transitions use.
        val stepMotion = AppMotion.rememberOffsetSpatialSpec()
        AnimatedContent(
            targetState = uiState.step,
            transitionSpec = {
                val forward = targetState.ordinal >= initialState.ordinal
                val direction = if (forward) 1 else -1
                (
                    slideInHorizontally(stepMotion) { width -> direction * width / 4 } +
                        fadeIn(tween(200, delayMillis = 40))
                    ).togetherWith(
                    slideOutHorizontally(stepMotion) { width -> -direction * width / 4 } +
                        fadeOut(tween(140))
                ).using(SizeTransform(clip = false))
            },
            label = "OnboardingStep"
        ) { step ->
            when (step) {
                // The welcome step takes no inset padding from here: its artwork bleeds to every
                // edge and only the copy beside it has to clear the bars, so it applies its own.
                OnboardingStep.WELCOME -> WelcomeStep(
                    covers = uiState.heroCovers,
                    onContinue = { viewModel.onAction(OnboardingAction.ContinueWithAniList) },
                    onCreateAccount = {
                        AppLinksUtil.openInBrowser(context, ANILIST_REGISTER_URL)
                    }
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
                    onBack = goBack.takeIf { step.previous != null },
                    modifier = Modifier.systemBarsOnly()
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
                    onStartScreenSelected = {
                        viewModel.onAction(OnboardingAction.SetStartScreen(it))
                    },
                    onSkip = { viewModel.onAction(OnboardingAction.Skip) },
                    onContinue = { viewModel.onAction(OnboardingAction.Next) },
                    onBack = goBack.takeIf { step.previous != null },
                    modifier = Modifier.systemBarsOnly()
                )

                OnboardingStep.DONE -> AllSetStep(
                    libraryEntries = uiState.sync.libraryEntries,
                    alertsOn = uiState.permissions.notifications,
                    linksOn = uiState.permissions.linksVerified,
                    widgetPinSupported = uiState.widgetPinSupported,
                    onAddWidget = { WidgetPin.requestUpNext(context) },
                    onFinish = { viewModel.onAction(OnboardingAction.Finish) },
                    modifier = Modifier.systemBarsOnly()
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
 * Onboarding draws edge to edge, so each step states which bars it wants to sit clear of. The two
 * screens built on artwork keep the status bar over the image and only dodge the gesture bar.
 */
@Composable
private fun Modifier.navigationBarsOnly(): Modifier =
    windowInsetsPadding(WindowInsets.navigationBars)

@Composable
private fun Modifier.systemBarsOnly(): Modifier =
    windowInsetsPadding(WindowInsets.systemBars)
