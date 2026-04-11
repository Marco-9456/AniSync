package com.anisync.android.domain.parser

internal object RichTextNormalizer {
    fun normalize(html: String): String {
        var processed = html.replace("\r", "")
        processed = convertMixedMarkdownLinksToHtml(processed)
        processed = fixMangledMarkdownLinks(processed)
        processed = decodeAniListEscapedParenthesis(processed)
        processed = convertLinkedImages(processed)
        processed = convertMarkdownSpoilerSpans(processed)
        processed = replaceCenterTags(processed)
        processed = replaceCenterMarkdownBlocks(processed)
        processed = replaceMarkdownSpoilers(processed)
        processed = preserveYoutubeDivs(processed)
        processed = normalizeMarkdownBlockquotes(processed)
        return processed
    }

    /**
     * Converts raw markdown links that contain HTML tags inside them into pure HTML <a> tags.
     * Example: `[ <img src='...'> Instagram ](https://...)` -> `<a href="https://..."><img src='...'> Instagram</a>`
     * This ensures Jsoup parses both the image and the surrounding text together as an anchor.
     */
    private fun convertMixedMarkdownLinksToHtml(html: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < html.length) {
            if (html[i] == '[') {
                var closeBracket = -1
                var bracketDepth = 1
                var j = i + 1
                while (j < html.length) {
                    if (html[j] == '[') bracketDepth++
                    else if (html[j] == ']') {
                        bracketDepth--
                        if (bracketDepth == 0) {
                            closeBracket = j
                            break
                        }
                    }
                    j++
                }

                if (closeBracket != -1 && closeBracket + 1 < html.length && html[closeBracket + 1] == '(') {
                    val closeParen = findBalancedCloseParen(html, closeBracket + 1)
                    if (closeParen != -1) {
                        val linkText = html.substring(i + 1, closeBracket)
                        val url = html.substring(closeBracket + 2, closeParen)

                        if (linkText.contains("<") && linkText.contains(">")) {
                            sb.append("<a href=\"").append(url).append("\">").append(linkText)
                                .append("</a>")
                            i = closeParen + 1
                            continue
                        }
                    }
                }
            }
            sb.append(html[i])
            i++
        }
        return sb.toString()
    }

    /**
     * AniList's backend often auto-links URLs *inside* markdown link syntax, replacing
     * the URL with full HTML <a> and <em> tags. This rebuilds the raw markdown link.
     */
    private fun fixMangledMarkdownLinks(html: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < html.length) {
            if (html[i] == '[') {
                var closeBracket = -1
                var bracketDepth = 1
                var j = i + 1
                while (j < html.length) {
                    if (html[j] == '[') bracketDepth++
                    else if (html[j] == ']') {
                        bracketDepth--
                        if (bracketDepth == 0) {
                            closeBracket = j
                            break
                        }
                    }
                    j++
                }

                if (closeBracket != -1 && closeBracket + 1 < html.length && html[closeBracket + 1] == '(') {
                    val closeParen = findBalancedCloseParen(html, closeBracket + 1)
                    if (closeParen != -1) {
                        val linkText = html.substring(i + 1, closeBracket)
                        val mangledUrl = html.substring(closeBracket + 2, closeParen)

                        if (mangledUrl.contains("<")) {
                            var restoredUrl = mangledUrl
                                .replace("<em>", "_")
                                .replace("</em>", "_")
                                .replace("<i>", "_")
                                .replace("</i>", "_")
                                .replace("<strong>", "**")
                                .replace("</strong>", "**")
                                .replace("<b>", "**")
                                .replace("</b>", "**")

                            // Strip any remaining HTML tags (like <a>)
                            restoredUrl = restoredUrl.replace(Regex("""<[^>]+>"""), "").trim()

                            sb.append("[$linkText]($restoredUrl)")
                            i = closeParen + 1
                            continue
                        }
                    }
                }
            }
            sb.append(html[i])
            i++
        }
        return sb.toString()
    }

    private fun findBalancedCloseParen(text: String, openParenIndex: Int): Int {
        var depth = 1
        var i = openParenIndex + 1
        while (i < text.length) {
            when {
                text[i] == '(' -> depth++
                text[i] == ')' -> {
                    depth--; if (depth == 0) return i
                }

                i + 2 < text.length && text[i] == '%' && text[i + 1] == '2' -> {
                    when (text[i + 2]) {
                        '8' -> {
                            depth++; i += 2
                        }

                        '9' -> {
                            depth--; if (depth == 0) return i + 2; i += 2
                        }
                    }
                }
            }
            i++
        }
        return -1
    }

    private fun decodeAniListEscapedParenthesis(text: String): String =
        text
            .replace("&amp;rpar;", ")")
            .replace("&amp;rpar", ")")
            .replace("&rpar;", ")")
            .replace("&rpar", ")")
            .replace("&amp;lpar;", "(")
            .replace("&amp;lpar", "(")
            .replace("&lpar;", "(")
            .replace("&lpar", "(")
            .replace("&#41;", ")")
            .replace("&#40;", "(")

    private val linkedImgRegex = Regex(
        """\[\s*(<img\s[^>]*>)\s*]\(([^)]+)\)"""
    )

    private fun convertLinkedImages(text: String): String =
        text.replace(linkedImgRegex) { match ->
            val imgTag = match.groupValues[1]
            val linkUrl = match.groupValues[2]
            "<a href=\"$linkUrl\">$imgTag</a>"
        }

    private fun replaceCenterTags(text: String): String =
        text
            .replace("<center>", "<div align=\"center\">")
            .replace("</center>", "</div>")

    private fun replaceCenterMarkdownBlocks(text: String): String {
        val regex = Regex("""~~~(?:center)?(.*?)~~~""", RegexOption.DOT_MATCHES_ALL)
        return text.replace(regex) { match ->
            "<div align=\"center\">${match.groupValues[1]}</div>"
        }
    }

    private fun replaceMarkdownSpoilers(text: String): String {
        val spoilerRegex = Regex("""~!(.*?)!~""", RegexOption.DOT_MATCHES_ALL)
        return text.replace(spoilerRegex) { match ->
            "<spoiler>${match.groupValues[1]}</spoiler>"
        }
    }

    private fun preserveYoutubeDivs(text: String): String {
        val youtubeRegex = Regex(
            """<div([^>]*)class=['\"]youtube['\"]([^>]*)>(.*?)</div>""",
            RegexOption.DOT_MATCHES_ALL
        )
        return text.replace(youtubeRegex) { match ->
            "<youtube${match.groupValues[1]}class=\"youtube\"${match.groupValues[2]}>${match.groupValues[3]}</youtube>"
        }
    }

    private val markdownSpoilerOpenRegex = Regex(
        """<span\s+class=['"]markdown_spoiler['"]\s*>\s*<span\s*>"""
    )

    private fun convertMarkdownSpoilerSpans(html: String): String {
        val openMatches = markdownSpoilerOpenRegex.findAll(html).toList()
        if (openMatches.isEmpty()) return html

        val sb = StringBuilder()
        var lastEnd = 0
        for (match in openMatches) {
            sb.append(html, lastEnd, match.range.first)
            sb.append("<div rel=\"spoiler\">")
            lastEnd = match.range.last + 1
        }
        sb.append(html, lastEnd, html.length)

        var result = sb.toString()
        val closeTag = "</span></span>"
        var remaining = openMatches.size
        while (remaining > 0) {
            val idx = result.indexOf(closeTag)
            if (idx == -1) break
            result = result.substring(0, idx) + "</div>" + result.substring(idx + closeTag.length)
            remaining--
        }

        return result
    }

    private fun normalizeMarkdownBlockquotes(text: String): String {
        val lines = text.split("\n")
        val out = StringBuilder()
        var currentDepth = 0
        var inCodeBlock = false

        for (line in lines) {
            val trimmed = line.trimStart()

            if (!inCodeBlock) {
                if (trimmed.startsWith("```") || trimmed.startsWith("<pre")) {
                    inCodeBlock = true
                }
            } else {
                if (trimmed.startsWith("```") || trimmed.contains("</pre>")) {
                    inCodeBlock = false
                }
            }

            if (inCodeBlock) {
                while (currentDepth > 0) {
                    out.append("</blockquote>\n")
                    currentDepth--
                }
                out.append(line).append("\n")
                continue
            }

            var depth = 0
            var content = trimmed
            while (content.startsWith(">")) {
                depth++
                content = content.substring(1).trimStart()
            }

            while (currentDepth < depth) {
                out.append("<blockquote>\n")
                currentDepth++
            }
            while (currentDepth > depth) {
                out.append("</blockquote>\n")
                currentDepth--
            }

            out.append(content).append("\n")
        }

        while (currentDepth > 0) {
            out.append("</blockquote>\n")
            currentDepth--
        }

        return out.toString()
    }
}