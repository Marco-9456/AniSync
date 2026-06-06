package com.anisync.android

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.presentation.util.LocalLinkPreviewProvider
import com.anisync.android.ui.theme.AppTheme
import com.anisync.android.ui.theme.PresetPalettes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.system.measureTimeMillis

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

    private val _newIntents = MutableSharedFlow<Intent>(extraBufferCapacity = 4)
    val newIntents: SharedFlow<Intent> = _newIntents.asSharedFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
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

            setContent {
                val themeMode by appSettings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
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
                        seedColor = seedColor,
                        paletteStyle = paletteStyle,
                        typographyOverrides = typographyOverrides
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
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
                                    MainScreen()
                                }
                            } else {
                                LoginScreen()
                            }

                            AppUpdateHandler(updateManager = updateManager)

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
                                    CircularProgressIndicator()
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
        _newIntents.tryEmit(intent)
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