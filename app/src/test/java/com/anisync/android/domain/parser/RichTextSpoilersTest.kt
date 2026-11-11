package com.anisync.android.domain.parser

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spoilers as AniList's website draws them since 4/11/2026, fed the HTML its API actually returns
 * for each shape. The leak checks are the point: nothing between `~!` and `!~` may reach the text a
 * reader sees before tapping.
 */
class RichTextSpoilersTest {

    @Test
    fun `a label inside AniList's span becomes the spoiler's label`() = runBlocking {
        val parsed = RichTextParser.parse(
            "<p>Sad ending. <span class='markdown_spoiler'><span>{Season 3 spoilers}Myne was adopted" +
                "</span></span>. Hope it heals.</p>"
        )

        val spoiler = parsed.spoilers().single()
        assertEquals("Season 3 spoilers", spoiler.label)
        assertEquals("Myne was adopted", spoiler.hiddenText().trim())
        assertFalse(parsed.visibleText().contains("Myne"))
        assertFalse(parsed.visibleText().contains("{"))
    }

    @Test
    fun `raw markers around several paragraphs hide all of them`() = runBlocking {
        // asHtml leaves `~!` raw when a spoiler starting with a space runs over paragraphs.
        val parsed = RichTextParser.parse(
            "<p>Thoughts below.</p>\n" +
                "<p>~! the first secret paragraph</p>\n" +
                "<p>the second secret paragraph</p>\n" +
                "<p>the third secret paragraph!~</p>\n" +
                "<p>Bye.</p>"
        )

        val visible = parsed.visibleText()
        assertFalse(visible.contains("secret"))
        assertTrue(visible.contains("Thoughts below."))
        assertTrue(visible.contains("Bye."))
        val hidden = parsed.spoilers().single().hiddenText()
        assertTrue(hidden.contains("first secret"))
        assertTrue(hidden.contains("second secret"))
        assertTrue(hidden.contains("third secret"))
    }

    @Test
    fun `AniList's span crossing paragraphs keeps every paragraph inside`() = runBlocking {
        val parsed = RichTextParser.parse(
            "<p>Rant</p>\n<p><span class='markdown_spoiler'><span>first line<br />\nsecond line</p>\n" +
                "<p>middle paragraph</p>\n<p>last paragraph</span></span></p>"
        )

        val visible = parsed.visibleText()
        assertFalse(visible.contains("line"))
        assertFalse(visible.contains("paragraph"))
        assertTrue(parsed.spoilers().single().hiddenText().contains("last paragraph"))
    }

    @Test
    fun `a spoiler opened in a list item takes in what follows up to its closer`() = runBlocking {
        // The shape of the 4/11/2026 changelog, comment 3264061.
        val parsed = RichTextParser.parse(
            "<ul>\n<li>item one</li>\n<li>Markdown in spoilers:<br>~!\n<h1>Heading inside</h1></li>\n</ul>\n" +
                "<p><strong>Bold</strong> inside</p>\n" +
                "<blockquote>\n<ol>\n<li>quoted item</li>\n</ol>\n</blockquote>\n" +
                "<p><a href=\"https://anilist.co/anime/440/\">a link</a><br />\n!~</p>\n" +
                "<ul>\n<li>after the spoiler</li>\n</ul>"
        )

        val visible = parsed.visibleText()
        assertTrue(visible.contains("item one"))
        assertTrue(visible.contains("Markdown in spoilers:"))
        assertTrue(visible.contains("after the spoiler"))
        listOf("Heading inside", "Bold", "quoted item", "a link").forEach {
            assertFalse("$it leaked", visible.contains(it))
        }
        assertFalse(visible.contains("~!"))
        assertFalse(visible.contains("!~"))
    }

    @Test
    fun `escaped markers in a code span stay literal`() = runBlocking {
        val parsed = RichTextParser.parse("<p>Syntax: <code>~&excl; Spoiler Text &excl;~</code></p>")

        assertTrue(parsed.spoilers().isEmpty())
        assertTrue(parsed.visibleText().contains("~! Spoiler Text !~"))
    }

    @Test
    fun `a closer pairs with the nearest opener so spoilers nest`() = runBlocking {
        val parsed = RichTextParser.parse("~!outer ~!inner!~ rest!~")

        val outer = parsed.blocks.filterIsInstance<RichTextBlock.Spoiler>().single()
        assertTrue(outer.hiddenText().contains("outer"))
        assertTrue(outer.hiddenText().contains("rest"))
        assertTrue(outer.children.anySpoiler())
        assertEquals("", parsed.visibleText().trim())
    }

    @Test
    fun `a fourth level stays text inside the third`() = runBlocking {
        val parsed = RichTextParser.parse("~!a ~!b ~!c ~!d!~ c2!~ b2!~ a2!~")

        assertEquals(3, parsed.spoilerDepth())
        // Its markers stay, and a single tilde pair reads as strikethrough there as on the website.
        assertTrue(parsed.allText().contains("!d!"))
        assertEquals("", parsed.visibleText().trim())
    }

    @Test
    fun `empty braces are content and blank braces are no label`() = runBlocking {
        val braces = RichTextParser.parse("~!{} text!~").spoilers().single()
        assertNull(braces.label)
        assertTrue(braces.hiddenText().contains("{} text"))

        val blank = RichTextParser.parse("~!{   } text!~").spoilers().single()
        assertNull(blank.label)
        assertEquals("text", blank.hiddenText().trim())
    }

    @Test
    fun `a label cannot hold angle brackets or braces`() = runBlocking {
        val spoiler = RichTextParser.parse("~!{a &lt;b&gt; c} text!~").spoilers().single()
        assertNull(spoiler.label)
    }

    @Test
    fun `a closer with nothing open is skipped`() = runBlocking {
        val parsed = RichTextParser.parse("a !~ b ~!c!~")

        assertEquals("c", parsed.spoilers().single().hiddenText().trim())
        assertTrue(parsed.visibleText().contains("a !~ b"))
    }

    @Test
    fun `an unclosed opener stays literal`() = runBlocking {
        val parsed = RichTextParser.parse("<p>wow ~! nothing hidden here</p>")

        assertTrue(parsed.spoilers().isEmpty())
        assertTrue(parsed.visibleText().contains("~! nothing hidden here"))
    }

    @Test
    fun `the website's element reads its label and its escaped content`() = runBlocking {
        val parsed = RichTextParser.parse(
            "<div class=\"markdown-spoiler\" data-label=\"Ending\"><i class=\"hide-spoiler\"></i>" +
                "<div data-content=\"&lt;p&gt;Bobby lives&lt;/p&gt;\"></div></div>"
        )

        val spoiler = parsed.spoilers().single()
        assertEquals("Ending", spoiler.label)
        assertEquals("Bobby lives", spoiler.hiddenText().trim())
    }

    @Test
    fun `stripping drops spoilers with what they hide`() {
        assertEquals(
            "Before  after",
            RichTextSpoilers.stripSpoilers("Before ~!{Ending}Bobby ~!dies!~ twice!~ after")
        )
        assertEquals("no spoiler !~ here", RichTextSpoilers.stripSpoilers("no spoiler !~ here"))
        assertEquals(
            "<p>Before . After</p>",
            RichTextSpoilers.stripSpoilers(
                "<p>Before <span class='markdown_spoiler'><span>it ends</span></span>. After</p>"
            )
        )
    }

    private fun ParsedRichText.spoilers(): List<RichTextBlock.Spoiler> = blocks.spoilerBlocks()

    private fun List<RichTextBlock>.spoilerBlocks(): List<RichTextBlock.Spoiler> = flatMap { block ->
        when (block) {
            is RichTextBlock.Spoiler -> listOf(block)
            else -> block.childBlocks().spoilerBlocks()
        }
    }

    private fun List<RichTextBlock>.anySpoiler(): Boolean = spoilerBlocks().isNotEmpty()

    private fun ParsedRichText.spoilerDepth(): Int {
        fun depth(blocks: List<RichTextBlock>): Int = blocks.maxOfOrNull { block ->
            when (block) {
                is RichTextBlock.Spoiler -> 1 + depth(block.children)
                else -> depth(block.childBlocks())
            }
        } ?: 0
        return depth(blocks)
    }

    private fun RichTextBlock.childBlocks(): List<RichTextBlock> = when (this) {
        is RichTextBlock.Spoiler -> children
        is RichTextBlock.InlineGroup -> children
        is RichTextBlock.Blockquote -> children
        is RichTextBlock.ListBlock -> items.flatMap { it.children }
        is RichTextBlock.Table -> rows.flatMap { row -> row.cells.flatMap { it.children } }
        else -> emptyList()
    }

    private fun ParsedRichText.visibleText(): String = buildString { appendText(blocks, includeSpoilers = false) }

    private fun ParsedRichText.allText(): String = buildString { appendText(blocks, includeSpoilers = true) }

    private fun RichTextBlock.Spoiler.hiddenText(): String = buildString { appendText(children, includeSpoilers = true) }

    private fun StringBuilder.appendText(blocks: List<RichTextBlock>, includeSpoilers: Boolean) {
        for (block in blocks) {
            when (block) {
                is RichTextBlock.Text -> {
                    appendInlines(block.inlines)
                    append('\n')
                }
                is RichTextBlock.CodeBlock -> append(block.code).append('\n')
                is RichTextBlock.Spoiler -> if (includeSpoilers) appendText(block.children, true)
                else -> appendText(block.childBlocks(), includeSpoilers)
            }
        }
    }

    private fun StringBuilder.appendInlines(inlines: List<RichTextInline>) {
        for (inline in inlines) {
            when (inline) {
                is RichTextInline.Text -> append(inline.value)
                is RichTextInline.LineBreak -> append('\n')
                is RichTextInline.InlineCode -> append(inline.code)
                is RichTextInline.Bold -> appendInlines(inline.children)
                is RichTextInline.Italic -> appendInlines(inline.children)
                is RichTextInline.BoldItalic -> appendInlines(inline.children)
                is RichTextInline.Strikethrough -> appendInlines(inline.children)
                is RichTextInline.Link -> appendInlines(inline.children)
                is RichTextInline.Image -> Unit
            }
        }
    }
}
