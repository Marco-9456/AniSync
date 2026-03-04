package com.anisync.android.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [parseHtmlToBlocks] covering rendering issues found in
 * AniList forum thread 67067. Tests validate block types, text content,
 * SpanStyles (bold, italic, bold+italic, links, strikethrough), and alignment.
 */
class AniListHtmlRendererTest {

    // Stable colors for test rendering context
    private val linkColor = Color.Blue
    private val codeBackground = Color.LightGray
    private val spoilerColor = Color.Gray

    private fun parse(html: String): List<RenderBlock> =
        parseHtmlToBlocks(html, linkColor, codeBackground, spoilerColor)

    // =========================================================================
    // Issue 1: Raw markdown in <li> not parsed
    // =========================================================================

    @Test
    fun `li with raw bold markdown renders bold span`() {
        val blocks = parse("<ul><li>__bold text__</li></ul>")
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        // Should contain the bullet and "bold text"
        assertTrue("Text should contain 'bold text'", text.text.contains("bold text"))

        // Should have a Bold SpanStyle
        val boldSpans = text.spanStyles.filter {
            it.item.fontWeight == FontWeight.Bold
        }
        assertTrue("Should have bold span for __bold text__", boldSpans.isNotEmpty())
    }

    @Test
    fun `li with raw italic markdown renders italic span`() {
        val blocks = parse("<ul><li>You _can_ rewatch episodes</li></ul>")
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Text should contain 'can'", text.text.contains("can"))

        val italicSpans = text.spanStyles.filter {
            it.item.fontStyle == FontStyle.Italic
        }
        assertTrue("Should have italic span for _can_", italicSpans.isNotEmpty())
    }

    @Test
    fun `li with raw link markdown renders link annotation`() {
        val blocks = parse("<ul><li>Check [AniList](https://anilist.co) for more</li></ul>")
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Text should contain 'AniList'", text.text.contains("AniList"))

        // LinkAnnotation should be present (via getUrlAnnotations or checking link annotations)
        val links = text.getLinkAnnotations(0, text.length)
        assertTrue("Should have link annotation for [AniList](...)", links.isNotEmpty())
    }

    @Test
    fun `li with bold wrapping a link renders both bold and link`() {
        val html = "<ul><li>__Enter for a chance: [Chiaki](https://example.com)__</li></ul>"
        val blocks = parse(html)
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Text should contain 'Enter for a chance:'", text.text.contains("Enter for a chance:"))
        assertTrue("Text should contain 'Chiaki'", text.text.contains("Chiaki"))

        val boldSpans = text.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertTrue("Should have bold span", boldSpans.isNotEmpty())

        val links = text.getLinkAnnotations(0, text.length)
        assertTrue("Should have link annotation for Chiaki", links.isNotEmpty())
    }

    // =========================================================================
    // Issue 2: <center> inside <h1> not handled
    // =========================================================================

    @Test
    fun `h1 containing center with raw bold markdown renders bold`() {
        val html = "<h1><center>__AWC Gacha Challenge__</center></h1>"
        val blocks = parse(html)
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Text should contain 'AWC Gacha Challenge'", text.text.contains("AWC Gacha Challenge"))
        // Should NOT contain raw underscores
        assertTrue(
            "Text should not contain raw underscores",
            !text.text.contains("__AWC")
        )

        val boldSpans = text.spanStyles.filter { it.item.fontWeight != null }
        assertTrue("Should have bold spans (from h1 and/or markdown)", boldSpans.isNotEmpty())
    }

    @Test
    fun `h1 containing center with stars bold markdown renders bold`() {
        val html = "<h1><center>**Video Games**</center></h1>"
        val blocks = parse(html)
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Text should contain 'Video Games'", text.text.contains("Video Games"))
        assertTrue(
            "Text should not contain raw asterisks",
            !text.text.contains("**Video")
        )
    }

    // =========================================================================
    // Issue 3: Raw markdown directly in <ul> text lost entirely
    // =========================================================================

    @Test
    fun `raw text directly in ul is not lost`() {
        val html = "<ul>**There are 3 types of banners:**<li>Type A</li></ul>"
        val blocks = parse(html)
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue(
            "Text should contain 'There are 3 types of banners:'",
            text.text.contains("There are 3 types of banners:")
        )

        val boldSpans = text.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertTrue("Should have bold span for **...**", boldSpans.isNotEmpty())

        // Also check that the <li> is still rendered
        assertTrue("Text should also contain 'Type A'", text.text.contains("Type A"))
    }

    // =========================================================================
    // Issue 4: Italic inside center (~~~_Test_~~~)
    // =========================================================================

    @Test
    fun `center with italic markdown renders italic`() {
        val html = "<center>_Test_</center>"
        val blocks = parse(html)
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Text should contain 'Test'", text.text.contains("Test"))
        assertTrue(
            "Text should not contain raw underscores around Test",
            !text.text.contains("_Test_")
        )

        val italicSpans = text.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        assertTrue("Should have italic span for _Test_", italicSpans.isNotEmpty())

        // Should be center-aligned
        assertEquals("Should be center-aligned", TextAlign.Center, textBlock.textAlign)
    }

    @Test
    fun `center with bold and link markdown renders both`() {
        val html = "<center>**Rewards:** A random badge and 2 [Challenge Points](https://example.com) per play</center>"
        val blocks = parse(html)
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Text should contain 'Rewards:'", text.text.contains("Rewards:"))
        assertTrue("Text should contain 'Challenge Points'", text.text.contains("Challenge Points"))
        assertTrue(
            "Should not contain raw ** markers",
            !text.text.contains("**Rewards")
        )

        val boldSpans = text.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertTrue("Should have bold span for **Rewards:**", boldSpans.isNotEmpty())

        val links = text.getLinkAnnotations(0, text.length)
        assertTrue("Should have link annotation for Challenge Points", links.isNotEmpty())
    }

    // =========================================================================
    // Issue 5: Triple underscore ___text___ not handled
    // =========================================================================

    @Test
    fun `triple underscore renders bold and italic`() {
        val html = "<ul><li>___Your profile must be set to public___</li></ul>"
        val blocks = parse(html)
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue(
            "Text should contain 'Your profile must be set to public'",
            text.text.contains("Your profile must be set to public")
        )
        assertTrue(
            "Should not contain raw ___ markers",
            !text.text.contains("___Your")
        )

        // Should have bold+italic span
        val boldItalicSpans = text.spanStyles.filter {
            it.item.fontWeight == FontWeight.Bold && it.item.fontStyle == FontStyle.Italic
        }
        assertTrue("Should have bold+italic span for ___...___", boldItalicSpans.isNotEmpty())
    }

    @Test
    fun `triple asterisk renders bold and italic`() {
        val html = "<center>***Important Notice***</center>"
        val blocks = parse(html)
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue(
            "Text should contain 'Important Notice'",
            text.text.contains("Important Notice")
        )

        val boldItalicSpans = text.spanStyles.filter {
            it.item.fontWeight == FontWeight.Bold && it.item.fontStyle == FontStyle.Italic
        }
        assertTrue("Should have bold+italic span for ***...***", boldItalicSpans.isNotEmpty())
    }

    // =========================================================================
    // Issue 6: <HR> variant
    // =========================================================================

    @Test
    fun `hr with width attribute renders as HorizontalRule block`() {
        val html = "<p>Before</p><HR width=50%><p>After</p>"
        val blocks = parse(html)
        val hrBlocks = blocks.filterIsInstance<RenderBlock.HorizontalRule>()
        assertTrue("Should have at least one HorizontalRule block", hrBlocks.isNotEmpty())
    }

    // =========================================================================
    // Basic rendering sanity checks
    // =========================================================================

    @Test
    fun `basic bold HTML renders correctly`() {
        val blocks = parse("<p><b>Hello</b> world</p>")
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertEquals("Hello world", text.text.trim())

        val boldSpans = text.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertTrue("Should have bold span for <b>Hello</b>", boldSpans.isNotEmpty())
    }

    @Test
    fun `basic italic HTML renders correctly`() {
        val blocks = parse("<p><i>Hello</i> world</p>")
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertEquals("Hello world", text.text.trim())

        val italicSpans = text.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        assertTrue("Should have italic span for <i>Hello</i>", italicSpans.isNotEmpty())
    }

    @Test
    fun `strikethrough markdown in li renders correctly`() {
        val blocks = parse("<ul><li>~~removed~~</li></ul>")
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Text should contain 'removed'", text.text.contains("removed"))

        val strikeSpans = text.spanStyles.filter {
            it.item.textDecoration == TextDecoration.LineThrough
        }
        assertTrue("Should have strikethrough span for ~~removed~~", strikeSpans.isNotEmpty())
    }

    @Test
    fun `image block is parsed correctly`() {
        val blocks = parse("<img src='https://example.com/image.png'>")
        val imageBlocks = blocks.filterIsInstance<RenderBlock.Image>()
        assertTrue("Should have an Image block", imageBlocks.isNotEmpty())
        assertEquals("https://example.com/image.png", imageBlocks.first().url)
    }

    @Test
    fun `code block renders correctly`() {
        val blocks = parse("<pre><code>val x = 1</code></pre>")
        val codeBlocks = blocks.filterIsInstance<RenderBlock.Code>()
        assertTrue("Should have a Code block", codeBlocks.isNotEmpty())
        assertTrue(
            "Code block should contain 'val x = 1'",
            codeBlocks.first().code.contains("val x = 1")
        )
    }

    @Test
    fun `empty html returns empty list`() {
        val blocks = parse("")
        assertTrue("Empty HTML should return empty list", blocks.isEmpty())
    }

    @Test
    fun `ordered list with start attribute`() {
        val html = "<ol start=\"3\"><li>Third</li><li>Fourth</li></ol>"
        val blocks = parse(html)
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Should contain '3.'", text.text.contains("3."))
        assertTrue("Should contain '4.'", text.text.contains("4."))
        assertTrue("Should contain 'Third'", text.text.contains("Third"))
        assertTrue("Should contain 'Fourth'", text.text.contains("Fourth"))
    }

    // =========================================================================
    // Combined / complex patterns from thread 67067
    // =========================================================================

    @Test
    fun `blockquote with nested h1 center and raw markdown`() {
        val html = """
            <blockquote>
                <h1><center>__Section Title__</center></h1>
                <p>Some description</p>
            </blockquote>
        """.trimIndent()
        val blocks = parse(html)
        val bqBlocks = blocks.filterIsInstance<RenderBlock.Blockquote>()
        assertTrue("Should produce a Blockquote block", bqBlocks.isNotEmpty())

        // Extract text from blockquote children
        val childTexts = bqBlocks.first().children.filterIsInstance<RenderBlock.Text>()
        val allText = childTexts.joinToString(" ") { it.annotatedString.text }
        assertTrue("Should contain 'Section Title'", allText.contains("Section Title"))
        assertTrue("Should not contain raw __", !allText.contains("__Section"))
    }

    @Test
    fun `center with image creates image block`() {
        val html = "<center><img width='150' src='https://example.com/img.png'></center>"
        val blocks = parse(html)
        val imageBlocks = blocks.filterIsInstance<RenderBlock.Image>()
        assertTrue("Should have an Image block", imageBlocks.isNotEmpty())
        assertEquals("https://example.com/img.png", imageBlocks.first().url)
        assertEquals(150, imageBlocks.first().width)
    }

    @Test
    fun `multiple li items with mixed markdown`() {
        val html = """
            <ul>
                <li>__Bold item__</li>
                <li>_Italic item_</li>
                <li>Normal item</li>
                <li>___Bold and italic___</li>
                <li>[Link item](https://example.com)</li>
            </ul>
        """.trimIndent()
        val blocks = parse(html)
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Should contain 'Bold item'", text.text.contains("Bold item"))
        assertTrue("Should contain 'Italic item'", text.text.contains("Italic item"))
        assertTrue("Should contain 'Normal item'", text.text.contains("Normal item"))
        assertTrue("Should contain 'Bold and italic'", text.text.contains("Bold and italic"))
        assertTrue("Should contain 'Link item'", text.text.contains("Link item"))

        // Verify no raw markdown markers
        assertTrue("No raw __ markers", !text.text.contains("__Bold"))
        assertTrue("No raw _ markers around Italic", !text.text.contains("_Italic"))
        assertTrue("No raw ___ markers", !text.text.contains("___Bold"))
        assertTrue("No raw [] markers", !text.text.contains("[Link"))
    }

    // =========================================================================
    // Bug fix: Header bold/italic not rendering (### __Header Bold__)
    // AniList converts ### to <h3> but leaves __ as raw text.
    // =========================================================================

    @Test
    fun `h3 with raw bold markdown renders bold`() {
        val blocks = parse("<h3>__Header Bold__</h3>")
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Text should contain 'Header Bold'", text.text.contains("Header Bold"))
        assertTrue(
            "Text should not contain raw underscores",
            !text.text.contains("__Header")
        )

        val boldSpans = text.spanStyles.filter { it.item.fontWeight != null }
        assertTrue("Should have bold span for __Header Bold__", boldSpans.isNotEmpty())
    }

    @Test
    fun `h3 with raw italic markdown renders italic`() {
        val blocks = parse("<h3>_Header Italic_</h3>")
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Text should contain 'Header Italic'", text.text.contains("Header Italic"))
        assertTrue(
            "Text should not contain raw underscores",
            !text.text.contains("_Header")
        )

        val italicSpans = text.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        assertTrue("Should have italic span for _Header Italic_", italicSpans.isNotEmpty())
    }

    @Test
    fun `h2 with raw bold asterisks renders bold`() {
        val blocks = parse("<h2>**Bold Header**</h2>")
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Text should contain 'Bold Header'", text.text.contains("Bold Header"))
        assertTrue(
            "Text should not contain raw asterisks",
            !text.text.contains("**Bold")
        )
    }

    // =========================================================================
    // Bug fix: Center italic not rendering when <p> wraps content
    // Jsoup or AniList may wrap center content in <p>, breaking markdown parsing.
    // =========================================================================

    @Test
    fun `center with p-wrapped italic markdown renders italic`() {
        val blocks = parse("<center><p>_Test_</p></center>")
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Text should contain 'Test'", text.text.contains("Test"))
        assertTrue(
            "Text should not contain raw underscores around Test",
            !text.text.contains("_Test_")
        )

        val italicSpans = text.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        assertTrue("Should have italic span for _Test_", italicSpans.isNotEmpty())

        // Should be center-aligned
        assertEquals("Should be center-aligned", TextAlign.Center, textBlock.textAlign)
    }

    @Test
    fun `center with p-wrapped bold markdown renders bold`() {
        val blocks = parse("<center><p>**Bold Text**</p></center>")
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Text should contain 'Bold Text'", text.text.contains("Bold Text"))
        assertTrue(
            "Text should not contain raw asterisks",
            !text.text.contains("**Bold")
        )

        val boldSpans = text.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertTrue("Should have bold span for **Bold Text**", boldSpans.isNotEmpty())

        assertEquals("Should be center-aligned", TextAlign.Center, textBlock.textAlign)
    }

    @Test
    fun `center with p-wrapped bold+italic markdown renders both`() {
        val blocks = parse("<center><p>***Bold Italic***</p></center>")
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Text should contain 'Bold Italic'", text.text.contains("Bold Italic"))

        val boldItalicSpans = text.spanStyles.filter {
            it.item.fontWeight == FontWeight.Bold && it.item.fontStyle == FontStyle.Italic
        }
        assertTrue("Should have bold+italic span", boldItalicSpans.isNotEmpty())
    }

    // =========================================================================
    // Blockquote rendering: proper RenderBlock.Blockquote output
    // AniList blockquotes use <blockquote> tags; nested quotes nest the tags.
    // =========================================================================

    @Test
    fun `blockquote produces Blockquote block with text content`() {
        val blocks = parse("<blockquote><p>Hello world</p></blockquote>")
        val bqBlocks = blocks.filterIsInstance<RenderBlock.Blockquote>()
        assertTrue("Should produce a Blockquote block", bqBlocks.isNotEmpty())

        val children = bqBlocks.first().children
        val textBlocks = children.filterIsInstance<RenderBlock.Text>()
        assertTrue("Blockquote should have text children", textBlocks.isNotEmpty())

        val text = textBlocks.first().annotatedString.text
        assertTrue("Text should contain 'Hello world'", text.contains("Hello world"))
    }

    @Test
    fun `nested blockquote produces nested Blockquote blocks`() {
        val html = """
            <blockquote>
                <p>outer</p>
                <blockquote>
                    <p>inner</p>
                </blockquote>
            </blockquote>
        """.trimIndent()
        val blocks = parse(html)
        val outerBq = blocks.filterIsInstance<RenderBlock.Blockquote>()
        assertTrue("Should have outer Blockquote", outerBq.isNotEmpty())

        // Outer blockquote should contain text "outer" and a nested Blockquote
        val outerChildren = outerBq.first().children
        val outerText = outerChildren.filterIsInstance<RenderBlock.Text>()
            .joinToString(" ") { it.annotatedString.text }
        assertTrue("Outer should contain 'outer'", outerText.contains("outer"))

        val innerBq = outerChildren.filterIsInstance<RenderBlock.Blockquote>()
        assertTrue("Should have inner nested Blockquote", innerBq.isNotEmpty())

        val innerText = innerBq.first().children.filterIsInstance<RenderBlock.Text>()
            .joinToString(" ") { it.annotatedString.text }
        assertTrue("Inner should contain 'inner'", innerText.contains("inner"))
    }

    @Test
    fun `blockquote with bold and italic HTML renders formatting`() {
        val html = "<blockquote><p><b>Bold</b> and <i>italic</i> text</p></blockquote>"
        val blocks = parse(html)
        val bqBlocks = blocks.filterIsInstance<RenderBlock.Blockquote>()
        assertTrue("Should produce a Blockquote block", bqBlocks.isNotEmpty())

        val textBlock = bqBlocks.first().children.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Should contain 'Bold'", text.text.contains("Bold"))
        assertTrue("Should contain 'italic'", text.text.contains("italic"))

        val boldSpans = text.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertTrue("Should have bold span", boldSpans.isNotEmpty())

        val italicSpans = text.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        assertTrue("Should have italic span", italicSpans.isNotEmpty())
    }

    @Test
    fun `blockquote with image produces Image block inside children`() {
        val html = "<blockquote><p>Text</p><img src='https://example.com/img.png'></blockquote>"
        val blocks = parse(html)
        val bqBlocks = blocks.filterIsInstance<RenderBlock.Blockquote>()
        assertTrue("Should produce a Blockquote block", bqBlocks.isNotEmpty())

        val children = bqBlocks.first().children
        val imageBlocks = children.filterIsInstance<RenderBlock.Image>()
        assertTrue("Blockquote should contain an Image block", imageBlocks.isNotEmpty())
        assertEquals("https://example.com/img.png", imageBlocks.first().url)
    }

    @Test
    fun `blockquote with link preserves href`() {
        val html = "<blockquote><p>Visit <a href='https://example.com'>here</a></p></blockquote>"
        val blocks = parse(html)
        val bqBlocks = blocks.filterIsInstance<RenderBlock.Blockquote>()
        assertTrue("Should produce a Blockquote block", bqBlocks.isNotEmpty())

        val textBlock = bqBlocks.first().children.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Should contain 'here'", text.text.contains("here"))
        assertTrue("Should contain 'Visit'", text.text.contains("Visit"))
    }

    @Test
    fun `empty blockquote produces no blocks`() {
        val blocks = parse("<blockquote></blockquote>")
        val bqBlocks = blocks.filterIsInstance<RenderBlock.Blockquote>()
        assertTrue("Empty blockquote should produce no Blockquote block", bqBlocks.isEmpty())
    }

    @Test
    fun `blockquote does not mix into surrounding text blocks`() {
        val html = "<p>Before</p><blockquote><p>Quoted</p></blockquote><p>After</p>"
        val blocks = parse(html)

        // Should have: Text("Before"), Blockquote, Text("After")
        assertTrue("Should have at least 3 blocks", blocks.size >= 3)

        val textBlocks = blocks.filterIsInstance<RenderBlock.Text>()
        val bqBlocks = blocks.filterIsInstance<RenderBlock.Blockquote>()

        assertTrue("Should have text blocks", textBlocks.isNotEmpty())
        assertTrue("Should have a Blockquote block", bqBlocks.isNotEmpty())

        // Surrounding text should NOT contain "Quoted"
        val surroundingText = textBlocks.joinToString(" ") { it.annotatedString.text }
        assertTrue("Surrounding text should contain 'Before'", surroundingText.contains("Before"))
        assertTrue("Surrounding text should contain 'After'", surroundingText.contains("After"))
        assertTrue(
            "Surrounding text should NOT contain 'Quoted'",
            !surroundingText.contains("Quoted")
        )

        // Blockquote text should contain "Quoted"
        val quotedText = bqBlocks.first().children.filterIsInstance<RenderBlock.Text>()
            .joinToString(" ") { it.annotatedString.text }
        assertTrue("Blockquote should contain 'Quoted'", quotedText.contains("Quoted"))
    }

    // =========================================================================
    // Thread 67067: Body-level raw markdown (not inside any HTML element)
    // AniList API sometimes leaves raw markdown at body level that isn't
    // wrapped in <p>, <center>, or any other HTML tag.
    // =========================================================================

    @Test
    fun `body-level raw markdown link is parsed as clickable link`() {
        val html = "[__All AWC Challenges__](https://anilist.co/activity/26266744)"
        val blocks = parse(html)
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Should contain 'All AWC Challenges'", text.text.contains("All AWC Challenges"))
        assertTrue("Should NOT contain raw brackets", !text.text.contains("[__All"))

        val boldSpans = text.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertTrue("Should have bold span for __...__", boldSpans.isNotEmpty())

        val links = text.getLinkAnnotations(0, text.length)
        assertTrue("Should have link annotation", links.isNotEmpty())
    }

    @Test
    fun `body-level raw bold markdown is parsed`() {
        val html = "**Credits**"
        val blocks = parse(html)
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Should contain 'Credits'", text.text.contains("Credits"))
        assertTrue("Should NOT contain raw asterisks", !text.text.contains("**Credits"))

        val boldSpans = text.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertTrue("Should have bold span", boldSpans.isNotEmpty())
    }

    @Test
    fun `multiple body-level markdown links on separate lines`() {
        val html = "[__Link One__](https://example.com/1)\n[__Link Two__](https://example.com/2)"
        val blocks = parse(html)
        val textBlock = blocks.filterIsInstance<RenderBlock.Text>().first()
        val text = textBlock.annotatedString

        assertTrue("Should contain 'Link One'", text.text.contains("Link One"))
        assertTrue("Should contain 'Link Two'", text.text.contains("Link Two"))

        val links = text.getLinkAnnotations(0, text.length)
        assertTrue("Should have at least 2 links", links.size >= 2)
    }

    // =========================================================================
    // Thread 67067: Raw markdown --- as horizontal rule
    // =========================================================================

    @Test
    fun `body-level triple dash creates HorizontalRule block`() {
        val html = "<p>Before</p>\n---\n<p>After</p>"
        val blocks = parse(html)
        val hrBlocks = blocks.filterIsInstance<RenderBlock.HorizontalRule>()
        assertTrue("Should have HR block for ---", hrBlocks.isNotEmpty())
    }

    @Test
    fun `standalone triple dash between elements creates HR`() {
        val html = "<ul><li>item</li></ul>\n\n---\n\n<center>text</center>"
        val blocks = parse(html)
        val hrBlocks = blocks.filterIsInstance<RenderBlock.HorizontalRule>()
        assertTrue("Should have HR block for ---", hrBlocks.isNotEmpty())
    }

    // =========================================================================
    // Thread 67067: Non-<li> elements in <ul> should not be lost
    // AniList generates <ul>**<a>NEW!</a>**<li>...</li></ul>
    // =========================================================================

    @Test
    fun `anchor element inside ul before li is not lost`() {
        val html = "<ul><a>NEW!</a><li>Quarter Banners</li></ul>"
        val blocks = parse(html)
        val textBlocks = blocks.filterIsInstance<RenderBlock.Text>()
        val allText = textBlocks.joinToString(" ") { it.annotatedString.text }

        assertTrue("Should contain 'NEW!'", allText.contains("NEW!"))
        assertTrue("Should contain 'Quarter Banners'", allText.contains("Quarter Banners"))
    }

    @Test
    fun `strong element inside ul before li is not lost`() {
        val html = "<ul><strong>Header text</strong><li>List item</li></ul>"
        val blocks = parse(html)
        val textBlocks = blocks.filterIsInstance<RenderBlock.Text>()
        val allText = textBlocks.joinToString(" ") { it.annotatedString.text }

        assertTrue("Should contain 'Header text'", allText.contains("Header text"))
        assertTrue("Should contain 'List item'", allText.contains("List item"))
    }

    // =========================================================================
    // Thread 67067: Bare # markdown prefix suppressed
    // AniList generates: #<ul>**text**</ul> where # is a header prefix
    // =========================================================================

    @Test
    fun `bare hash prefix before ul is suppressed`() {
        val html = "#<ul>**Badge Levels**</ul>"
        val blocks = parse(html)
        val textBlocks = blocks.filterIsInstance<RenderBlock.Text>()
        val allText = textBlocks.joinToString(" ") { it.annotatedString.text }

        assertTrue("Should NOT show literal # prefix", !allText.trimStart().startsWith("#"))
        assertTrue("Should contain 'Badge Levels'", allText.contains("Badge Levels"))
    }

    @Test
    fun `bare angle bracket prefix before ul is suppressed`() {
        val html = "><ul>**N - Normal** - Base rarity</ul>"
        val blocks = parse(html)
        val textBlocks = blocks.filterIsInstance<RenderBlock.Text>()
        val allText = textBlocks.joinToString(" ") { it.annotatedString.text }

        assertTrue("Should NOT show literal > prefix", !allText.trimStart().startsWith(">"))
        assertTrue("Should contain 'Normal'", allText.contains("Normal"))
    }

    // =========================================================================
    // Thread 67067: <h1> wrapping <ul> (AniList quirk)
    // =========================================================================

    @Test
    fun `h1 wrapping ul with strong renders header text`() {
        val html = "<h1><ul><strong>How to Play!</strong></ul></h1>"
        val blocks = parse(html)
        val textBlocks = blocks.filterIsInstance<RenderBlock.Text>()
        val allText = textBlocks.joinToString(" ") { it.annotatedString.text }

        assertTrue("Should contain 'How to Play!'", allText.contains("How to Play!"))
    }

    // =========================================================================
    // Thread 67067: Complex <h1> with <br>, <center>, raw markdown, newlines
    // =========================================================================

    @Test
    fun `h1 with br and center containing raw bold markdown`() {
        val html = "<h1><br><center>\u0001F3AE **Video Games**\n=== Banner ===</center></h1>"
        val blocks = parse(html)
        val textBlocks = blocks.filterIsInstance<RenderBlock.Text>()
        val allText = textBlocks.joinToString(" ") { it.annotatedString.text }

        assertTrue("Should contain 'Video Games'", allText.contains("Video Games"))
        assertTrue("Should not contain raw ** markers", !allText.contains("**Video"))
    }

    // =========================================================================
    // Thread 67067: Smoke test — full thread body parses without crash
    // =========================================================================

    @Test
    fun `smoke test full thread 67067 does not crash and produces content`() {
        // Representative subset of the actual thread body HTML
        val html = """
            <center><a href="https://anilist.co/user/AWC/">
            <img width='150' src='https://i.postimg.cc/Z4Gc6m5j/awc.png'>
            </a></center>
            <h1><center>__AWC Gacha Challenge__</center></h1>
            <center><img width='80%' src='https://i.postimg.cc/HpNVQ2cV/G1.png'></center>
            <hr />
            <p>The Gacha Challenge is inspired by real life gacha games.</p>
            <blockquote>
            <hr />
            <h1><center> <a><strong><code>Active Quarter Banner: Video Games!</code></strong></a> </center></h1>
            <hr />
            <p>The Winter 2026 banner is now active!</p>
            <center><img width='' src='https://i.postimg.cc/YpNr4q9S/26Q1-B.png'></center>
            <ul><li>__Enter for a chance to pull the new character: [Chiaki Nanami](https://anilist.co/character/73205)__</li>
            <li>50:50 chance for the new character</li></ul>
            </blockquote>
            <hr />
            <center><img width='' src='https://i.postimg.cc/W2zdFK4B/G2.png'></center>
            <center>Badges designed by [AWC Staff](https://anilist.co/user/AWC/social)</center>
            <center>**Rewards:** A random badge and 2 [Challenge Points](https://anilist.co/activity/93614073) per play</center>
            #<ul>**Badge Levels & Rarity**</ul>
            <ul><li>Every time you complete the Gacha Challenge you will earn a random character badge</li>
            <li>__Character badges are available in 4 levels of rarity:__</li></ul>
            ><ul> **N - Normal** - This is the base rarity</ul>
            ---
            <h1><ul><strong>How to Play!</strong></ul></h1>
            <ul><li>To play the Gacha Challenge, simply post a comment</li></ul>
            <ul>**There are 3 types of banners:**
            <li>**General Banners:** Standard & Adult</li></ul>
            <ul>**<a>NEW!</a>**<li>**Quarter Banners:** 1 per quarter from 2026</li></ul>
            [__All AWC Challenges__](https://anilist.co/activity/26266744)
            [__AWC Discord__](https://discord.com/invite/fbp2YH8)
            **Credits** <span class='markdown_spoiler'><span>Credit details here</span></span>
        """.trimIndent()

        val blocks = parse(html)
        assertTrue("Should produce blocks", blocks.isNotEmpty())

        // Verify key content exists somewhere in the output
        val textBlocks = blocks.filterIsInstance<RenderBlock.Text>()
        val allText = textBlocks.joinToString(" ") { it.annotatedString.text }

        assertTrue("Should contain 'AWC Gacha Challenge'", allText.contains("AWC Gacha Challenge"))
        assertTrue("Should contain 'How to Play'", allText.contains("How to Play"))
        assertTrue("Should contain 'Gacha Challenge is inspired'", allText.contains("Gacha Challenge is inspired"))

        // Verify structural blocks
        val imageBlocks = blocks.filterIsInstance<RenderBlock.Image>()
        assertTrue("Should have image blocks", imageBlocks.isNotEmpty())

        val hrBlocks = blocks.filterIsInstance<RenderBlock.HorizontalRule>()
        assertTrue("Should have HR blocks", hrBlocks.isNotEmpty())

        val bqBlocks = blocks.filterIsInstance<RenderBlock.Blockquote>()
        assertTrue("Should have blockquote blocks", bqBlocks.isNotEmpty())

        val spoilerBlocks = blocks.filterIsInstance<RenderBlock.Spoiler>()
        assertTrue("Should have spoiler blocks", spoilerBlocks.isNotEmpty())
    }

    // =========================================================================
    // ~~~ center marker handling (not confused with ~~ strikethrough)
    // =========================================================================

    @Test
    fun `triple tilde center markers are stripped not treated as strikethrough`() {
        val blocks = parse("<center>~~~Staff Picks Challenge: 2025~~~</center>")
        val textBlocks = blocks.filterIsInstance<RenderBlock.Text>()
        val allText = textBlocks.joinToString("") { it.annotatedString.text }

        assertTrue(
            "Should contain 'Staff Picks Challenge: 2025'",
            allText.contains("Staff Picks Challenge: 2025")
        )
        // Should NOT have strikethrough style from misinterpreting ~~
        val strikeThroughSpans = textBlocks.flatMap { it.annotatedString.spanStyles }
            .filter { it.item.textDecoration == TextDecoration.LineThrough }
        assertTrue("Should not have strikethrough spans", strikeThroughSpans.isEmpty())
    }

    @Test
    fun `triple tilde with bold inside center`() {
        val blocks = parse("<center>~~~__Header Text__~~~</center>")
        val textBlocks = blocks.filterIsInstance<RenderBlock.Text>()
        val allText = textBlocks.joinToString("") { it.annotatedString.text }

        assertTrue(
            "Should contain 'Header Text'",
            allText.contains("Header Text")
        )
        // Should have bold from __ markers
        val boldSpans = textBlocks.flatMap { it.annotatedString.spanStyles }
            .filter { it.item.fontWeight == FontWeight.Bold }
        assertTrue("Should have bold spans from __ markers", boldSpans.isNotEmpty())
    }

    @Test
    fun `triple tilde in body-level text node`() {
        // Body-level ~~~text~~~ would normally be converted to <center> by AniList API.
        // But when appearing as raw text in a TextNode, parseInlineMarkdown strips
        // the ~~~ markers and renders the inner content inline (no alignment change).
        val blocks = parse("~~~centered text~~~")
        val textBlocks = blocks.filterIsInstance<RenderBlock.Text>()
        val allText = textBlocks.joinToString("") { it.annotatedString.text }

        assertTrue(
            "Should contain 'centered text'",
            allText.contains("centered text")
        )
        // The ~~~ markers should be stripped (not rendered as text or strikethrough)
        assertTrue(
            "Should NOT contain tilde characters",
            !allText.contains("~")
        )
    }

    // =========================================================================
    // img() markdown handling
    // =========================================================================

    @Test
    fun `img markdown in body text emits Image block`() {
        val blocks = parse("img(https://example.com/photo.jpg)")
        val imageBlocks = blocks.filterIsInstance<RenderBlock.Image>()
        assertTrue("Should have an image block", imageBlocks.isNotEmpty())
        assertEquals("https://example.com/photo.jpg", imageBlocks.first().url)
    }

    @Test
    fun `img with width markdown emits Image block with width`() {
        val blocks = parse("img150(https://example.com/photo.jpg)")
        val imageBlocks = blocks.filterIsInstance<RenderBlock.Image>()
        assertTrue("Should have an image block", imageBlocks.isNotEmpty())
        assertEquals("https://example.com/photo.jpg", imageBlocks.first().url)
        assertEquals(150, imageBlocks.first().width)
    }

    @Test
    fun `img with percent width markdown emits Image block`() {
        val blocks = parse("img90%(https://example.com/photo.jpg)")
        val imageBlocks = blocks.filterIsInstance<RenderBlock.Image>()
        assertTrue("Should have an image block", imageBlocks.isNotEmpty())
        assertEquals("https://example.com/photo.jpg", imageBlocks.first().url)
        assertEquals(90, imageBlocks.first().width)
        assertTrue("Should be percent", imageBlocks.first().isPercentWidth)
    }

    @Test
    fun `img markdown inside anchor emits linked Image block`() {
        val blocks = parse("""<a href="https://anilist.co/anime/123">img(https://example.com/photo.jpg)</a>""")
        val imageBlocks = blocks.filterIsInstance<RenderBlock.Image>()
        assertTrue("Should have an image block", imageBlocks.isNotEmpty())
        assertEquals("https://example.com/photo.jpg", imageBlocks.first().url)
        assertEquals("https://anilist.co/anime/123", imageBlocks.first().linkUrl)
    }

    @Test
    fun `img markdown in list item emits Image block`() {
        val blocks = parse("<ul><li>img(https://example.com/photo.jpg)</li></ul>")
        val imageBlocks = blocks.filterIsInstance<RenderBlock.Image>()
        assertTrue("Should have an image block from list item", imageBlocks.isNotEmpty())
        assertEquals("https://example.com/photo.jpg", imageBlocks.first().url)
    }

    // =========================================================================
    // Bare URL handling
    // =========================================================================

    @Test
    fun `bare URL in body text rendered as clickable link`() {
        val blocks = parse("Visit https://anilist.co/anime/140499/ for details")
        val textBlocks = blocks.filterIsInstance<RenderBlock.Text>()
        val text = textBlocks.first().annotatedString

        assertTrue(
            "Should contain the URL text",
            text.text.contains("https://anilist.co/anime/140499/")
        )

        // Verify the URL is present as a link annotation.
        // LinkAnnotation.Url is pushed via pushLink — check that link annotations exist.
        assertTrue(
            "Should have link annotations for bare URL",
            text.hasLinkAnnotations(0, text.length)
        )
    }

    @Test
    fun `bare URL in list item rendered as clickable link`() {
        val blocks = parse("<ul><li>Check https://example.com/page for info</li></ul>")
        val textBlocks = blocks.filterIsInstance<RenderBlock.Text>()
        val allText = textBlocks.joinToString("") { it.annotatedString.text }

        assertTrue(
            "Should contain the URL text",
            allText.contains("https://example.com/page")
        )
    }

    // =========================================================================
    // Block-level elements in inline context
    // =========================================================================

    @Test
    fun `center tag inside inline context is rendered as block`() {
        val blocks = parse("<b><center>centered bold text</center></b>")
        val textBlocks = blocks.filterIsInstance<RenderBlock.Text>()
        val allText = textBlocks.joinToString("") { it.annotatedString.text }

        assertTrue(
            "Should contain 'centered bold text'",
            allText.contains("centered bold text")
        )
    }

    @Test
    fun `div inside inline context is rendered`() {
        val blocks = parse("<p>before<div>inside div</div>after</p>")
        val textBlocks = blocks.filterIsInstance<RenderBlock.Text>()
        val allText = textBlocks.joinToString(" ") { it.annotatedString.text }

        assertTrue("Should contain 'inside div'", allText.contains("inside div"))
    }

    @Test
    fun `br in renderInline uses ctx appendNewline`() {
        // Verify <br> doesn't cause issues with stale builder
        val blocks = parse("<p>line one<br>line two</p>")
        val textBlocks = blocks.filterIsInstance<RenderBlock.Text>()
        val allText = textBlocks.joinToString("") { it.annotatedString.text }

        assertTrue("Should contain 'line one'", allText.contains("line one"))
        assertTrue("Should contain 'line two'", allText.contains("line two"))
    }

    // =========================================================================
    // Stale builder fix validation
    // =========================================================================

    @Test
    fun `text after image in inline context is not lost`() {
        // This tests the stale builder fix: text after an image flush should not be lost
        val blocks = parse("<p>before <img src='https://example.com/img.jpg'> after</p>")
        val textBlocks = blocks.filterIsInstance<RenderBlock.Text>()
        val allText = textBlocks.joinToString(" ") { it.annotatedString.text }

        assertTrue("Should contain 'before'", allText.contains("before"))
        assertTrue("Should contain 'after'", allText.contains("after"))

        val imageBlocks = blocks.filterIsInstance<RenderBlock.Image>()
        assertTrue("Should have image block", imageBlocks.isNotEmpty())
    }

    @Test
    fun `anchor with img() markdown followed by text does not crash`() {
        val blocks = parse("""
            <a href="https://anilist.co/anime/123">img(https://example.com/img.jpg)</a>
            Some text after the link
        """.trimIndent())

        val imageBlocks = blocks.filterIsInstance<RenderBlock.Image>()
        assertTrue("Should have image block", imageBlocks.isNotEmpty())

        val textBlocks = blocks.filterIsInstance<RenderBlock.Text>()
        val allText = textBlocks.joinToString(" ") { it.annotatedString.text }
        assertTrue("Should contain text after the link", allText.contains("Some text after the link"))
    }
}
