package com.anisync.android.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ContentLimitsTest {

    @Test
    fun `plain text counts every character`() {
        assertEquals(5, ContentLimits.reviewBodyLength("a b c"))
    }

    @Test
    fun `markdown symbols do not count`() {
        assertEquals(16, ContentLimits.reviewBodyLength("**bold** _it_ ~~del~~ # head"))
        assertEquals(9, ContentLimits.reviewBodyLength("well-known"))
    }

    @Test
    fun `images and embeds do not count`() {
        assertEquals(4, ContentLimits.reviewBodyLength("img220(https://a.b/c.png) text"))
        assertEquals(4, ContentLimits.reviewBodyLength("text youtube(https://youtu.be/abcdefghijk)"))
        assertEquals(4, ContentLimits.reviewBodyLength("![cover](https://a.b/c.png)text"))
    }

    @Test
    fun `a link counts its label only`() {
        assertEquals(5, ContentLimits.reviewBodyLength("[label](https://anilist.co)"))
    }

    @Test
    fun `html tags do not count`() {
        assertEquals(2, ContentLimits.reviewBodyLength("<center>hi</center>"))
    }

    @Test
    fun `invisible characters and runs of whitespace count as one space`() {
        assertEquals(3, ContentLimits.reviewBodyLength("a​​b"))
        assertEquals(3, ContentLimits.reviewBodyLength("a \n\n b"))
    }

    @Test
    fun `an emoji counts as one character, as AniList counts it`() {
        assertEquals(1, ContentLimits.characterCount("🐢"))
        assertEquals(3, ContentLimits.characterCount("a🐢b"))
    }

    @Test
    fun `cap never splits an emoji`() {
        assertEquals("ab🐢", ContentLimits.cap("ab🐢cd", 3))
        assertEquals("short", ContentLimits.cap("short", 10))
    }
}
