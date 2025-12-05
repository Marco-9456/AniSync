package com.anisync.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.anisync.android.data.AuthRepository
import com.anisync.android.presentation.MainScreen
import com.anisync.android.presentation.login.LoginScreen
import com.anisync.android.ui.theme.AniSyncTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle initial intent (when app first opens with redirect)
        handleAuthRedirect(intent)
        
        setContent {
            AniSyncTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isLoggedIn by authRepository.isLoggedIn.collectAsState(initial = false)
                    
                    if (isLoggedIn) {
                        MainScreen()
                    } else {
                        LoginScreen()
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
            // Authorization Code flow: code is in query parameter
            val code = uri.getQueryParameter("code")
            if (code != null) {
                // Exchange code for token
                lifecycleScope.launch {
                    val success = authRepository.exchangeCodeForToken(code)
                    if (!success) {
                        android.util.Log.e("MainActivity", "Failed to exchange code for token")
                    }
                }
                return
            }
            
            // Fallback: Check for token in fragment (Implicit Grant - legacy)
            val fragment = uri.fragment
            if (fragment != null) {
                val params = fragment.split("&").associate { part ->
                    val parts = part.split("=", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
                }
                
                val accessToken = params["access_token"]
                if (accessToken != null) {
                    authRepository.saveToken(accessToken)
                }
            }
        }
    }
}
