package com.anisync.android.domain.parser

internal class RichTextInlineParser(
    val config: ParserConfig
) {
    private val youtubeRegex = Regex("""youtube\((.*?)\)""")
    private val webmRegex = Regex("""webm\((.*?)\)""")
    private val imageMdRegex = Regex("""img(\d+%?)?\((.*?)\)""")
    private val bareUrlRegex = Regex("""https?://[^\s<>"\[\]~!`)]+""")
    private val anilistLinkRegex = Regex(
        """https?://anilist\.co/(anime|manga|character|staff|user|activity)/([0-9]+)(?:/([A-Za-z0-9][A-Za-z0-9-]*))?/?"""
    )

    fun parseInto(text: String, ctx: ParseContext) {
        val preparedText = preprocessSetextUnderlines(text)
        var index = 0
        var lastAppend = 0
        val length = preparedText.length

        fun flushPlain() {
            if (index > lastAppend) {
                ctx.appendText(preparedText.substring(lastAppend, index))
            }
        }

        while (index < length) {
            val startOfLine =
                (index == 0 && (!ctx.hasBufferedInlineContent || ctx.endsWithNewline())) ||
                        (index > 0 && preparedText[index - 1] == '\n')

            if (startOfLine) {
                var ws = index
                while (ws < length && (preparedText[ws] == ' ' || preparedText[ws] == '\t')) ws++

                if (ws < length) {
                    val currentLineEnd =
                        preparedText.indexOf('\n', ws).let { if (it == -1) length else it }
                    val lineTextRaw = preparedText.substring(ws, currentLineEnd)
                    val lineText = lineTextRaw.trim()
                    val lineChar = preparedText[ws]

                    if (lineText.matches(Regex("[-*_]{3,}"))) {
                        flushPlain()
                        ctx.flushText()
                        ctx.emitBlock(RichTextBlock.HorizontalRule(align = ctx.align))

                        index = currentLineEnd
                        if (index < length && preparedText[index] == '\n') index++
                        lastAppend = index
                        continue
                    }

                    if (lineChar == '-' || lineChar == '*' || lineChar == '_') {
                        var j = ws
                        var count = 0
                        while (j < length) {
                            val current = preparedText[j]
                            if (current == lineChar) count++
                            else if (current != ' ' && current != '\t') break
                            j++
                        }

                        if (count >= 3 && (j == length || preparedText[j] == '\n' || preparedText[j] == '\r')) {
                            flushPlain()
                            ctx.flushText()
                            ctx.emitBlock(RichTextBlock.HorizontalRule(align = ctx.align))

                            index = if (j < length && preparedText[j] == '\r') j + 1 else j
                            if (index < length && preparedText[index] == '\n') index++
                            lastAppend = index
                            continue
                        }
                    }

                    if (lineChar == '#') {
                        var j = ws
                        while (j < length && preparedText[j] == '#') j++
                        val level = j - ws
                        if (level in 1..5 && j < length && preparedText[j] == ' ') {
                            var eol = preparedText.indexOf('\n', j)
                            if (eol == -1) eol = length

                            flushPlain()
                            ctx.flushText()

                            val headingText = preparedText.substring(j + 1, eol)
                            val headingInlines = parseInlineOnly(headingText, ctx.currentLinkUrl)
                            if (!isBlankInlineList(headingInlines)) {
                                ctx.emitBlock(
                                    RichTextBlock.Text(
                                        inlines = headingInlines,
                                        kind = headingKind(level),
                                        align = ctx.align
                                    )
                                )
                            }

                            index = eol
                            if (index < length && preparedText[index] == '\n') index++
                            lastAppend = index
                            continue
                        }
                    }
                }
            }

            val c = preparedText[index]

            if (c == '\\' && index + 1 < length) {
                flushPlain()
                ctx.appendText(preparedText[index + 1].toString())
                index += 2
                lastAppend = index
                continue
            }

            if (c == '`') {
                if (preparedText.startsWith("```", index)) {
                    val end = preparedText.indexOf("```", index + 3)
                    if (end != -1) {
                        flushPlain()
                        ctx.flushText()
                        val code = preparedText.substring(index + 3, end).trim('\n', '\r', ' ')
                        if (code.isNotBlank()) {
                            ctx.emitBlock(RichTextBlock.CodeBlock(code, ctx.align))
                        }
                        index = end + 3
                        lastAppend = index
                        continue
                    }
                }

                val end = preparedText.indexOf('`', index + 1)
                if (end != -1 && end > index) {
                    flushPlain()
                    ctx.appendInline(
                        RichTextInline.InlineCode(
                            preparedText.substring(
                                index + 1,
                                end
                            )
                        )
                    )
                    index = end + 1
                    lastAppend = index
                    continue
                }
            }

            if (c == 'y' && preparedText.startsWith("youtube(", index)) {
                val match = youtubeRegex.find(preparedText, index)
                if (match != null && match.range.first == index) {
                    flushPlain()
                    ctx.flushText()
                    val value = match.groupValues[1].trim()
                    if (value.isNotBlank()) {
                        ctx.emitBlock(RichTextBlock.YouTube(value, ctx.align))
                    }
                    index = match.range.last + 1
                    lastAppend = index
                    continue
                }
            }

            if (c == 'w' && preparedText.startsWith("webm(", index)) {
                val match = webmRegex.find(preparedText, index)
                if (match != null && match.range.first == index) {
                    flushPlain()
                    ctx.flushText()
                    val url = match.groupValues[1].trim()
                    if (url.isNotBlank()) {
                        ctx.emitBlock(RichTextBlock.Video(url, ctx.align))
                    }
                    index = match.range.last + 1
                    lastAppend = index
                    continue
                }
            }

            if (c == 'i' && preparedText.startsWith("img", index)) {
                val match = imageMdRegex.find(preparedText, index)
                if (match != null && match.range.first == index) {
                    flushPlain()
                    ctx.flushText()
                    val size = match.groupValues[1]
                    val url = match.groupValues[2].trim()
                    if (url.isNotBlank()) {
                        ctx.emitBlock(
                            RichTextBlock.Image(
                                url = url,
                                width = size.replace("%", "").toIntOrNull(),
                                isPercent = size.endsWith("%"),
                                linkUrl = ctx.currentLinkUrl,
                                align = ctx.align
                            )
                        )
                    }
                    index = match.range.last + 1
                    lastAppend = index
                    continue
                }
            }

            if (c == '~' && preparedText.startsWith("~~~", index)) {
                val end = preparedText.indexOf("~~~", index + 3)
                if (end != -1) {
                    flushPlain()
                    ctx.flushText()

                    var contentStart = index + 3
                    val maxCheck = minOf(contentStart + 10, preparedText.length)
                    val prefix = preparedText.substring(contentStart, maxCheck).lowercase()
                    if (prefix.startsWith("center")) {
                        contentStart += 6
                    } else if (prefix.startsWith(" center")) {
                        contentStart += 7
                    }

                    val centerCtx = ctx.detached(
                        align = RichTextAlignment.Center,
                        currentLinkUrl = ctx.currentLinkUrl,
                        listDepth = ctx.listDepth
                    )
                    parseInto(preparedText.substring(contentStart, end), centerCtx)
                    centerCtx.flushText()
                    ctx.blocks.addAll(centerCtx.blocks)

                    index = end + 3
                    lastAppend = index
                    continue
                }
            }

            if (c == '~') {
                val isDouble = preparedText.startsWith("~~", index)
                val marker = if (isDouble) "~~" else "~"
                val end = preparedText.indexOf(marker, index + marker.length)
                if (end > index + marker.length &&
                    !preparedText[index + marker.length].isWhitespace() &&
                    !preparedText[end - 1].isWhitespace()
                ) {
                    flushPlain()
                    val child = parseInlineOnly(
                        preparedText.substring(index + marker.length, end),
                        ctx.currentLinkUrl
                    )
                    ctx.appendInline(RichTextInline.Strikethrough(child))
                    index = end + marker.length
                    lastAppend = index
                    continue
                }
            }

            if (c == '*' || c == '_') {
                val marker = c.toString()
                val marker3 = marker.repeat(3)
                val marker2 = marker.repeat(2)

                if (preparedText.startsWith(marker3, index)) {
                    val end = preparedText.indexOf(marker3, index + 3)
                    if (end > index + 3 &&
                        !preparedText[index + 3].isWhitespace() &&
                        !preparedText[end - 1].isWhitespace()
                    ) {
                        flushPlain()
                        val child = parseInlineOnly(
                            preparedText.substring(index + 3, end),
                            ctx.currentLinkUrl
                        )
                        ctx.appendInline(RichTextInline.BoldItalic(child))
                        index = end + 3
                        lastAppend = index
                        continue
                    }
                }

                if (preparedText.startsWith(marker2, index)) {
                    val end = preparedText.indexOf(marker2, index + 2)
                    if (end > index + 2 &&
                        !preparedText[index + 2].isWhitespace() &&
                        !preparedText[end - 1].isWhitespace()
                    ) {
                        flushPlain()
                        val child = parseInlineOnly(
                            preparedText.substring(index + 2, end),
                            ctx.currentLinkUrl
                        )
                        ctx.appendInline(RichTextInline.Bold(child))
                        index = end + 2
                        lastAppend = index
                        continue
                    }
                }

                val end = preparedText.indexOf(marker, index + 1)
                if (end > index + 1 &&
                    !preparedText[index + 1].isWhitespace() &&
                    !preparedText[end - 1].isWhitespace()
                ) {
                    flushPlain()
                    val child =
                        parseInlineOnly(preparedText.substring(index + 1, end), ctx.currentLinkUrl)
                    ctx.appendInline(RichTextInline.Italic(child))
                    index = end + 1
                    lastAppend = index
                    continue
                }
            }

            if (c == '!' && preparedText.startsWith("![", index)) {
                val closeBracket = findBalancedCloseBracket(preparedText, index + 1)
                if (closeBracket != -1 && closeBracket + 1 < length && preparedText[closeBracket + 1] == '(') {
                    val closeParen = findBalancedCloseParen(preparedText, closeBracket + 1)
                    if (closeParen != -1) {
                        flushPlain()
                        ctx.flushText()
                        val url = preparedText.substring(closeBracket + 2, closeParen)
                        ctx.emitBlock(
                            RichTextBlock.Image(
                                url = url,
                                width = null,
                                isPercent = false,
                                linkUrl = ctx.currentLinkUrl,
                                align = ctx.align
                            )
                        )
                        index = closeParen + 1
                        lastAppend = index
                        continue
                    }
                }
            }

            if (c == '[' && preparedText.indexOf("](", index) != -1) {
                val closeBracket = findBalancedCloseBracket(preparedText, index)
                if (closeBracket != -1 && closeBracket + 1 < length && preparedText[closeBracket + 1] == '(') {
                    val closeParen = findBalancedCloseParen(preparedText, closeBracket + 1)
                    if (closeParen != -1) {
                        flushPlain()
                        val linkText = preparedText.substring(index + 1, closeBracket)
                        val url = preparedText.substring(closeBracket + 2, closeParen)

                        var isJustImage = false
                        if (linkText.startsWith("![")) {
                            val innerCloseBracket = findBalancedCloseBracket(linkText, 1)
                            if (innerCloseBracket != -1 && innerCloseBracket + 1 < linkText.length && linkText[innerCloseBracket + 1] == '(') {
                                val innerCloseParen =
                                    findBalancedCloseParen(linkText, innerCloseBracket + 1)
                                if (innerCloseParen == linkText.length - 1) {
                                    ctx.flushText()
                                    val imgUrl =
                                        linkText.substring(innerCloseBracket + 2, innerCloseParen)
                                    ctx.emitBlock(
                                        RichTextBlock.Image(
                                            url = imgUrl,
                                            width = null,
                                            isPercent = false,
                                            linkUrl = url,
                                            align = ctx.align
                                        )
                                    )
                                    isJustImage = true
                                }
                            }
                        }

                        if (!isJustImage) {
                            val linkChildren = parseInlineOnly(linkText, url)
                            ctx.appendInline(
                                RichTextInline.Link(
                                    url = url,
                                    children = linkChildren
                                )
                            )
                        }

                        index = closeParen + 1
                        lastAppend = index
                        continue
                    }
                }
            }

            if (c == 'h' && (preparedText.startsWith(
                    "http://",
                    index
                ) || preparedText.startsWith("https://", index))
            ) {
                val match = bareUrlRegex.find(preparedText, index)
                if (match != null && match.range.first == index) {
                    flushPlain()
                    val url = match.value

                    val anilistMatch = anilistLinkRegex.find(url)
                    if (config.enableAniListLinkBlocks && anilistMatch != null && anilistMatch.range.first == 0) {
                        ctx.flushText()
                        val type = anilistMatch.groupValues[1]
                        val id = anilistMatch.groupValues[2].toIntOrNull() ?: 0
                        val slug = anilistMatch.groupValues.getOrNull(3)?.takeIf { it.isNotEmpty() }
                        ctx.emitBlock(RichTextBlock.AnilistLink(type, id, url, slug, ctx.align))
                        index = match.range.last + 1
                        lastAppend = index
                        continue
                    }

                    ctx.appendInline(
                        RichTextInline.Link(
                            url = url,
                            children = listOf(RichTextInline.Text(url))
                        )
                    )
                    index = match.range.last + 1
                    lastAppend = index
                    continue
                }
            }

            index++
        }

        flushPlain()
    }

    private fun preprocessSetextUnderlines(text: String): String {
        if (!text.contains('\n')) return text
        val lines = text.split('\n').toMutableList()
        var i = 0
        while (i < lines.size - 1) {
            val current = lines[i]
            val next = lines[i + 1].trim()
            val isSetext = next.matches(Regex("[=-]{2,}"))
            if (isSetext) {
                val level = if (next.first() == '=') 1 else 2
                val currentTrimmed = current.trimStart()
                if (!currentTrimmed.startsWith("#") && !currentTrimmed.contains('<') && !currentTrimmed.contains(
                        '>'
                    )
                ) {
                    val prefixWhitespace = current.takeWhile { it == ' ' || it == '\t' }
                    lines[i] = "$prefixWhitespace${"#".repeat(level)} ${current.trim()}"
                    lines.removeAt(i + 1)
                    continue
                }
            }
            i++
        }
        return lines.joinToString("\n")
    }

    private fun parseInlineOnly(
        text: String,
        currentLinkUrl: String?,
        depth: Int = 0
    ): List<RichTextInline> {
        if (text.isEmpty()) return emptyList()
        if (depth > 32) return listOf(RichTextInline.Text(text))

        val result = mutableListOf<RichTextInline>()
        var index = 0
        var lastAppend = 0
        val length = text.length

        fun appendText(value: String) {
            if (value.isEmpty()) return
            val last = result.lastOrNull()
            if (last is RichTextInline.Text) {
                result[result.lastIndex] = RichTextInline.Text(last.value + value)
            } else {
                result.add(RichTextInline.Text(value))
            }
        }

        fun flushPlain() {
            if (index > lastAppend) {
                appendText(text.substring(lastAppend, index))
            }
        }

        while (index < length) {
            val c = text[index]

            if (c == '\\' && index + 1 < length) {
                flushPlain()
                appendText(text[index + 1].toString())
                index += 2
                lastAppend = index
                continue
            }

            if (c == '`') {
                if (text.startsWith("```", index)) {
                    val end = text.indexOf("```", index + 3)
                    if (end != -1) {
                        flushPlain()
                        val code = text.substring(index + 3, end).trim('\n', '\r', ' ')
                        result.add(RichTextInline.InlineCode(code))
                        index = end + 3
                        lastAppend = index
                        continue
                    }
                }

                val end = text.indexOf('`', index + 1)
                if (end != -1 && end > index) {
                    flushPlain()
                    result.add(RichTextInline.InlineCode(text.substring(index + 1, end)))
                    index = end + 1
                    lastAppend = index
                    continue
                }
            }

            if (c == '~') {
                val isDouble = text.startsWith("~~", index)
                val marker = if (isDouble) "~~" else "~"
                val end = text.indexOf(marker, index + marker.length)
                if (end > index + marker.length &&
                    !text[index + marker.length].isWhitespace() &&
                    !text[end - 1].isWhitespace()
                ) {
                    flushPlain()
                    val child = parseInlineOnly(
                        text.substring(index + marker.length, end),
                        currentLinkUrl,
                        depth + 1
                    )
                    result.add(RichTextInline.Strikethrough(child))
                    index = end + marker.length
                    lastAppend = index
                    continue
                }
            }

            if (c == '*' || c == '_') {
                val marker = c.toString()
                val marker3 = marker.repeat(3)
                val marker2 = marker.repeat(2)

                if (text.startsWith(marker3, index)) {
                    val end = text.indexOf(marker3, index + 3)
                    if (end > index + 3 &&
                        !text[index + 3].isWhitespace() &&
                        !text[end - 1].isWhitespace()
                    ) {
                        flushPlain()
                        val child = parseInlineOnly(
                            text.substring(index + 3, end),
                            currentLinkUrl,
                            depth + 1
                        )
                        result.add(RichTextInline.BoldItalic(child))
                        index = end + 3
                        lastAppend = index
                        continue
                    }
                }

                if (text.startsWith(marker2, index)) {
                    val end = text.indexOf(marker2, index + 2)
                    if (end > index + 2 &&
                        !text[index + 2].isWhitespace() &&
                        !text[end - 1].isWhitespace()
                    ) {
                        flushPlain()
                        val child = parseInlineOnly(
                            text.substring(index + 2, end),
                            currentLinkUrl,
                            depth + 1
                        )
                        result.add(RichTextInline.Bold(child))
                        index = end + 2
                        lastAppend = index
                        continue
                    }
                }

                val end = text.indexOf(marker, index + 1)
                if (end > index + 1 &&
                    !text[index + 1].isWhitespace() &&
                    !text[end - 1].isWhitespace()
                ) {
                    flushPlain()
                    val child = parseInlineOnly(
                        text.substring(index + 1, end),
                        currentLinkUrl,
                        depth + 1
                    )
                    result.add(RichTextInline.Italic(child))
                    index = end + 1
                    lastAppend = index
                    continue
                }
            }

            if (c == '!' && text.startsWith("![", index)) {
                val closeBracket = findBalancedCloseBracket(text, index + 1)
                if (closeBracket != -1 && closeBracket + 1 < length && text[closeBracket + 1] == '(') {
                    val closeParen = findBalancedCloseParen(text, closeBracket + 1)
                    if (closeParen != -1) {
                        flushPlain()
                        val altText = text.substring(index + 2, closeBracket)
                        val url = text.substring(closeBracket + 2, closeParen)
                        result.add(
                            RichTextInline.Link(
                                url = url,
                                children = listOf(RichTextInline.Text(altText.ifEmpty { "Image" }))
                            )
                        )
                        index = closeParen + 1
                        lastAppend = index
                        continue
                    }
                }
            }

            if (c == '[' && text.indexOf("](", index) != -1) {
                val closeBracket = findBalancedCloseBracket(text, index)
                if (closeBracket != -1 && closeBracket + 1 < length && text[closeBracket + 1] == '(') {
                    val closeParen = findBalancedCloseParen(text, closeBracket + 1)
                    if (closeParen != -1) {
                        flushPlain()
                        val linkText = text.substring(index + 1, closeBracket)
                        val url = text.substring(closeBracket + 2, closeParen)
                        val linkChildren = parseInlineOnly(linkText, url, depth + 1)
                        result.add(RichTextInline.Link(url = url, children = linkChildren))
                        index = closeParen + 1
                        lastAppend = index
                        continue
                    }
                }
            }

            if (c == 'h' && (text.startsWith("http://", index) || text.startsWith(
                    "https://",
                    index
                ))
            ) {
                val match = bareUrlRegex.find(text, index)
                if (match != null && match.range.first == index) {
                    flushPlain()
                    val url = match.value
                    result.add(
                        RichTextInline.Link(
                            url = url,
                            children = listOf(RichTextInline.Text(url))
                        )
                    )
                    index = match.range.last + 1
                    lastAppend = index
                    continue
                }
            }

            index++
        }

        flushPlain()
        return result
    }

    private fun findBalancedCloseBracket(text: String, openBracketIndex: Int): Int {
        var depth = 1
        var i = openBracketIndex + 1
        while (i < text.length) {
            if (text[i] == '[') depth++
            else if (text[i] == ']') {
                depth--
                if (depth == 0) return i
            }
            i++
        }
        return -1
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
}