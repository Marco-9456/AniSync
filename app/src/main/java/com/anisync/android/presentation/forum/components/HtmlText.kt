package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
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
 * Renders AniList-Flavored Markdown as styled text with inline images.
 *
 * Content is split into blocks:
 * - **Text blocks** rendered via [ClickableText] with styled [AnnotatedString]
 * - **Image blocks** rendered via Coil [AsyncImage]
 *
 * Supported syntax:
 * - `# Header` through `##### Header 5` (scaled font sizes)
 * - `**bold**` / `__bold__`, `*italic*` / `_italic_`, `~~strikethrough~~`
 * - `~!spoiler text!~` (hidden behind matching-color overlay)
 * - `[text](url)` links, `![alt](url)` markdown images
 * - `img###(url)` AniList-specific sized images
 * - `youtube(id/url)` / `webm(url)` → "[video]" link
 * - `> blockquote`, `- list item`, `1. ordered list`
 * - `` `inline code` ``, `---`/`___` horizontal rule, `~~~` (stripped)
 * - HTML tags: `<b>`, `<i>`, `<a>`, `<img>`, `<h1>`–`<h5>`, etc.
 * - HTML entities: named and numeric
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
    val uriHandler = LocalUriHandler.current
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
                    ClickableText(
                        text = block.annotatedString,
                        style = style.copy(color = color),
                        onClick = { offset ->
                            block.annotatedString.getStringAnnotations("URL", offset, offset)
                                .firstOrNull()
                                ?.let { annotation ->
                                    try { uriHandler.openUri(annotation.item) } catch (_: Exception) { }
                                }
                        }
                    )
                }

                is ContentBlock.Image -> {
                    // Collect consecutive images into a row
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
                                val widthDp = img.width?.dp ?: 160.dp
                                AsyncImage(
                                    model = img.url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .widthIn(max = widthDp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { fullscreenImageUrl = img.url }
                                )
                            }
                        }
                    } else {
                        val img = imageGroup.first()
                        AsyncImage(
                            model = img.url,
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { fullscreenImageUrl = img.url }
                        )
                    }
                }
            }
            i++
        }
    }

    // Fullscreen image viewer dialog
    fullscreenImageUrl?.let { imageUrl ->
        FullscreenImageViewer(
            imageUrl = imageUrl,
            onDismiss = { fullscreenImageUrl = null }
        )
    }
}

/**
 * Fullscreen image viewer with pinch-to-zoom and a close button.
 */
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

            // Close button
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
// Content block model
// =============================================================================

private sealed class ContentBlock {
    data class Text(val annotatedString: AnnotatedString) : ContentBlock()
    data class Image(val url: String, val width: Int? = null) : ContentBlock()
}

// =============================================================================
// Parser: AniList Markdown → List<ContentBlock>
// =============================================================================

/**
 * Converts AniList-Flavored Markdown to a list of [ContentBlock]s.
 * Images are extracted as separate [ContentBlock.Image] blocks;
 * everything else is rendered as styled [ContentBlock.Text].
 */
private fun parseAniListMarkdownBlocks(
    raw: String,
    linkColor: Color,
    codeBackground: Color,
    spoilerColor: Color
): List<ContentBlock> {
    // Pre-process
    var text = raw
    text = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
    text = text.replace(Regex("</?p>", RegexOption.IGNORE_CASE), "\n")
    text = text.replace(Regex("</?div>", RegexOption.IGNORE_CASE), "\n")
    text = text.replace(Regex("</?center>", RegexOption.IGNORE_CASE), "")
    text = stripAlignmentWrappers(text)
    text = text.replace(Regex("\n{3,}"), "\n\n")

    val blocks = mutableListOf<ContentBlock>()
    val textBuffer = StringBuilder()

    fun flushText() {
        val buffered = textBuffer.toString().trimEnd('\n')
        if (buffered.isNotBlank()) {
            val annotated = buildTextAnnotatedString(buffered, linkColor, codeBackground, spoilerColor)
            blocks.add(ContentBlock.Text(annotated))
        }
        textBuffer.clear()
    }

    val lines = text.split("\n")
    for ((lineIndex, rawLine) in lines.withIndex()) {
        val line = rawLine.trimEnd()

        // Extract images that appear on the line before building text blocks
        val imageResults = extractImages(line)

        if (imageResults.images.isNotEmpty()) {
            // Flush any text before the images
            val remainingText = imageResults.remainingText.trim()
            if (remainingText.isNotEmpty()) {
                textBuffer.append(remainingText)
                if (lineIndex < lines.lastIndex) textBuffer.append("\n")
                flushText()
            } else if (textBuffer.isNotEmpty()) {
                flushText()
            }
            // Add images as separate blocks
            imageResults.images.forEach { blocks.add(it) }
        } else {
            // Regular text line
            textBuffer.append(line)
            if (lineIndex < lines.lastIndex) textBuffer.append("\n")
        }
    }

    flushText()
    return blocks
}

/**
 * Data class to hold the result of image extraction from a line.
 */
private data class ImageExtractionResult(
    val images: List<ContentBlock.Image>,
    val remainingText: String
)

/**
 * Extracts images from a line of text. Returns the list of image blocks
 * and the remaining text (with image syntax removed).
 */
private fun extractImages(line: String): ImageExtractionResult {
    val images = mutableListOf<ContentBlock.Image>()
    var remaining = line

    // img###(url)
    val imgPattern = Regex("""img(\d*)\(([^)]+)\)""")
    imgPattern.findAll(remaining).toList().reversed().forEach { match ->
        val width = match.groupValues[1].toIntOrNull()
        val url = match.groupValues[2]
        images.add(0, ContentBlock.Image(url, width))
        remaining = remaining.removeRange(match.range)
    }

    // ![alt](url) markdown images
    val mdImgPattern = Regex("""!\[([^\]]*)\]\(([^)]+)\)""")
    mdImgPattern.findAll(remaining).toList().reversed().forEach { match ->
        val url = match.groupValues[2]
        images.add(0, ContentBlock.Image(url))
        remaining = remaining.removeRange(match.range)
    }

    // <img src="url" ...> HTML images
    val htmlImgPattern = Regex("""<img\s+[^>]*src="([^"]*)"[^>]*/?>""", RegexOption.IGNORE_CASE)
    htmlImgPattern.findAll(remaining).toList().reversed().forEach { match ->
        val url = match.groupValues[1]
        if (url.isNotEmpty()) {
            images.add(0, ContentBlock.Image(url))
        }
        remaining = remaining.removeRange(match.range)
    }

    return ImageExtractionResult(images, remaining)
}

// =============================================================================
// Text-only AnnotatedString builder (no images — those are handled above)
// =============================================================================

/**
 * Builds a styled [AnnotatedString] for text content (images excluded).
 */
private fun buildTextAnnotatedString(
    text: String,
    linkColor: Color,
    codeBackground: Color,
    spoilerColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        for ((lineIndex, rawLine) in lines.withIndex()) {
            val line = rawLine.trimEnd()

            when {
                // ~~~ lines — decorative wrappers, invisible
                line.matches(Regex("^\\s*~~~\\s*$")) -> { }

                // Horizontal rule
                line.matches(Regex("^\\s*[-_]{3,}\\s*$")) -> {
                    withStyle(SpanStyle(color = spoilerColor.copy(alpha = 0.5f))) {
                        append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    }
                }

                // Headers: # through #####
                line.matches(Regex("^#{1,5}\\s+.+")) -> {
                    val hashes = line.takeWhile { it == '#' }.length
                    val headerText = line.drop(hashes).trimStart()
                    val headerStyle = headerSpanStyle(hashes)
                    withStyle(headerStyle) {
                        parseInlineMarkdown(headerText, linkColor, codeBackground, spoilerColor)
                    }
                }

                // Unordered list items
                line.matches(Regex("^\\s*[-*+]\\s+.+")) -> {
                    val content = line.replace(Regex("^\\s*[-*+]\\s+"), "")
                    append("  • ")
                    parseInlineMarkdown(content, linkColor, codeBackground, spoilerColor)
                }

                // Ordered list items
                line.matches(Regex("^\\s*\\d+\\.\\s+.+")) -> {
                    val match = Regex("^\\s*(\\d+)\\.\\s+(.+)").find(line)
                    if (match != null) {
                        val (num, content) = match.destructured
                        append("  $num. ")
                        parseInlineMarkdown(content, linkColor, codeBackground, spoilerColor)
                    } else {
                        parseInlineMarkdown(line, linkColor, codeBackground, spoilerColor)
                    }
                }

                // Blockquote
                line.startsWith(">") -> {
                    val content = line.removePrefix(">").trimStart()
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = spoilerColor)) {
                        append("┃ ")
                        parseInlineMarkdown(content, linkColor, codeBackground, spoilerColor)
                    }
                }

                // HTML headers
                line.matches(Regex(".*<h[1-5]>.*</h[1-5]>.*", RegexOption.IGNORE_CASE)) -> {
                    val htmlMatch = Regex("<h([1-5])>(.*?)</h\\1>", RegexOption.IGNORE_CASE).find(line)
                    if (htmlMatch != null) {
                        val level = htmlMatch.groupValues[1].toInt()
                        val content = htmlMatch.groupValues[2]
                        withStyle(headerSpanStyle(level)) {
                            parseInlineMarkdown(content, linkColor, codeBackground, spoilerColor)
                        }
                    } else {
                        parseInlineMarkdown(line, linkColor, codeBackground, spoilerColor)
                    }
                }

                // Regular line
                else -> {
                    parseInlineMarkdown(line, linkColor, codeBackground, spoilerColor)
                }
            }

            if (lineIndex < lines.lastIndex) append("\n")
        }
    }
}

// =============================================================================
// Helpers
// =============================================================================

private fun stripAlignmentWrappers(text: String): String {
    var result = text
    for (prefix in listOf("center", "left", "right")) {
        val pattern = Regex("(?s)${prefix}\\((.*?)\\)", RegexOption.DOT_MATCHES_ALL)
        result = pattern.replace(result) { it.groupValues[1] }
    }
    return result
}

private fun headerSpanStyle(level: Int): SpanStyle = when (level) {
    1 -> SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    2 -> SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
    3 -> SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
    4 -> SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    5 -> SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    else -> SpanStyle(fontWeight = FontWeight.Bold)
}

/**
 * Parses inline markdown elements within a single logical line.
 */
private fun AnnotatedString.Builder.parseInlineMarkdown(
    text: String,
    linkColor: Color,
    codeBackground: Color,
    spoilerColor: Color
) {
    var i = 0
    while (i < text.length) {
        when {
            // ~!spoiler!~
            text.startsWith("~!", i) -> {
                val end = text.indexOf("!~", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(
                        background = spoilerColor.copy(alpha = 0.8f),
                        color = spoilerColor.copy(alpha = 0.8f)
                    )) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else { append(text[i]); i++ }
            }

            // img###(url) — already extracted at block level, but handle leftover inline
            text.startsWith("img", i) && i + 3 < text.length && (text[i + 3].isDigit() || text[i + 3] == '(') -> {
                val parenStart = text.indexOf('(', i)
                val parenEnd = if (parenStart != -1) text.indexOf(')', parenStart + 1) else -1
                if (parenStart != -1 && parenEnd != -1) {
                    i = parenEnd + 1 // skip — images rendered at block level
                } else { append(text[i]); i++ }
            }

            // youtube(id/url) → [video]
            text.startsWith("youtube(", i) -> {
                val end = text.indexOf(')', i + 8)
                if (end != -1) {
                    val content = text.substring(i + 8, end)
                    val url = if (content.startsWith("http")) content
                              else "https://www.youtube.com/watch?v=$content"
                    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                        pushStringAnnotation("URL", url)
                        append("▶ Video")
                        pop()
                    }
                    i = end + 1
                } else { append(text[i]); i++ }
            }

            // webm(url) → [video]
            text.startsWith("webm(", i) -> {
                val end = text.indexOf(')', i + 5)
                if (end != -1) {
                    val url = text.substring(i + 5, end)
                    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                        pushStringAnnotation("URL", url)
                        append("▶ Video")
                        pop()
                    }
                    i = end + 1
                } else { append(text[i]); i++ }
            }

            // ![alt](url) — already extracted at block level, skip leftovers
            text.startsWith("![", i) -> {
                val closeBracket = text.indexOf(']', i + 2)
                if (closeBracket != -1 && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        i = closeParen + 1 // skip — image rendered at block level
                    } else { append(text[i]); i++ }
                } else { append(text[i]); i++ }
            }

            // [text](url) links
            text[i] == '[' -> {
                val closeBracket = text.indexOf(']', i + 1)
                if (closeBracket != -1 && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val linkText = text.substring(i + 1, closeBracket)
                        val linkUrl = text.substring(closeBracket + 2, closeParen)
                        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                            pushStringAnnotation("URL", linkUrl)
                            append(linkText)
                            pop()
                        }
                        i = closeParen + 1
                    } else { append(text[i]); i++ }
                } else { append(text[i]); i++ }
            }

            // <a href="url">text</a>
            text.startsWith("<a ", i) -> {
                val hrefMatch = Regex("""<a\s+href="([^"]*)"[^>]*>(.*?)</a>""", RegexOption.IGNORE_CASE)
                    .find(text, i)
                if (hrefMatch != null && hrefMatch.range.first == i) {
                    val url = hrefMatch.groupValues[1]
                    val linkText = hrefMatch.groupValues[2]
                    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                        pushStringAnnotation("URL", url)
                        append(linkText)
                        pop()
                    }
                    i = hrefMatch.range.last + 1
                } else { append(text[i]); i++ }
            }

            // <b> / <strong>
            text.startsWith("<b>", i) || text.startsWith("<strong>", i) -> {
                val tag = if (text.startsWith("<b>", i)) "b" else "strong"
                val endTag = "</$tag>"
                val end = text.indexOf(endTag, i, ignoreCase = true)
                val start = i + tag.length + 2
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        parseInlineMarkdown(text.substring(start, end), linkColor, codeBackground, spoilerColor)
                    }
                    i = end + endTag.length
                } else { append(text[i]); i++ }
            }

            // <i> / <em>
            text.startsWith("<i>", i) || text.startsWith("<em>", i) -> {
                val tag = if (text.startsWith("<i>", i)) "i" else "em"
                val endTag = "</$tag>"
                val end = text.indexOf(endTag, i, ignoreCase = true)
                val start = i + tag.length + 2
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        parseInlineMarkdown(text.substring(start, end), linkColor, codeBackground, spoilerColor)
                    }
                    i = end + endTag.length
                } else { append(text[i]); i++ }
            }

            // <del> / <strike>
            text.startsWith("<del>", i) || text.startsWith("<strike>", i) -> {
                val tag = if (text.startsWith("<del>", i)) "del" else "strike"
                val endTag = "</$tag>"
                val end = text.indexOf(endTag, i, ignoreCase = true)
                val start = i + tag.length + 2
                if (end != -1) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        parseInlineMarkdown(text.substring(start, end), linkColor, codeBackground, spoilerColor)
                    }
                    i = end + endTag.length
                } else { append(text[i]); i++ }
            }

            // <img> tags — already extracted at block level, skip
            text.startsWith("<img", i) -> {
                val end = text.indexOf('>', i)
                if (end != -1) { i = end + 1 } else { append(text[i]); i++ }
            }

            // Skip unrecognized HTML tags
            text[i] == '<' && i + 1 < text.length && (text[i + 1].isLetter() || text[i + 1] == '/') -> {
                val end = text.indexOf('>', i)
                if (end != -1) { i = end + 1 } else { append(text[i]); i++ }
            }

            // Inline code: `code`
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end != -1 && end > i + 1) {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground,
                        fontSize = 13.sp
                    )) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else { append(text[i]); i++ }
            }

            // **bold** / __bold__
            text.startsWith("**", i) || text.startsWith("__", i) -> {
                val marker = text.substring(i, i + 2)
                val end = text.indexOf(marker, i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        parseInlineMarkdown(text.substring(i + 2, end), linkColor, codeBackground, spoilerColor)
                    }
                    i = end + 2
                } else { append(text[i]); i++ }
            }

            // ~~strikethrough~~
            text.startsWith("~~", i) -> {
                val end = text.indexOf("~~", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        parseInlineMarkdown(text.substring(i + 2, end), linkColor, codeBackground, spoilerColor)
                    }
                    i = end + 2
                } else { append(text[i]); i++ }
            }

            // *italic* / _italic_ (single char, not preceding double)
            (text[i] == '*' || text[i] == '_') && (i == 0 || text[i - 1] != text[i]) -> {
                val marker = text[i]
                val end = text.indexOf(marker, i + 1)
                if (end != -1 && end > i + 1 && i + 1 < text.length && text[i + 1] != marker) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        parseInlineMarkdown(text.substring(i + 1, end), linkColor, codeBackground, spoilerColor)
                    }
                    i = end + 1
                } else { append(text[i]); i++ }
            }

            // Named HTML entities
            text.startsWith("&amp;", i) -> { append("&"); i += 5 }
            text.startsWith("&lt;", i) -> { append("<"); i += 4 }
            text.startsWith("&gt;", i) -> { append(">"); i += 4 }
            text.startsWith("&quot;", i) -> { append("\""); i += 6 }
            text.startsWith("&nbsp;", i) -> { append(" "); i += 6 }
            text.startsWith("&apos;", i) -> { append("'"); i += 6 }

            // Numeric HTML entities
            text.startsWith("&#", i) -> {
                val semiColon = text.indexOf(';', i + 2)
                if (semiColon != -1 && semiColon - i < 12) {
                    val entityBody = text.substring(i + 2, semiColon)
                    val codePoint = if (entityBody.startsWith("x", ignoreCase = true))
                        entityBody.substring(1).toIntOrNull(16) else entityBody.toIntOrNull()
                    if (codePoint != null) {
                        append(String(Character.toChars(codePoint)))
                        i = semiColon + 1
                    } else { append(text[i]); i++ }
                } else { append(text[i]); i++ }
            }

            else -> { append(text[i]); i++ }
        }
    }
}
