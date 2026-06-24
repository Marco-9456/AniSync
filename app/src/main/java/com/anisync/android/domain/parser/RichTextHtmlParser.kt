package com.anisync.android.domain.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

internal data class HtmlParseResult(
    val blocks: List<RichTextBlock>,
    val warnings: List<ParseWarning>
)

private val NON_DIGIT_REGEX = Regex("[^0-9]")
private val ESCAPED_HTML_TAG_REGEX = Regex("""<[a-zA-Z/][^>]*>""")

// A genuine `<code class="language-…">` token is a short language identifier (kotlin, c++,
// objective-c, asp.net). Any character outside this set in the token means AniList's fence misparse
// swallowed real post content there instead, so it must be recovered rather than dropped.
private val LANGUAGE_TOKEN_REGEX = Regex("""[A-Za-z0-9#+.-]+""")
private val TRIPLE_TILDE_REGEX = Regex("~~~")

// AniList's asHtml endpoint sometimes dumps an entire rich post — or a rich section of a
// review — into a <pre><code> block instead of rendering it (a markdown misparse, usually
// triggered by a heading or HTML tag at the very start). looksLikeEscapedHtml only catches
// HTML-tag-heavy posts (>=3 tags early on); these wrapped posts are mostly markdown/prose with
// as few as zero HTML tags up front. These patterns spot the AniList-specific rich signals that
// a genuine source-code block would not carry, so such posts get re-parsed as rich content.
private val WRAPPED_RICH_HTML_TAG_REGEX =
    Regex("""<\s*(?:img|video|iframe|center)\b|<\s*a\s""", RegexOption.IGNORE_CASE)
private val WRAPPED_MARKDOWN_LINK_REGEX = Regex("""!?\[[^\]\n]*]\(\s*https?://""")
private val WRAPPED_MARKDOWN_IMAGE_REGEX =
    Regex("""\bimg\d+%?\(\s*https?://""", RegexOption.IGNORE_CASE)
private val TABLE_CELL_TAGS: Set<String> = setOf("td", "th")
private val NESTED_LIST_TAGS: Set<String> = setOf("ul", "ol")
private val BLOCK_TAGS: Set<String> = setOf(
    "p", "div", "ul", "ol", "li", "table", "blockquote",
    "h1", "h2", "h3", "h4", "h5", "hr", "pre", "center",
    "youtube", "video", "iframe"
)

internal class RichTextHtmlParser(
    private val inlineParser: RichTextInlineParser
) {
    private val nonDigitRegex get() = NON_DIGIT_REGEX
    private val blockTags get() = BLOCK_TAGS

    fun parse(root: Element): HtmlParseResult {
        val rootContext = ParseContext(inlineParser.config)
        walkChildren(root, rootContext)
        rootContext.flushText()
        return HtmlParseResult(
            blocks = rootContext.blocks.toList(),
            warnings = rootContext.warnings.toList()
        )
    }

    private fun walkChildren(parent: Element, ctx: ParseContext) {
        ctx.flushText()
        var inlineCtx = ctx.detached(
            align = ctx.align,
            currentLinkUrl = ctx.currentLinkUrl,
            listDepth = ctx.listDepth
        )

        fun flushInlineCtx() {
            inlineCtx.flushText()
            val blocks = inlineCtx.blocks
            if (blocks.isNotEmpty()) {
                val currentGroup = mutableListOf<RichTextBlock>()

                fun emitGroup() {
                    if (currentGroup.isEmpty()) return
                    val containsText = currentGroup.any { it is RichTextBlock.Text }
                    if (containsText && currentGroup.size > 1) {
                        ctx.emitBlock(
                            RichTextBlock.InlineGroup(
                                currentGroup.toList(),
                                inlineCtx.align
                            )
                        )
                    } else {
                        currentGroup.forEach { ctx.emitBlock(it) }
                    }
                    currentGroup.clear()
                }

                for (block in blocks) {
                    val isInlineable =
                        (block is RichTextBlock.Text && block.kind == RichTextTextKind.Paragraph) ||
                                block is RichTextBlock.Image ||
                                block is RichTextBlock.AnilistLink

                    if (isInlineable) {
                        if (isStealthBreak(block)) {
                            emitGroup()
                            ctx.emitBlock(block)
                        } else if (block is RichTextBlock.Image) {
                            // Only small, absolute-sized images should naturally flow inline with text (like icons).
                            // Large/unknown images or percents should break the flow to prevent buggy Compose wrapping
                            // and to allow the PostProcessor to assemble adjacent images into grids.
                            val isIcon =
                                block.width != null && !block.isPercent && block.width <= 128 &&
                                    block.floatSide == RichTextFloat.None

                            if (isIcon) {
                                currentGroup.add(block)
                            } else {
                                emitGroup()
                                ctx.emitBlock(block)
                            }
                        } else if (block is RichTextBlock.AnilistLink) {
                            // Anilist links render as large cards. They must break inline flow.
                            emitGroup()
                            ctx.emitBlock(block)
                        } else {
                            currentGroup.add(block)
                        }
                    } else {
                        emitGroup()
                        ctx.emitBlock(block)
                    }
                }
                emitGroup()

                inlineCtx = ctx.detached(
                    align = ctx.align,
                    currentLinkUrl = ctx.currentLinkUrl,
                    listDepth = ctx.listDepth
                )
            }
        }

        for (node in parent.childNodes()) {
            val isBlockNode = node is Element && node.tagName().lowercase() in blockTags

            if (isBlockNode) {
                flushInlineCtx()
                walkNode(node, ctx)
            } else {
                walkNode(node, inlineCtx)
            }
        }
        flushInlineCtx()
    }

    private fun walkNode(node: Node, ctx: ParseContext) {
        when (node) {
            is TextNode -> {
                val text = normalizeWhitespace(node.wholeText)
                if (text.isBlank() && text.contains("\n")) {
                    if (ctx.hasBufferedInlineContent) {
                        ctx.appendText("\n")
                    }
                    return
                }
                inlineParser.parseInto(text, ctx)
            }

            is Element -> walkElement(node, ctx)
        }
    }

    private fun walkElement(element: Element, ctx: ParseContext) {
        val tag = element.tagName().lowercase()
        val textAlign = parseAlignment(tag, element.attr("align"), ctx.align, element.attr("style"))
        val isNewAlign = textAlign != ctx.align

        val workingCtx = if (isNewAlign) {
            ctx.flushText()
            ctx.shared(
                align = textAlign,
                currentLinkUrl = ctx.currentLinkUrl,
                listDepth = ctx.listDepth
            )
        } else {
            ctx
        }

        when (tag) {
            "p" -> {
                workingCtx.flushText()
                walkChildren(element, workingCtx)
                workingCtx.flushText()
            }

            "h1", "h2", "h3", "h4", "h5" -> {
                handleHeading(element, workingCtx, tag[1].digitToInt())
            }

            "spoiler" -> handleSpoilerBlock(element, workingCtx)
            "blockquote" -> handleBlockquote(element, workingCtx)
            "hr" -> {
                workingCtx.flushText()
                val widthAttr = element.attr("width").replace("%", "").toIntOrNull()
                workingCtx.emitBlock(
                    RichTextBlock.HorizontalRule(
                        widthPercent = widthAttr,
                        align = workingCtx.align
                    )
                )
            }

            "pre" -> {
                workingCtx.flushText()
                val codeEl = element.selectFirst("code")
                val code = codeEl?.wholeText() ?: element.wholeText()
                if (code.isNotBlank()) {
                    val trimmed = code.trim()
                    // AniList's asHtml endpoint sometimes fences an entire rich post/bio into
                    // <pre><code> (a markdown misparse). Two recoverable, often combined, signatures:
                    //  • content swallowed into the `language-…` info string — a space-less ``` or
                    //    `~~~` fence that opened straight onto an <img>, a URL, or a centred title
                    //    (e.g. `~~~_Cute Clothes~_`); the swallowed text used to be silently dropped.
                    //  • a `~~~` centre block whose opening fence was eaten, leaving only the closing
                    //    `~~~` at the tail — re-add a leading `~~~` so it re-pairs and centres, like
                    //    AniList's web render (e.g. activity 1091931291 / 1094838111, centred bios).
                    // Recover both and re-parse as rich content instead of dumping a literal code block.
                    val swallowed = codeEl?.let { swallowedLanguageContent(it.attr("class")) }
                    if (swallowed != null || looksLikeWrappedRichText(trimmed)) {
                        val withSwallowed = if (swallowed != null) "$swallowed\n$trimmed" else trimmed
                        val reparseSource =
                            if (needsCenterFenceRepair(trimmed)) "~~~\n$withSwallowed" else withSwallowed
                        val reparsed = Jsoup.parseBodyFragment(
                            RichTextNormalizer.normalize(reparseSource)
                        ).body()
                        walkChildren(reparsed, workingCtx)
                        workingCtx.flushText()
                    } else {
                        workingCtx.emitBlock(
                            RichTextBlock.CodeBlock(
                                code = trimmed,
                                align = workingCtx.align
                            )
                        )
                    }
                }
            }

            "table" -> handleTable(element, workingCtx)
            "ul", "ol" -> handleList(element, workingCtx, isOrdered = tag == "ol")
            "img" -> handleImage(element, workingCtx, workingCtx.currentLinkUrl)
            "br" -> workingCtx.appendInline(RichTextInline.LineBreak)
            "center" -> {
                workingCtx.flushText()
                walkChildren(element, workingCtx)
                workingCtx.flushText()
            }

            "youtube" -> handleYoutubeElement(element, workingCtx)
            "div" -> handleDiv(element, workingCtx)
            "span" -> handleSpan(element, workingCtx)
            "video" -> handleVideoElement(element, workingCtx)
            "b", "strong" -> handleInlineWrapper(element, workingCtx) { RichTextInline.Bold(it) }
            "i", "em" -> handleInlineWrapper(element, workingCtx) { RichTextInline.Italic(it) }
            "del", "strike", "s" -> {
                handleInlineWrapper(element, workingCtx) { RichTextInline.Strikethrough(it) }
            }

            "a" -> handleAnchor(element, workingCtx)
            "code" -> {
                val code = element.wholeText()
                if (code.isNotBlank()) {
                    workingCtx.appendInline(RichTextInline.InlineCode(code))
                }
            }

            "iframe" -> handleIframe(element, workingCtx)
            "style", "head", "script" -> { /* strip CSS/JS blocks entirely */
            }

            "html", "body" -> walkChildren(element, workingCtx)
            else -> walkChildren(element, workingCtx)
        }

        if (isNewAlign) {
            workingCtx.flushText()
        }
    }

    private fun walkInlineChildren(parent: Element, ctx: ParseContext) {
        for (node in parent.childNodes()) {
            when (node) {
                is TextNode -> {
                    val text = normalizeWhitespace(node.wholeText)
                    if (text.isBlank() && text.contains("\n")) {
                        if (ctx.hasBufferedInlineContent) {
                            ctx.appendText("\n")
                        }
                        continue
                    }
                    inlineParser.parseInto(text, ctx)
                }

                is Element -> walkInlineElement(node, ctx)
            }
        }
    }

    private fun walkInlineElement(element: Element, ctx: ParseContext) {
        when (element.tagName().lowercase()) {
            "b", "strong" -> handleInlineWrapper(element, ctx) { RichTextInline.Bold(it) }
            "i", "em" -> handleInlineWrapper(element, ctx) { RichTextInline.Italic(it) }
            "del", "strike", "s" -> handleInlineWrapper(
                element,
                ctx
            ) { RichTextInline.Strikethrough(it) }

            "a" -> handleAnchor(element, ctx)
            "code" -> {
                val code = element.wholeText()
                if (code.isNotBlank()) {
                    ctx.appendInline(RichTextInline.InlineCode(code))
                }
            }

            "br" -> ctx.appendInline(RichTextInline.LineBreak)
            "span" -> handleSpan(element, ctx)
            "img" -> {
                // In an inline context (e.g. an emoji `<img width=20>` after a heading word),
                // keep an emoji-sized image in the text flow instead of breaking to its own block.
                val src = element.attr("src")
                val widthAttr = element.attr("width")
                val hashWidth = widthAttr.startsWith("#")
                val width = if (hashWidth) null else widthAttr.replace(nonDigitRegex, "").toIntOrNull()
                val isPercent = !hashWidth && widthAttr.contains("%")
                if (src.isNotBlank() && !isPercent && width != null && width <= INLINE_IMAGE_MAX_WIDTH) {
                    ctx.appendInline(RichTextInline.Image(upgradeImageScheme(src), width, null))
                } else {
                    handleImage(element, ctx, ctx.currentLinkUrl)
                }
            }
            else -> walkElement(element, ctx)
        }
    }

    private fun handleHeading(element: Element, ctx: ParseContext, level: Int) {
        ctx.flushText()

        val headingCtx = ctx.detached(
            align = ctx.align,
            currentLinkUrl = ctx.currentLinkUrl,
            listDepth = ctx.listDepth
        )
        walkInlineChildren(element, headingCtx)

        if (headingCtx.blocks.isEmpty()) {
            val inlines = trimEdgeInlineText(headingCtx.consumeInlineBufferTrimmed())
            if (!isBlankInlineList(inlines)) {
                ctx.emitBlock(
                    RichTextBlock.Text(
                        inlines = inlines,
                        kind = headingKind(level),
                        align = ctx.align
                    )
                )
            }
            return
        }

        headingCtx.flushText()
        for (block in headingCtx.blocks) {
            ctx.emitBlock(applyHeadingKind(block, headingKind(level)))
        }
    }

    private fun applyHeadingKind(block: RichTextBlock, kind: RichTextTextKind): RichTextBlock =
        when (block) {
            is RichTextBlock.Text -> block.copy(kind = kind)
            is RichTextBlock.InlineGroup -> block.copy(
                children = block.children.map { child ->
                    if (child is RichTextBlock.Text) child.copy(kind = kind) else child
                }
            )

            else -> block
        }

    private fun handleSpoilerBlock(element: Element, ctx: ParseContext) {
        ctx.flushText()
        val spoilerCtx = ctx.detached(
            align = ctx.align,
            currentLinkUrl = ctx.currentLinkUrl,
            listDepth = ctx.listDepth
        )
        walkChildren(element, spoilerCtx)
        spoilerCtx.flushText()
        if (spoilerCtx.blocks.isNotEmpty()) {
            ctx.emitBlock(
                RichTextBlock.Spoiler(
                    children = spoilerCtx.blocks,
                    align = ctx.align
                )
            )
        }
    }

    private fun handleBlockquote(element: Element, ctx: ParseContext) {
        ctx.flushText()
        val quoteCtx = ctx.detached(
            align = ctx.align,
            currentLinkUrl = ctx.currentLinkUrl
        )
        walkChildren(element, quoteCtx)
        quoteCtx.flushText()
        if (quoteCtx.blocks.isNotEmpty()) {
            ctx.emitBlock(
                RichTextBlock.Blockquote(
                    children = quoteCtx.blocks,
                    align = ctx.align
                )
            )
        }
    }

    private fun handleTable(element: Element, ctx: ParseContext) {
        ctx.flushText()
        val rows = mutableListOf<TableRow>()

        for (tr in element.select("tr")) {
            val cells = mutableListOf<TableCell>()
            for (td in tr.children()) {
                if (td.tagName() !in TABLE_CELL_TAGS) continue
                val cellAlign = parseAlignment(td.tagName(), td.attr("align"), ctx.align, td.attr("style"))
                val cellCtx = ctx.detached(
                    align = cellAlign,
                    currentLinkUrl = ctx.currentLinkUrl
                )
                walkChildren(td, cellCtx)
                cellCtx.flushText()
                cells.add(TableCell(cellCtx.blocks.toList(), td.tagName() == "th", cellAlign))
            }
            if (cells.isNotEmpty()) {
                rows.add(TableRow(cells))
            }
        }

        if (rows.isNotEmpty()) {
            ctx.emitBlock(RichTextBlock.Table(rows = rows, align = ctx.align))
        }
    }

    private fun handleList(element: Element, ctx: ParseContext, isOrdered: Boolean) {
        ctx.flushText()
        val listItems = mutableListOf<ListItem>()
        val depth = ctx.listDepth
        val bulletSymbol = bulletSymbol(depth)

        var itemIndex = element.attr("start").toIntOrNull() ?: 1

        val looseCtx = ctx.detached(
            align = ctx.align,
            currentLinkUrl = ctx.currentLinkUrl,
            listDepth = depth + 1
        )

        for (child in element.childNodes()) {
            if (child is Element && child.tagName().lowercase() == "li") {
                looseCtx.flushText()
                if (looseCtx.blocks.isNotEmpty()) {
                    listItems.add(ListItem(looseCtx.blocks.toList(), null))
                    looseCtx.blocks.clear()
                }

                val itemCtx = ctx.detached(
                    align = ctx.align,
                    currentLinkUrl = ctx.currentLinkUrl,
                    listDepth = depth + 1
                )
                walkChildren(child, itemCtx)
                itemCtx.flushText()
                listItems.add(
                    ListItem(
                        children = itemCtx.blocks.toList(),
                        bullet = if (isOrdered) "${itemIndex++}." else bulletSymbol
                    )
                )
                continue
            }

            if (child is Element && child.tagName().lowercase() in NESTED_LIST_TAGS) {
                looseCtx.flushText()
                if (looseCtx.blocks.isNotEmpty()) {
                    listItems.add(ListItem(looseCtx.blocks.toList(), null))
                    looseCtx.blocks.clear()
                }

                walkElement(child, looseCtx)
                looseCtx.flushText()
                if (looseCtx.blocks.isNotEmpty()) {
                    listItems.add(ListItem(looseCtx.blocks.toList(), null))
                    looseCtx.blocks.clear()
                }
                continue
            }

            if (child is TextNode) {
                val text = normalizeWhitespace(child.wholeText)
                if (text.isBlank() && text.contains("\n")) continue
            }

            walkNode(child, looseCtx)
        }

        looseCtx.flushText()
        if (looseCtx.blocks.isNotEmpty()) {
            listItems.add(ListItem(looseCtx.blocks.toList(), null))
        }

        if (listItems.isNotEmpty()) {
            ctx.emitBlock(RichTextBlock.ListBlock(items = listItems, align = ctx.align))
        }
    }

    private fun handleImage(element: Element, ctx: ParseContext, linkUrl: String?) {
        ctx.flushText()
        val src = element.attr("src")
        if (src.isBlank()) return

        val widthAttr = element.attr("width")
        val heightAttr = element.attr("height")
        val hashWidth = widthAttr.startsWith("#")
        ctx.emitBlock(
            RichTextBlock.Image(
                url = upgradeImageScheme(src),
                width = if (hashWidth) null else widthAttr.replace(nonDigitRegex, "").toIntOrNull(),
                height = heightAttr.replace(nonDigitRegex, "").toIntOrNull(),
                isPercent = if (hashWidth) false else widthAttr.contains("%"),
                linkUrl = linkUrl,
                align = ctx.align,
                floatSide = parseImageFloat(element.attr("align"), element.attr("style"))
            )
        )
    }

    /**
     * HTML `align="left|right"` on an <img> (and CSS `float`) floats the image so following content
     * wraps beside it — distinct from block alignment. Vertical aligns (top/middle/bottom) and
     * `center` are not floats.
     */
    private fun parseImageFloat(alignAttr: String, styleAttr: String): RichTextFloat {
        when (alignAttr.trim().lowercase()) {
            "right" -> return RichTextFloat.End
            "left" -> return RichTextFloat.Start
        }
        return when (cssFloat(styleAttr)) {
            "right" -> RichTextFloat.End
            "left" -> RichTextFloat.Start
            else -> RichTextFloat.None
        }
    }

    private fun youtubeUrl(id: String): String =
        if (id.contains("://")) id else "https://www.youtube.com/watch?v=$id"

    private fun handleYoutubeElement(element: Element, ctx: ParseContext) {
        ctx.flushText()
        val id = element.id().trim()
        if (id.isNotBlank()) {
            ctx.emitBlock(
                RichTextBlock.YouTube(
                    videoIdOrUrl = youtubeUrl(id),
                    align = ctx.align
                )
            )
        } else {
            walkChildren(element, ctx)
        }
    }

    private fun handleDiv(element: Element, ctx: ParseContext) {
        if (element.hasClass("youtube")) {
            ctx.flushText()
            val id = element.id().trim()
            if (id.isNotBlank()) {
                ctx.emitBlock(
                    RichTextBlock.YouTube(
                        videoIdOrUrl = youtubeUrl(id),
                        align = ctx.align
                    )
                )
            }
            return
        }

        if (element.attr("rel").lowercase() == "spoiler") {
            handleSpoilerBlock(element, ctx)
            return
        }

        walkChildren(element, ctx)
    }

    private fun handleSpan(element: Element, ctx: ParseContext) {
        if (element.hasClass("markdown_spoiler")) {
            handleSpoilerBlock(element, ctx)
            return
        }

        if (hasBlockChildren(element)) {
            walkChildren(element, ctx)
        } else {
            walkInlineChildren(element, ctx)
        }
    }

    private fun handleVideoElement(element: Element, ctx: ParseContext) {
        ctx.flushText()
        var src = element.attr("src")
        if (src.isBlank()) {
            src = element.getElementsByTag("source").firstOrNull()?.attr("src") ?: ""
        }
        if (src.isNotBlank()) {
            ctx.emitBlock(RichTextBlock.Video(url = src.trim(), align = ctx.align))
        }
    }

    private fun handleAnchor(element: Element, ctx: ParseContext) {
        val href = decodeHtmlAttribute(element.attr("href").trim()).trim('"', '\'', '\\')
        val image = element.selectFirst("img")
        if (image != null && href.isNotBlank() && element.text().isBlank()) {
            handleImage(image, ctx, href)
            return
        }

        if (href.isBlank()) {
            walkInlineChildren(element, ctx)
            return
        }

        val subCtx = ctx.detached(
            align = ctx.align,
            currentLinkUrl = href,
            listDepth = ctx.listDepth
        )
        walkInlineChildren(element, subCtx)

        if (subCtx.blocks.isEmpty()) {
            val inlines = trimEdgeInlineText(subCtx.consumeInlineBufferTrimmed())
            if (!isBlankInlineList(inlines)) {
                ctx.appendInline(RichTextInline.Link(url = href, children = inlines))
            }
            return
        }

        subCtx.flushText()
        ctx.flushText()
        for (block in subCtx.blocks) {
            ctx.emitBlock(applyLinkToBlock(block, href))
        }
    }

    private fun applyLinkToBlock(block: RichTextBlock, href: String): RichTextBlock {
        if (block is RichTextBlock.Image && block.linkUrl == null) return block.copy(linkUrl = href)
        return transformTextInlines(block) { inlines -> listOf(RichTextInline.Link(href, inlines)) }
    }

    private fun handleInlineWrapper(
        element: Element,
        ctx: ParseContext,
        wrap: (List<RichTextInline>) -> RichTextInline
    ) {
        val subCtx = ctx.detached(
            align = ctx.align,
            currentLinkUrl = ctx.currentLinkUrl,
            listDepth = ctx.listDepth
        )
        walkInlineChildren(element, subCtx)

        if (subCtx.blocks.isEmpty()) {
            val inlines = trimEdgeInlineText(subCtx.consumeInlineBufferTrimmed())
            if (!isBlankInlineList(inlines)) {
                ctx.appendInline(wrap(inlines))
            }
            return
        }

        subCtx.flushText()
        ctx.flushText()
        for (block in subCtx.blocks) {
            ctx.emitBlock(transformTextInlines(block) { inlines -> listOf(wrap(inlines)) })
        }
    }

    private fun transformTextInlines(
        block: RichTextBlock,
        transform: (List<RichTextInline>) -> List<RichTextInline>
    ): RichTextBlock = when (block) {
        is RichTextBlock.Text -> block.copy(inlines = transform(block.inlines))
        is RichTextBlock.InlineGroup -> block.copy(
            children = block.children.map { transformTextInlines(it, transform) }
        )

        is RichTextBlock.Spoiler -> block.copy(
            children = block.children.map { transformTextInlines(it, transform) }
        )

        is RichTextBlock.Blockquote -> block.copy(
            children = block.children.map { transformTextInlines(it, transform) }
        )

        is RichTextBlock.ListBlock -> block.copy(
            items = block.items.map { item ->
                item.copy(children = item.children.map { transformTextInlines(it, transform) })
            }
        )

        is RichTextBlock.Table -> block.copy(
            rows = block.rows.map { row ->
                row.copy(
                    cells = row.cells.map { cell ->
                        cell.copy(children = cell.children.map {
                            transformTextInlines(
                                it,
                                transform
                            )
                        })
                    }
                )
            }
        )

        else -> block
    }

    private fun handleIframe(element: Element, ctx: ParseContext) {
        ctx.flushText()
        val src = element.attr("src")
        if (src.contains("youtube", ignoreCase = true) || src.contains(
                "youtu.be",
                ignoreCase = true
            )
        ) {
            ctx.emitBlock(RichTextBlock.YouTube(videoIdOrUrl = src, align = ctx.align))
        } else if (src.isNotBlank()) {
            ctx.emitBlock(RichTextBlock.Video(url = src, align = ctx.align))
        }
    }

    private fun decodeHtmlAttribute(value: String): String =
        org.jsoup.parser.Parser.unescapeEntities(value, false)

    private fun looksLikeEscapedHtml(text: String): Boolean {
        val sample = if (text.length > 500) text.substring(0, 500) else text
        val tagCount = ESCAPED_HTML_TAG_REGEX.findAll(sample).count()
        return tagCount >= 3
    }

    /**
     * True when a `<pre><code>` block is really an AniList rich post that the asHtml endpoint
     * wrongly wrapped, rather than a genuine code listing. Broader than [looksLikeEscapedHtml]:
     * also fires on markdown-heavy posts that carry too few HTML tags for the tag-count
     * heuristic — a single image, a markdown link/image to a URL, or an AniList spoiler / youtube
     * / center construct. Pure source code (no images, links, or AniList markup) stays a code block.
     */
    private fun looksLikeWrappedRichText(text: String): Boolean =
        looksLikeEscapedHtml(text) ||
            WRAPPED_RICH_HTML_TAG_REGEX.containsMatchIn(text) ||
            WRAPPED_MARKDOWN_LINK_REGEX.containsMatchIn(text) ||
            WRAPPED_MARKDOWN_IMAGE_REGEX.containsMatchIn(text) ||
            text.contains("markdown_spoiler") ||
            text.contains("class='youtube'") ||
            text.contains("class=\"youtube\"") ||
            text.contains("~!") ||
            text.contains("~~~")

    /**
     * A `<code class="language-…">` info string normally names a language (e.g. `language-kotlin`).
     * When AniList's fence parser swallows post content into it (an unclosed/space-less ```` ```<img …> ````
     * or a `~~~` centre fence opened straight onto its first line), the info string carries real
     * markup, a URL, or a centred title instead. Returns that swallowed content so the caller can
     * re-parse it, or null for a genuine language token.
     */
    private fun swallowedLanguageContent(codeClass: String): String? {
        val trimmed = codeClass.trim()
        if (!trimmed.startsWith("language-")) return null
        val info = trimmed.removePrefix("language-")
        if (info.isBlank()) return null
        // A real language token is a short identifier (kotlin, c++, objective-c). AniList's fence
        // misparse stuffs real post content here instead — an HTML tag, a bare URL, or markdown/prose
        // such as a centred italic title (`_Cute Clothes~_`). Anything that isn't a plain language
        // identifier is recovered so it isn't silently dropped (the centred title used to vanish).
        return info.takeUnless { LANGUAGE_TOKEN_REGEX.matches(it) }
    }

    /**
     * True when [trimmed] carries a single dangling `~~~` centre fence at its tail. AniList's asHtml
     * misparse eats the opening `~~~` (into the fence / language token) but leaves the closing one,
     * so an odd `~~~` count ending in `~~~` means exactly one unpaired closer — a leading `~~~` can
     * be re-added to re-pair and centre the post. An even (balanced) count is left untouched so a
     * genuine inline `~~~ … ~~~` span isn't hijacked.
     */
    private fun needsCenterFenceRepair(trimmed: String): Boolean =
        trimmed.endsWith("~~~") && TRIPLE_TILDE_REGEX.findAll(trimmed).count() % 2 == 1

    private fun hasBlockChildren(element: Element): Boolean =
        element.children().any { it.tagName().lowercase() in blockTags }

    private fun isStealthBreak(block: RichTextBlock): Boolean {
        if (block !is RichTextBlock.Text || block.kind != RichTextTextKind.Paragraph) return false
        if (block.inlines.size != 1) return false
        var current: RichTextInline = block.inlines.first()
        while (true) {
            when (current) {
                is RichTextInline.Text -> return current.value == "\u200B"
                is RichTextInline.Link -> if (current.children.size == 1) current =
                    current.children.first() else return false

                is RichTextInline.Bold -> if (current.children.size == 1) current =
                    current.children.first() else return false

                is RichTextInline.Italic -> if (current.children.size == 1) current =
                    current.children.first() else return false

                is RichTextInline.BoldItalic -> if (current.children.size == 1) current =
                    current.children.first() else return false

                is RichTextInline.Strikethrough -> if (current.children.size == 1) current =
                    current.children.first() else return false

                else -> return false
            }
        }
    }
}