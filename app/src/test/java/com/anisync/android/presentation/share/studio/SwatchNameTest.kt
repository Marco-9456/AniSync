package com.anisync.android.presentation.share.studio

import com.anisync.android.R
import com.materialkolor.hct.Hct
import org.junit.Assert.assertEquals
import org.junit.Test

class SwatchNameTest {

    private fun assertName(expected: Int, argb: Long) {
        val hct = Hct.fromInt(argb.toInt())
        assertEquals("hue ${hct.hue} chroma ${hct.chroma} tone ${hct.tone}", expected, swatchName(argb.toInt()))
    }

    @Test
    fun `the frames' cover swatches keep their names`() {
        assertName(R.string.share_swatch_wine, 0xFF6D313D)
        assertName(R.string.share_swatch_slate, 0xFF3B5364)
        assertName(R.string.share_swatch_sage, 0xFF8AAC8E)
        assertName(R.string.share_swatch_cream, 0xFFE9CEA9)
    }

    @Test
    fun `vivid colours take the vivid name of their family`() {
        assertName(R.string.share_swatch_crimson, 0xFFE13333)
        assertName(R.string.share_swatch_cobalt, 0xFF3DB4F2)
        assertName(R.string.share_swatch_leaf, 0xFF4CCB48)
        assertName(R.string.share_swatch_violet, 0xFFC063FF)
        assertName(R.string.share_swatch_amber, 0xFFEF881A)
        assertName(R.string.share_swatch_fuchsia, 0xFFFC4DB8)
    }
}
