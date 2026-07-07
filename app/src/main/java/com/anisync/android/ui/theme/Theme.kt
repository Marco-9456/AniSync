package com.anisync.android.ui.theme
import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.anisync.android.data.ThemeMode
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

private val mediumContrastLightColorScheme = lightColorScheme(
    primary = primaryLightMediumContrast,
    onPrimary = onPrimaryLightMediumContrast,
    primaryContainer = primaryContainerLightMediumContrast,
    onPrimaryContainer = onPrimaryContainerLightMediumContrast,
    secondary = secondaryLightMediumContrast,
    onSecondary = onSecondaryLightMediumContrast,
    secondaryContainer = secondaryContainerLightMediumContrast,
    onSecondaryContainer = onSecondaryContainerLightMediumContrast,
    tertiary = tertiaryLightMediumContrast,
    onTertiary = onTertiaryLightMediumContrast,
    tertiaryContainer = tertiaryContainerLightMediumContrast,
    onTertiaryContainer = onTertiaryContainerLightMediumContrast,
    error = errorLightMediumContrast,
    onError = onErrorLightMediumContrast,
    errorContainer = errorContainerLightMediumContrast,
    onErrorContainer = onErrorContainerLightMediumContrast,
    background = backgroundLightMediumContrast,
    onBackground = onBackgroundLightMediumContrast,
    surface = surfaceLightMediumContrast,
    onSurface = onSurfaceLightMediumContrast,
    surfaceVariant = surfaceVariantLightMediumContrast,
    onSurfaceVariant = onSurfaceVariantLightMediumContrast,
    outline = outlineLightMediumContrast,
    outlineVariant = outlineVariantLightMediumContrast,
    scrim = scrimLightMediumContrast,
    inverseSurface = inverseSurfaceLightMediumContrast,
    inverseOnSurface = inverseOnSurfaceLightMediumContrast,
    inversePrimary = inversePrimaryLightMediumContrast,
    surfaceDim = surfaceDimLightMediumContrast,
    surfaceBright = surfaceBrightLightMediumContrast,
    surfaceContainerLowest = surfaceContainerLowestLightMediumContrast,
    surfaceContainerLow = surfaceContainerLowLightMediumContrast,
    surfaceContainer = surfaceContainerLightMediumContrast,
    surfaceContainerHigh = surfaceContainerHighLightMediumContrast,
    surfaceContainerHighest = surfaceContainerHighestLightMediumContrast,
)

private val highContrastLightColorScheme = lightColorScheme(
    primary = primaryLightHighContrast,
    onPrimary = onPrimaryLightHighContrast,
    primaryContainer = primaryContainerLightHighContrast,
    onPrimaryContainer = onPrimaryContainerLightHighContrast,
    secondary = secondaryLightHighContrast,
    onSecondary = onSecondaryLightHighContrast,
    secondaryContainer = secondaryContainerLightHighContrast,
    onSecondaryContainer = onSecondaryContainerLightHighContrast,
    tertiary = tertiaryLightHighContrast,
    onTertiary = onTertiaryLightHighContrast,
    tertiaryContainer = tertiaryContainerLightHighContrast,
    onTertiaryContainer = onTertiaryContainerLightHighContrast,
    error = errorLightHighContrast,
    onError = onErrorLightHighContrast,
    errorContainer = errorContainerLightHighContrast,
    onErrorContainer = onErrorContainerLightHighContrast,
    background = backgroundLightHighContrast,
    onBackground = onBackgroundLightHighContrast,
    surface = surfaceLightHighContrast,
    onSurface = onSurfaceLightHighContrast,
    surfaceVariant = surfaceVariantLightHighContrast,
    onSurfaceVariant = onSurfaceVariantLightHighContrast,
    outline = outlineLightHighContrast,
    outlineVariant = outlineVariantLightHighContrast,
    scrim = scrimLightHighContrast,
    inverseSurface = inverseSurfaceLightHighContrast,
    inverseOnSurface = inverseOnSurfaceLightHighContrast,
    inversePrimary = inversePrimaryLightHighContrast,
    surfaceDim = surfaceDimLightHighContrast,
    surfaceBright = surfaceBrightLightHighContrast,
    surfaceContainerLowest = surfaceContainerLowestLightHighContrast,
    surfaceContainerLow = surfaceContainerLowLightHighContrast,
    surfaceContainer = surfaceContainerLightHighContrast,
    surfaceContainerHigh = surfaceContainerHighLightHighContrast,
    surfaceContainerHighest = surfaceContainerHighestLightHighContrast,
)

private val mediumContrastDarkColorScheme = darkColorScheme(
    primary = primaryDarkMediumContrast,
    onPrimary = onPrimaryDarkMediumContrast,
    primaryContainer = primaryContainerDarkMediumContrast,
    onPrimaryContainer = onPrimaryContainerDarkMediumContrast,
    secondary = secondaryDarkMediumContrast,
    onSecondary = onSecondaryDarkMediumContrast,
    secondaryContainer = secondaryContainerDarkMediumContrast,
    onSecondaryContainer = onSecondaryContainerDarkMediumContrast,
    tertiary = tertiaryDarkMediumContrast,
    onTertiary = onTertiaryDarkMediumContrast,
    tertiaryContainer = tertiaryContainerDarkMediumContrast,
    onTertiaryContainer = onTertiaryContainerDarkMediumContrast,
    error = errorDarkMediumContrast,
    onError = onErrorDarkMediumContrast,
    errorContainer = errorContainerDarkMediumContrast,
    onErrorContainer = onErrorContainerDarkMediumContrast,
    background = backgroundDarkMediumContrast,
    onBackground = onBackgroundDarkMediumContrast,
    surface = surfaceDarkMediumContrast,
    onSurface = onSurfaceDarkMediumContrast,
    surfaceVariant = surfaceVariantDarkMediumContrast,
    onSurfaceVariant = onSurfaceVariantDarkMediumContrast,
    outline = outlineDarkMediumContrast,
    outlineVariant = outlineVariantDarkMediumContrast,
    scrim = scrimDarkMediumContrast,
    inverseSurface = inverseSurfaceDarkMediumContrast,
    inverseOnSurface = inverseOnSurfaceDarkMediumContrast,
    inversePrimary = inversePrimaryDarkMediumContrast,
    surfaceDim = surfaceDimDarkMediumContrast,
    surfaceBright = surfaceBrightDarkMediumContrast,
    surfaceContainerLowest = surfaceContainerLowestDarkMediumContrast,
    surfaceContainerLow = surfaceContainerLowDarkMediumContrast,
    surfaceContainer = surfaceContainerDarkMediumContrast,
    surfaceContainerHigh = surfaceContainerHighDarkMediumContrast,
    surfaceContainerHighest = surfaceContainerHighestDarkMediumContrast,
)

private val highContrastDarkColorScheme = darkColorScheme(
    primary = primaryDarkHighContrast,
    onPrimary = onPrimaryDarkHighContrast,
    primaryContainer = primaryContainerDarkHighContrast,
    onPrimaryContainer = onPrimaryContainerDarkHighContrast,
    secondary = secondaryDarkHighContrast,
    onSecondary = onSecondaryDarkHighContrast,
    secondaryContainer = secondaryContainerDarkHighContrast,
    onSecondaryContainer = onSecondaryContainerDarkHighContrast,
    tertiary = tertiaryDarkHighContrast,
    onTertiary = onTertiaryDarkHighContrast,
    tertiaryContainer = tertiaryContainerDarkHighContrast,
    onTertiaryContainer = onTertiaryContainerDarkHighContrast,
    error = errorDarkHighContrast,
    onError = onErrorDarkHighContrast,
    errorContainer = errorContainerDarkHighContrast,
    onErrorContainer = onErrorContainerDarkHighContrast,
    background = backgroundDarkHighContrast,
    onBackground = onBackgroundDarkHighContrast,
    surface = surfaceDarkHighContrast,
    onSurface = onSurfaceDarkHighContrast,
    surfaceVariant = surfaceVariantDarkHighContrast,
    onSurfaceVariant = onSurfaceVariantDarkHighContrast,
    outline = outlineDarkHighContrast,
    outlineVariant = outlineVariantDarkHighContrast,
    scrim = scrimDarkHighContrast,
    inverseSurface = inverseSurfaceDarkHighContrast,
    inverseOnSurface = inverseOnSurfaceDarkHighContrast,
    inversePrimary = inversePrimaryDarkHighContrast,
    surfaceDim = surfaceDimDarkHighContrast,
    surfaceBright = surfaceBrightDarkHighContrast,
    surfaceContainerLowest = surfaceContainerLowestDarkHighContrast,
    surfaceContainerLow = surfaceContainerLowDarkHighContrast,
    surfaceContainer = surfaceContainerDarkHighContrast,
    surfaceContainerHigh = surfaceContainerHighDarkHighContrast,
    surfaceContainerHighest = surfaceContainerHighestDarkHighContrast,
)

@Immutable
data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color
)

val unspecified_scheme = ColorFamily(
    Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified
)

/**
 * Converts a dark [ColorScheme] into an AMOLED ("pure black") one.
 *
 * Blackening only `background`/`surface` (what MaterialKolor's own `isAmoled` does) leaves the
 * `surfaceContainer*` roles at mid-grey, so cards, the search bar and the nav pill float as grey
 * patches on a pure-black base. Instead — following Seal's high-contrast theme — we also blacken the
 * lowest container and **shift the whole elevation ladder down one tonal step**. The base goes truly
 * black while elevated surfaces stay near-black greys that keep their *relative* depth, so the result
 * reads as one cohesive pitch-black theme rather than grey-on-black.
 *
 * The right-hand sides reference the original (receiver) values, so each role is pulled from the step
 * below it. Only meaningful on a dark scheme.
 */
internal fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = surfaceContainerLowest,
    surfaceContainer = surfaceContainerLow,
    surfaceContainerHigh = surfaceContainer,
    surfaceContainerHighest = surfaceContainerHigh,
)

/**
 * Whether the *system* is in dark theme. Not [isSystemInDarkTheme]: AppCompatDelegate overrides
 * the activity's uiMode to the persisted night mode, and an activity created under a forced mode
 * never receives uiMode config dispatches again — so both the value and the change signal must
 * come from the application context, which the delegate never touches.
 */
@Composable
private fun isSystemActuallyDark(): Boolean {
    val appContext = LocalContext.current.applicationContext
    fun read(configuration: Configuration): Boolean =
        (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    var isDark by remember(appContext) {
        mutableStateOf(read(appContext.resources.configuration))
    }
    DisposableEffect(appContext) {
        val callbacks = object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                isDark = read(newConfig)
            }

            override fun onLowMemory() = Unit
        }
        appContext.registerComponentCallbacks(callbacks)
        onDispose { appContext.unregisterComponentCallbacks(callbacks) }
    }
    return isDark
}

/**
 * Resolves the user's [ThemeMode] to the effective dark-theme flag. The single source of truth
 * for "is the app dark?" — never re-derive it from [isSystemInDarkTheme].
 */
@Composable
fun ThemeMode.resolveDarkTheme(): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemActuallyDark()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    // Pure-black ("AMOLED") backgrounds. Only honored while [darkTheme] is active.
    amoled: Boolean = false,
    // Custom seed color for MaterialKolor-generated schemes
    seedColor: Color? = null,
    // Palette style for MaterialKolor color generation
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    // Live per-category variable-font axis overrides from the developer font playground
    typographyOverrides: TypographyOverrides = TypographyOverrides.None,
    content: @Composable () -> Unit
) {
    // AMOLED is a dark-only treatment; ignore it in light mode so toggling it can't blacken a
    // light theme (relevant under SYSTEM mode during the day).
    val useAmoled = amoled && darkTheme

    // Priority order for color scheme selection:
    // 1. Custom seed color (user-selected palette)
    // 2. Android 12+ dynamic colors (wallpaper-based)
    // 3. Static fallback scheme
    val colorScheme = when {
        // Priority 1: Custom seed color from user selection.
        // isAmoled is left off here: MaterialKolor's variant only blackens background/surface, which
        // looks patchy. We apply our own toAmoled() (Seal-style container shift) for a cohesive look
        // consistent with the other two paths.
        seedColor != null -> {
            val base = rememberDynamicColorScheme(
                seedColor = seedColor,
                isDark = darkTheme,
                isAmoled = false,
                style = paletteStyle
            )
            if (useAmoled) base.toAmoled() else base
        }
        // Priority 2: Dynamic color (Android 12+)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val base = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (useAmoled) base.toAmoled() else base
        }
        // Priority 3: Static fallback
        darkTheme -> if (useAmoled) darkScheme.toAmoled() else darkScheme
        else -> lightScheme
    }

    // Set status bar icons color based on theme
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as android.app.Activity).window
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
            // Both bars: enableEdgeToEdge() styles them from the system theme once at creation,
            // which goes stale when the in-app theme differs or changes without a recreate.
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    // Rebuild typography only when the font-axis overrides change, so dragging a slider in
    // the developer font playground re-renders the whole app's text in real time.
    val typography = remember(typographyOverrides) { buildAppTypography(typographyOverrides) }
    val expressiveTypography = remember(typographyOverrides) {
        defaultExpressiveTypography(typographyOverrides)
    }

    CompositionLocalProvider(
        LocalExpressiveTypography provides expressiveTypography,
        LocalAppThemeSpec provides AppThemeSpec(
            dynamicColor = dynamicColor,
            seedColor = seedColor,
            paletteStyle = paletteStyle
        )
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = AppShapes,
            motionScheme = MotionScheme.expressive(),
            content = content
        )
    }
}

/**
 * The palette inputs [AppTheme] resolved its scheme from, minus the light/dark polarity. Lets a
 * consumer (share cards) re-derive **the same palette** in a *forced* polarity instead of falling
 * back to the static brand scheme.
 */
@Immutable
data class AppThemeSpec(
    val dynamicColor: Boolean,
    val seedColor: Color?,
    val paletteStyle: PaletteStyle,
)

val LocalAppThemeSpec = androidx.compose.runtime.staticCompositionLocalOf {
    AppThemeSpec(dynamicColor = false, seedColor = null, paletteStyle = PaletteStyle.TonalSpot)
}

/**
 * A color scheme for a **share card**: the app's own palette (user seed > Android dynamic > static,
 * via [LocalAppThemeSpec]) re-derived in the forced [dark] polarity, so Light/Dark/AMOLED cards
 * differ from the in-app look only by brightness — never by hue. [seed] (artwork color) overrides
 * the palette entirely for the COVER theme. AMOLED is our Seal-style container shift, dark-only.
 * Lives here (not in the share package) so it can reach the file-private base schemes and [toAmoled].
 */
@Composable
fun shareCardColorScheme(dark: Boolean, amoled: Boolean = false, seed: Color? = null): ColorScheme {
    val spec = LocalAppThemeSpec.current
    val base = when {
        seed != null -> rememberDynamicColorScheme(
            seedColor = seed,
            isDark = dark,
            isAmoled = false,
            style = PaletteStyle.TonalSpot
        )
        spec.seedColor != null -> rememberDynamicColorScheme(
            seedColor = spec.seedColor,
            isDark = dark,
            isAmoled = false,
            style = spec.paletteStyle
        )
        spec.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> darkScheme
        else -> lightScheme
    }
    return if (amoled && dark) base.toAmoled() else base
}

