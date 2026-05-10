package com.anisync.android.util

import com.anisync.android.util.AniListTextEncoder.encodeForAniList
import org.junit.Assert.assertEquals
import org.junit.Test

class AniListTextEncoderTest {

    @Test
    fun `empty input passes through`() {
        assertEquals("", encodeForAniList(""))
    }

    @Test
    fun `pure ascii unchanged`() {
        assertEquals("Hello, world!", encodeForAniList("Hello, world!"))
    }

    @Test
    fun `cjk text in BMP unchanged`() {
        assertEquals("你好,世界", encodeForAniList("你好,世界"))
    }

    @Test
    fun `cyrillic text in BMP unchanged`() {
        assertEquals("Привет", encodeForAniList("Привет"))
    }

    @Test
    fun `single thinking-face emoji encoded to decimal entity`() {
        assertEquals("&#129300;", encodeForAniList("🤔"))
    }

    @Test
    fun `mixed ascii and emoji preserves order`() {
        assertEquals("a&#129300;b", encodeForAniList("a🤔b"))
    }

    @Test
    fun `flag emoji encodes both regional indicator codepoints`() {
        // 🇯🇵 = U+1F1EF U+1F1F5 (two surrogate pairs)
        val flag = "🇯🇵"
        assertEquals("&#127471;&#127477;", encodeForAniList(flag))
    }

    @Test
    fun `already-encoded entity is idempotent`() {
        assertEquals("&#129300;", encodeForAniList("&#129300;"))
    }

    @Test
    fun `BMP boundary U+FFFD passes through unchanged`() {
        assertEquals("�", encodeForAniList("�"))
    }

    @Test
    fun `mixed BMP and supplementary preserves BMP runs`() {
        // 你 (BMP) + 🤔 (supp) + 好 (BMP)
        assertEquals("你&#129300;好", encodeForAniList("你🤔好"))
    }
}
