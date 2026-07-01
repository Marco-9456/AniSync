package com.anisync.android

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.anisync.android.data.AppLocale
import com.anisync.android.data.AppSettings
import com.anisync.android.data.AuthRepository
import com.anisync.android.data.ThemeMode
import com.anisync.android.data.account.AccountManager
import com.anisync.android.data.update.UpdateManager
import com.anisync.android.data.update.UpdateState
import com.anisync.android.domain.LinkPreviewProvider
import com.anisync.android.presentation.MainScreen
import com.anisync.android.presentation.login.LoginScreen
import com.anisync.android.presentation.settings.UpdateDialog
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.LocalStatusBarColor
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.presentation.util.LocalGridColumnCount
import com.anisync.android.presentation.util.LocalGridColumnsAuto
import com.anisync.android.presentation.util.LocalLinkPreviewProvider
import com.anisync.android.presentation.util.rememberAdaptiveInfo
import com.anisync.android.ui.theme.AppTheme
import com.anisync.android.ui.theme.PresetPalettes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.system.measureTimeMillis

/** A notification deep link to be handled by the MainScreen built at [epoch] (post-account-switch). */
data class PendingDeepLink(val intent: Intent, val epoch: Int)

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var appSettings: AppSettings

    @Inject
    lateinit var notificationScheduler: com.anisync.android.worker.NotificationScheduler

    @Inject
    lateinit var updateManager: UpdateManager

    @Inject
    lateinit var linkPreviewProvider: LinkPreviewProvider

    @Inject
    lateinit var userOptionsRepository: com.anisync.android.domain.UserOptionsRepository

    @Inject
    lateinit var appLockManager: com.anisync.android.data.security.AppLockManager

    private val _newIntents = MutableSharedFlow<Intent>(extraBufferCapacity = 4)
    val newIntents: SharedFlow<Intent> = _newIntents.asSharedFlow()

    /**
     * Cross-account notification deep link, delivered after the switch settles and tagged with the
     * **session epoch** of the rebuilt MainScreen that should handle it. Retained (StateFlow) so it
     * survives the switch's subtree rebuild; the epoch tag ensures the pre-switch MainScreen (older
     * epoch) doesn't consume it first.
     */
    private val _pendingDeepLink = MutableStateFlow<PendingDeepLink?>(null)
    val pendingDeepLink: StateFlow<PendingDeepLink?> = _pendingDeepLink.asStateFlow()

    fun consumePendingDeepLink() { _pendingDeepLink.value = null }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the androidx splash screen before super.onCreate so the branded launch window
        // (theme-aware background + icon) shows through cold start, then hands off to Theme.AniSync.
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashProvider ->
            // Graceful handoff: the splash fades and drifts up slightly into the app content, instead
            // of a hard cut. Framework animators only — no extra deps, runs on every supported API.
            val splashView = splashProvider.view
            val fade = ObjectAnimator.ofFloat(splashView, View.ALPHA, splashView.alpha, 0f)
            val rise = ObjectAnimator.ofFloat(
                splashView, View.TRANSLATION_Y, 0f, -splashView.height * 0.04f
            )
            AnimatorSet().apply {
                playTogether(fade, rise)
                duration = 260L
                interpolator = DecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) = splashProvider.remove()
                })
                start()
            }
        }

        val onCreateTime = measureTimeMillis {
            super.onCreate(savedInstanceState)

            enableEdgeToEdge()

            val savedLocale = appSettings.appLocale.value
            if (savedLocale != AppLocale.SYSTEM) {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(savedLocale.tag)
                )
            }

            handleAuthRedirect(intent)
            routeAccountDeepLink(intent)

            // Resolve a migrated legacy login + claim its pre-ownerId library rows for the account.
            lifecycleScope.launch(Dispatchers.IO) {
                accountManager.reconcileActiveAccount()
            }

            lifecycleScope.launch(Dispatchers.IO) {
                combine(
                    appSettings.notificationsEnabled,
                    authRepository.isLoggedIn
                ) { enabled, loggedIn ->
                    enabled && loggedIn
                }
                    .distinctUntilChanged()
                    .collect { shouldSchedule ->
                        if (shouldSchedule) {
                            val scheduleTime = measureTimeMillis {
                                notificationScheduler.schedule()
                            }
                            Log.d(
                                "PerfMetrics",
                                "Notification scheduled in ${scheduleTime}ms via IO Thread"
                            )
                        }
                    }
            }

            // Silent auto-update check on launch
            if (appSettings.autoUpdateEnabled.value) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val allowPrerelease = appSettings.allowPrerelease.value
                    val updateCheckTime = measureTimeMillis {
                        updateManager.checkForUpdate(allowPrerelease)
                    }
                    Log.d(
                        "PerfMetrics",
                        "Update check completed in ${updateCheckTime}ms via IO Thread"
                    )
                }
            }

            // App lock: on Android 13+ keep AniSync's content out of the recents/app-switcher preview
            // while the lock is on, without FLAG_SECURE — so in-app screenshots still work. Older
            // versions can't do both, so they keep screenshots and don't hide the recents preview.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                lifecycleScope.launch {
                    appLockManager.enabled.collect { lockEnabled ->
                        setRecentsScreenshotEnabled(!lockEnabled)
                    }
                }
            }

            setContent {
                val themeMode by appSettings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
                val amoledEnabled by appSettings.amoledEnabled.collectAsStateWithLifecycle(initialValue = false)
                val selectedPaletteId by appSettings.selectedPaletteId.collectAsStateWithLifecycle(
                    initialValue = "dynamic"
                )
                val customSeedColor by appSettings.customSeedColor.collectAsStateWithLifecycle(
                    initialValue = null
                )
                val paletteStyle by appSettings.paletteStyle.collectAsStateWithLifecycle(
                    initialValue = com.materialkolor.PaletteStyle.TonalSpot
                )
                val coverQuality by appSettings.coverQuality.collectAsStateWithLifecycle(
                    initialValue = com.anisync.android.data.CoverQuality.LARGE
                )
                val gridColumnsAuto by appSettings.gridColumnsAuto.collectAsStateWithLifecycle(
                    initialValue = true
                )
                val gridColumnCount by appSettings.gridColumnCount.collectAsStateWithLifecycle(
                    initialValue = com.anisync.android.data.AppSettings.DEFAULT_GRID_COLUMNS
                )
                val typographyOverrides by appSettings.typographyOverrides.collectAsStateWithLifecycle(
                    initialValue = com.anisync.android.ui.theme.TypographyOverrides.None
                )
                val avatarShape by appSettings.avatarShape.collectAsStateWithLifecycle(
                    initialValue = com.anisync.android.data.AvatarShape.CLOVER_8_LEAF
                )
                val avatarBackgroundEnabled by appSettings.avatarBackgroundEnabled.collectAsStateWithLifecycle(
                    initialValue = true
                )
                val disableAvatarShapeProfile by appSettings.disableAvatarShapeProfile.collectAsStateWithLifecycle(
                    initialValue = false
                )
                val isSystemDark = isSystemInDarkTheme()

                val useDarkTheme = remember(themeMode, isSystemDark) {
                    when (themeMode) {
                        ThemeMode.LIGHT -> false
                        ThemeMode.DARK -> true
                        ThemeMode.SYSTEM -> isSystemDark
                    }
                }

                val seedColor = remember(selectedPaletteId, customSeedColor) {
                    when (selectedPaletteId) {
                        "dynamic" -> null
                        "custom" -> customSeedColor
                        else -> PresetPalettes.findById(selectedPaletteId)?.seedColor
                    }
                }

                val useDynamicColor = remember(selectedPaletteId) {
                    selectedPaletteId == "dynamic"
                }

                CompositionLocalProvider(
                    LocalAdaptiveInfo provides rememberAdaptiveInfo(),
                    LocalGridColumnsAuto provides gridColumnsAuto,
                    LocalGridColumnCount provides gridColumnCount,
                    LocalAppSettings provides appSettings,
                    LocalLinkPreviewProvider provides linkPreviewProvider,
                    com.anisync.android.domain.LocalCoverQuality provides coverQuality,
                    com.anisync.android.ui.theme.LocalAvatarShape provides avatarShape.toComposeShape(),
                    com.anisync.android.ui.theme.LocalAvatarShapeId provides avatarShape,
                    com.anisync.android.ui.theme.LocalAvatarBackgroundEnabled provides avatarBackgroundEnabled,
                    com.anisync.android.ui.theme.LocalDisableAvatarShapeProfile provides disableAvatarShapeProfile
                ) {
                    AppTheme(
                        darkTheme = useDarkTheme,
                        dynamicColor = useDynamicColor,
                        amoled = amoledEnabled,
                        seedColor = seedColor,
                        paletteStyle = paletteStyle,
                        typographyOverrides = typographyOverrides
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                          // Holder for the status-bar protection color: the active layer (MainScreen)
                          // publishes a tone, the protection Spacer below reads it. The Spacer is a
                          // sibling of the content, so a CompositionLocal bridges the two subtrees.
                          val statusBarColor = remember { mutableStateOf(Color.Unspecified) }
                          CompositionLocalProvider(LocalStatusBarColor provides statusBarColor) {
                          Box(modifier = Modifier.fillMaxSize()) {
                          // Keep all content out from under the system status bar: pad it down by the
                          // status-bar inset (which also CONSUMES it, so child screens don't re-apply
                          // it); the freed strip is filled by the protection Spacer below.
                          Box(
                              modifier = Modifier
                                  .fillMaxSize()
                                  .windowInsetsPadding(WindowInsets.statusBars)
                          ) {
                            val isLoggedIn by authRepository.isLoggedIn.collectAsStateWithLifecycle(
                                initialValue = false
                            )

                            // Session expired dialog
                            var showSessionExpiredDialog by remember { mutableStateOf(false) }

                            LaunchedEffect(Unit) {
                                authRepository.sessionExpired.collect {
                                    showSessionExpiredDialog = true
                                }
                            }

                            if (showSessionExpiredDialog) {
                                AlertDialog(
                                    onDismissRequest = { showSessionExpiredDialog = false },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Lock,
                                            contentDescription = null
                                        )
                                    },
                                    title = { Text("Session Expired") },
                                    text = {
                                        Text("Your session has expired. Please log in again to continue using AniSync.")
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showSessionExpiredDialog = false }) {
                                            Text("OK")
                                        }
                                    }
                                )
                            }

                            // Keyed on the account session epoch: switching accounts bumps the
                            // epoch, which tears down and rebuilds the entire MainScreen subtree
                            // (fresh NavController + ViewModels) so screens refetch the new account.
                            val sessionEpoch by accountManager.sessionEpoch.collectAsStateWithLifecycle()
                            if (isLoggedIn) {
                                key(sessionEpoch) {
                                    MainScreen(builtAtEpoch = sessionEpoch)
                                }
                            } else {
                                LoginScreen()
                            }

                            AppUpdateHandler(updateManager = updateManager)

                            // App-wide options sync-conflict prompt (surfaces right after launch,
                            // not only on the AniList Settings screen).
                            if (isLoggedIn) {
                                com.anisync.android.presentation.settings.UserOptionsConflictHandler(
                                    repository = userOptionsRepository,
                                )
                            }

                            // Blocking loader while an account add/switch/remove is in flight.
                            val isAccountBusy by accountManager.isBusy.collectAsStateWithLifecycle(
                                initialValue = false
                            )
                            if (isAccountBusy) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AppCircularProgressIndicator()
                                }
                            }

                          }

                            // M3 status-bar protection: an opaque bar filling the status-bar strip
                            // (surfaceContainer, per the Android system-bars guidance), drawn above the
                            // padded content so nothing shows under the system status bar.
                            Spacer(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .windowInsetsTopHeight(WindowInsets.statusBars)
                                    .background(
                                        statusBarColor.value.takeOrElse {
                                            MaterialTheme.colorScheme.surfaceContainer
                                        }
                                    )
                            )

                            // App-lock privacy gate: drawn above the content AND the status-bar strip
                            // so nothing shows while locked. No-op when the feature is off/unlocked.
                            com.anisync.android.presentation.security.AppLockGate(appLockManager)
                          }
                          }
                        }
                    }
                }
            }
        }
        Log.d("PerfMetrics", "MainActivity onCreate completed in ${onCreateTime}ms")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthRedirect(intent)
        if (!routeAccountDeepLink(intent)) {
            _newIntents.tryEmit(intent)
        }
    }

    /**
     * Handles a notification deep link tagged with an `account` query param.
     *
     * Only **diverts** when a different account must become active first: in that case it switches,
     * waits for the switch to settle, then replays the cleaned link into the rebuilt [MainScreen] via
     * [pendingDeepLink], and returns true (consumed).
     *
     * When the target account is already active (or unknown), it just strips the `account` param and
     * returns false, leaving the cleaned `intent.data` for the proven native paths — Compose's
     * cold-start auto-handle and [onNewIntent]'s `newIntents` — so same-account taps land on the
     * exact target reliably.
     */
    private fun routeAccountDeepLink(intent: Intent?): Boolean {
        val data = intent?.data ?: return false
        if (data.scheme != "anisync" || data.host == "auth") return false
        val target = (data.getQueryParameter("account") ?: return false).toIntOrNull()

        val cleanedUri = stripAccountParam(data)
        val known = target != null && accountManager.accounts.value.any { it.id == target }
        val active = accountManager.activeAccount.value?.id

        if (!known || target == active) {
            // Right account already active — let the native deep-link handlers take the cleaned link.
            intent.data = cleanedUri
            return false
        }

        // Cross-account: don't auto-handle in the wrong account; switch, then replay once the
        // subtree has rebuilt — tagged with the new epoch so only the post-switch MainScreen handles it.
        intent.data = null
        val epochBefore = accountManager.sessionEpoch.value
        lifecycleScope.launch {
            accountManager.switch(target!!)
            val newEpoch = accountManager.sessionEpoch.first { it != epochBefore }
            _pendingDeepLink.value = PendingDeepLink(Intent(Intent.ACTION_VIEW, cleanedUri), newEpoch)
        }
        return true
    }

    private fun stripAccountParam(uri: Uri): Uri {
        val builder = uri.buildUpon().clearQuery()
        for (name in uri.queryParameterNames) {
            if (name == "account") continue
            for (value in uri.getQueryParameters(name)) builder.appendQueryParameter(name, value)
        }
        return builder.build()
    }

    private fun handleAuthRedirect(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "anisync" || uri.host != "auth") return

        val fragment = uri.fragment ?: return
        val params = parseFragment(fragment)
        val accessToken = params["access_token"] ?: return
        val expiresIn = params["expires_in"]?.toLongOrNull() ?: 0L

        // addAccount activates the new account and bumps the session epoch; the keyed MainScreen
        // subtree rebuilds itself, so no activity recreate is needed here.
        lifecycleScope.launch {
            when (accountManager.addAccount(accessToken, expiresIn)) {
                is AccountManager.AddResult.Success -> Unit
                AccountManager.AddResult.Failed -> {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.account_sign_in_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /** Parses an OAuth implicit-grant URL fragment (`a=1&b=2`) into a key→value map. */
    private fun parseFragment(fragment: String): Map<String, String> =
        fragment.split('&').mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq <= 0) null else part.substring(0, eq) to Uri.decode(part.substring(eq + 1))
        }.toMap()
}

@Composable
private fun AppUpdateHandler(updateManager: UpdateManager) {
    val context = LocalContext.current
    val installSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updateManager.installApk()
        }

    val updateState by updateManager.updateState.collectAsStateWithLifecycle()

    val dialogRelease = when (val state = updateState) {
        is UpdateState.UpdateAvailable -> state.release
        is UpdateState.Downloading -> state.release
        is UpdateState.ReadyToInstall -> state.release
        else -> null
    }

    if (dialogRelease != null) {
        UpdateDialog(
            updateState = updateState,
            release = dialogRelease,
            onDismiss = { updateManager.dismissUpdate() },
            onDownload = {
                updateManager.startDownload(dialogRelease)
            },
            onCancel = { updateManager.cancelDownload() },
            onInstall = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (context.packageManager.canRequestPackageInstalls()) {
                        updateManager.installApk()
                    } else {
                        installSettingsLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                } else {
                    updateManager.installApk()
                }
            }
        )
    }
}