package com.anisync.android.domain.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

internal data class HtmlParseResult(
    val blocks: List<RichTextBlock>,
    val warnings: List<ParseWarning>
)

internal class RichTextHtmlParser(
    private val inlineParser: RichTextInlineParser
) {
    private val nonDigitRegex = Regex("[^0-9]")

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
        for (node in parent.childNodes()) {
            walkNode(node, ctx)
        }
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
        val textAlign = parseAlignment(tag, element.attr("align"), ctx.align)
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
                val code = element.selectFirst("code")?.wholeText() ?: element.wholeText()
                if (code.isNotBlank()) {
                    val trimmed = code.trim()
                    if (looksLikeEscapedHtml(trimmed)) {
                        val reparsed = Jsoup.parseBodyFragment(
                            RichTextNormalizer.normalize(trimmed)
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
            "style", "head", "script" -> { /* strip CSS/JS blocks entirely */ }
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
            "del", "strike", "s" -> handleInlineWrapper(element, ctx) { RichTextInline.Strikethrough(it) }
            "a" -> handleAnchor(element, ctx)
            "code" -> {
                val code = element.wholeText()
                if (code.isNotBlank()) {
                    ctx.appendInline(RichTextInline.InlineCode(code))
                }
            }

            "br" -> ctx.appendInline(RichTextInline.LineBreak)
            "span" -> handleSpan(element, ctx)
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

    private fun applyHeadingKind(block: RichTextBlock, kind: RichTextTextKind): RichTextBlock = when (block) {
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
                if (td.tagName() !in listOf("td", "th")) continue
                val cellAlign = parseAlignment(td.tagName(), td.attr("align"), ctx.align)
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
        val bulletSymbol = when (depth % 3) {
            0 -> "•"
            1 -> "◦"
            else -> "▪"
        }

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

            if (child is Element && child.tagName().lowercase() in listOf("ul", "ol")) {
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
                url = src,
                width = if (hashWidth) null else widthAttr.replace(nonDigitRegex, "").toIntOrNull(),
                height = heightAttr.replace(nonDigitRegex, "").toIntOrNull(),
                isPercent = if (hashWidth) false else widthAttr.contains("%"),
                linkUrl = linkUrl,
                align = ctx.align
            )
        )
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
                        cell.copy(children = cell.children.map { transformTextInlines(it, transform) })
                    }
                )
            }
        )

        else -> block
    }

    private fun handleIframe(element: Element, ctx: ParseContext) {
        ctx.flushText()
        val src = element.attr("src")
        if (src.contains("youtube", ignoreCase = true) || src.contains("youtu.be", ignoreCase = true)) {
            ctx.emitBlock(RichTextBlock.YouTube(videoIdOrUrl = src, align = ctx.align))
        } else if (src.isNotBlank()) {
            ctx.emitBlock(RichTextBlock.Video(url = src, align = ctx.align))
        }
    }

    private fun decodeHtmlAttribute(value: String): String =
        org.jsoup.parser.Parser.unescapeEntities(value, false)

    private fun looksLikeEscapedHtml(text: String): Boolean {
        val sample = if (text.length > 500) text.substring(0, 500) else text
        val tagCount = Regex("""<[a-zA-Z/][^>]*>""").findAll(sample).count()
        return tagCount >= 3
    }

    private val blockTags = setOf(
        "p", "div", "ul", "ol", "li", "table", "blockquote",
        "h1", "h2", "h3", "h4", "h5", "hr", "pre", "img", "br"
    )

    private fun hasBlockChildren(element: Element): Boolean =
        element.children().any { it.tagName().lowercase() in blockTags }
}
