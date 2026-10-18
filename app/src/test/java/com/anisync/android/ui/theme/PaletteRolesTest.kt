package com.anisync.android.ui.theme

import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme
import org.junit.Assert.assertEquals
import org.junit.Test

class PaletteRolesTest {

    @Test
    fun `roles match the full scheme for every style and polarity`() {
        val seeds = PresetPalettes.all.filterNot { it.isDynamic }.map { it.seedColor }
        for (style in AppPaletteStyles) {
            for (isDark in listOf(false, true)) {
                for (seed in seeds) {
                    val scheme = dynamicColorScheme(
                        seedColor = seed,
                        isDark = isDark,
                        isAmoled = false,
                        style = style,
                        specVersion = ColorSpec.SpecVersion.SPEC_2026
                    )
                    val roles = paletteRoles(seed, isDark, style)
                    val case = "$style, dark $isDark, seed $seed"
                    assertEquals(case, scheme.primary, roles.primary())
                    assertEquals(case, scheme.secondary, roles.secondary())
                    assertEquals(case, scheme.tertiary, roles.tertiary())
                    assertEquals(case, scheme.primaryContainer, roles.primaryContainer())
                    assertEquals(case, scheme.secondaryContainer, roles.secondaryContainer())
                    assertEquals(case, scheme.surfaceContainer, roles.surfaceContainer())
                    assertEquals(case, scheme.surfaceContainerHigh, roles.surfaceContainerHigh())
                    assertEquals(case, scheme.outlineVariant, roles.outlineVariant())
                    assertEquals(case, scheme.onSurfaceVariant, roles.onSurfaceVariant())
                }
            }
        }
    }
}
