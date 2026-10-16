package com.anisync.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.anisync.android.data.DisplaySize

/**
 * Scales everything [content] draws by [size]. Only the density changes, so text grows with
 * the layout and keeps the phone's own font scale on top of it.
 */
@Composable
fun ProvideDisplaySize(size: DisplaySize, content: @Composable () -> Unit) {
    val base = LocalDensity.current
    val density = remember(base, size) {
        if (size == DisplaySize.DEFAULT) base else Density(base.density * size.scale, base.fontScale)
    }
    CompositionLocalProvider(LocalDensity provides density, content = content)
}
