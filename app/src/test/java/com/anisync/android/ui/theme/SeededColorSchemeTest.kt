package com.anisync.android.ui.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SeededColorSchemeTest {

    private val seed = Color(0xFFE4A15D)

    @Test
    fun `matches the scheme MaterialKolor builds for every style and polarity`() {
        for (style in AppPaletteStyles) {
            for (isDark in listOf(false, true)) {
                val expected = dynamicColorScheme(
                    seedColor = seed,
                    isDark = isDark,
                    isAmoled = false,
                    style = style,
                    specVersion = ColorSpec.SpecVersion.SPEC_2026
                )
                val actual = seededColorScheme(seed, isDark, amoled = false, paletteStyle = style)
                assertEquals("$style, dark $isDark", expected.toString(), actual.toString())
            }
        }
    }

    @Test
    fun `a repeat request reuses the scheme already built`() {
        val first = seededColorScheme(seed, darkTheme = true, amoled = false, PaletteStyle.Vibrant)
        val second = seededColorScheme(seed, darkTheme = true, amoled = false, PaletteStyle.Vibrant)
        assertSame(first, second)
    }

    @Test
    fun `the cache only answers for schemes already built`() {
        val unseen = Color(0xFF2E7D5B)
        assertNull(cachedSeededColorScheme(unseen, darkTheme = false, amoled = false, PaletteStyle.Expressive))
        val built = seededColorScheme(unseen, darkTheme = false, amoled = false, PaletteStyle.Expressive)
        assertSame(built, cachedSeededColorScheme(unseen, darkTheme = false, amoled = false, PaletteStyle.Expressive))
    }

    @Test
    fun `amoled shifts the dark scheme and leaves the light one alone`() {
        val dark = seededColorScheme(seed, darkTheme = true, amoled = false, PaletteStyle.TonalSpot)
        val black = seededColorScheme(seed, darkTheme = true, amoled = true, PaletteStyle.TonalSpot)
        assertNotSame(dark, black)
        assertEquals(dark.toAmoled().toString(), black.toString())

        val light = seededColorScheme(seed, darkTheme = false, amoled = false, PaletteStyle.TonalSpot)
        val lightAmoled = seededColorScheme(seed, darkTheme = false, amoled = true, PaletteStyle.TonalSpot)
        assertSame(light, lightAmoled)
    }
}
