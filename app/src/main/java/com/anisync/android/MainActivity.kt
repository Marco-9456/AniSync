package com.anisync.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.anisync.android.presentation.MainScreen
import com.anisync.android.presentation.login.LoginScreen
import com.anisync.android.presentation.util.LocalAppSettings
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

                CompositionLocalProvider(LocalAppSettings provides appSettings) {
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

                            if (isLoggedIn) {
                                MainScreen()
                            } else {
                                LoginScreen()
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