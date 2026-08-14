package com.anisync.android.presentation.statistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatusExcerptTest {

    @Test
    fun `plain text keeps its first line`() {
        assertEquals(
            "Finally cleared the backlog",
            statusExcerpt("Finally cleared the backlog\nand started something new")
        )
    }

    @Test
    fun `an image embed is skipped in favour of the words under it`() {
        assertEquals(
            "Manga challenge, day 4",
            statusExcerpt("~~~img320(https://i.ibb.co/abc/CHALLENGE.gif)~~~\nManga challenge, day 4")
        )
    }

    @Test
    fun `a post that is only an embed has no excerpt`() {
        assertNull(statusExcerpt("~~~img320(https://i.ibb.co/abc/CHALLENGE.gif)~~~"))
        assertNull(statusExcerpt("youtube(https://youtu.be/abcdefg)"))
    }

    @Test
    fun `a link keeps its label and drops the address`() {
        assertEquals("Read my review here", statusExcerpt("[Read my review here](https://anilist.co/review/1)"))
    }

    @Test
    fun `a bare address is not a title`() {
        assertEquals("Watch this", statusExcerpt("https://example.com/very/long/path Watch this"))
    }

    @Test
    fun `html from the rendered field is stripped`() {
        assertEquals("Hi there", statusExcerpt("<p>Hi there</p>"))
    }

    @Test
    fun `markdown emphasis does not leak into the title`() {
        assertEquals("Spoilers ahead", statusExcerpt("## __Spoilers ahead__"))
    }

    @Test
    fun `nothing in, nothing out`() {
        assertNull(statusExcerpt(null))
        assertNull(statusExcerpt("   \n  "))
    }
}
