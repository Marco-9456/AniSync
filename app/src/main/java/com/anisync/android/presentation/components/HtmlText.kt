package com.anisync.android.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/**
 * Renders AniList-Flavored Markdown as styled text with inline images and code blocks.
 *
 * Content is split into blocks:
 * - **Text blocks** rendered via [Text] with styled [AnnotatedString]
 * - **Image blocks** rendered via Coil [AsyncImage]
 * - **Code blocks** rendered via [Text] with monospace font and background
 *
 * Spec: https://anilist.co/forum/thread/6125
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val spoilerColor = MaterialTheme.colorScheme.onSurfaceVariant

    val blocks = remember(html, linkColor, codeBackground) {
        parseAniListMarkdownBlocks(html, linkColor, codeBackground, spoilerColor)
    }

    // Fullscreen image viewer state
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.clipToBounds(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var i = 0
        while (i < blocks.size) {
            when (val block = blocks[i]) {
                is ContentBlock.Text -> {
                    Text(
                        text = block.annotatedString,
                        style = style.copy(color = color)
                    )
                }

                is ContentBlock.Image -> {
                    val imageGroup = mutableListOf(block)
                    while (i + 1 < blocks.size && blocks[i + 1] is ContentBlock.Image) {
                        i++
                        imageGroup.add(blocks[i] as ContentBlock.Image)
                    }

                    if (imageGroup.size > 1) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            imageGroup.forEach { img ->
                                val widthModifier = if (img.isPercentWidth && img.width != null) {
                                    Modifier.fillMaxWidth(img.width / 100f)
                                } else {
                                    val widthDp = img.width?.dp ?: 160.dp
                                    Modifier.widthIn(max = widthDp)
                                }
                                AsyncImage(
                                    model = img.url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = widthModifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { fullscreenImageUrl = img.url }
                                )
                            }
                        }
                    } else {
                        val img = imageGroup.first()
                        val imgModifier = if (img.isPercentWidth && img.width != null) {
                            Modifier.fillMaxWidth(img.width / 100f)
                        } else if (img.width != null) {
                            Modifier.widthIn(max = img.width.dp)
                        } else {
                            Modifier.fillMaxWidth()
                        }
                        AsyncImage(
                            model = img.url,
                            contentDescription = null,
                            contentScale = if (img.width != null) ContentScale.Fit else ContentScale.FillWidth,
                            modifier = imgModifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { fullscreenImageUrl = img.url }
                        )
                    }
                }

                is ContentBlock.Code -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(codeBackground)
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = block.code,
                            style = style.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = color
                            )
                        )
                    }
                }
            }
            i++
        }
    }

    fullscreenImageUrl?.let { imageUrl ->
        FullscreenImageViewer(
            imageUrl = imageUrl,
            onDismiss = { fullscreenImageUrl = null }
        )
    }
}

@Composable
private fun FullscreenImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(indication = null, interactionSource = null) { onDismiss() }
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }

            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
                    .clickable(indication = null, interactionSource = null) { /* consume */ }
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

// =============================================================================
// Data Models
// =============================================================================

private sealed class ContentBlock {
    data class Text(val annotatedString: AnnotatedString) : ContentBlock()
    data class Image(
        val url: String,
        val width: Int? = null,
        val isPercentWidth: Boolean = false
    ) : ContentBlock()

    data class Code(val code: String) : ContentBlock()
}

// =============================================================================
// Intermediate segment for splitting code blocks before image extraction
// =============================================================================

private sealed class Segment {
    data class TextSegment(val text: String) : Segment()
    data class CodeSegment(val code: String) : Segment()
}

// =============================================================================
// Preprocessing Pipeline
// =============================================================================

/**
 * Full HTML preprocessing: converts HTML constructs to their markdown equivalents
 * so the main parser only needs to handle markdown syntax.
 */
private fun preprocessHtml(raw: String): String {
    var text = raw

    // Convert <br> to newlines
    text = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")

    // Convert <p> to double newlines
    text = text.replace(Regex("</?p(?:\\s[^>]*)?>", RegexOption.IGNORE_CASE), "\n")

    // Convert <pre> blocks to fenced code blocks (before stripping divs)
    text = Regex("<pre>(.*?)</pre>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .replace(text) { "\n```\n${it.groupValues[1].trim()}\n```\n" }

    // Convert <blockquote> to > prefix
    text = convertBlockquotes(text)

    // Convert <div rel="spoiler"> to ~!...!~ (before stripping generic divs)
    text = Regex(
        """<div\s+rel="spoiler">(.*?)</div>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    ).replace(text) { "~!${it.groupValues[1]}!~" }

    // Convert <hr> / <hr /> / <HR width=50%> to ---
    text = text.replace(Regex("<hr\\b[^>]*/?>", RegexOption.IGNORE_CASE), "\n---\n")

    // Convert HTML lists to markdown
    text = convertHtmlLists(text)

    // Unwrap <a> tags that wrap images — keep just the image to prevent orphaning
    // during image extraction. Handles img###(...), ![alt](...), and <img> inside <a>.
    text = Regex(
        """<a\s[^>]*href="[^"]*"[^>]*>\s*(img[a-zA-Z0-9%]*\([^)]+\)|!\[[^\]]*\]\([^)]+\)|<img\s[^>]*/?>)\s*</a>""",
        setOf(RegexOption.IGNORE_CASE)
    ).replace(text) { it.groupValues[1] }

    // Strip <div> and <center> (alignment wrappers)
    text = text.replace(Regex("</?div(?:\\s[^>]*)?>", RegexOption.IGNORE_CASE), "\n")
    text = text.replace(Regex("</?center>", RegexOption.IGNORE_CASE), "")

    // Strip ~~~ center alignment markers at line boundaries
    text = text.replace(Regex("^\\s*~~~\\s*$", RegexOption.MULTILINE), "")
    text = text.replace(Regex("^(\\s*)~~~", RegexOption.MULTILINE), "$1")
    text = text.replace(Regex("~~~(\\s*)$", RegexOption.MULTILINE), "$1")

    // Convert setext-style headers
    text = convertSetextHeaders(text)

    // Collapse excessive newlines
    text = text.replace(Regex("\n{3,}"), "\n\n")

    // Strip alignment wrappers: center(...), left(...), right(...)
    text = stripAlignmentWrappers(text)

    return text
}

/**
 * Convert `<blockquote>` HTML tags to `>` prefix markdown.
 * Handles nesting by adding multiple `>` prefixes.
 */
private fun convertBlockquotes(text: String): String {
    var result = text
    // Iteratively replace innermost blockquotes to handle nesting
    var changed = true
    var safetyLimit = 10
    while (changed && safetyLimit > 0) {
        safetyLimit--
        val before = result
        result = Regex(
            "<blockquote>(.*?)</blockquote>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).replace(result) { match ->
            val content = match.groupValues[1].trim()
            content.lines().joinToString("\n") { line ->
                if (line.trimStart().startsWith(">")) ">$line" else "> $line"
            }
        }
        changed = result != before
    }
    return result
}

/**
 * Convert `<ul>/<ol>/<li>` HTML lists to markdown list syntax.
 * Handles HTML5 implicit `</li>` close (e.g. `<li>Item<li>Item</ul>`).
 */
private fun convertHtmlLists(text: String): String {
    var result = text

    // Iteratively process innermost list blocks
    var changed = true
    var safetyLimit = 10
    while (changed && safetyLimit > 0) {
        safetyLimit--
        val before = result

        // Ordered lists: replace <li> with numbered prefix
        result = Regex(
            "<ol>(.*?)</ol>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).replace(result) { match ->
            var idx = 1
            match.groupValues[1]
                .replace(Regex("<li>", RegexOption.IGNORE_CASE)) { "\n${idx++}. " }
                .replace(Regex("</li>", RegexOption.IGNORE_CASE), "")
        }

        // Unordered lists: replace <li> with bullet prefix
        result = Regex(
            "<ul>(.*?)</ul>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).replace(result) { match ->
            match.groupValues[1]
                .replace(Regex("<li>", RegexOption.IGNORE_CASE), "\n- ")
                .replace(Regex("</li>", RegexOption.IGNORE_CASE), "")
        }

        changed = result != before
    }

    // Clean up any remaining stray list tags
    result = result.replace(Regex("<li>", RegexOption.IGNORE_CASE), "\n- ")
    result = result.replace(Regex("</li>", RegexOption.IGNORE_CASE), "")
    result = result.replace(Regex("</?[ou]l>", RegexOption.IGNORE_CASE), "")

    return result
}

/**
 * Convert setext-style headers:
 * ```
 * Header 1
 * ========
 * ```
 * becomes `# Header 1`, and
 * ```
 * Header 2
 * --------
 * ```
 * becomes `## Header 2`.
 */
private fun convertSetextHeaders(text: String): String {
    val lines = text.lines().toMutableList()
    var i = 0
    while (i < lines.size - 1) {
        val nextTrimmed = lines[i + 1].trim()
        val currentTrimmed = lines[i].trim()
        if (currentTrimmed.isNotEmpty() && nextTrimmed.length >= 2) {
            when {
                nextTrimmed.all { it == '=' } -> {
                    lines[i] = "# $currentTrimmed"
                    lines.removeAt(i + 1)
                    continue
                }

                nextTrimmed.all { it == '-' } && !currentTrimmed.startsWith("#") -> {
                    lines[i] = "## $currentTrimmed"
                    lines.removeAt(i + 1)
                    continue
                }
            }
        }
        i++
    }
    return lines.joinToString("\n")
}

// =============================================================================
// Code Block Splitting
// =============================================================================

/**
 * Split text into text segments and fenced code block segments.
 * Fenced code blocks start with ``` on their own line and end with ```.
 * Also handles 4-space / 1-tab indented code blocks.
 */
private fun splitCodeBlocks(text: String): List<Segment> {
    val segments = mutableListOf<Segment>()
    val lines = text.lines()
    val textBuffer = StringBuilder()
    val codeBuffer = StringBuilder()
    var inFencedCode = false
    var inIndentCode = false

    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        if (!inFencedCode && !inIndentCode && trimmed.startsWith("```")) {
            // Start fenced code block
            if (textBuffer.isNotBlank()) {
                segments.add(Segment.TextSegment(textBuffer.toString().trimEnd()))
            }
            textBuffer.clear()
            inFencedCode = true
            // Skip the opening ``` line (may have language hint after it)
            i++
            continue
        }

        if (inFencedCode) {
            if (trimmed.startsWith("```")) {
                // End fenced code block
                segments.add(Segment.CodeSegment(codeBuffer.toString().trimEnd()))
                codeBuffer.clear()
                inFencedCode = false
                i++
                continue
            }
            if (codeBuffer.isNotEmpty()) codeBuffer.append('\n')
            codeBuffer.append(line)
            i++
            continue
        }

        // 4-space / tab indented code blocks
        if (!inIndentCode && (line.startsWith("    ") || line.startsWith("\t")) && trimmed.isNotEmpty()) {
            // Check that previous line is blank or we're at start
            val prevLine = if (i > 0) lines[i - 1].trim() else ""
            if (prevLine.isEmpty() || i == 0) {
                if (textBuffer.isNotBlank()) {
                    segments.add(Segment.TextSegment(textBuffer.toString().trimEnd()))
                }
                textBuffer.clear()
                inIndentCode = true
                val stripped = if (line.startsWith("\t")) line.substring(1) else line.substring(4)
                codeBuffer.append(stripped)
                i++
                continue
            }
        }

        if (inIndentCode) {
            if ((line.startsWith("    ") || line.startsWith("\t")) && trimmed.isNotEmpty()) {
                codeBuffer.append('\n')
                val stripped =
                    if (line.startsWith("\t")) line.substring(1) else line.substring(4)
                codeBuffer.append(stripped)
                i++
                continue
            } else if (trimmed.isEmpty()) {
                // Blank line might continue the code block
                codeBuffer.append('\n')
                i++
                continue
            } else {
                // End indent code block
                segments.add(Segment.CodeSegment(codeBuffer.toString().trimEnd()))
                codeBuffer.clear()
                inIndentCode = false
                // Don't increment i; reprocess this line as text
                continue
            }
        }

        if (textBuffer.isNotEmpty()) textBuffer.append('\n')
        textBuffer.append(line)
        i++
    }

    // Flush remaining buffers
    if (inFencedCode || inIndentCode) {
        if (codeBuffer.isNotEmpty()) {
            segments.add(Segment.CodeSegment(codeBuffer.toString().trimEnd()))
        }
    }
    if (textBuffer.isNotBlank()) {
        segments.add(Segment.TextSegment(textBuffer.toString().trimEnd()))
    }

    return segments
}

// =============================================================================
// Image Extraction (from text segments)
// =============================================================================

/**
 * Image pattern matching: `img###(url)`, `![alt](url)`, `<img src="...">`.
 * The `###` in `img###(url)` may contain digits, `%`, or `px`.
 */
private val imgPattern = Regex(
    """img([a-zA-Z0-9%]*)\(([^)]+)\)|!\[([^\]]*)\]\(([^)]+)\)|<img\s+[^>]*src="([^"]*)"[^>]*/?>""",
    RegexOption.IGNORE_CASE
)

private fun splitImagesAndText(
    text: String,
    linkColor: Color,
    codeBackground: Color,
    spoilerColor: Color
): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    var currentIndex = 0

    imgPattern.findAll(text).forEach { match ->
        if (match.range.first > currentIndex) {
            val subText = text.substring(currentIndex, match.range.first).trim()
            if (subText.isNotBlank()) {
                blocks.add(
                    ContentBlock.Text(
                        buildTextAnnotatedString(subText, linkColor, codeBackground, spoilerColor)
                    )
                )
            }
        }

        val widthStr = match.groups[1]?.value ?: ""
        val isPercent = widthStr.contains('%')
        val widthNum = widthStr.replace(Regex("[^0-9]"), "").toIntOrNull()
        val url =
            match.groups[2]?.value ?: match.groups[4]?.value ?: match.groups[5]?.value ?: ""

        if (url.isNotBlank()) {
            blocks.add(ContentBlock.Image(url, widthNum, isPercent))
        }
        currentIndex = match.range.last + 1
    }

    if (currentIndex < text.length) {
        val subText = text.substring(currentIndex).trim()
        if (subText.isNotBlank()) {
            blocks.add(
                ContentBlock.Text(
                    buildTextAnnotatedString(subText, linkColor, codeBackground, spoilerColor)
                )
            )
        }
    }

    return blocks
}

// =============================================================================
// Parser Engine
// =============================================================================

private fun parseAniListMarkdownBlocks(
    raw: String,
    linkColor: Color,
    codeBackground: Color,
    spoilerColor: Color
): List<ContentBlock> {
    val preprocessed = preprocessHtml(raw)
    val segments = splitCodeBlocks(preprocessed)
    val blocks = mutableListOf<ContentBlock>()

    for (segment in segments) {
        when (segment) {
            is Segment.CodeSegment -> {
                if (segment.code.isNotBlank()) {
                    blocks.add(ContentBlock.Code(segment.code))
                }
            }

            is Segment.TextSegment -> {
                blocks.addAll(
                    splitImagesAndText(segment.text, linkColor, codeBackground, spoilerColor)
                )
            }
        }
    }

    return blocks
}

private fun buildTextAnnotatedString(
    text: String,
    linkColor: Color,
    codeBackground: Color,
    spoilerColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        parseMarkdown(text, linkColor, codeBackground, spoilerColor, isStartOfLine = true)
    }
}

/**
 * Recursive descent string-builder that handles both block-level (headers, blockquotes,
 * lists, horizontal rules) and inline-level (bold, italic, strikethrough, code, links,
 * spoilers, etc.) AniList-Flavored Markdown.
 */
private fun AnnotatedString.Builder.parseMarkdown(
    text: String,
    linkColor: Color,
    codeBackground: Color,
    spoilerColor: Color,
    isStartOfLine: Boolean
) {
    var i = 0
    var lineStart = isStartOfLine

    while (i < text.length) {

        // === BACKSLASH ESCAPING ===
        if (text[i] == '\\' && i + 1 < text.length) {
            val next = text[i + 1]
            if (next in ESCAPABLE_CHARS) {
                append(next)
                i += 2
                lineStart = false
                continue
            }
        }

        // === BLOCK LEVEL PARSING (at start of line) ===
        if (lineStart) {
            val spaceCount = text.drop(i).takeWhile { it == ' ' || it == '\t' }.length
            val idx = i + spaceCount

            // Horizontal rules: ---, ___, ***, - - -, * * *
            if (isHorizontalRule(text, idx)) {
                val endOfLine = text.indexOf('\n', idx)
                val eol = if (endOfLine == -1) text.length else endOfLine
                withStyle(SpanStyle(color = spoilerColor.copy(alpha = 0.5f))) {
                    append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }
                i = eol
                continue
            }

            // Center alignment: ~~~ (consumed as delimiter, content between ~~~ pairs rendered normally)
            if (text.startsWith("~~~", idx)) {
                val endOfLine = text.indexOf('\n', idx)
                val eol = if (endOfLine == -1) text.length else endOfLine
                val lineContent = text.substring(idx, eol).trim()
                if (lineContent == "~~~") {
                    i = eol
                    continue
                }
                // ~~~text~~~ on a single line — centered text
                if (lineContent.startsWith("~~~") && lineContent.endsWith("~~~") && lineContent.length > 6) {
                    val inner = lineContent.substring(3, lineContent.length - 3).trim()
                    if (inner.isNotEmpty()) {
                        parseMarkdown(inner, linkColor, codeBackground, spoilerColor, false)
                    }
                    i = eol
                    continue
                }
            }

            // Headers: # through #####
            var hashes = 0
            var hIdx = idx
            while (hIdx < text.length && text[hIdx] == '#') {
                hashes++; hIdx++
            }
            if (hashes in 1..5 && hIdx < text.length && text[hIdx] != '\n') {
                val contentStart = if (text[hIdx] == ' ') hIdx + 1 else hIdx
                val endOfLine = text.indexOf('\n', hIdx)
                val eol = if (endOfLine == -1) text.length else endOfLine
                withStyle(headerSpanStyle(hashes)) {
                    parseMarkdown(
                        text.substring(contentStart, eol),
                        linkColor, codeBackground, spoilerColor, false
                    )
                }
                i = eol
                continue
            }

            // Blockquotes: > (with nesting support: >>, >>>, > > etc.)
            if (idx < text.length && text[idx] == '>') {
                val endOfLine = text.indexOf('\n', idx)
                val eol = if (endOfLine == -1) text.length else endOfLine
                // Count nesting depth
                var depth = 0
                var qIdx = idx
                while (qIdx < eol) {
                    if (text[qIdx] == '>') {
                        depth++
                        qIdx++
                        if (qIdx < eol && text[qIdx] == ' ') qIdx++
                    } else {
                        break
                    }
                }
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = spoilerColor)) {
                    append("┃ ".repeat(depth))
                    parseMarkdown(
                        text.substring(qIdx, eol),
                        linkColor, codeBackground, spoilerColor, true
                    )
                }
                i = eol
                continue
            }

            // Unordered list (with sub-list indent support)
            val indentLevel = spaceCount / 2
            if (idx < text.length &&
                (text.startsWith("- ", idx) || text.startsWith("* ", idx) || text.startsWith(
                    "+ ",
                    idx
                ))
            ) {
                // Don't confuse ** (bold) or * followed by non-space with list items
                if (text[idx] == '*' && idx + 1 < text.length && text[idx + 1] != ' ') {
                    // Not a list item, fall through
                } else {
                    val indent = "  ".repeat(indentLevel)
                    append("$indent  \u2022 ")
                    i = idx + 2
                    lineStart = false
                    continue
                }
            }

            // Ordered list (with sub-list indent support)
            val digitMatch =
                Regex("^\\d+\\.\\s").find(text.substring(idx, minOf(idx + 10, text.length)))
            if (digitMatch != null) {
                val indent = "  ".repeat(indentLevel)
                append("$indent  ${digitMatch.value.trim()} ")
                i = idx + digitMatch.value.length
                lineStart = false
                continue
            }
        }

        // Newlines reset lineStart
        if (text[i] == '\n') {
            append('\n')
            lineStart = true
            i++
            continue
        } else {
            lineStart = false
        }

        // === INLINE LEVEL PARSING ===

        // Spoilers: ~! ... !~
        if (text.startsWith("~!", i)) {
            val end = text.indexOf("!~", i + 2)
            if (end != -1) {
                withStyle(
                    SpanStyle(
                        background = spoilerColor.copy(alpha = 0.8f),
                        color = spoilerColor.copy(alpha = 0.8f)
                    )
                ) {
                    parseMarkdown(
                        text.substring(i + 2, end),
                        linkColor, codeBackground, spoilerColor, false
                    )
                }
                i = end + 2
                continue
            }
        }

        // Center alignment inline: ~~~...~~~ (handles ~~~ inside HTML tags etc.)
        if (text.startsWith("~~~", i)) {
            val closingIdx = text.indexOf("~~~", i + 3)
            if (closingIdx != -1) {
                parseMarkdown(
                    text.substring(i + 3, closingIdx),
                    linkColor, codeBackground, spoilerColor, false
                )
                i = closingIdx + 3
                continue
            }
        }

        // Strikethrough: ~~ ... ~~
        if (text.startsWith("~~", i)) {
            val end = text.indexOf("~~", i + 2)
            if (end != -1) {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    parseMarkdown(
                        text.substring(i + 2, end),
                        linkColor, codeBackground, spoilerColor, false
                    )
                }
                i = end + 2
                continue
            }
        }

        // Bold: ** ** or __ __
        if (text.startsWith("**", i) || text.startsWith("__", i)) {
            val marker = text.substring(i, i + 2)
            val end = text.indexOf(marker, i + 2)
            if (end != -1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    parseMarkdown(
                        text.substring(i + 2, end),
                        linkColor, codeBackground, spoilerColor, false
                    )
                }
                i = end + 2
                continue
            }
        }

        // Italic: * * or _ _ (single marker, not part of ** or __)
        if ((text[i] == '*' || text[i] == '_') && (i == 0 || text[i - 1] != text[i])) {
            val marker = text[i].toString()
            val end = text.indexOf(marker, i + 1)
            if (end != -1 && end > i + 1 && (end + 1 == text.length || text[end + 1] != text[i])) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    parseMarkdown(
                        text.substring(i + 1, end),
                        linkColor, codeBackground, spoilerColor, false
                    )
                }
                i = end + 1
                continue
            }
        }

        // Inline Code: ` ` (no markdown parsing inside backtick code)
        if (text[i] == '`') {
            val end = text.indexOf('`', i + 1)
            if (end != -1) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground,
                        fontSize = 13.sp
                    )
                ) {
                    append(text.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }

        // YouTube / Webm embeds
        if (text.startsWith("youtube(", i, ignoreCase = true) || text.startsWith(
                "webm(",
                i,
                ignoreCase = true
            )
        ) {
            val isYoutube = text.startsWith("y", i, ignoreCase = true)
            val prefixLen = if (isYoutube) 8 else 5
            val end = text.indexOf(')', i + prefixLen)
            if (end != -1) {
                val content = text.substring(i + prefixLen, end)
                val url = if (isYoutube && !content.startsWith("http", ignoreCase = true))
                    "https://www.youtube.com/watch?v=$content" else content
                pushLink(
                    LinkAnnotation.Url(
                        url,
                        TextLinkStyles(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    )
                )
                append("\u25B6 Video")
                pop()
                i = end + 1
                continue
            }
        }

        // Markdown Links: [text](url)
        if (text[i] == '[') {
            var closeBracket = -1
            var nested = 0
            for (j in i + 1 until text.length) {
                if (text[j] == '[') nested++
                else if (text[j] == ']') {
                    if (nested == 0) {
                        closeBracket = j; break
                    } else nested--
                }
            }
            if (closeBracket != -1 && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') {
                val closeParen = text.indexOf(')', closeBracket + 2)
                if (closeParen != -1) {
                    val linkText = text.substring(i + 1, closeBracket)
                    val linkUrl = text.substring(closeBracket + 2, closeParen)
                    pushLink(
                        LinkAnnotation.Url(
                            linkUrl,
                            TextLinkStyles(
                                SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        )
                    )
                    parseMarkdown(linkText, linkColor, codeBackground, spoilerColor, false)
                    pop()
                    i = closeParen + 1
                    continue
                }
            }
        }

        // Auto-linking: <url> angle bracket syntax
        if (text[i] == '<' && i + 1 < text.length &&
            (text.startsWith("http://", i + 1) || text.startsWith("https://", i + 1))
        ) {
            val closeBracket = text.indexOf('>', i + 1)
            if (closeBracket != -1) {
                val url = text.substring(i + 1, closeBracket)
                pushLink(
                    LinkAnnotation.Url(
                        url,
                        TextLinkStyles(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    )
                )
                append(url)
                pop()
                i = closeBracket + 1
                continue
            }
        }

        // HTML Tags & Links
        if (text[i] == '<') {
            val tagEnd = text.indexOf('>', i)
            if (tagEnd != -1) {
                val tagLength = tagEnd - i + 1
                if (tagLength < 500) {
                    val tag = text.substring(i, tagEnd + 1)
                    val lowerTag = tag.lowercase()

                    // <a href="...">...</a> — link with href
                    if (lowerTag.startsWith("<a ") && lowerTag.contains("href=")) {
                        val hrefMatch = Regex("""href="([^"]*)"""").find(tag)
                        val endA = text.indexOf("</a>", tagEnd + 1, ignoreCase = true)
                        if (hrefMatch != null && endA != -1) {
                            val url = hrefMatch.groupValues[1]
                            pushLink(
                                LinkAnnotation.Url(
                                    url,
                                    TextLinkStyles(
                                        SpanStyle(
                                            color = linkColor,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    )
                                )
                            )
                            parseMarkdown(
                                text.substring(tagEnd + 1, endA),
                                linkColor, codeBackground, spoilerColor, false
                            )
                            pop()
                            i = endA + 4
                            continue
                        } else {
                            // Orphaned <a> tag (e.g. wrapping an extracted image) — skip it
                            i = tagEnd + 1
                            continue
                        }
                    }

                    // <a>text</a> — colored text (no href)
                    if (lowerTag == "<a>" || (lowerTag.startsWith("<a") && !lowerTag.contains("href"))) {
                        val endA = text.indexOf("</a>", tagEnd + 1, ignoreCase = true)
                        if (endA != -1) {
                            withStyle(SpanStyle(color = linkColor)) {
                                parseMarkdown(
                                    text.substring(tagEnd + 1, endA),
                                    linkColor, codeBackground, spoilerColor, false
                                )
                            }
                            i = endA + 4
                            continue
                        }
                    }

                    // <code>...</code> — inline code with markdown parsing inside
                    if (lowerTag == "<code>") {
                        val endCode = text.indexOf("</code>", tagEnd + 1, ignoreCase = true)
                        if (endCode != -1) {
                            withStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    background = codeBackground,
                                    fontSize = 13.sp
                                )
                            ) {
                                parseMarkdown(
                                    text.substring(tagEnd + 1, endCode),
                                    linkColor, codeBackground, spoilerColor, false
                                )
                            }
                            i = endCode + 7 // "</code>".length == 7
                            continue
                        }
                    }

                    // HTML formatting tags: <b>, <strong>, <i>, <em>, <del>, <strike>
                    val resolved = resolveHtmlFormattingTag(lowerTag)
                    if (resolved != null) {
                        val (spanStyle, endTag, altEndTag) = resolved
                        val closeIdx = text.indexOf(endTag, tagEnd + 1, ignoreCase = true)
                        val altCloseIdx = if (altEndTag != null) text.indexOf(
                            altEndTag,
                            tagEnd + 1,
                            ignoreCase = true
                        ) else -1

                        // Use whichever close tag appears first (or whichever is found)
                        val actualClose: Int
                        val endTagLen: Int
                        if (closeIdx != -1 && (altCloseIdx == -1 || closeIdx <= altCloseIdx)) {
                            actualClose = closeIdx
                            endTagLen = endTag.length
                        } else if (altCloseIdx != -1) {
                            actualClose = altCloseIdx
                            endTagLen = altEndTag?.length ?: 0
                        } else {
                            actualClose = -1
                            endTagLen = 0
                        }

                        if (actualClose != -1) {
                            withStyle(spanStyle) {
                                parseMarkdown(
                                    text.substring(tagEnd + 1, actualClose),
                                    linkColor, codeBackground, spoilerColor, false
                                )
                            }
                            i = actualClose + endTagLen
                            continue
                        }
                    }

                    // HTML Header tags: <h1> through <h5>
                    if (lowerTag.matches(Regex("^<h[1-5]>$"))) {
                        val level = lowerTag[2].digitToInt()
                        val closeTag = "</h$level>"
                        val closeIdx = text.indexOf(closeTag, tagEnd + 1, ignoreCase = true)
                        if (closeIdx != -1) {
                            withStyle(headerSpanStyle(level)) {
                                parseMarkdown(
                                    text.substring(tagEnd + 1, closeIdx),
                                    linkColor, codeBackground, spoilerColor, false
                                )
                            }
                            i = closeIdx + closeTag.length
                            continue
                        }
                    }

                    // Bypass unknown HTML tags (but be lenient — don't swallow large chunks)
                    if (tagLength < 30 && (lowerTag.length > 1 && (lowerTag[1].isLetter() || lowerTag[1] == '/'))) {
                        i = tagEnd + 1
                        continue
                    }
                }
            }
        }

        // Auto-linking bare URLs: https://... or http://...
        if ((text.startsWith("https://", i) || text.startsWith("http://", i)) &&
            (i == 0 || !text[i - 1].isLetterOrDigit())
        ) {
            val urlEnd = findUrlEnd(text, i)
            if (urlEnd > i) {
                val url = text.substring(i, urlEnd)
                pushLink(
                    LinkAnnotation.Url(
                        url,
                        TextLinkStyles(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    )
                )
                append(url)
                pop()
                i = urlEnd
                continue
            }
        }

        // HTML Entities
        if (text.startsWith("&", i)) {
            val semi = text.indexOf(';', i)
            if (semi != -1 && semi - i < 12) {
                val entity = text.substring(i, semi + 1).lowercase()
                val resolved = resolveHtmlEntity(entity)
                if (resolved != null) {
                    append(resolved)
                    i = semi + 1
                    continue
                }
                // Numeric entities: &#123; or &#x1F;
                if (entity.startsWith("&#")) {
                    val num = entity.substring(2, entity.length - 1)
                    val code = if (num.startsWith("x", ignoreCase = true))
                        num.substring(1).toIntOrNull(16) else num.toIntOrNull()
                    if (code != null) {
                        try {
                            append(String(Character.toChars(code)))
                        } catch (_: Exception) { /* Malformed — ignore */
                        }
                        i = semi + 1
                        continue
                    }
                }
            }
        }

        // Normal text fallback
        append(text[i])
        i++
    }
}

// =============================================================================
// Helper: Horizontal Rule Detection
// =============================================================================

/**
 * Checks whether the text starting at [idx] is a horizontal rule line.
 * Recognized patterns: `---`, `___`, `***`, `- - -`, `* * *`
 */
private fun isHorizontalRule(text: String, idx: Int): Boolean {
    val endOfLine = text.indexOf('\n', idx)
    val eol = if (endOfLine == -1) text.length else endOfLine
    if (idx >= eol) return false
    val content = text.substring(idx, eol).trim()
    if (content.length < 3) return false

    // All same character: ---, ___, ***
    if (content.all { it == '-' } || content.all { it == '_' } || content.all { it == '*' }) {
        return true
    }
    // Spaced variants: - - -, * * *
    val stripped = content.replace(" ", "")
    if (stripped.length >= 3 && (stripped.all { it == '-' } || stripped.all { it == '*' })) {
        return true
    }
    return false
}

// =============================================================================
// Helper: HTML Formatting Tag Resolution
// =============================================================================

/**
 * Returns (SpanStyle, primaryEndTag, alternateEndTag) for recognized HTML formatting tags.
 * Fix for bug #1: each open tag maps to its canonical close tag as primary,
 * with the alternate synonym as fallback.
 */
private data class HtmlTagInfo(
    val style: SpanStyle,
    val endTag: String,
    val altEndTag: String?
)

private fun resolveHtmlFormattingTag(lowerTag: String): HtmlTagInfo? {
    return when (lowerTag) {
        "<b>" -> HtmlTagInfo(
            SpanStyle(fontWeight = FontWeight.Bold),
            "</b>", "</strong>"
        )

        "<strong>" -> HtmlTagInfo(
            SpanStyle(fontWeight = FontWeight.Bold),
            "</strong>", "</b>"
        )

        "<i>" -> HtmlTagInfo(
            SpanStyle(fontStyle = FontStyle.Italic),
            "</i>", "</em>"
        )

        "<em>" -> HtmlTagInfo(
            SpanStyle(fontStyle = FontStyle.Italic),
            "</em>", "</i>"
        )

        "<del>" -> HtmlTagInfo(
            SpanStyle(textDecoration = TextDecoration.LineThrough),
            "</del>", "</strike>"
        )

        "<strike>" -> HtmlTagInfo(
            SpanStyle(textDecoration = TextDecoration.LineThrough),
            "</strike>", "</del>"
        )

        else -> null
    }
}

// =============================================================================
// Helper: URL End Detection
// =============================================================================

/**
 * Given a URL starting at [start] in [text], find where the URL ends.
 * URLs end at whitespace, or certain punctuation that isn't typically part of URLs.
 */
private fun findUrlEnd(text: String, start: Int): Int {
    var i = start
    var parenDepth = 0
    while (i < text.length) {
        val c = text[i]
        if (c.isWhitespace()) break
        if (c == '(') parenDepth++
        if (c == ')') {
            if (parenDepth > 0) parenDepth--
            else break
        }
        // Stop at common sentence-ending punctuation not part of URLs
        if (c in listOf(',', ';', '"', '\'', '<', '>') && i > start) break
        i++
    }
    // Trim trailing periods/colons that are likely sentence punctuation
    while (i > start && text[i - 1] in listOf('.', ':', '!', '?')) {
        i--
    }
    return i
}

// =============================================================================
// Helper: HTML Entity Resolution
// =============================================================================

/**
 * Expanded HTML entity table covering the AniList spec and common entities.
 */
private fun resolveHtmlEntity(entity: String): String? {
    return HTML_ENTITIES[entity]
}

private val HTML_ENTITIES = mapOf(
    // Standard
    "&amp;" to "&",
    "&lt;" to "<",
    "&gt;" to ">",
    "&quot;" to "\"",
    "&nbsp;" to "\u00A0",
    "&apos;" to "'",

    // Punctuation & symbols
    "&excl;" to "!",
    "&num;" to "#",
    "&dollar;" to "$",
    "&percnt;" to "%",
    "&lpar;" to "(",
    "&rpar;" to ")",
    "&ast;" to "*",
    "&plus;" to "+",
    "&comma;" to ",",
    "&hyphen;" to "-",
    "&minus;" to "\u2212",
    "&period;" to ".",
    "&sol;" to "/",
    "&colon;" to ":",
    "&semi;" to ";",
    "&equals;" to "=",
    "&quest;" to "?",
    "&commat;" to "@",
    "&lsqb;" to "[",
    "&rsqb;" to "]",
    "&lbrack;" to "[",
    "&rbrack;" to "]",
    "&bsol;" to "\\",
    "&Hat;" to "^",  // caret
    "&lowbar;" to "_",
    "&grave;" to "`",
    "&lbrace;" to "{",
    "&rbrace;" to "}",
    "&lcub;" to "{",
    "&rcub;" to "}",
    "&vert;" to "|",
    "&verbar;" to "|",
    "&tilde;" to "~",

    // Typographic
    "&ndash;" to "\u2013",
    "&mdash;" to "\u2014",
    "&lsquo;" to "\u2018",
    "&rsquo;" to "\u2019",
    "&sbquo;" to "\u201A",
    "&ldquo;" to "\u201C",
    "&rdquo;" to "\u201D",
    "&bdquo;" to "\u201E",
    "&hellip;" to "\u2026",
    "&bull;" to "\u2022",
    "&middot;" to "\u00B7",
    "&prime;" to "\u2032",
    "&Prime;" to "\u2033",
    "&laquo;" to "\u00AB",
    "&raquo;" to "\u00BB",
    "&lsaquo;" to "\u2039",
    "&rsaquo;" to "\u203A",

    // Currency
    "&cent;" to "\u00A2",
    "&pound;" to "\u00A3",
    "&yen;" to "\u00A5",
    "&euro;" to "\u20AC",
    "&curren;" to "\u00A4",

    // Math & technical
    "&times;" to "\u00D7",
    "&divide;" to "\u00F7",
    "&plusmn;" to "\u00B1",
    "&ne;" to "\u2260",
    "&le;" to "\u2264",
    "&ge;" to "\u2265",
    "&infin;" to "\u221E",
    "&deg;" to "\u00B0",
    "&micro;" to "\u00B5",
    "&frac12;" to "\u00BD",
    "&frac14;" to "\u00BC",
    "&frac34;" to "\u00BE",
    "&sup1;" to "\u00B9",
    "&sup2;" to "\u00B2",
    "&sup3;" to "\u00B3",
    "&fnof;" to "\u0192",

    // Arrows
    "&larr;" to "\u2190",
    "&uarr;" to "\u2191",
    "&rarr;" to "\u2192",
    "&darr;" to "\u2193",
    "&harr;" to "\u2194",

    // Misc symbols
    "&copy;" to "\u00A9",
    "&reg;" to "\u00AE",
    "&trade;" to "\u2122",
    "&para;" to "\u00B6",
    "&sect;" to "\u00A7",
    "&dagger;" to "\u2020",
    "&Dagger;" to "\u2021",
    "&hearts;" to "\u2665",
    "&diams;" to "\u2666",
    "&spades;" to "\u2660",
    "&clubs;" to "\u2663",
    "&check;" to "\u2713",
    "&cross;" to "\u2717",
    "&star;" to "\u2606",
    "&starf;" to "\u2605",

    // Special whitespace
    "&ensp;" to "\u2002",
    "&emsp;" to "\u2003",
    "&thinsp;" to "\u2009",
    "&zwnj;" to "\u200C",
    "&zwj;" to "\u200D",

    // Latin extended (common accented chars)
    "&iexcl;" to "\u00A1",
    "&iquest;" to "\u00BF",
    "&ordf;" to "\u00AA",
    "&ordm;" to "\u00BA",
    "&not;" to "\u00AC",
    "&shy;" to "\u00AD",
    "&macr;" to "\u00AF",
    "&acute;" to "\u00B4",
    "&cedil;" to "\u00B8",

    // Greek letters (commonly used)
    "&alpha;" to "\u03B1",
    "&beta;" to "\u03B2",
    "&gamma;" to "\u03B3",
    "&delta;" to "\u03B4",
    "&epsilon;" to "\u03B5",
    "&theta;" to "\u03B8",
    "&lambda;" to "\u03BB",
    "&mu;" to "\u03BC",
    "&pi;" to "\u03C0",
    "&sigma;" to "\u03C3",
    "&omega;" to "\u03C9",

    // Braces & brackets
    "&lang;" to "\u27E8",
    "&rang;" to "\u27E9",
)

// =============================================================================
// Escapable Characters
// =============================================================================

private val ESCAPABLE_CHARS = setOf(
    '\\', '`', '*', '_', '{', '}', '[', ']', '(', ')',
    '#', '+', '-', '.', '!', '|', '~', '>', '<', '&'
)

// =============================================================================
// Alignment Wrapper Stripping
// =============================================================================

private fun stripAlignmentWrappers(text: String): String {
    var result = text
    for (prefix in listOf("center", "left", "right")) {
        val pattern = Regex("${prefix}\\((.*?)\\)", RegexOption.DOT_MATCHES_ALL)
        result = pattern.replace(result) { it.groupValues[1] }
    }
    return result
}

// =============================================================================
// Header Styling
// =============================================================================

private fun headerSpanStyle(level: Int): SpanStyle = when (level) {
    1 -> SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    2 -> SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
    3 -> SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
    4 -> SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    5 -> SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    else -> SpanStyle(fontWeight = FontWeight.Bold)
}
