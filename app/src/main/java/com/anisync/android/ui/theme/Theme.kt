package com.anisync.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = OliveDrab,
    onPrimary = Color.White,
    secondary = BeigeYellow,
    onSecondary = TextDark,
    tertiary = PastelPink, // Using for negative actions implies it might be used as error or custom, but generic slot here
    background = CreamBackground,
    onBackground = TextDark,
    surface = SurfacePinkWhite, // Or White, user said "Surface Colors: Pale pinkish-white for cards"
    onSurface = TextDark,
    error = BehindRed,
    onError = Color.White,
    surfaceVariant = SurfacePinkWhite, // For cards if they use defaults
    onSurfaceVariant = TextDark
)

// Defining DarkColorScheme same as Light for now to enforce the requested aesthetic, 
// as the user asked for a "Permanent theme change" based on specific screenshots which are light/warm.
// If dark mode is strictly required to be dark, we would need different colors, but "Cream" background implies light theme.
// defaulting to the "Warm" theme for both for now to ensure consistency with the request "pixel-perfectly match".
private val DarkColorScheme = LightColorScheme

@Composable
fun AniSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        //     val context = LocalContext.current
        //     if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        // }
        // Enforcing the custom theme as per request for "Global Theme Update" to match screenshots
        else -> LightColorScheme 
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
