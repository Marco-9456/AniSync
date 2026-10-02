package com.anisync.android.ui.theme

import com.materialkolor.PaletteStyle
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The palette preference stores an index into [AppPaletteStyles], so reordering the list silently
 * changes every user's saved style. New styles may only be appended.
 */
class AppPaletteStylesTest {

    @Test
    fun `stored indices keep their style`() {
        val expected = listOf(
            PaletteStyle.TonalSpot,
            PaletteStyle.Neutral,
            PaletteStyle.Vibrant,
            PaletteStyle.Expressive,
            PaletteStyle.Rainbow,
            PaletteStyle.FruitSalad,
            PaletteStyle.Monochrome,
            PaletteStyle.Fidelity,
            PaletteStyle.Content,
        )
        assertEquals(expected, AppPaletteStyles.take(expected.size))
    }

    @Test
    fun `cmf is appended last`() {
        assertEquals(10, AppPaletteStyles.size)
        assert(AppPaletteStyles.last() is PaletteStyle.Cmf)
    }
}
