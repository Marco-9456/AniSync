package com.anisync.android.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class RichTextPlainTextTest {

    @Test
    fun `a spoiler crossing paragraphs stays out of the excerpt`() {
        val html = "<p>Rant</p>\n<p><span class='markdown_spoiler'><span>first line<br />\nsecond line</p>\n" +
            "<p>middle</p>\n<p>last</span></span></p>"

        assertEquals("Rant", RichTextPlainText.excerpt(html))
    }

    @Test
    fun `raw markers left by asHtml stay out of the excerpt`() {
        val html = "<p>Thoughts</p>\n<p>~! secret one</p>\n<p>secret two!~</p>\n<p>Bye</p>"

        assertEquals("Thoughts Bye", RichTextPlainText.excerpt(html))
    }

    @Test
    fun `an inline spoiler leaves the sentence around it`() {
        val html = "<p>It ends <span class='markdown_spoiler'><span>{Finale}badly</span></span>, sadly.</p>"

        assertEquals("It ends , sadly.", RichTextPlainText.excerpt(html))
    }

    @Test
    fun `a post asHtml fenced into a code block keeps its spoiler hidden`() {
        val html = "<pre><code>Intro line\n&lt;span class='markdown_spoiler'&gt;&lt;span&gt;hidden words" +
            "&lt;/span&gt;&lt;/span&gt;\nOutro</code></pre>"

        val excerpt = RichTextPlainText.excerpt(html)
        assertEquals(false, excerpt.contains("hidden"))
        assertEquals(true, excerpt.contains("Intro line"))
        assertEquals(true, excerpt.contains("Outro"))
    }

    @Test
    fun `an opener swallowed into the language token still hides what follows`() {
        val html = "<pre><code class=\"language-&lt;span class=&#039;markdown_spoiler&#039;&gt;&lt;span&gt;{Ending}\">\n" +
            "the hidden quote&lt;/span&gt;&lt;/span&gt;\nOutro</code></pre>"

        val excerpt = RichTextPlainText.excerpt(html)
        assertEquals(false, excerpt.contains("hidden"))
        assertEquals(true, excerpt.contains("Outro"))
    }

    @Test
    fun `blank input gives an empty excerpt`() {
        assertEquals("", RichTextPlainText.excerpt("  "))
    }
}
