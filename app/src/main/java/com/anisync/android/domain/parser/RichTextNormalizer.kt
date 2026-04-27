package com.anisync.android.domain.parser

internal object RichTextNormalizer {
    // Hoisted regex constants. Each was previously declared inside the function body, so
    // every normalize() call recompiled the pattern. Recompilation walks the regex AST and
    // builds a fresh DFA — measurably costly on large profile descriptions / forum bodies.
    private val CENTER_MARKDOWN_REGEX =
        Regex("""~~~(?:center)?(.*?)~~~""", RegexOption.DOT_MATCHES_ALL)
    private val SPOILER_MARKDOWN_REGEX =
        Regex("""~!(.*?)!~""", RegexOption.DOT_MATCHES_ALL)
    private val YOUTUBE_DIV_REGEX = Regex(
        """<div([^>]*)class=['\"]youtube['\"]([^>]*)>(.*?)</div>""",
        RegexOption.DOT_MATCHES_ALL
    )
    private val ANY_HTML_TAG_REGEX = Regex("""<[^>]+>""")

    fun normalize(html: String): String {
        // Skip the entire pipeline when there is nothing interesting. Cheap pre-check
        // dominated by short empty/plain-text descriptions in lists.
        if (html.isEmpty()) return html

        var processed = if (html.indexOf('\r') >= 0) html.replace("\r", "") else html
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

                            // Strip any remaining HTML tags (like <a>) using the hoisted compiled regex.
                            restoredUrl = restoredUrl.replace(ANY_HTML_TAG_REGEX, "").trim()

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

    /**
     * Single-pass HTML-entity rewriter. The previous implementation chained ten
     * `String.replace(needle, replacement)` calls — each one scanned the entire
     * string and allocated a new String. For a 10KB description that meant
     * ~110KB of throw-away char[] per call. This version walks the source once,
     * matches the longest known token at each `&`, and writes either the decoded
     * char or the original substring into a single StringBuilder.
     *
     * Behavior preserved exactly: the same eight HTML-entity-style tokens plus
     * the two numeric character references map to '(' and ')'.
     */
    private fun decodeAniListEscapedParenthesis(text: String): String {
        // Cheap rejection: if the input contains no '&' we cannot have any escape to decode,
        // so skip allocating a StringBuilder. This short-circuit handles the vast majority
        // of forum comments (which never carry these AniList-internal escapes).
        val firstAmp = text.indexOf('&')
        if (firstAmp < 0) return text

        val sb = StringBuilder(text.length)
        sb.append(text, 0, firstAmp)
        var i = firstAmp
        val len = text.length
        while (i < len) {
            val c = text[i]
            if (c != '&') {
                sb.append(c); i++; continue
            }
            // Try the longest-prefix match first, then fall back. Order matters: the longer
            // tokens are checked before their shorter sub-prefixes so we don't decode "&amp;rpar"
            // as "&" + "amp;rpar".
            val replaced: Char? = when {
                text.startsWith("&amp;rpar;", i) -> { i += 10; ')' }
                text.startsWith("&amp;rpar",  i) -> { i +=  9; ')' }
                text.startsWith("&rpar;",     i) -> { i +=  6; ')' }
                text.startsWith("&rpar",      i) -> { i +=  5; ')' }
                text.startsWith("&amp;lpar;", i) -> { i += 10; '(' }
                text.startsWith("&amp;lpar",  i) -> { i +=  9; '(' }
                text.startsWith("&lpar;",     i) -> { i +=  6; '(' }
                text.startsWith("&lpar",      i) -> { i +=  5; '(' }
                text.startsWith("&#41;",      i) -> { i +=  5; ')' }
                text.startsWith("&#40;",      i) -> { i +=  5; '(' }
                else -> null
            }
            if (replaced != null) {
                sb.append(replaced)
            } else {
                sb.append(c); i++
            }
        }
        return sb.toString()
    }

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
        // Bail out before scanning if the source has no triple-tilde sequences. This
        // avoids constructing a Sequence + Iterator for the common case where there
        // are no center blocks at all (95%+ of inputs).
        if (text.indexOf("~~~") < 0) return text
        return text.replace(CENTER_MARKDOWN_REGEX) { match ->
            "<div align=\"center\">${match.groupValues[1]}</div>"
        }
    }

    private fun replaceMarkdownSpoilers(text: String): String {
        if (text.indexOf("~!") < 0) return text
        return text.replace(SPOILER_MARKDOWN_REGEX) { match ->
            "<spoiler>${match.groupValues[1]}</spoiler>"
        }
    }

    private fun preserveYoutubeDivs(text: String): String {
        // Skip the regex entirely when no `<div ... youtube ...>` literal can appear.
        // indexOf is a simple SIMD-friendly char scan; it's hundreds of ns even on long inputs.
        if (!text.contains("youtube")) return text
        return text.replace(YOUTUBE_DIV_REGEX) { match ->
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