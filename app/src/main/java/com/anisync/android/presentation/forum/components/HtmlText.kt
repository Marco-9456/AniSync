package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.sp

/**
 * Renders AniList-Flavored Markdown as styled text.
 *
 * Supported syntax:
 * - `# Header` through `##### Header 5` (scaled font sizes)
 * - `**bold**` / `__bold__`
 * - `*italic*` / `_italic_`
 * - `~~strikethrough~~`
 * - `~!spoiler text!~` (tappable reveal)
 * - `[text](url)` links
 * - `img###(url)` → "[image]" link
 * - `youtube(id/url)` → "[video]" link
 * - `webm(url)` → "[video]" link
 * - `> blockquote` (line prefix)
 * - `- list item` / `* list item` / `+ list item` (unordered)
 * - `1. ordered item`
 * - `` `inline code` ``
 * - `---` / `___` horizontal rule
 * - `center(text)` alignment blocks (rendered as-is, wrapper stripped)
 * - HTML tags: <b>, <i>, <br>, <a href>, <del>, <strike>, <strong>, <em>, <img>, <h1>-<h5>
 * - HTML entities: named (&amp;, etc.) and numeric (&#xxxxx;, &#xHHHH;)
 */
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

    val annotatedString = remember(html, linkColor, codeBackground) {
        parseAniListMarkdown(html, linkColor, codeBackground, spoilerColor)
    }

    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = style.copy(color = color),
        onClick = { offset ->
            annotatedString.getStringAnnotations("URL", offset, offset)
                .firstOrNull()
                ?.let { annotation ->
                    try {
                        uriHandler.openUri(annotation.item)
                    } catch (_: Exception) { }
                }
        }
    )
}

/**
 * Converts AniList-Flavored Markdown string to [AnnotatedString].
 */
private fun parseAniListMarkdown(
    raw: String,
    linkColor: Color,
    codeBackground: Color,
    spoilerColor: Color
): AnnotatedString {
    // Step 1: Pre-process — normalize line breaks and strip block-level HTML wrappers
    var text = raw

    // Replace <br> / <br/> with newlines
    text = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
    // Replace <p>...</p> with content + newlines
    text = text.replace(Regex("</?p>", RegexOption.IGNORE_CASE), "\n")
    // Replace <div>...</div> with content + newlines
    text = text.replace(Regex("</?div>", RegexOption.IGNORE_CASE), "\n")
    // Strip <center> tags (keep content)
    text = text.replace(Regex("</?center>", RegexOption.IGNORE_CASE), "")
    // Strip alignment wrappers: center(...), left(...), right(...)
    text = stripAlignmentWrappers(text)

    // Collapse 3+ newlines to 2
    text = text.replace(Regex("\n{3,}"), "\n\n")

    // Step 2: Build annotated string by scanning for patterns line-by-line & inline
    return buildAnnotatedString {
        val lines = text.split("\n")
        for ((lineIndex, rawLine) in lines.withIndex()) {
            val line = rawLine.trimEnd()

            when {
                // ~~~ lines — AniList uses these as decorative wrappers, they're invisible
                line.matches(Regex("^\\s*~~~\\s*$")) -> {
                    // Skip — don't render anything for ~~~ lines
                }

                // Horizontal rule: --- or ___ (at least 3 chars, only dashes/underscores)
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

                // Unordered list items: - item, * item, + item
                line.matches(Regex("^\\s*[-*+]\\s+.+")) -> {
                    val content = line.replace(Regex("^\\s*[-*+]\\s+"), "")
                    append("  • ")
                    parseInlineMarkdown(content, linkColor, codeBackground, spoilerColor)
                }

                // Ordered list items: 1. item, 2. item, etc.
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

                // Blockquote: > text
                line.startsWith(">") -> {
                    val content = line.removePrefix(">").trimStart()
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = spoilerColor)) {
                        append("┃ ")
                        parseInlineMarkdown(content, linkColor, codeBackground, spoilerColor)
                    }
                }

                // HTML headers: <h1>...</h1> through <h5>...</h5>
                line.matches(Regex(".*<h[1-5]>.*</h[1-5]>.*", RegexOption.IGNORE_CASE)) -> {
                    val htmlHeaderMatch = Regex("<h([1-5])>(.*?)</h\\1>", RegexOption.IGNORE_CASE).find(line)
                    if (htmlHeaderMatch != null) {
                        val level = htmlHeaderMatch.groupValues[1].toInt()
                        val content = htmlHeaderMatch.groupValues[2]
                        withStyle(headerSpanStyle(level)) {
                            parseInlineMarkdown(content, linkColor, codeBackground, spoilerColor)
                        }
                    } else {
                        parseInlineMarkdown(line, linkColor, codeBackground, spoilerColor)
                    }
                }

                // Regular line — parse inline markdown
                else -> {
                    parseInlineMarkdown(line, linkColor, codeBackground, spoilerColor)
                }
            }

            // Add newline between lines (but not after the last)
            if (lineIndex < lines.lastIndex) {
                append("\n")
            }
        }
    }
}

/**
 * Strips AniList alignment wrapper functions: center(...), left(...), right(...).
 * These can span multiple lines, so we handle them as text-level transforms.
 */
private fun stripAlignmentWrappers(text: String): String {
    var result = text
    // Match center(...), left(...), right(...) — greedily match inner content
    for (prefix in listOf("center", "left", "right")) {
        val pattern = Regex("(?s)${prefix}\\((.*?)\\)", RegexOption.DOT_MATCHES_ALL)
        result = pattern.replace(result) { it.groupValues[1] }
    }
    return result
}

/**
 * Returns a SpanStyle for headers of the given level (1-5).
 */
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
 * Handles: bold, italic, strikethrough, spoilers, links, images, videos,
 * inline code, HTML inline tags, and HTML entities.
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
            // ~!spoiler!~ (AniList's actual spoiler syntax)
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
                } else {
                    append(text[i])
                    i++
                }
            }

            // img###(url) → [image]
            text.startsWith("img", i) && i + 3 < text.length && (text[i + 3].isDigit() || text[i + 3] == '(') -> {
                val parenStart = text.indexOf('(', i)
                val parenEnd = if (parenStart != -1) text.indexOf(')', parenStart + 1) else -1
                if (parenStart != -1 && parenEnd != -1) {
                    val url = text.substring(parenStart + 1, parenEnd)
                    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                        pushStringAnnotation("URL", url)
                        append("[image]")
                        pop()
                    }
                    i = parenEnd + 1
                } else {
                    append(text[i])
                    i++
                }
            }

            // youtube(id or url) → [video]
            text.startsWith("youtube(", i) -> {
                val end = text.indexOf(')', i + 8)
                if (end != -1) {
                    val content = text.substring(i + 8, end)
                    val url = if (content.startsWith("http")) content
                              else "https://www.youtube.com/watch?v=$content"
                    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                        pushStringAnnotation("URL", url)
                        append("[video]")
                        pop()
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }

            // webm(url) → [video]
            text.startsWith("webm(", i) -> {
                val end = text.indexOf(')', i + 5)
                if (end != -1) {
                    val url = text.substring(i + 5, end)
                    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                        pushStringAnnotation("URL", url)
                        append("[video]")
                        pop()
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }

            // [text](url) Markdown links
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
                    } else {
                        append(text[i])
                        i++
                    }
                } else {
                    append(text[i])
                    i++
                }
            }

            // <a href="url">text</a> HTML links
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
                } else {
                    append(text[i])
                    i++
                }
            }

            // <b> or <strong> → bold
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
                } else {
                    append(text[i])
                    i++
                }
            }

            // <i> or <em> → italic
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
                } else {
                    append(text[i])
                    i++
                }
            }

            // <del> or <strike> → strikethrough
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
                } else {
                    append(text[i])
                    i++
                }
            }

            // <img ...> tags → [image]
            text.startsWith("<img", i) -> {
                val end = text.indexOf('>', i)
                if (end != -1) {
                    val srcMatch = Regex("""src="([^"]*)".*""").find(text.substring(i, end + 1))
                    val url = srcMatch?.groupValues?.getOrNull(1) ?: ""
                    if (url.isNotEmpty()) {
                        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                            pushStringAnnotation("URL", url)
                            append("[image]")
                            pop()
                        }
                    } else {
                        append("[image]")
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }

            // Skip any remaining unrecognized HTML tags
            text[i] == '<' && i + 1 < text.length && (text[i + 1].isLetter() || text[i + 1] == '/') -> {
                val end = text.indexOf('>', i)
                if (end != -1) {
                    i = end + 1 // skip the tag
                } else {
                    append(text[i])
                    i++
                }
            }

            // Inline code: `code`
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end != -1 && end > i + 1) {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground,
                        fontSize = 13.sp
                    )) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }

            // **bold** or __bold__
            (text.startsWith("**", i) || text.startsWith("__", i)) -> {
                val marker = text.substring(i, i + 2)
                val end = text.indexOf(marker, i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        parseInlineMarkdown(text.substring(i + 2, end), linkColor, codeBackground, spoilerColor)
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }

            // ~~strikethrough~~
            text.startsWith("~~", i) -> {
                val end = text.indexOf("~~", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        parseInlineMarkdown(text.substring(i + 2, end), linkColor, codeBackground, spoilerColor)
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }

            // *italic* or _italic_ (single char, not double)
            (text[i] == '*' || text[i] == '_') && (i == 0 || text[i - 1] != text[i]) -> {
                val marker = text[i]
                val end = text.indexOf(marker, i + 1)
                if (end != -1 && end > i + 1) {
                    // Make sure it's not actually ** or __
                    if (i + 1 < text.length && text[i + 1] != marker) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            parseInlineMarkdown(text.substring(i + 1, end), linkColor, codeBackground, spoilerColor)
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                } else {
                    append(text[i])
                    i++
                }
            }

            // Named HTML entities
            text.startsWith("&amp;", i) -> { append("&"); i += 5 }
            text.startsWith("&lt;", i) -> { append("<"); i += 4 }
            text.startsWith("&gt;", i) -> { append(">"); i += 4 }
            text.startsWith("&quot;", i) -> { append("\""); i += 6 }
            text.startsWith("&nbsp;", i) -> { append(" "); i += 6 }
            text.startsWith("&apos;", i) -> { append("'"); i += 6 }

            // Numeric HTML entities: &#12345; or &#x1F4F8;
            text.startsWith("&#", i) -> {
                val semiColon = text.indexOf(';', i + 2)
                if (semiColon != -1 && semiColon - i < 12) {
                    val entityBody = text.substring(i + 2, semiColon)
                    val codePoint = if (entityBody.startsWith("x", ignoreCase = true)) {
                        entityBody.substring(1).toIntOrNull(16)
                    } else {
                        entityBody.toIntOrNull()
                    }
                    if (codePoint != null) {
                        append(String(Character.toChars(codePoint)))
                        i = semiColon + 1
                    } else {
                        append(text[i])
                        i++
                    }
                } else {
                    append(text[i])
                    i++
                }
            }

            else -> {
                append(text[i])
                i++
            }
        }
    }
}
