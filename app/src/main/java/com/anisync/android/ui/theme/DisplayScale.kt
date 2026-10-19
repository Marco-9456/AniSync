package com.anisync.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.anisync.android.data.DisplaySize

/** The display size the app draws at, read again inside sheets, dialogs and popups. */
val LocalDisplaySize = staticCompositionLocalOf { DisplaySize.DEFAULT }

/**
 * Scales everything [content] draws by [size]. Only the density changes, so text grows with
 * the layout and keeps the phone's own font scale on top of it.
 *
 * Sheets, dialogs and popups compose in a window of their own, and Compose gives each window the
 * phone's density back. Their content calls this with no [size] to take the app's again. The scale
 * applies to the window's own density, so nesting it does not compound.
 */
@Composable
fun ProvideDisplaySize(size: DisplaySize = LocalDisplaySize.current, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = remember(context, configuration, size) {
        val base = Density(context)
        if (size == DisplaySize.DEFAULT) base else Density(base.density * size.scale, base.fontScale)
    }
    CompositionLocalProvider(
        LocalDisplaySize provides size,
        LocalDensity provides density,
        content = content
    )
}
