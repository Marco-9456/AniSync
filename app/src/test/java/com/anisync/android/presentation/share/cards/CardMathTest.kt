package com.anisync.android.presentation.share.cards

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class CardMathTest {

    @Test
    fun `dim at its default gives the frames' scrim`() {
        assertEquals(0.25f, dimmed(0.25f, CardSpec.DEFAULT_DIM), 0.0001f)
        assertEquals(0.96f, dimmed(0.96f, CardSpec.DEFAULT_DIM), 0.0001f)
    }

    @Test
    fun `no dim clears the scrim and more dim only darkens`() {
        assertEquals(0f, dimmed(0.55f, 0f), 0.0001f)
        val more = dimmed(0.55f, 0.8f)
        assert(more > 0.55f && more <= 1f)
    }

    @Test
    fun `a flat image stays flat under the blur`() {
        val pixels = IntArray(16) { 0xFF336699.toInt() }
        boxBlur(pixels, 4, 4, 2)
        assertArrayEquals(IntArray(16) { 0xFF336699.toInt() }, pixels)
    }

    @Test
    fun `the blur spreads a bright pixel over its neighbours`() {
        val pixels = IntArray(9) { 0xFF000000.toInt() }
        pixels[4] = 0xFFFFFFFF.toInt()
        boxBlur(pixels, 3, 3, 1)
        val red = (pixels[0] shr 16) and 0xFF
        assertEquals(255 / 9, red)
        assertEquals(pixels[0], pixels[8])
    }

    @Test
    fun `ranks that fit keep their full width`() {
        assertEquals(Int.MAX_VALUE, fairShareCap(listOf(80, 90, 100), 300))
    }

    @Test
    fun `a long rank gives way before the short ones`() {
        val cap = fairShareCap(listOf(60, 400, 400), 300)
        assertEquals(120, cap)
        assertEquals(300, listOf(60, 400, 400).sumOf { minOf(it, cap) })
    }

    @Test
    fun `a short rank stays whole when only one is long`() {
        assertEquals(220, fairShareCap(listOf(40, 500, 40), 300))
    }
}
