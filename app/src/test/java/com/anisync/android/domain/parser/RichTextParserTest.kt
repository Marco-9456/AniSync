package com.anisync.android.domain.parser

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextParserTest {

    @Test
    fun `parses basic bold markdown in list item`() = runBlocking {
        val parsed = RichTextParser.parse("<ul><li>__bold text__</li></ul>")

        val list = parsed.blocks.filterIsInstance<RichTextBlock.ListBlock>().first()
        val textBlock = list.items.first().children.filterIsInstance<RichTextBlock.Text>().first()
        val asDebug = textBlock.debugInlineText()

        assertTrue(asDebug.contains("bold text"))
        assertTrue(textBlock.hasBold())
    }

    @Test
    fun `parses center heading alignment`() = runBlocking {
        val parsed = RichTextParser.parse("<h1><center>**Video Games**</center></h1>")
        val heading = parsed.blocks.filterIsInstance<RichTextBlock.Text>().first()

        assertEquals(RichTextAlignment.Center, heading.align)
        assertEquals(RichTextTextKind.Heading1, heading.kind)
        assertTrue(heading.hasBold())
    }

    @Test
    fun `body level markdown links become inline links`() = runBlocking {
        val parsed = RichTextParser.parse("[__All AWC Challenges__](https://anilist.co/activity/26266744)")
        val textBlock = parsed.blocks.filterIsInstance<RichTextBlock.Text>().first()

        assertTrue(textBlock.debugInlineText().contains("All AWC Challenges"))
        assertTrue(textBlock.hasLink("https://anilist.co/activity/26266744"))
        assertTrue(textBlock.hasBold())
    }

    @Test
    fun `anilist media urls become preview blocks`() = runBlocking {
        val parsed = RichTextParser.parse("Visit https://anilist.co/anime/16498/attack-on-titan")

        val linkBlock = parsed.blocks.deepBlocks().filterIsInstance<RichTextBlock.AnilistLink>().first()
        assertEquals("anime", linkBlock.type)
        assertEquals(16498, linkBlock.id)
        assertEquals(LinkPreviewKey("anime", 16498), linkBlock.previewKey)
    }

    @Test
    fun `parses markdown image with percentage width`() = runBlocking {
        val parsed = RichTextParser.parse("img90%(https://example.com/photo.jpg)")

        val image = parsed.blocks.filterIsInstance<RichTextBlock.Image>().first()
        assertEquals("https://example.com/photo.jpg", image.url)
        assertEquals(90, image.width)
        assertTrue(image.isPercent)
        assertEquals(listOf("https://example.com/photo.jpg"), parsed.imageUrls)
    }

    @Test
    fun `parses blockquote with nested text and image`() = runBlocking {
        val parsed = RichTextParser.parse(
            """
            <blockquote>
                <p>Quoted</p>
                <img src='https://example.com/img.png'>
            </blockquote>
            """.trimIndent()
        )

        val quote = parsed.blocks.deepBlocks().filterIsInstance<RichTextBlock.Blockquote>().first()
        val quoteDescendants = quote.children.deepBlocks()
        assertTrue(quoteDescendants.any { it is RichTextBlock.Text && it.debugInlineText().contains("Quoted") })
        assertTrue(quoteDescendants.any { it is RichTextBlock.Image && it.url == "https://example.com/img.png" })
    }

    @Test
    fun `parses youtube and webm markdown as media blocks`() = runBlocking {
        val parsed = RichTextParser.parse("youtube(dQw4w9WgXcQ)\nwebm(https://example.com/clip.webm)")

        val youtube = parsed.blocks.filterIsInstance<RichTextBlock.YouTube>().first()
        val video = parsed.blocks.filterIsInstance<RichTextBlock.Video>().first()

        assertEquals("dQw4w9WgXcQ", youtube.videoIdOrUrl)
        assertEquals("https://example.com/clip.webm", video.url)
    }

    @Test
    fun `parses hr from markdown separator`() = runBlocking {
        val parsed = RichTextParser.parse("<p>Before</p>\n---\n<p>After</p>")

        assertTrue(parsed.blocks.deepBlocks().none { it is RichTextBlock.HorizontalRule })
    }

    @Test
    fun `anchor with image markdown keeps trailing text`() = runBlocking {
        val parsed = RichTextParser.parse(
            """
            <a href=\"https://anilist.co/anime/123\">img(https://example.com/img.jpg)</a>
            Some text after the link
            """.trimIndent()
        )

        val deep = parsed.blocks.deepBlocks()
        val image = deep.filterIsInstance<RichTextBlock.Image>().first()
        val text = deep.filterIsInstance<RichTextBlock.Text>()
            .joinToString(" ") { it.debugInlineText() }

        assertEquals("https://anilist.co/anime/123", image.linkUrl?.replace("\"", ""))
        assertTrue(text.contains("Some text after the link"))
    }

    @Test
    fun `markdown link with parentheses in url`() = runBlocking {
        val parsed = RichTextParser.parse("[Wiki](https://en.wikipedia.org/wiki/Kotlin_(language))")
        val text = parsed.blocks.filterIsInstance<RichTextBlock.Text>().first()
        assertTrue(text.hasLink("https://en.wikipedia.org/wiki/Kotlin_(language)"))
    }

    @Test
    fun `parses html table with headers`() = runBlocking {
        val parsed = RichTextParser.parse("<table><tr><th>Name</th></tr><tr><td>Value</td></tr></table>")
        val table = parsed.blocks.filterIsInstance<RichTextBlock.Table>().first()
        assertEquals(2, table.rows.size)
        assertTrue(table.rows[0].cells[0].isHeader)
        assertFalse(table.rows[1].cells[0].isHeader)
    }

    @Test
    fun `fenced code block with triple backticks`() = runBlocking {
        val parsed = RichTextParser.parse("```\nval x = 1\n```")
        val code = parsed.blocks.filterIsInstance<RichTextBlock.CodeBlock>().first()
        assertEquals("val x = 1", code.code)
    }

    @Test
    fun `strikethrough markdown`() = runBlocking {
        val parsed = RichTextParser.parse("~~deleted~~")
        val text = parsed.blocks.filterIsInstance<RichTextBlock.Text>().first()
        assertTrue(text.inlines.containsInline { it is RichTextInline.Strikethrough })
    }

    @Test
    fun `nested unordered list`() = runBlocking {
        val parsed = RichTextParser.parse("<ul><li>A<ul><li>B</li></ul></li></ul>")
        val list = parsed.blocks.filterIsInstance<RichTextBlock.ListBlock>().first()
        val innerList = list.items.first().children.deepBlocks()
            .filterIsInstance<RichTextBlock.ListBlock>()
        assertTrue(innerList.isNotEmpty())
    }

    @Test
    fun `malformed html does not throw`() = runBlocking {
        val parsed = RichTextParser.parse("<div><span><b>unclosed")
        assertTrue(parsed.blocks.isNotEmpty() || parsed.warnings.isNotEmpty())
    }

    @Test
    fun `anilist user link becomes preview block`() = runBlocking {
        val parsed = RichTextParser.parse("https://anilist.co/user/12345")
        val link = parsed.blocks.deepBlocks().filterIsInstance<RichTextBlock.AnilistLink>().first()
        assertEquals("user", link.type)
        assertEquals(12345, link.id)
    }

    @Test
    fun `center alignment via tilde markdown`() = runBlocking {
        val parsed = RichTextParser.parse("~~~center\nCentered text\n~~~")
        val centered = parsed.blocks.deepBlocks()
            .filterIsInstance<RichTextBlock.Text>()
            .first { it.debugInlineText().contains("Centered text") }
        assertEquals(RichTextAlignment.Center, centered.align)
    }

    @Test
    fun `empty input returns empty result`() = runBlocking {
        val parsed = RichTextParser.parse("  ")
        assertTrue(parsed.blocks.isEmpty())
        assertTrue(parsed.imageUrls.isEmpty())
        assertTrue(parsed.warnings.isEmpty())
    }

    @Test
    fun `markdown spoilers are converted to spoiler blocks`() = runBlocking {
        val parsed = RichTextParser.parse("~!Secret!~")
        val spoiler = parsed.blocks.filterIsInstance<RichTextBlock.Spoiler>().first()
        val text = spoiler.children.filterIsInstance<RichTextBlock.Text>().first().debugInlineText()

        assertEquals("Secret", text)
    }

    @Test
    fun `inline code keeps literal content`() = runBlocking {
        val parsed = RichTextParser.parse("Use `val x = 1` now")
        val text = parsed.blocks.filterIsInstance<RichTextBlock.Text>().first()

        assertTrue(text.hasInlineCode("val x = 1"))
        assertFalse(text.debugInlineText().contains("`"))
    }

    private fun RichTextBlock.Text.debugInlineText(): String = inlines.toDebugPlainText()

    private fun RichTextBlock.Text.hasBold(): Boolean = inlines.containsInline { it is RichTextInline.Bold || it is RichTextInline.BoldItalic }

    private fun RichTextBlock.Text.hasLink(url: String): Boolean = inlines.containsInline {
        it is RichTextInline.Link && it.url == url
    }

    private fun RichTextBlock.Text.hasInlineCode(code: String): Boolean = inlines.containsInline {
        it is RichTextInline.InlineCode && it.code == code
    }

    private fun List<RichTextInline>.containsInline(predicate: (RichTextInline) -> Boolean): Boolean {
        for (inline in this) {
            if (predicate(inline)) return true
            val nested = when (inline) {
                is RichTextInline.Bold -> inline.children
                is RichTextInline.Italic -> inline.children
                is RichTextInline.BoldItalic -> inline.children
                is RichTextInline.Strikethrough -> inline.children
                is RichTextInline.Link -> inline.children
                else -> emptyList()
            }
            if (nested.containsInline(predicate)) return true
        }
        return false
    }

    private fun List<RichTextInline>.toDebugPlainText(): String = buildString {
        appendInlineText(this@toDebugPlainText)
    }

    private fun List<RichTextBlock>.deepBlocks(): List<RichTextBlock> {
        val result = mutableListOf<RichTextBlock>()
        fun walk(block: RichTextBlock) {
            result.add(block)
            when (block) {
                is RichTextBlock.InlineGroup -> block.children.forEach(::walk)
                is RichTextBlock.Spoiler -> block.children.forEach(::walk)
                is RichTextBlock.Blockquote -> block.children.forEach(::walk)
                is RichTextBlock.ListBlock -> block.items.forEach { item -> item.children.forEach(::walk) }
                is RichTextBlock.Table -> block.rows.forEach { row -> row.cells.forEach { cell -> cell.children.forEach(::walk) } }
                else -> Unit
            }
        }

        forEach(::walk)
        return result
    }

    private fun StringBuilder.appendInlineText(inlines: List<RichTextInline>) {
        for (inline in inlines) {
            when (inline) {
                is RichTextInline.Text -> append(inline.value)
                is RichTextInline.LineBreak -> append("\n")
                is RichTextInline.InlineCode -> append(inline.code)
                is RichTextInline.Bold -> appendInlineText(inline.children)
                is RichTextInline.Italic -> appendInlineText(inline.children)
                is RichTextInline.BoldItalic -> appendInlineText(inline.children)
                is RichTextInline.Strikethrough -> appendInlineText(inline.children)
                is RichTextInline.Link -> appendInlineText(inline.children)
            }
        }
    }
}
