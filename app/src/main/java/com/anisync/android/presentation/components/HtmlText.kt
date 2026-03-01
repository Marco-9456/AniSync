package com.anisync.android.presentation.components

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
 * Renders AniList-Flavored Markdown as styled text with inline images.
 *
 * Content is split into blocks:
 * - **Text blocks** rendered via [Text] with styled [AnnotatedString]
 * - **Image blocks** rendered via Coil [AsyncImage]
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
    data class Image(val url: String, val width: Int? = null) : ContentBlock()
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
    var text = raw
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</?p>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</?div>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</?center>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\n{3,}"), "\n\n")

    text = stripAlignmentWrappers(text)

    val blocks = mutableListOf<ContentBlock>()

    // Updated image matching to capture formatting like img220%(...) or img100px(...)
    val imgPattern = Regex(
        """img([a-zA-Z0-9%]*)\(([^)]+)\)|!\[([^\]]*)\]\(([^)]+)\)|<img\s+[^>]*src="([^"]*)"[^>]*/?>""",
        RegexOption.IGNORE_CASE
    )

    var currentIndex = 0
    imgPattern.findAll(text).forEach { match ->
        if (match.range.first > currentIndex) {
            val subText = text.substring(currentIndex, match.range.first).trim()
            if (subText.isNotBlank()) {
                blocks.add(
                    ContentBlock.Text(
                        buildTextAnnotatedString(
                            subText,
                            linkColor,
                            codeBackground,
                            spoilerColor
                        )
                    )
                )
            }
        }

        // Strip % and px from AniList img sizes to satisfy Coil
        val widthStr = match.groups[1]?.value
        val width = widthStr?.replace(Regex("[^0-9]"), "")?.toIntOrNull()
        val url = match.groups[2]?.value ?: match.groups[4]?.value ?: match.groups[5]?.value ?: ""

        if (url.isNotBlank()) {
            blocks.add(ContentBlock.Image(url, width))
        }
        currentIndex = match.range.last + 1
    }

    if (currentIndex < text.length) {
        val subText = text.substring(currentIndex).trim()
        if (subText.isNotBlank()) {
            blocks.add(
                ContentBlock.Text(
                    buildTextAnnotatedString(
                        subText,
                        linkColor,
                        codeBackground,
                        spoilerColor
                    )
                )
            )
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
 * Recursive descent string-builder. Ensures that formats like `**` or `<strike>`
 * cannot "bleed over" infinitely, as they must have a closed match found ahead.
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
        // --- BLOCK LEVEL PARSING (Only applies if at start of a line) ---
        if (lineStart) {
            val spaceCount = text.drop(i).takeWhile { it == ' ' || it == '\t' }.length
            val idx = i + spaceCount

            // Horizontal rules
            if (text.startsWith("---", idx) || text.startsWith("___", idx)) {
                val endOfLine = text.indexOf('\n', idx)
                val eol = if (endOfLine == -1) text.length else endOfLine
                val content = text.substring(idx, eol).trim()
                if (content.all { it == '-' } || content.all { it == '_' }) {
                    withStyle(SpanStyle(color = spoilerColor.copy(alpha = 0.5f))) {
                        append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    }
                    i = eol
                    continue
                }
            }

            // Decorative ~~~
            if (text.startsWith("~~~", idx)) {
                val endOfLine = text.indexOf('\n', idx)
                val eol = if (endOfLine == -1) text.length else endOfLine
                if (text.substring(idx, eol).trim() == "~~~") {
                    i = eol
                    continue
                }
            }

            // Headers
            var hashes = 0
            var hIdx = idx
            while (hIdx < text.length && text[hIdx] == '#') {
                hashes++; hIdx++
            }
            if (hashes in 1..5 && hIdx < text.length && text[hIdx] == ' ') {
                val endOfLine = text.indexOf('\n', hIdx)
                val eol = if (endOfLine == -1) text.length else endOfLine
                withStyle(headerSpanStyle(hashes)) {
                    parseMarkdown(
                        text.substring(hIdx + 1, eol),
                        linkColor,
                        codeBackground,
                        spoilerColor,
                        false
                    )
                }
                i = eol
                continue
            }

            // Blockquote
            if (text.startsWith(">", idx)) {
                val endOfLine = text.indexOf('\n', idx)
                val eol = if (endOfLine == -1) text.length else endOfLine
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = spoilerColor)) {
                    append("┃ ")
                    val contentStart =
                        if (idx + 1 < text.length && text[idx + 1] == ' ') idx + 2 else idx + 1
                    parseMarkdown(
                        text.substring(contentStart, eol),
                        linkColor,
                        codeBackground,
                        spoilerColor,
                        false
                    )
                }
                i = eol
                continue
            }

            // Unordered list
            if (text.startsWith("- ", idx) || text.startsWith("* ", idx) || text.startsWith(
                    "+ ",
                    idx
                )
            ) {
                append("  • ")
                i = idx + 2
                lineStart = false
                continue
            }

            // Ordered list
            val digitMatch =
                Regex("^\\d+\\.\\s").find(text.substring(idx, minOf(idx + 10, text.length)))
            if (digitMatch != null) {
                append("  ${digitMatch.value.trim()} ")
                i = idx + digitMatch.value.length
                lineStart = false
                continue
            }
        }

        // Break Block Line Spans
        if (text[i] == '\n') {
            append('\n')
            lineStart = true
            i++
            continue
        } else {
            lineStart = false
        }

        // --- INLINE LEVEL PARSING ---

        // Spoilers: ~! !~
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
                        linkColor,
                        codeBackground,
                        spoilerColor,
                        false
                    )
                }
                i = end + 2
                continue
            }
        }

        // Strikethrough: ~~ ~~
        if (text.startsWith("~~", i)) {
            val end = text.indexOf("~~", i + 2)
            if (end != -1) {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    parseMarkdown(
                        text.substring(i + 2, end),
                        linkColor,
                        codeBackground,
                        spoilerColor,
                        false
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
                        linkColor,
                        codeBackground,
                        spoilerColor,
                        false
                    )
                }
                i = end + 2
                continue
            }
        }

        // Italic: * * or _ _
        if ((text[i] == '*' || text[i] == '_') && (i == 0 || text[i - 1] != text[i])) {
            val marker = text[i].toString()
            val end = text.indexOf(marker, i + 1)
            if (end != -1 && end > i + 1 && (end + 1 == text.length || text[end + 1] != text[i])) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    parseMarkdown(
                        text.substring(i + 1, end),
                        linkColor,
                        codeBackground,
                        spoilerColor,
                        false
                    )
                }
                i = end + 1
                continue
            }
        }

        // Inline Code: ` `
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

        // YouTube / Webm Links
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
                val url = if (isYoutube && !content.startsWith(
                        "http",
                        ignoreCase = true
                    )
                ) "https://www.youtube.com/watch?v=$content" else content
                pushLink(LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))))
                append("▶ Video")
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

                    pushLink(LinkAnnotation.Url(linkUrl, TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))))
                    parseMarkdown(linkText, linkColor, codeBackground, spoilerColor, false)
                    pop()
                    i = closeParen + 1
                    continue
                }
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

                    if (lowerTag.startsWith("<a ") && lowerTag.contains("href=")) {
                        val hrefMatch = Regex("""href="([^"]*)"""").find(tag)
                        val endA = text.indexOf("</a>", tagEnd + 1, ignoreCase = true)
                        if (hrefMatch != null && endA != -1) {
                            val url = hrefMatch.groupValues[1]
                            pushLink(LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))))
                            parseMarkdown(
                                text.substring(tagEnd + 1, endA),
                                linkColor,
                                codeBackground,
                                spoilerColor,
                                false
                            )
                            pop()
                            i = endA + 4
                            continue
                        }
                    }

                    val endTag = when (lowerTag) {
                        "<b>", "<strong>" -> "</b>"
                        "<i>", "<em>" -> "</i>"
                        "<del>", "<strike>" -> "</del>"
                        else -> null
                    }
                    val style = when (lowerTag) {
                        "<b>", "<strong>" -> SpanStyle(fontWeight = FontWeight.Bold)
                        "<i>", "<em>" -> SpanStyle(fontStyle = FontStyle.Italic)
                        "<del>", "<strike>" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                        else -> null
                    }

                    if (endTag != null && style != null) {
                        val closeIdx = text.indexOf(endTag, tagEnd + 1, ignoreCase = true)
                        val altEndTag = when (lowerTag) {
                            "<b>" -> "</strong>"
                            "<strong>" -> "</b>"
                            "<del>" -> "</strike>"
                            "<strike>" -> "</del>"
                            else -> null
                        }
                        val altCloseIdx = if (altEndTag != null) text.indexOf(
                            altEndTag,
                            tagEnd + 1,
                            ignoreCase = true
                        ) else -1

                        val actualClose = if (closeIdx != -1) closeIdx else altCloseIdx
                        if (actualClose != -1) {
                            val endTagLen =
                                if (closeIdx != -1) endTag.length else altEndTag?.length ?: 0

                            withStyle(style) {
                                parseMarkdown(
                                    text.substring(tagEnd + 1, actualClose),
                                    linkColor,
                                    codeBackground,
                                    spoilerColor,
                                    false
                                )
                            }
                            i = actualClose + endTagLen
                            continue
                        }
                    }

                    if (lowerTag.matches(Regex("^<h[1-5]>$"))) {
                        val level = lowerTag[2].digitToInt()
                        val closeTag = "</h$level>"
                        val closeIdx = text.indexOf(closeTag, tagEnd + 1, ignoreCase = true)
                        if (closeIdx != -1) {
                            withStyle(headerSpanStyle(level)) {
                                parseMarkdown(
                                    text.substring(tagEnd + 1, closeIdx),
                                    linkColor,
                                    codeBackground,
                                    spoilerColor,
                                    false
                                )
                            }
                            i = closeIdx + closeTag.length
                            continue
                        }
                    }

                    // Bypass unknown HTML tags
                    if (tagLength < 20 && (lowerTag[1].isLetter() || lowerTag[1] == '/')) {
                        i = tagEnd + 1
                        continue
                    }
                }
            }
        }

        // HTML Entities
        if (text.startsWith("&", i)) {
            val semi = text.indexOf(';', i)
            if (semi != -1 && semi - i < 10) {
                val entity = text.substring(i, semi + 1).lowercase()
                val char = when (entity) {
                    "&amp;" -> "&"
                    "&lt;" -> "<"
                    "&gt;" -> ">"
                    "&quot;" -> "\""
                    "&nbsp;" -> " "
                    "&apos;" -> "'"
                    else -> null
                }
                if (char != null) {
                    append(char)
                    i = semi + 1
                    continue
                }
                if (entity.startsWith("&#")) {
                    val num = entity.substring(2, entity.length - 1)
                    val code = if (num.startsWith("x")) num.substring(1)
                        .toIntOrNull(16) else num.toIntOrNull()
                    if (code != null) {
                        try {
                            append(String(Character.toChars(code)))
                        } catch (e: Exception) { /* Malformed Ignore */
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

private fun stripAlignmentWrappers(text: String): String {
    var result = text
    for (prefix in listOf("center", "left", "right")) {
        val pattern = Regex("${prefix}\\((.*?)\\)", RegexOption.DOT_MATCHES_ALL)
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