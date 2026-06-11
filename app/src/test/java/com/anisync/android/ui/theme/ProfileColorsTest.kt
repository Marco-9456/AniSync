package com.anisync.android.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileColorsTest {

    @Test
    fun `named color resolves to its preset hex`() {
        assertEquals(Color(0xFF3DB4F2), aniListProfileSeedColor("blue"))
        assertEquals(Color(0xFF4CCB48), aniListProfileSeedColor("green"))
    }

    @Test
    fun `named color is case and whitespace insensitive`() {
        assertEquals(AniListProfileColors["purple"], aniListProfileSeedColor("  Purple "))
    }

    @Test
    fun `six digit hex parses as opaque color`() {
        assertEquals(Color(0xFFAABBCC), aniListProfileSeedColor("#aabbcc"))
    }

    @Test
    fun `eight digit hex preserves alpha`() {
        assertEquals(Color(0x80112233), aniListProfileSeedColor("#80112233"))
    }

    @Test
    fun `null blank and default fall through to app theme`() {
        assertNull(aniListProfileSeedColor(null))
        assertNull(aniListProfileSeedColor(""))
        assertNull(aniListProfileSeedColor("   "))
        assertNull(aniListProfileSeedColor("default"))
    }

    @Test
    fun `unknown name or malformed hex returns null`() {
        assertNull(aniListProfileSeedColor("chartreuse"))
        assertNull(aniListProfileSeedColor("#12"))
        assertNull(aniListProfileSeedColor("#gggggg"))
    }
}
