package com.anisync.android

import android.content.Intent
import android.os.Bundle
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository
    
    @Inject
    lateinit var appSettings: AppSettings

    @Inject
    lateinit var notificationScheduler: com.anisync.android.worker.NotificationScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Apply saved locale on startup
        val savedLocale = appSettings.appLocale.value
        if (savedLocale != AppLocale.SYSTEM) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(savedLocale.tag)
            )
        }
        
        // Handle initial intent (when app first opens with redirect)
        handleAuthRedirect(intent)

        // Ensure notifications are scheduled if enabled
        lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(
                appSettings.notificationsEnabled,
                authRepository.isLoggedIn
            ) { enabled, loggedIn ->
                if (enabled && loggedIn) {
                    notificationScheduler.schedule()
                }
            }.collect {}
        }
        
setContent {
            val themeMode by appSettings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val selectedPaletteId by appSettings.selectedPaletteId.collectAsStateWithLifecycle(initialValue = "dynamic")
            val customSeedColor by appSettings.customSeedColor.collectAsStateWithLifecycle(initialValue = null)
            val paletteStyle by appSettings.paletteStyle.collectAsStateWithLifecycle(initialValue = com.materialkolor.PaletteStyle.TonalSpot)
            val isSystemDark = isSystemInDarkTheme()
            
            // Determine if dark theme should be used based on settings
            val useDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemDark
            }
            
            // Resolve seed color from selection
            val seedColor = remember(selectedPaletteId, customSeedColor) {
                when (selectedPaletteId) {
                    "dynamic" -> null  // Use system dynamic colors
                    "custom" -> customSeedColor
                    else -> PresetPalettes.findById(selectedPaletteId)?.seedColor
                }
            }
            
            // Determine if we should use dynamic color
            val useDynamicColor = selectedPaletteId == "dynamic"
            
            // Provide AppSettings to the entire Compose tree via CompositionLocal
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
                        val isLoggedIn by authRepository.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
                        
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthRedirect(intent)
    }

    private fun handleAuthRedirect(intent: Intent?) {
        val uri = intent?.data ?: return

        // Check if this is our auth redirect
        if (uri.scheme == "anisync" && uri.host == "auth") {
            // Implicit Grant: token is in URL fragment
            val fragment = uri.fragment
            if (fragment != null) {
                val params = fragment.split("&").associate { part ->
                    val parts = part.split("=", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
                }

                val accessToken = params["access_token"]
                if (accessToken != null) {
                    lifecycleScope.launch {
                        authRepository.saveToken(accessToken)
                    }
                }
            }
        }
    }
}

