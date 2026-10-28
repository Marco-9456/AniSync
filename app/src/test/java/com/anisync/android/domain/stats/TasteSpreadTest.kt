package com.anisync.android.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The cut-offs are on AniList's 100 point σ: under 10 Focused, under 16 Balanced. */
class TasteSpreadTest {

    @Test
    fun `no spread has no word`() {
        assertNull(tasteSpread(0.0))
        assertNull(tasteSpread(-1.0))
    }

    @Test
    fun `focused stops just under 10`() {
        assertEquals(TasteSpread.FOCUSED, tasteSpread(0.4))
        assertEquals(TasteSpread.FOCUSED, tasteSpread(9.9))
        assertEquals(TasteSpread.BALANCED, tasteSpread(10.0))
    }

    @Test
    fun `balanced stops just under 16`() {
        assertEquals(TasteSpread.BALANCED, tasteSpread(15.9))
        assertEquals(TasteSpread.ECLECTIC, tasteSpread(16.0))
    }

    @Test
    fun `a wide spread such as 19 point 9 is eclectic`() {
        assertEquals(TasteSpread.ECLECTIC, tasteSpread(19.9))
    }
}
