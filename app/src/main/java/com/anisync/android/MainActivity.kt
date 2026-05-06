package com.anisync.android

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.anisync.android.data.AppLocale
import com.anisync.android.data.AppSettings
import com.anisync.android.data.AuthRepository
import com.anisync.android.data.ThemeMode
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
    lateinit var appSettings: AppSettings

    @Inject
    lateinit var notificationScheduler: com.anisync.android.worker.NotificationScheduler

    @Inject
    lateinit var updateManager: UpdateManager

    @Inject
    lateinit var linkPreviewProvider: LinkPreviewProvider

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

            // Schedule notifications when enabled and logged in, only while activity is active
            lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
                                Log.d("PerfMetrics", "Notification scheduled in ${scheduleTime}ms")
                            }
                        }
                }
            }

            // Silent auto-update check on launch (no UI unless update found)
            if (appSettings.autoUpdateEnabled.value) {
                lifecycleScope.launch {
                    val allowPrerelease = appSettings.allowPrerelease.value
                    updateManager.checkForUpdate(allowPrerelease)
                    // Result is reflected in updateManager.updateState;
                    // the dialog composable below will react if an update is found.
                    // No toast/snackbar for "up to date" or "error" — completely silent.
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
                    com.anisync.android.domain.LocalCoverQuality provides coverQuality
                ) {
                    AppTheme(
                        darkTheme = useDarkTheme,
                        dynamicColor = useDynamicColor,
                        seedColor = seedColor,
                        paletteStyle = paletteStyle
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            val isLoggedIn by authRepository.isLoggedIn.collectAsStateWithLifecycle(
                                initialValue = false
                            )

                            // Session expired dialog — triggered by AuthorizationInterceptor on HTTP 401
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

                            if (isLoggedIn) {
                                MainScreen()
                            } else {
                                LoginScreen()
                            }

                            // Global update dialog — overlays the entire app when an update is found.
                            // This dialog appears silently after the auto-check on launch completes.
                            val updateState by updateManager.updateState.collectAsStateWithLifecycle()

                            val dialogRelease = when (val state = updateState) {
                                is UpdateState.UpdateAvailable -> state.release
                                is UpdateState.Downloading -> state.release
                                is UpdateState.ReadyToInstall -> state.release
                                else -> null
                            }

                            // Launcher for "Install Unknown Apps" system settings
                            val installSettingsLauncher =
                                rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                                    updateManager.installApk()
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
                                            if (packageManager.canRequestPackageInstalls()) {
                                                updateManager.installApk()
                                            } else {
                                                installSettingsLauncher.launch(
                                                    Intent(
                                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                                        Uri.parse("package:$packageName")
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
                    }
                }
            }
        }
        Log.d("PerfMetrics", "MainActivity onCreate completed in ${onCreateTime}ms")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthRedirect(intent)
    }

    private fun handleAuthRedirect(intent: Intent?) {
        val uri = intent?.data ?: return

        if (uri.scheme == "anisync" && uri.host == "auth") {
            val fragment = uri.fragment
            if (fragment != null) {
                val startNanos = System.nanoTime()

                // Parse token using zero-allocation sequence
                var accessToken: String? = null
                val sequence = fragment.splitToSequence("&")

                for (param in sequence) {
                    val eqIndex = param.indexOf('=')
                    if (eqIndex > 0) {
                        val key = param.substring(0, eqIndex)
                        if (key == "access_token") {
                            accessToken = param.substring(eqIndex + 1)
                            break
                        }
                    }
                }

                if (accessToken != null) {
                    authRepository.saveToken(accessToken)
                }

                val parseTimeMs = (System.nanoTime() - startNanos) / 1_000_000.0
                Log.d("PerfMetrics", "Auth token parsing took $parseTimeMs ms")
            }
        }
    }
}