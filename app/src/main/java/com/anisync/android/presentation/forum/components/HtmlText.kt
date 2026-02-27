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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/**
 * Renders AniList-Flavored Markdown as styled text.
 *
 * AniList forums use a custom markdown that mixes standard Markdown with
 * custom syntax like `img220(url)`, `youtube(id)`, `webm(url)`, and
 * `~~~spoiler~~~`. This composable converts that into an [AnnotatedString]
 * with clickable links.
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

    val annotatedString = remember(html, linkColor) {
        parseAniListMarkdown(html, linkColor)
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
 * Converts an AniList-Flavored Markdown string to [AnnotatedString].
 *
 * Supports:
 * - **bold** / __bold__
 * - *italic* / _italic_
 * - ~~strikethrough~~
 * - [text](url) links
 * - img###(url) → "[image]"
 * - youtube(id) → "[video]"
 * - webm(url) → "[video]"
 * - ~~~spoiler~~~ → italic dimmed text
 * - > blockquote (line prefix)
 * - HTML tags: <b>, <i>, <br>, <a href>, <del>, <strike>, <strong>, <em>
 */
private fun parseAniListMarkdown(raw: String, linkColor: Color): AnnotatedString {
    // Step 1: Pre-process to clean up and normalize
    var text = raw

    // Replace <br> / <br/> with newlines
    text = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
    // Replace <p>...</p> with content + newlines
    text = text.replace(Regex("</?p>", RegexOption.IGNORE_CASE), "\n")
    // Replace <div>...</div> with content + newlines
    text = text.replace(Regex("</?div>", RegexOption.IGNORE_CASE), "\n")

    // Step 2: Build annotated string by scanning for patterns
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // ~~~spoiler~~~ blocks
                text.startsWith("~~~", i) -> {
                    val end = text.indexOf("~~~", i + 3)
                    if (end != -1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color.Gray)) {
                            append(text.substring(i + 3, end))
                        }
                        i = end + 3
                    } else {
                        append("~~~")
                        i += 3
                    }
                }

                // img###(url) → [image]
                text.startsWith("img", i) && i + 3 < text.length && (text[i + 3].isDigit() || text[i + 3] == '(') -> {
                    val parenStart = text.indexOf('(', i)
                    val parenEnd = text.indexOf(')', parenStart + 1)
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

                // youtube(id) → [video]
                text.startsWith("youtube(", i) -> {
                    val end = text.indexOf(')', i + 8)
                    if (end != -1) {
                        val id = text.substring(i + 8, end)
                        val url = "https://www.youtube.com/watch?v=$id"
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
                            append(text.substring(start, end))
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
                            append(text.substring(start, end))
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
                            append(text.substring(start, end))
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

                // **bold** or __bold__
                (text.startsWith("**", i) || text.startsWith("__", i)) -> {
                    val marker = text.substring(i, i + 2)
                    val end = text.indexOf(marker, i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
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
                            append(text.substring(i + 2, end))
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
                                append(text.substring(i + 1, end))
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

                // > blockquote prefix at start of line
                text[i] == '>' && (i == 0 || text[i - 1] == '\n') -> {
                    // Skip the > and optional space
                    i++
                    if (i < text.length && text[i] == ' ') i++
                    // Find end of line
                    val eol = text.indexOf('\n', i)
                    val lineEnd = if (eol != -1) eol else text.length
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color.Gray)) {
                        append("┃ ")
                        append(text.substring(i, lineEnd))
                    }
                    i = lineEnd
                }

                // HTML entities
                text.startsWith("&amp;", i) -> { append("&"); i += 5 }
                text.startsWith("&lt;", i) -> { append("<"); i += 4 }
                text.startsWith("&gt;", i) -> { append(">"); i += 4 }
                text.startsWith("&quot;", i) -> { append("\""); i += 6 }
                text.startsWith("&nbsp;", i) -> { append(" "); i += 6 }

                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
