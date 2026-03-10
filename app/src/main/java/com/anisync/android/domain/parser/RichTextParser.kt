package com.anisync.android.domain.parser

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

// =============================================================================
// Immutable UI Models
// =============================================================================

data class ParsedRichText(
    val blocks: List<RichTextBlock>,
    val imageUrls: List<String>
)

sealed interface RichTextBlock {
    val align: TextAlign

    data class Text(val text: AnnotatedString, override val align: TextAlign = TextAlign.Start) :
        RichTextBlock

    data class ImageGroup(
        val images: List<Image>,
        override val align: TextAlign = TextAlign.Start
    ) : RichTextBlock

    data class Image(
        val url: String,
        val width: Int?,
        val isPercent: Boolean,
        val linkUrl: String?,
        override val align: TextAlign = TextAlign.Start
    ) : RichTextBlock

    data class Table(val rows: List<TableRow>, override val align: TextAlign = TextAlign.Start) :
        RichTextBlock

    data class ListBlock(
        val items: List<ListItem>,
        override val align: TextAlign = TextAlign.Start
    ) : RichTextBlock

    data class CodeBlock(val code: String, override val align: TextAlign = TextAlign.Start) :
        RichTextBlock

    data class Spoiler(
        val children: List<RichTextBlock>,
        override val align: TextAlign = TextAlign.Start
    ) : RichTextBlock

    data class Blockquote(
        val children: List<RichTextBlock>,
        override val align: TextAlign = TextAlign.Start
    ) : RichTextBlock

    data class HorizontalRule(
        val widthPercent: Int? = null,
        override val align: TextAlign = TextAlign.Start
    ) : RichTextBlock

    data class YouTube(val videoId: String, override val align: TextAlign = TextAlign.Start) :
        RichTextBlock

    data class Video(val url: String, override val align: TextAlign = TextAlign.Start) :
        RichTextBlock

    data class AnilistLink(
        val type: String,
        val id: Int,
        val url: String,
        override val align: TextAlign = TextAlign.Start
    ) : RichTextBlock
}

data class TableRow(val cells: List<TableCell>)
data class TableCell(val children: List<RichTextBlock>, val isHeader: Boolean)
data class ListItem(val children: List<RichTextBlock>, val bullet: String?)

data class ParserConfig(
    val linkColor: Color,
    val codeBackground: Color,
    val spoilerColor: Color
)

// =============================================================================
// Background Parser Engine
// =============================================================================

object RichTextParser {

    private val HEADER_REGEX = Regex("^(#{1,5})\\s+(.*)")
    private val BOLD_WRAPPED_HEADER_REGEX =
        Regex("^(?:\\*\\*|__)(#{1,5})\\s*(.*?)(?:\\*\\*|__)\\s*$")

    private val YOUTUBE_REGEX = Regex("""youtube\((.*?)\)""")
    private val WEBM_REGEX = Regex("""webm\((.*?)\)""")
    private val IMAGE_MD_REGEX = Regex("""img(\d+%?)?\((.*?)\)""")
    private val BARE_URL_REGEX = Regex("""https?://[^\s<>"\[\]~!`)]+""")

    // Upgraded to match multi-line contents safely
    private val SPOILER_MD_REGEX = Regex("""~!(.*?)!~""", RegexOption.DOT_MATCHES_ALL)
    private val NON_DIGIT_REGEX = Regex("[^0-9]")
    private val ANILIST_LINK_REGEX =
        Regex("""https?://anilist\.co/(anime|manga|character|staff|user|activity)/([0-9]+)""")

    suspend fun parse(html: String, config: ParserConfig): ParsedRichText =
        withContext(Dispatchers.Default) {
            if (html.isBlank()) return@withContext ParsedRichText(emptyList(), emptyList())

            val preprocessedHtml = preprocessHtml(html)
            val doc = Jsoup.parseBodyFragment(preprocessedHtml)
            doc.outputSettings().prettyPrint(false)

            val rootContext = ParseContext(config)
            walkChildren(doc.body(), rootContext)
            rootContext.flushText()

            val groupedBlocks = groupImages(rootContext.blocks)
            val allUrls = mutableListOf<String>()
            extractImageUrls(groupedBlocks, allUrls)

            ParsedRichText(groupedBlocks, allUrls)
        }

    /**
     * Sanitizes complex DOM structures before Jsoup applies strict HTML5 rules.
     */
    private fun preprocessHtml(html: String): String {
        var processed = html.replace("\r", "")

        // 1. Prevent Jsoup from hoisting YouTube blocks out of inline spoilers
        // Converts `<div class="youtube">...</div>` to `<youtube>...</youtube>`
        val youtubeRegex = Regex(
            """<div([^>]*)class=['"]youtube['"]([^>]*)>(.*?)</div>""",
            RegexOption.DOT_MATCHES_ALL
        )
        processed = processed.replace(youtubeRegex) { match ->
            "<youtube${match.groupValues[1]}class=\"youtube\"${match.groupValues[2]}>${match.groupValues[3]}</youtube>"
        }

        // 2. Convert markdown quotes to blocks to prevent inline breakages
        val lines = processed.split("\n")
        val sb = StringBuilder()
        var currentDepth = 0
        var inCodeBlock = false

        for (line in lines) {
            val trimmed = line.trimStart()

            if (trimmed.startsWith("```") || trimmed.startsWith("<pre>")) inCodeBlock = !inCodeBlock
            if (trimmed.contains("</pre>")) inCodeBlock = false

            if (inCodeBlock) {
                while (currentDepth > 0) {
                    sb.append("</blockquote>\n")
                    currentDepth--
                }
                sb.append(line).append("\n")
                continue
            }

            var depth = 0
            var content = trimmed
            while (content.startsWith(">")) {
                depth++
                content = content.substring(1).trimStart()
            }

            while (currentDepth < depth) {
                sb.append("<blockquote>\n")
                currentDepth++
            }
            while (currentDepth > depth) {
                sb.append("</blockquote>\n")
                currentDepth--
            }

            sb.append(content).append("\n")
        }

        while (currentDepth > 0) {
            sb.append("</blockquote>\n")
            currentDepth--
        }

        return sb.toString()
    }

    private class ParseContext(
        val config: ParserConfig,
        val blocks: MutableList<RichTextBlock> = mutableListOf(),
        val align: TextAlign = TextAlign.Start,
        val currentLinkUrl: String? = null,
        val listDepth: Int = 0
    ) {
        var builder = AnnotatedString.Builder()
        var hasContent = false

        fun flushText() {
            if (hasContent) {
                val str = trimAnnotatedString(builder.toAnnotatedString())
                if (str.isNotBlank()) blocks.add(RichTextBlock.Text(str, align))
                builder = AnnotatedString.Builder()
                hasContent = false
            }
        }

        fun ensureParagraphBreak() {
            if (!hasContent) return
            val text = builder.toAnnotatedString().text
            if (text.isEmpty()) return
            if (!text.endsWith("\n\n")) builder.append(if (text.endsWith("\n")) "\n" else "\n\n")
        }

        fun ensureNewline() {
            if (!hasContent) return
            val text = builder.toAnnotatedString().text
            if (text.isNotEmpty() && !text.endsWith("\n")) builder.append("\n")
        }
    }

    // =========================================================================
    // Core DOM Walker
    // =========================================================================

    private fun walkChildren(parent: Element, ctx: ParseContext) {
        for (node in parent.childNodes()) walkNode(node, ctx)
    }

    private fun walkNode(node: Node, ctx: ParseContext) {
        when (node) {
            is TextNode -> {
                val text = normalizeWhitespace(node.wholeText)
                if (text.isBlank() && text.contains("\n")) {
                    if (ctx.hasContent) {
                        ctx.builder.append("\n"); ctx.hasContent = true
                    }
                    return
                }
                parseInlineMarkdown(text, ctx)
            }

            is Element -> walkElement(node, ctx)
        }
    }

    private fun walkElement(element: Element, ctx: ParseContext) {
        val tag = element.tagName().lowercase()

        val alignAttr = element.attr("align").lowercase()
        val isCenterTag = tag == "center"

        val textAlign = if (isCenterTag) TextAlign.Center else when (alignAttr) {
            "center" -> TextAlign.Center
            "right" -> TextAlign.Right
            "justify" -> TextAlign.Justify
            else -> ctx.align
        }

        val isNewAlign = textAlign != ctx.align
        val workingCtx = if (isNewAlign) {
            ctx.flushText()
            ParseContext(ctx.config, ctx.blocks, textAlign, ctx.currentLinkUrl, ctx.listDepth)
        } else ctx

        when (tag) {
            "p" -> {
                workingCtx.flushText(); walkChildren(element, workingCtx); workingCtx.flushText()
            }

            "h1", "h2", "h3", "h4", "h5" -> {
                workingCtx.flushText()
                workingCtx.builder.withStyle(headerSpanStyle(tag[1].digitToInt())) {
                    renderInline(element, workingCtx)
                }
                workingCtx.hasContent = true
                workingCtx.flushText()
            }

            "blockquote" -> {
                workingCtx.flushText()
                val bqCtx = ParseContext(
                    ctx.config,
                    align = workingCtx.align,
                    currentLinkUrl = workingCtx.currentLinkUrl
                )
                walkChildren(element, bqCtx)
                bqCtx.flushText()
                if (bqCtx.blocks.isNotEmpty()) workingCtx.blocks.add(
                    RichTextBlock.Blockquote(
                        bqCtx.blocks,
                        workingCtx.align
                    )
                )
            }

            "hr" -> {
                workingCtx.flushText()
                val widthAttr = element.attr("width").replace("%", "").toIntOrNull()
                workingCtx.blocks.add(
                    RichTextBlock.HorizontalRule(
                        widthPercent = widthAttr,
                        align = workingCtx.align
                    )
                )
            }

            "pre" -> {
                workingCtx.flushText()
                val code = element.selectFirst("code")?.wholeText() ?: element.wholeText()
                if (code.isNotBlank()) workingCtx.blocks.add(
                    RichTextBlock.CodeBlock(
                        code.trim(),
                        workingCtx.align
                    )
                )
            }

            "table" -> {
                workingCtx.flushText()
                val rows = mutableListOf<TableRow>()
                for (tr in element.select("tr")) {
                    val cells = mutableListOf<TableCell>()
                    for (td in tr.children()) {
                        if (td.tagName() in listOf("td", "th")) {
                            val isHeader = td.tagName() == "th"
                            val cellCtx = ParseContext(
                                ctx.config,
                                align = workingCtx.align,
                                currentLinkUrl = workingCtx.currentLinkUrl
                            )
                            walkChildren(td, cellCtx)
                            cellCtx.flushText()
                            cells.add(TableCell(cellCtx.blocks.toList(), isHeader))
                        }
                    }
                    if (cells.isNotEmpty()) rows.add(TableRow(cells))
                }
                if (rows.isNotEmpty()) workingCtx.blocks.add(
                    RichTextBlock.Table(
                        rows,
                        workingCtx.align
                    )
                )
            }

            "ul", "ol" -> handleList(element, workingCtx, tag == "ol")
            "img" -> handleImage(element, workingCtx, workingCtx.currentLinkUrl)
            "br" -> {
                workingCtx.builder.append("\n"); workingCtx.hasContent = true
            }

            "center" -> {
                workingCtx.flushText()
                walkChildren(element, workingCtx)
                workingCtx.flushText()
            }

            "youtube" -> { // Handles preprocessed safe YouTube block
                workingCtx.flushText()
                val id = element.id()
                if (id.isNotBlank()) {
                    workingCtx.blocks.add(
                        RichTextBlock.YouTube(
                            "[https://www.youtube.com/watch?v=$id](https://www.youtube.com/watch?v=$id)",
                            workingCtx.align
                        )
                    )
                } else {
                    walkChildren(element, workingCtx) // Fallback if it wraps an <iframe>
                }
            }

            "div" -> handleDiv(element, workingCtx)
            "span" -> handleSpan(element, workingCtx)
            "video" -> {
                workingCtx.flushText()
                val src =
                    element.attr("src").ifBlank { element.selectFirst("source")?.attr("src") ?: "" }
                if (src.isNotBlank()) workingCtx.blocks.add(
                    RichTextBlock.Video(
                        src,
                        workingCtx.align
                    )
                )
            }

            "b", "strong" -> {
                workingCtx.builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    renderInline(
                        element,
                        workingCtx
                    )
                }
                workingCtx.hasContent = true
            }

            "i", "em" -> {
                workingCtx.builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    renderInline(
                        element,
                        workingCtx
                    )
                }
                workingCtx.hasContent = true
            }

            "del", "strike", "s" -> {
                workingCtx.builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    renderInline(
                        element,
                        workingCtx
                    )
                }
                workingCtx.hasContent = true
            }

            "a" -> handleAnchor(element, workingCtx)
            "code" -> {
                val isLightBackground = ctx.config.codeBackground.luminance() > 0.5f
                val textColor = if (isLightBackground) Color.Black else Color.White
                workingCtx.builder.withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = ctx.config.codeBackground,
                        color = textColor,
                        fontSize = 13.sp
                    )
                ) { append(element.wholeText()) }
                workingCtx.hasContent = true
            }

            "iframe" -> {
                workingCtx.flushText()
                val src = element.attr("src")
                if (src.contains("youtube", ignoreCase = true) || src.contains(
                        "youtu.be",
                        ignoreCase = true
                    )
                ) {
                    workingCtx.blocks.add(RichTextBlock.YouTube(src, workingCtx.align))
                } else if (src.isNotBlank()) {
                    workingCtx.blocks.add(RichTextBlock.Video(src, workingCtx.align))
                }
            }

            else -> walkChildren(element, workingCtx)
        }

        if (isNewAlign) workingCtx.flushText()
    }

    // =========================================================================
    // Markdown Scanner
    // =========================================================================

    private fun parseInlineMarkdown(text: String, ctx: ParseContext) {
        var i = 0
        var lastAppend = 0
        val len = text.length

        fun flush() {
            if (i > lastAppend) {
                ctx.builder.append(text.subSequence(lastAppend, i))
                ctx.hasContent = true
            }
        }

        while (i < len) {
            val isStartOfLine =
                (i == 0 && (!ctx.hasContent || ctx.builder.toAnnotatedString().text.endsWith('\n'))) || (i > 0 && text[i - 1] == '\n')
            if (isStartOfLine) {
                var ws = i
                while (ws < len && (text[ws] == ' ' || text[ws] == '\t')) ws++

                if (ws < len) {
                    val cLine = text[ws]

                    val currentLineEnd = text.indexOf('\n', ws)
                    if (currentLineEnd != -1) {
                        val nextLineStart = currentLineEnd + 1
                        var nextLineWs = nextLineStart
                        while (nextLineWs < len && (text[nextLineWs] == ' ' || text[nextLineWs] == '\t')) nextLineWs++
                        val nextChar = if (nextLineWs < len) text[nextLineWs] else ' '

                        if (nextChar == '=' || nextChar == '-') {
                            var count = 0
                            var j = nextLineWs
                            while (j < len && text[j] == nextChar) {
                                count++; j++
                            }
                            while (j < len && (text[j] == ' ' || text[j] == '\t')) j++

                            if (count >= 2 && (j == len || text[j] == '\n' || text[j] == '\r')) {
                                flush(); ctx.flushText()
                                val level = if (nextChar == '=') 1 else 2
                                ctx.builder.withStyle(headerSpanStyle(level)) {
                                    val subCtx = ParseContext(
                                        ctx.config,
                                        align = ctx.align,
                                        currentLinkUrl = ctx.currentLinkUrl
                                    )
                                    parseInlineMarkdown(text.substring(ws, currentLineEnd), subCtx)
                                    append(subCtx.builder.toAnnotatedString())
                                }
                                ctx.hasContent = true; ctx.flushText()
                                i = if (j < len && text[j] == '\r') j + 1 else j
                                if (i < len && text[i] == '\n') i++
                                lastAppend = i
                                continue
                            }
                        }
                    }

                    if (cLine == '-' || cLine == '*' || cLine == '_') {
                        var j = ws
                        var count = 0
                        while (j < len) {
                            val ch = text[j]
                            if (ch == cLine) count++
                            else if (ch != ' ' && ch != '\t') break
                            j++
                        }
                        if (count >= 3 && (j == len || text[j] == '\n' || text[j] == '\r')) {
                            flush(); ctx.flushText()
                            ctx.blocks.add(RichTextBlock.HorizontalRule(align = ctx.align))
                            i = if (j < len && text[j] == '\r') j + 1 else j
                            if (i < len && text[i] == '\n') i++
                            lastAppend = i
                            continue
                        }
                    }

                    if (cLine == '#') {
                        var j = ws
                        while (j < len && text[j] == '#') j++
                        val level = j - ws
                        if (level in 1..5 && j < len && text[j] == ' ') {
                            var eol = text.indexOf('\n', j)
                            if (eol == -1) eol = len
                            flush(); ctx.flushText()
                            ctx.builder.withStyle(headerSpanStyle(level)) {
                                val subCtx = ParseContext(
                                    ctx.config,
                                    align = ctx.align,
                                    currentLinkUrl = ctx.currentLinkUrl
                                )
                                parseInlineMarkdown(text.substring(j + 1, eol), subCtx)
                                append(subCtx.builder.toAnnotatedString())
                            }
                            ctx.hasContent = true; ctx.flushText()
                            i = eol
                            if (i < len && text[i] == '\n') i++
                            lastAppend = i
                            continue
                        }
                    }
                }
            }

            val c = text[i]

            if (c == '\\' && i + 1 < len) {
                flush(); ctx.builder.append(text[i + 1]); ctx.hasContent =
                    true; i += 2; lastAppend = i; continue
            }
            if (c == '`') {
                if (text.startsWith("```", i)) {
                    val end = text.indexOf("```", i + 3)
                    if (end != -1) {
                        flush(); ctx.flushText()
                        ctx.blocks.add(
                            RichTextBlock.CodeBlock(
                                text.substring(i + 3, end).trim('\n', '\r', ' '), ctx.align
                            )
                        )
                        i = end + 3; lastAppend = i; continue
                    }
                }
                val end = text.indexOf('`', i + 1)
                if (end != -1 && end > i) {
                    flush()
                    val isLightBackground = ctx.config.codeBackground.luminance() > 0.5f
                    val textColor = if (isLightBackground) Color.Black else Color.White
                    ctx.builder.withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = ctx.config.codeBackground,
                            color = textColor,
                            fontSize = 13.sp
                        )
                    ) { append(text.substring(i + 1, end)) }
                    ctx.hasContent = true
                    i = end + 1; lastAppend = i; continue
                }
            }
            if (c == 'y' && text.startsWith("youtube(", i)) {
                val match = YOUTUBE_REGEX.find(text, i)
                if (match != null && match.range.first == i) {
                    flush(); ctx.flushText()
                    var url = match.groupValues[1]
                    if (!url.startsWith("http")) url =
                        "[https://www.youtube.com/watch?v=$url](https://www.youtube.com/watch?v=$url)"
                    ctx.blocks.add(RichTextBlock.YouTube(url, ctx.align))
                    i = match.range.last + 1; lastAppend = i; continue
                }
            }
            if (c == 'w' && text.startsWith("webm(", i)) {
                val match = WEBM_REGEX.find(text, i)
                if (match != null && match.range.first == i) {
                    flush(); ctx.flushText()
                    ctx.blocks.add(RichTextBlock.Video(match.groupValues[1], ctx.align))
                    i = match.range.last + 1; lastAppend = i; continue
                }
            }
            if (c == 'i' && (text.startsWith("img(", i) || text.startsWith("img", i))) {
                val match = IMAGE_MD_REGEX.find(text, i)
                if (match != null && match.range.first == i) {
                    flush(); ctx.flushText()
                    val sizeStr = match.groupValues[1]
                    ctx.blocks.add(
                        RichTextBlock.Image(
                            url = match.groupValues[2],
                            width = sizeStr.replace("%", "").toIntOrNull(),
                            isPercent = sizeStr.endsWith("%"),
                            linkUrl = ctx.currentLinkUrl,
                            align = ctx.align
                        )
                    )
                    i = match.range.last + 1; lastAppend = i; continue
                }
            }
            if (c == '~' && text.startsWith("~~~", i)) {
                val end = text.indexOf("~~~", i + 3)
                if (end != -1) {
                    flush(); ctx.flushText()
                    var contentStart = i + 3
                    val maxCheck = minOf(contentStart + 10, text.length)
                    val afterTildes = text.substring(contentStart, maxCheck).lowercase()
                    if (afterTildes.startsWith("center")) contentStart += 6
                    else if (afterTildes.startsWith(" center")) contentStart += 7

                    val centerCtx = ParseContext(
                        ctx.config,
                        align = TextAlign.Center,
                        currentLinkUrl = ctx.currentLinkUrl,
                        listDepth = ctx.listDepth
                    )
                    parseInlineMarkdown(text.substring(contentStart, end), centerCtx)
                    centerCtx.flushText()
                    ctx.blocks.addAll(centerCtx.blocks)
                    i = end + 3; lastAppend = i; continue
                }
            }
            if (c == '~') {
                if (text.startsWith("~!", i)) {
                    val match = SPOILER_MD_REGEX.find(text, i)
                    if (match != null && match.range.first == i) {
                        flush(); ctx.flushText()
                        val spoilerCtx = ParseContext(
                            ctx.config,
                            align = ctx.align,
                            currentLinkUrl = ctx.currentLinkUrl
                        )
                        parseInlineMarkdown(match.groupValues[1], spoilerCtx)
                        spoilerCtx.flushText()
                        if (spoilerCtx.blocks.isNotEmpty()) ctx.blocks.add(
                            RichTextBlock.Spoiler(
                                spoilerCtx.blocks,
                                ctx.align
                            )
                        )
                        i = match.range.last + 1; lastAppend = i; continue
                    }
                } else {
                    val isDouble = text.startsWith("~~", i)
                    val marker = if (isDouble) "~~" else "~"
                    val end = text.indexOf(marker, i + marker.length)
                    if (end != -1 && i + marker.length < len && !text[i + marker.length].isWhitespace()) {
                        flush()
                        ctx.builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            val subCtx = ParseContext(
                                ctx.config,
                                align = ctx.align,
                                currentLinkUrl = ctx.currentLinkUrl
                            )
                            parseInlineMarkdown(text.substring(i + marker.length, end), subCtx)
                            append(subCtx.builder.toAnnotatedString())
                        }
                        ctx.hasContent = true
                        i = end + marker.length; lastAppend = i; continue
                    }
                }
            }

            val isAsterisk = c == '*'
            val isUnderscore = c == '_'
            if (isAsterisk || isUnderscore) {
                val charStr = c.toString()
                val marker3 = charStr.repeat(3)
                val marker2 = charStr.repeat(2)

                if (text.startsWith(marker3, i)) {
                    val end = text.indexOf(marker3, i + 3)
                    if (end != -1) {
                        flush()
                        ctx.builder.withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic
                            )
                        ) {
                            val subCtx = ParseContext(
                                ctx.config,
                                align = ctx.align,
                                currentLinkUrl = ctx.currentLinkUrl
                            )
                            parseInlineMarkdown(
                                text.substring(i + 3, end),
                                subCtx
                            ); append(subCtx.builder.toAnnotatedString())
                        }
                        ctx.hasContent = true
                        i = end + 3; lastAppend = i; continue
                    }
                }
                if (text.startsWith(marker2, i)) {
                    val end = text.indexOf(marker2, i + 2)
                    if (end != -1) {
                        flush()
                        ctx.builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            val subCtx = ParseContext(
                                ctx.config,
                                align = ctx.align,
                                currentLinkUrl = ctx.currentLinkUrl
                            )
                            parseInlineMarkdown(
                                text.substring(i + 2, end),
                                subCtx
                            ); append(subCtx.builder.toAnnotatedString())
                        }
                        ctx.hasContent = true
                        i = end + 2; lastAppend = i; continue
                    }
                }
                val end = text.indexOf(charStr, i + 1)
                if (end != -1 && i + 1 < len && !text[i + 1].isWhitespace()) {
                    flush()
                    ctx.builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        val subCtx = ParseContext(
                            ctx.config,
                            align = ctx.align,
                            currentLinkUrl = ctx.currentLinkUrl
                        )
                        parseInlineMarkdown(
                            text.substring(i + 1, end),
                            subCtx
                        ); append(subCtx.builder.toAnnotatedString())
                    }
                    ctx.hasContent = true
                    i = end + 1; lastAppend = i; continue
                }
            }

            if (c == '[' && text.indexOf("](", i) != -1) {
                val closeBracket = text.indexOf(']', i + 1)
                if (closeBracket != -1 && closeBracket + 1 < len && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        flush()
                        val linkText = text.substring(i + 1, closeBracket)
                        val url = text.substring(closeBracket + 2, closeParen)
                        ctx.builder.pushLink(
                            LinkAnnotation.Url(
                                url,
                                TextLinkStyles(
                                    SpanStyle(
                                        color = ctx.config.linkColor,
                                        textDecoration = TextDecoration.Underline
                                    )
                                )
                            )
                        )
                        val subCtx =
                            ParseContext(ctx.config, align = ctx.align, currentLinkUrl = url)
                        parseInlineMarkdown(
                            linkText,
                            subCtx
                        ); ctx.builder.append(subCtx.builder.toAnnotatedString())
                        ctx.builder.pop()
                        ctx.hasContent = true
                        i = closeParen + 1; lastAppend = i; continue
                    }
                }
            }

            if (c == 'h' && (text.startsWith("http://", i) || text.startsWith("https://", i))) {
                val match = BARE_URL_REGEX.find(text, i)
                if (match != null && match.range.first == i) {
                    flush()
                    val url = match.value

                    val anilistMatch = ANILIST_LINK_REGEX.find(url)
                    if (anilistMatch != null && anilistMatch.range.first == 0) {
                        ctx.flushText()
                        val type = anilistMatch.groupValues[1]
                        val id = anilistMatch.groupValues[2].toIntOrNull() ?: 0
                        ctx.blocks.add(RichTextBlock.AnilistLink(type, id, url, ctx.align))
                        i = match.range.last + 1
                        lastAppend = i
                        continue
                    }

                    ctx.builder.pushLink(
                        LinkAnnotation.Url(
                            url,
                            TextLinkStyles(
                                SpanStyle(
                                    color = ctx.config.linkColor,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        )
                    )
                    ctx.builder.append(url)
                    ctx.builder.pop()
                    ctx.hasContent = true
                    i = match.range.last + 1; lastAppend = i; continue
                }
            }

            if (!c.isWhitespace()) {
                ctx.hasContent = true
            }
            i++
        }
        flush()
    }

    // =========================================================================
    // Element Specific Handlers
    // =========================================================================

    private fun handleAnchor(element: Element, ctx: ParseContext) {
        val href = element.attr("href")
        val img = element.selectFirst("img")
        if (img != null && href.isNotBlank() && element.text().isBlank()) {
            handleImage(img, ctx, href); return
        }

        if (href.isNotBlank()) {
            ctx.builder.pushLink(
                LinkAnnotation.Url(
                    href,
                    TextLinkStyles(
                        SpanStyle(
                            color = ctx.config.linkColor,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                )
            )
            val subCtx = ParseContext(
                ctx.config,
                ctx.blocks,
                ctx.align,
                currentLinkUrl = href,
                listDepth = ctx.listDepth
            )
            subCtx.builder = ctx.builder
            renderInline(element, subCtx)
            ctx.builder.pop()
        } else {
            ctx.builder.withStyle(SpanStyle(color = ctx.config.linkColor)) {
                renderInline(
                    element,
                    ctx
                )
            }
        }
        ctx.hasContent = true
    }

    private fun handleImage(element: Element, ctx: ParseContext, linkUrl: String?) {
        ctx.flushText()
        val src = element.attr("src")
        if (src.isNotBlank()) {
            val widthAttr = element.attr("width")
            ctx.blocks.add(
                RichTextBlock.Image(
                    src,
                    widthAttr.replace(NON_DIGIT_REGEX, "").toIntOrNull(),
                    widthAttr.contains("%"),
                    linkUrl,
                    ctx.align
                )
            )
        }
    }

    private fun handleDiv(element: Element, ctx: ParseContext) {
        if (element.className().lowercase().contains("youtube")) {
            ctx.flushText()
            element.id().takeIf { it.isNotBlank() }?.let {
                ctx.blocks.add(
                    RichTextBlock.YouTube(
                        "[https://www.youtube.com/watch?v=$it](https://www.youtube.com/watch?v=$it)",
                        ctx.align
                    )
                )
            }
            return
        }
        if (element.attr("rel").lowercase() == "spoiler") {
            ctx.flushText()
            val spoilerCtx = ParseContext(
                ctx.config,
                align = ctx.align,
                currentLinkUrl = ctx.currentLinkUrl,
                listDepth = ctx.listDepth
            )
            walkChildren(element, spoilerCtx)
            spoilerCtx.flushText()
            if (spoilerCtx.blocks.isNotEmpty()) ctx.blocks.add(
                RichTextBlock.Spoiler(
                    spoilerCtx.blocks,
                    ctx.align
                )
            )
            return
        }
        walkChildren(element, ctx)
    }

    private fun handleSpan(element: Element, ctx: ParseContext) {
        if (element.className().lowercase().contains("markdown_spoiler")) {
            ctx.flushText()
            val spoilerCtx = ParseContext(
                ctx.config,
                align = ctx.align,
                currentLinkUrl = ctx.currentLinkUrl,
                listDepth = ctx.listDepth
            )
            walkChildren(element.selectFirst(":root > span") ?: element, spoilerCtx)
            spoilerCtx.flushText()
            if (spoilerCtx.blocks.isNotEmpty()) ctx.blocks.add(
                RichTextBlock.Spoiler(
                    spoilerCtx.blocks,
                    ctx.align
                )
            )
            return
        }
        renderInline(element, ctx); ctx.hasContent = true
    }

    private fun handleList(element: Element, ctx: ParseContext, isOrdered: Boolean) {
        ctx.flushText()
        val listItems = mutableListOf<ListItem>()
        val depth = ctx.listDepth
        val bulletSymbol = when (depth % 3) {
            0 -> "•"; 1 -> "◦"; else -> "▪"
        }
        var itemIndex = element.attr("start").toIntOrNull() ?: 1

        var liCtx = ParseContext(
            ctx.config,
            align = ctx.align,
            currentLinkUrl = ctx.currentLinkUrl,
            listDepth = depth + 1
        )

        for (child in element.childNodes()) {
            if (child is Element && child.tagName().lowercase() == "li") {
                liCtx.flushText()
                if (liCtx.blocks.isNotEmpty()) {
                    listItems.add(ListItem(liCtx.blocks.toList(), null))
                    liCtx.blocks.clear()
                }

                val newLiCtx = ParseContext(
                    ctx.config,
                    align = ctx.align,
                    currentLinkUrl = ctx.currentLinkUrl,
                    listDepth = depth + 1
                )
                walkChildren(child, newLiCtx)
                newLiCtx.flushText()
                listItems.add(
                    ListItem(
                        newLiCtx.blocks.toList(),
                        if (isOrdered) "${itemIndex++}." else bulletSymbol
                    )
                )
            } else if (child is Element && child.tagName().lowercase() in listOf("ul", "ol")) {
                liCtx.flushText()
                if (liCtx.blocks.isNotEmpty()) {
                    listItems.add(ListItem(liCtx.blocks.toList(), null))
                    liCtx.blocks.clear()
                }
                walkElement(child, liCtx)
                liCtx.flushText()
                if (liCtx.blocks.isNotEmpty()) {
                    listItems.add(ListItem(liCtx.blocks.toList(), null))
                    liCtx.blocks.clear()
                }
            } else if (child is TextNode) {
                val text = normalizeWhitespace(child.wholeText)
                if (text.isBlank() && text.contains("\n")) continue
                walkNode(child, liCtx)
            } else {
                walkNode(child, liCtx)
            }
        }

        liCtx.flushText()
        if (liCtx.blocks.isNotEmpty()) {
            listItems.add(ListItem(liCtx.blocks.toList(), null))
        }

        if (listItems.isNotEmpty()) {
            ctx.blocks.add(RichTextBlock.ListBlock(listItems, ctx.align))
        }
    }

    private fun renderInline(element: Element, ctx: ParseContext) {
        walkChildren(element, ctx)
    }

    // =========================================================================
    // Utilities & Formatting
    // =========================================================================

    private fun groupImages(blocks: List<RichTextBlock>): List<RichTextBlock> {
        val result = mutableListOf<RichTextBlock>()
        var i = 0
        while (i < blocks.size) {
            val block = blocks[i]
            if (block is RichTextBlock.Image) {
                val group = mutableListOf<RichTextBlock.Image>()
                val currentAlign = block.align
                while (i < blocks.size && blocks[i] is RichTextBlock.Image && blocks[i].align == currentAlign) {
                    group.add(blocks[i] as RichTextBlock.Image); i++
                }
                result.add(
                    if (group.size > 1) RichTextBlock.ImageGroup(
                        group,
                        currentAlign
                    ) else group.first()
                )
            } else {
                result.add(
                    when (block) {
                        is RichTextBlock.Spoiler -> block.copy(children = groupImages(block.children))
                        is RichTextBlock.Blockquote -> block.copy(children = groupImages(block.children))
                        is RichTextBlock.Table -> block.copy(rows = block.rows.map { row ->
                            TableRow(
                                row.cells.map { TableCell(groupImages(it.children), it.isHeader) })
                        })

                        is RichTextBlock.ListBlock -> block.copy(items = block.items.map {
                            ListItem(
                                groupImages(it.children),
                                it.bullet
                            )
                        })

                        else -> block
                    }
                )
                i++
            }
        }
        return result
    }

    private fun extractImageUrls(blocks: List<RichTextBlock>, dest: MutableList<String>) {
        blocks.forEach { block ->
            when (block) {
                is RichTextBlock.Image -> dest.add(block.url)
                is RichTextBlock.ImageGroup -> block.images.forEach { dest.add(it.url) }
                is RichTextBlock.Spoiler -> extractImageUrls(block.children, dest)
                is RichTextBlock.Blockquote -> extractImageUrls(block.children, dest)
                is RichTextBlock.ListBlock -> block.items.forEach {
                    extractImageUrls(
                        it.children,
                        dest
                    )
                }

                is RichTextBlock.Table -> block.rows.forEach { row ->
                    row.cells.forEach {
                        extractImageUrls(
                            it.children,
                            dest
                        )
                    }
                }

                else -> {}
            }
        }
    }

    private fun normalizeWhitespace(text: String): String {
        return text.replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
    }

    private fun trimAnnotatedString(str: AnnotatedString): AnnotatedString {
        val text = str.text
        val start = text.indexOfFirst { it != '\n' && it != '\r' && it != ' ' }
        val end = text.indexOfLast { it != '\n' && it != '\r' && it != ' ' }
        if (start == -1 || start > end) return AnnotatedString("")
        return str.subSequence(start, end + 1)
    }

    private fun headerSpanStyle(level: Int): SpanStyle = when (level) {
        1 -> SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        2 -> SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
        3 -> SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
        4 -> SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        5 -> SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        else -> SpanStyle(fontWeight = FontWeight.Bold)
    }
}