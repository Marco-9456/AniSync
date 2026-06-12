package com.anisync.android.domain.parser

internal object RichTextNormalizer {
    private val CENTER_MARKDOWN_REGEX =
        Regex("""~~~(?:center)?(.*?)~~~""", RegexOption.DOT_MATCHES_ALL)
    // A `~~~` centre block whose emphasis straddles the closing fence, e.g.
    // `~~~_Day 90: __title__~~~_` — the italic `_` opens just inside the fence and closes just
    // outside it. Converting `~~~…~~~` to a block first would split those markers into separate
    // nodes that can never pair, leaving literal underscores. Capture the overlap up front and emit
    // proper nesting (`<div><em>…</em></div>`); Jsoup repairs the order and the inner `__…__` is
    // handled normally. group1 = the leading marker run (1–3 of `_`/`*`), group2 = the centred body.
    private val CENTER_DANGLING_EMPHASIS_REGEX =
        Regex("""~~~(?:center)?(_{1,3}|\*{1,3})((?:(?!~~~).)*?)~~~\1""", RegexOption.DOT_MATCHES_ALL)
    private val SPOILER_MARKDOWN_REGEX =
        Regex("""~!(.*?)!~""", RegexOption.DOT_MATCHES_ALL)
    private val YOUTUBE_DIV_REGEX = Regex(
        """<div([^>]*)class=['\"]youtube['\"]([^>]*)>(.*?)</div>""",
        RegexOption.DOT_MATCHES_ALL
    )
    private val ANY_HTML_TAG_REGEX = Regex("""<[^>]+>""")
    private val PRE_BLOCK_REGEX =
        Regex("""<pre\b[^>]*>.*?</pre>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    fun normalize(html: String): String {
        if (html.isEmpty()) return html

        var processed = if (html.indexOf('\r') >= 0) html.replace("\r", "") else html
        // Shield <pre>…</pre> from the markdown→HTML rewrites below. Their content is either genuine
        // code (must stay verbatim) or a rich post AniList wrongly fenced — which the HTML parser
        // re-normalizes from the code's own text later. Rewriting it here corrupts that text: e.g. a
        // ~!spoiler!~ inside the fence would be turned into <spoiler> tags that Jsoup then strips when
        // the code's wholeText is read, losing the spoiler before it can be recovered.
        val masked = maskPreBlocks(processed)
        processed = masked.text
        processed = convertMixedMarkdownLinksToHtml(processed)
        processed = fixMangledMarkdownLinks(processed)
        processed = decodeAniListEscapedParenthesis(processed)
        processed = convertLinkedImages(processed)
        processed = unwrapEmptyAnchors(processed)
        processed = convertMarkdownSpoilerSpans(processed)
        processed = replaceCenterTags(processed)
        processed = replaceDanglingCenterEmphasis(processed)
        processed = replaceCenterMarkdownBlocks(processed)
        processed = replaceMarkdownSpoilers(processed)
        processed = preserveYoutubeDivs(processed)
        processed = normalizeMarkdownBlockquotes(processed)
        return restorePreBlocks(processed, masked.blocks)
    }

    private class MaskedHtml(val text: String, val blocks: List<String>)

    /** Replaces each `<pre>…</pre>` with an inert placeholder so the normalization steps skip it. */
    private fun maskPreBlocks(html: String): MaskedHtml {
        if (!html.contains("<pre", ignoreCase = true)) return MaskedHtml(html, emptyList())
        val blocks = mutableListOf<String>()
        val masked = PRE_BLOCK_REGEX.replace(html) { match ->
            val token = "PRE${blocks.size}"
            blocks.add(match.value)
            token
        }
        return MaskedHtml(masked, blocks)
    }

    private fun restorePreBlocks(html: String, blocks: List<String>): String {
        if (blocks.isEmpty()) return html
        var result = html
        for (i in blocks.indices) {
            result = result.replace("PRE${i}", blocks[i])
        }
        return result
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
     * Decodes AniList's escaped parenthesis tokens back to '(' and ')'.
     * Order matters: longer tokens are checked before their shorter sub-prefixes
     * so "&amp;rpar" isn't decoded as "&" + "amp;rpar".
     */
    private fun decodeAniListEscapedParenthesis(text: String): String {
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

    // AniList turns an empty markdown link `[text]()` into an attribute-less `<a>text</a>`. Such an
    // anchor has no destination and, worse, splits the surrounding text into separate jsoup nodes —
    // breaking markdown emphasis that spans it (e.g. `__text <a>x</a> more__` would keep its literal
    // underscores). Unwrap these dead anchors to their text so the run rejoins before parsing.
    private val EMPTY_ANCHOR_REGEX = Regex("""<a\s*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)

    private fun unwrapEmptyAnchors(text: String): String {
        if (!text.contains("<a")) return text
        return text.replace(EMPTY_ANCHOR_REGEX) { it.groupValues[1] }
    }

    private fun replaceCenterTags(text: String): String =
        text
            .replace("<center>", "<div align=\"center\">")
            .replace("</center>", "</div>")

    private fun replaceDanglingCenterEmphasis(text: String): String {
        if (text.indexOf("~~~") < 0) return text
        return text.replace(CENTER_DANGLING_EMPHASIS_REGEX) { match ->
            val marker = match.groupValues[1]
            val body = match.groupValues[2]
            val (open, close) = when (marker.length) {
                1 -> "<em>" to "</em>"
                2 -> "<strong>" to "</strong>"
                else -> "<em><strong>" to "</strong></em>"
            }
            "<div align=\"center\">$open$body$close</div>"
        }
    }

    private fun replaceCenterMarkdownBlocks(text: String): String {
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