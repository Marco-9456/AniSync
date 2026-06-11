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
        val parsed =
            RichTextParser.parse("[__All AWC Challenges__](https://anilist.co/activity/26266744)")
        val textBlock = parsed.blocks.filterIsInstance<RichTextBlock.Text>().first()

        assertTrue(textBlock.debugInlineText().contains("All AWC Challenges"))
        assertTrue(textBlock.hasLink("https://anilist.co/activity/26266744"))
        assertTrue(textBlock.hasBold())
    }

    @Test
    fun `anilist media urls become preview blocks`() = runBlocking {
        val parsed = RichTextParser.parse("Visit https://anilist.co/anime/16498/attack-on-titan")

        val linkBlock =
            parsed.blocks.deepBlocks().filterIsInstance<RichTextBlock.AnilistLink>().first()
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
        assertTrue(quoteDescendants.any {
            it is RichTextBlock.Text && it.debugInlineText().contains("Quoted")
        })
        assertTrue(quoteDescendants.any { it is RichTextBlock.Image && it.url == "https://example.com/img.png" })
    }

    @Test
    fun `parses youtube and webm markdown as media blocks`() = runBlocking {
        val parsed =
            RichTextParser.parse("youtube(dQw4w9WgXcQ)\nwebm(https://example.com/clip.webm)")

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
    fun `full-width divider is not grouped into a thumbnail grid`() = runBlocking {
        // A wide divider (width=500) directly before a row of 20% "Day N" thumbnails must stay on
        // its own row; otherwise it gets packed into the grid's FlowRow and shoves it out of line.
        val html = "<center><img width='500' src='https://x/divider.png'>" +
            "[<img width='20%' src='https://x/d1.jpg'>](https://anilist.co/activity/1)" +
            "[<img width='20%' src='https://x/d2.jpg'>](https://anilist.co/activity/2)" +
            "[<img width='20%' src='https://x/d3.jpg'>](https://anilist.co/activity/3)</center>"
        val parsed = RichTextParser.parse(html)

        val grid = parsed.blocks.filterIsInstance<RichTextBlock.InlineGroup>()
            .firstOrNull { it.children.size == 3 && it.children.all { c -> c is RichTextBlock.Image } }
        assertTrue("expected a 3-thumbnail grid", grid != null)
        assertTrue(
            "the 500px divider must not be inside the grid",
            grid!!.children.none { (it as RichTextBlock.Image).width == 500 }
        )
        assertTrue(
            "the divider should be a standalone image block",
            parsed.blocks.any { it is RichTextBlock.Image && it.width == 500 }
        )
    }

    @Test
    fun `bold spanning an empty anchor renders bold without literal underscores`() = runBlocking {
        // AniList turns `[Fanart(s)]()` into an empty <a>, which split the run and left the
        // surrounding `__ __` as literal underscores.
        val parsed = RichTextParser.parse(
            "<center>__Direct Source is added in the <a>Fanart(s)</a>__</center>"
        )
        val text = parsed.blocks.deepBlocks().filterIsInstance<RichTextBlock.Text>()
            .first { it.debugInlineText().contains("Direct Source") }

        assertTrue(text.hasBold())
        assertTrue(text.debugInlineText().contains("Fanart(s)"))
        assertFalse(text.debugInlineText().contains("__"))
    }

    @Test
    fun `parses html table with headers`() = runBlocking {
        val parsed =
            RichTextParser.parse("<table><tr><th>Name</th></tr><tr><td>Value</td></tr></table>")
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
    fun `anilist user link becomes preview block keyed by username`() = runBlocking {
        // AniList user URLs use a username, not a numeric id.
        val parsed = RichTextParser.parse("https://anilist.co/user/Goldiizz")
        val link = parsed.blocks.deepBlocks().filterIsInstance<RichTextBlock.AnilistLink>().first()
        assertEquals("user", link.type)
        assertEquals("Goldiizz", link.slug)
        assertEquals("Goldiizz", link.displayTitle)
        assertEquals(LinkPreviewKey("user", 0, "Goldiizz"), link.previewKey)
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
    fun `http image urls are upgraded to https`() = runBlocking {
        val parsed = RichTextParser.parse("<img src='http://i.imgur.com/abc.jpg'>")
        val image = parsed.blocks.deepBlocks().filterIsInstance<RichTextBlock.Image>().first()
        assertEquals("https://i.imgur.com/abc.jpg", image.url)
        assertEquals(listOf("https://i.imgur.com/abc.jpg"), parsed.imageUrls)
    }

    @Test
    fun `linked http image keeps destination and upgrades scheme`() = runBlocking {
        val parsed = RichTextParser.parse(
            "<center>[<img src='http://i.imgur.com/hy3tXpl.jpg'>](https://www.patreon.com/ani_chart_list)</center>"
        )
        val image = parsed.blocks.deepBlocks().filterIsInstance<RichTextBlock.Image>().first()
        assertEquals("https://i.imgur.com/hy3tXpl.jpg", image.url)
        assertEquals("https://www.patreon.com/ani_chart_list", image.linkUrl)
        assertEquals(RichTextAlignment.Center, image.align)
    }

    @Test
    fun `https image urls are left untouched`() = runBlocking {
        val parsed = RichTextParser.parse("<img src='https://i.imgur.com/CY32yPS.png'>")
        val image = parsed.blocks.deepBlocks().filterIsInstance<RichTextBlock.Image>().first()
        assertEquals("https://i.imgur.com/CY32yPS.png", image.url)
    }

    @Test
    fun `css text-align center is honored`() = runBlocking {
        val parsed = RichTextParser.parse("<p style=\"text-align: center;\">styled</p>")
        val text = parsed.blocks.filterIsInstance<RichTextBlock.Text>().first()
        assertEquals(RichTextAlignment.Center, text.align)
    }

    @Test
    fun `css text-align right is honored`() = runBlocking {
        val parsed = RichTextParser.parse("<div style=\"text-align:right\">styled</div>")
        val text = parsed.blocks.filterIsInstance<RichTextBlock.Text>().first()
        assertEquals(RichTextAlignment.End, text.align)
    }

    @Test
    fun `align attribute wins over conflicting style`() = runBlocking {
        val parsed =
            RichTextParser.parse("<p align=\"right\" style=\"text-align:center\">x</p>")
        val text = parsed.blocks.filterIsInstance<RichTextBlock.Text>().first()
        assertEquals(RichTextAlignment.End, text.align)
    }

    @Test
    fun `markdown dash bullets become a list`() = runBlocking {
        val parsed = RichTextParser.parse("- one\n- two\n- three")
        val list = parsed.blocks.filterIsInstance<RichTextBlock.ListBlock>().first()
        assertEquals(3, list.items.size)
        val first = list.items.first().children.filterIsInstance<RichTextBlock.Text>().first()
        assertEquals("one", first.debugInlineText())
        // The literal "- " marker must not leak into the rendered text.
        assertFalse(first.debugInlineText().startsWith("-"))
    }

    @Test
    fun `markdown star and plus bullets become a list`() = runBlocking {
        val parsed = RichTextParser.parse("* a\n+ b")
        val list = parsed.blocks.filterIsInstance<RichTextBlock.ListBlock>().first()
        assertEquals(2, list.items.size)
    }

    @Test
    fun `emphasis at line start is not treated as a bullet`() = runBlocking {
        val parsed = RichTextParser.parse("*italic* text")
        assertTrue(parsed.blocks.none { it is RichTextBlock.ListBlock })
        val text = parsed.blocks.filterIsInstance<RichTextBlock.Text>().first()
        assertTrue(text.inlines.containsInline { it is RichTextInline.Italic })
    }

    @Test
    fun `adjacent markdown bullet and html list merge into one`() = runBlocking {
        val parsed = RichTextParser.parse("- zero\n<ul><li>one</li><li>two</li></ul>")
        val lists = parsed.blocks.filterIsInstance<RichTextBlock.ListBlock>()
        assertEquals(1, lists.size)
        assertEquals(3, lists.first().items.size)
    }

    @Test
    fun `stranded markdown bullet in spoiler merges with html list`() = runBlocking {
        // Mirrors AniList's malformed output (e.g. Marina Inoue's bio): the spoiler span
        // opens inside a <p> with the first list item as a raw "- " bullet, the rest arrive
        // as a real <ul>, and the span closes inside the final <li>.
        val html = "<p><strong>Roles:</strong><br />\n" +
            "<span class='markdown_spoiler'><span>- Pallas - Arknights (VG)</p>\n" +
            "<ul>\n<li>Elysia - Honkai Impact 3rd (VG)</li>\n" +
            "<li>Yanqing - Honkai: Star Rail (VG)<br />\n</span></span></li>\n</ul>"
        val parsed = RichTextParser.parse(html)
        val spoiler = parsed.blocks.deepBlocks().filterIsInstance<RichTextBlock.Spoiler>().first()
        val list = spoiler.children.filterIsInstance<RichTextBlock.ListBlock>().first()
        assertEquals(3, list.items.size)
        val firstItem = list.items.first().children.filterIsInstance<RichTextBlock.Text>().first()
        assertEquals("Pallas - Arknights (VG)", firstItem.debugInlineText())
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

    @Test
    fun `small icon image inside a link renders inline`() = runBlocking {
        val parsed =
            RichTextParser.parse("[ <img src='https://example.com/icon.png' width='16'> Instagram](https://instagram.com)")

        // A 16px icon is emoji-sized, so it flows inline within the link (icon + label as one
        // clickable run) rather than breaking out to a standalone image block.
        val texts = parsed.blocks.deepBlocks().filterIsInstance<RichTextBlock.Text>()
        assertTrue(
            texts.any { text ->
                text.inlines.containsInline {
                    it is RichTextInline.Image && it.url == "https://example.com/icon.png"
                }
            }
        )

        val labelled = texts.first { it.debugInlineText().contains("Instagram") }
        assertTrue(labelled.hasLink("https://instagram.com"))
    }

    @Test
    fun `contiguous inline blocks are grouped together`() = runBlocking {
        val parsed =
            RichTextParser.parse("Some text <img src='https://example.com/img.png' width='32'> **bold**")

        val group = parsed.blocks.filterIsInstance<RichTextBlock.InlineGroup>().first()
        assertTrue(group.children.any {
            it is RichTextBlock.Text && it.debugInlineText().contains("Some text")
        })
        assertTrue(group.children.any { it is RichTextBlock.Image })
        assertTrue(group.children.any { it is RichTextBlock.Text && it.hasBold() })
    }

    @Test
    fun `block level elements break inline grouping`() = runBlocking {
        val parsed = RichTextParser.parse("<p>Line 1</p><div>Line 2</div><p>Line 3</p>")
        val textBlocks = parsed.blocks.filterIsInstance<RichTextBlock.Text>()

        assertEquals(3, textBlocks.size)
        // Ensures no InlineGroup was artificially created to bundle these blocks
        assertTrue(parsed.blocks.none { it is RichTextBlock.InlineGroup })
    }

    @Test
    fun `unclosed code fence whose language swallowed an img is recovered as rich content`() =
        runBlocking {
            // AniList activity 1085212654: a ```<img …> fence with no space wraps the entire post
            // in <pre><code class="language-<img …>">. It must render as rich content, not as a
            // literal code dump.
            val html = """
                <pre><code class="language-&lt;img width=&#039;350&#039; src=&#039;https://i.postimg.cc/bY2HsjQc/IMG-3950.png&#039;&gt;">_____
                &lt;img width='100' src='https://fontmeme.com/permalink/260531/a91260bc.png'&gt;

                &lt;img width='250' src='https://i.postimg.cc/0245B09D/IMG-4344.jpg'&gt;

                __"Our Aim Is To Satisfy by Red Snapper"__
                _____
                [ Source Site: 1001 Album Generator ](https://1001albumsgenerator.com/)
                __[&lt;— Day 20](https://anilist.co/activity/1084893698)__</code></pre>
            """.trimIndent()

            val parsed = RichTextParser.parse(html)
            val blocks = parsed.blocks.deepBlocks()

            // The whole post must NOT collapse into a literal code block anymore.
            assertTrue(blocks.none { it is RichTextBlock.CodeBlock })

            // The swallowed banner and the in-body album cover both render as images.
            assertTrue(parsed.imageUrls.contains("https://i.postimg.cc/bY2HsjQc/IMG-3950.png"))
            assertTrue(parsed.imageUrls.contains("https://i.postimg.cc/0245B09D/IMG-4344.jpg"))

            // Body text and links survive as rich content (markdown links stay inline links).
            val textBlocks = blocks.filterIsInstance<RichTextBlock.Text>()
            val allText = textBlocks.joinToString("\n") { it.debugInlineText() }
            assertTrue(allText.contains("Our Aim Is To Satisfy by Red Snapper"))
            assertTrue(textBlocks.any { it.hasLink("https://1001albumsgenerator.com/") })
            assertTrue(textBlocks.any { it.hasLink("https://anilist.co/activity/1084893698") })
        }

    @Test
    fun `genuine code block with a real language is still rendered as code`() = runBlocking {
        val parsed = RichTextParser.parse(
            "<pre><code class=\"language-kotlin\">val x = 1</code></pre>"
        )
        val code = parsed.blocks.filterIsInstance<RichTextBlock.CodeBlock>().firstOrNull()
        assertTrue(code != null && code.code.contains("val x = 1"))
    }

    @Test
    fun `whole post wrapped in pre-code with a single image is recovered`() = runBlocking {
        // AniList activity 1088995401: asHtml dumped the entire post into <pre><code>. It carries
        // only ONE escaped <img> tag, so the >=3-tag heuristic skipped recovery and it rendered as
        // a literal code block (raw "#[Day 1365…]", "<img …>", "[Source](…)" on screen).
        val html = """
            <pre><code>#[Day 1365 of posting ARGONAVIS fanart to convince people to watch it~](https://anilist.co/review/21995)

            &lt;img width='500' src='https://i.ibb.co/m1mBK9H/Day-1365.jpg'&gt;

            [Source](https://x.com/kyoji_46/status/2054215104477229081?s=46) (kyoji)</code></pre>
        """.trimIndent()

        val parsed = RichTextParser.parse(html)
        val blocks = parsed.blocks.deepBlocks()

        assertTrue(blocks.none { it is RichTextBlock.CodeBlock })
        assertTrue(parsed.imageUrls.contains("https://i.ibb.co/m1mBK9H/Day-1365.jpg"))
        val textBlocks = blocks.filterIsInstance<RichTextBlock.Text>()
        assertTrue(textBlocks.any { it.kind == RichTextTextKind.Heading1 })
        assertTrue(textBlocks.any { it.hasLink("https://x.com/kyoji_46/status/2054215104477229081?s=46") })
    }

    @Test
    fun `wrapped post whose first image is past the heuristic window is recovered`() = runBlocking {
        // AniList activity 1091109491 (Pokemon challenge): the lone <img> sits well past the first
        // 500 chars of prose, so the tag-count window saw zero tags. A markdown heading-link and a
        // markdown_spoiler block must still trigger recovery.
        val html = """
            <pre><code>##__[Pokemon 30 days Challenge](https://anilist.co/forum/thread/88876)__
            by @ evi hameru ampri

            Pokemon series is one of my favourite things in the world, so of course I'm doing this challenge! I definitely won't be posting it daily, but I want to go through it. Salandit. I really don't like Salazzle, so I'd love to have a male one, or an unevolved female. Give him better Sp. Def, and maybe better stats overall so he is worth using on a team.

            &lt;img width='400' src='https://64.media.tumblr.com/eae881da.pnj'&gt;
            [artist](https://www.tumblr.com/wildragon/770041462033350656/salandit)

            &lt;span class='markdown_spoiler'&gt;&lt;span&gt;
            &lt;img width='300' src='https://i.postimg.cc/XJcqZ0pX/pokechallenge.png'&gt;
            &lt;/span&gt;&lt;/span&gt;</code></pre>
        """.trimIndent()

        val parsed = RichTextParser.parse(html)
        val blocks = parsed.blocks.deepBlocks()

        assertTrue(blocks.none { it is RichTextBlock.CodeBlock })
        assertTrue(blocks.any { it is RichTextBlock.Spoiler })
        assertTrue(parsed.imageUrls.contains("https://64.media.tumblr.com/eae881da.pnj"))
        val textBlocks = blocks.filterIsInstance<RichTextBlock.Text>()
        assertTrue(textBlocks.any { it.kind == RichTextTextKind.Heading2 })
        assertTrue(textBlocks.any { it.hasLink("https://anilist.co/forum/thread/88876") })
    }

    @Test
    fun `review section wrongly wrapped in pre-code with a spoiler and prose is recovered`() =
        runBlocking {
            // AniList review 13751 (Sonny Boy): asHtml emitted a <pre><code> mid-body holding an
            // escaped markdown_spoiler span followed by long prose — only two escaped tags up front,
            // so the old heuristic dumped "<span class='markdown_spoiler'><span>" + episodes as code.
            val html = """
                <p>intro</p>
                <pre><code>
                &lt;span class='markdown_spoiler'&gt;&lt;span&gt;

                Episode 1 is all about the ways in which people respond to crisis. We're abruptly dropped into a weird situation where a school is suspended in limbo. Different characters all react differently to what happened, whilst we take stock of the situation through character introductions over a long stretch of screen time.

                &lt;/span&gt;&lt;/span&gt;

                #**Tl;Dr**:

                &lt;img width='100%' src='https://example.com/tldr.gif'&gt;</code></pre>
                <p>outro</p>
            """.trimIndent()

            val parsed = RichTextParser.parse(html)
            val blocks = parsed.blocks.deepBlocks()

            assertTrue(blocks.none { it is RichTextBlock.CodeBlock })
            assertTrue(blocks.any { it is RichTextBlock.Spoiler })
            assertTrue(parsed.imageUrls.contains("https://example.com/tldr.gif"))
            val allText = blocks.filterIsInstance<RichTextBlock.Text>()
                .joinToString("\n") { it.debugInlineText() }
            assertTrue(allText.contains("Episode 1 is all about"))
            assertFalse(allText.contains("markdown_spoiler"))
        }

    @Test
    fun `genuine code block with hash comments is not mistaken for a wrapped post`() = runBlocking {
        // Pure source with markdown-looking "#" comment lines but no images, links, or AniList
        // markup must stay a literal code block — recovery must not over-trigger.
        val parsed = RichTextParser.parse(
            "<pre><code># install deps\npip install requests\nx = [1, 2, 3]  # a list</code></pre>"
        )
        val code = parsed.blocks.filterIsInstance<RichTextBlock.CodeBlock>().firstOrNull()
        assertTrue(code != null && code.code.contains("pip install requests"))
    }

    private fun RichTextBlock.Text.debugInlineText(): String = inlines.toDebugPlainText()

    private fun RichTextBlock.Text.hasBold(): Boolean =
        inlines.containsInline { it is RichTextInline.Bold || it is RichTextInline.BoldItalic }

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
                is RichTextBlock.Table -> block.rows.forEach { row ->
                    row.cells.forEach { cell ->
                        cell.children.forEach(
                            ::walk
                        )
                    }
                }

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
                is RichTextInline.Image -> Unit
            }
        }
    }
}