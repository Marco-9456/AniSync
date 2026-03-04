package com.anisync.android.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

// =============================================================================
// Pre-compiled Regex Constants
// =============================================================================

private val NON_DIGIT_REGEX = Regex("[^0-9]")
private val WHITESPACE_REGEX = Regex("[\\s]+")
private val HEADER_REGEX = Regex("^(#{1,5})\\s+(.*)")
private val HEADER_NO_SPACE_REGEX = Regex("^(#{1,5})([^#\\s].*)")
private val BOLD_WRAPPED_HEADER_REGEX = Regex("^(?:\\*\\*|__)(#{1,5})\\s*(.*?)(?:\\*\\*|__)\\s*$")
private val IMAGE_MD_REGEX = Regex("""img(\d+%?)?\(([^)]+)\)""")
private val BARE_URL_REGEX = Regex("""https?://[^\s<>"\[\]~!`)]+""")

// =============================================================================
// Public Composable
// =============================================================================

/**
 * Renders server-side AniList HTML (from `body(asHtml: true)`) as styled Compose content.
 *
 * Uses Jsoup for robust DOM parsing, then walks the tree to produce a list of
 * [RenderBlock]s. Text content flows within a single [AnnotatedString] as much as
 * possible (paragraphs separated by `\n\n`, headers/blockquotes/lists via SpanStyle).
 * Only images, code blocks, spoilers, horizontal rules, and video/YouTube embeds
 * break out into separate composables.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AniListHtmlRenderer(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val spoilerColor = MaterialTheme.colorScheme.onSurfaceVariant

    val blocks = remember(html, linkColor, codeBackground, spoilerColor) {
        parseHtmlToBlocks(html, linkColor, codeBackground, spoilerColor)
    }

    val allImageUrls = remember(blocks) { collectImageUrls(blocks) }
    var viewerInitialIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var i = 0
        while (i < blocks.size) {
            when (val block = blocks[i]) {
                is RenderBlock.Text -> {
                    if (block.annotatedString.isNotBlank()) {
                        Text(
                            text = block.annotatedString,
                            style = style.copy(color = color),
                            textAlign = block.textAlign,
                            modifier = if (block.textAlign == TextAlign.Center)
                                Modifier.fillMaxWidth() else Modifier
                        )
                    }
                }

                is RenderBlock.Image -> {
                    // Group consecutive images into a FlowRow
                    val imageGroup = mutableListOf(block)
                    while (i + 1 < blocks.size && blocks[i + 1] is RenderBlock.Image) {
                        i++
                        imageGroup.add(blocks[i] as RenderBlock.Image)
                    }

                    if (imageGroup.size > 1) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            imageGroup.forEach { img ->
                                AsyncImage(
                                    model = img.url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = imageModifier(img)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val idx = allImageUrls.indexOf(img.url)
                                            if (idx >= 0) viewerInitialIndex = idx
                                        }
                                )
                            }
                        }
                    } else {
                        val img = imageGroup.first()
                        AsyncImage(
                            model = img.url,
                            contentDescription = null,
                            contentScale = if (img.width != null) ContentScale.Fit else ContentScale.FillWidth,
                            modifier = imageModifier(img)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val idx = allImageUrls.indexOf(img.url)
                                    if (idx >= 0) viewerInitialIndex = idx
                                }
                        )
                    }
                }

                is RenderBlock.Code -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(codeBackground)
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = block.code,
                            style = style.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = color
                            )
                        )
                    }
                }

                is RenderBlock.Spoiler -> {
                    SpoilerBlock(
                        content = block.children,
                        style = style,
                        color = color,
                        linkColor = linkColor,
                        spoilerColor = spoilerColor,
                        codeBackground = codeBackground,
                        onImageClick = { url ->
                            val idx = allImageUrls.indexOf(url)
                            if (idx >= 0) viewerInitialIndex = idx
                        }
                    )
                }

                is RenderBlock.Blockquote -> {
                    BlockquoteBlock(
                        content = block.children,
                        style = style,
                        color = color,
                        linkColor = linkColor,
                        spoilerColor = spoilerColor,
                        codeBackground = codeBackground,
                        onImageClick = { url ->
                            val idx = allImageUrls.indexOf(url)
                            if (idx >= 0) viewerInitialIndex = idx
                        }
                    )
                }

                is RenderBlock.HorizontalRule -> {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = spoilerColor.copy(alpha = 0.5f))) {
                                append("\u2501".repeat(28))
                            }
                        },
                        style = style
                    )
                }

                is RenderBlock.YouTube -> {
                    val annotated = buildAnnotatedString {
                        pushLink(
                            LinkAnnotation.Url(
                                block.url,
                                TextLinkStyles(
                                    SpanStyle(
                                        color = linkColor,
                                        textDecoration = TextDecoration.Underline
                                    )
                                )
                            )
                        )
                        append("\u25B6 YouTube Video")
                        pop()
                    }
                    Text(text = annotated, style = style.copy(color = color))
                }

                is RenderBlock.Video -> {
                    VideoPlayer(url = block.url)
                }
            }
            i++
        }
    }

    viewerInitialIndex?.let { index ->
        ImageViewerDialog(
            imageUrls = allImageUrls,
            initialIndex = index,
            onDismiss = { viewerInitialIndex = null }
        )
    }
}

// =============================================================================
// Spoiler Block (click-to-reveal)
// =============================================================================

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SpoilerBlock(
    content: List<RenderBlock>,
    style: TextStyle,
    color: Color,
    linkColor: Color,
    spoilerColor: Color,
    codeBackground: Color,
    onImageClick: (String) -> Unit
) {
    var revealed by remember { mutableStateOf(false) }

    if (!revealed) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(spoilerColor.copy(alpha = 0.15f))
                .clickable { revealed = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Spoiler, click to view",
                style = style.copy(
                    color = spoilerColor.copy(alpha = 0.6f),
                    fontStyle = FontStyle.Italic
                )
            )
        }
    } else {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(spoilerColor.copy(alpha = 0.08f))
                .clickable { revealed = false }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            var j = 0
            while (j < content.size) {
                when (val block = content[j]) {
                    is RenderBlock.Text -> {
                        if (block.annotatedString.isNotBlank()) {
                            Text(
                                text = block.annotatedString,
                                style = style.copy(color = color),
                                textAlign = block.textAlign,
                                modifier = if (block.textAlign == TextAlign.Center)
                                    Modifier.fillMaxWidth() else Modifier
                            )
                        }
                    }

                    is RenderBlock.Image -> {
                        val imageGroup = mutableListOf(block)
                        while (j + 1 < content.size && content[j + 1] is RenderBlock.Image) {
                            j++
                            imageGroup.add(content[j] as RenderBlock.Image)
                        }
                        if (imageGroup.size > 1) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                imageGroup.forEach { img ->
                                    AsyncImage(
                                        model = img.url,
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = imageModifier(img)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onImageClick(img.url) }
                                    )
                                }
                            }
                        } else {
                            val img = imageGroup.first()
                            AsyncImage(
                                model = img.url,
                                contentDescription = null,
                                contentScale = if (img.width != null) ContentScale.Fit else ContentScale.FillWidth,
                                modifier = imageModifier(img)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onImageClick(img.url) }
                            )
                        }
                    }

                    is RenderBlock.Code -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(codeBackground)
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = block.code,
                                style = style.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = color
                                )
                            )
                        }
                    }

                    is RenderBlock.Spoiler -> {
                        SpoilerBlock(
                            content = block.children,
                            style = style,
                            color = color,
                            linkColor = linkColor,
                            spoilerColor = spoilerColor,
                            codeBackground = codeBackground,
                            onImageClick = onImageClick
                        )
                    }

                    is RenderBlock.Blockquote -> {
                        BlockquoteBlock(
                            content = block.children,
                            style = style,
                            color = color,
                            linkColor = linkColor,
                            spoilerColor = spoilerColor,
                            codeBackground = codeBackground,
                            onImageClick = onImageClick
                        )
                    }

                    is RenderBlock.HorizontalRule -> {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = spoilerColor.copy(alpha = 0.5f))) {
                                    append("\u2501".repeat(28))
                                }
                            },
                            style = style
                        )
                    }

                    is RenderBlock.YouTube -> {
                        val annotated = buildAnnotatedString {
                            pushLink(
                                LinkAnnotation.Url(
                                    block.url,
                                    TextLinkStyles(
                                        SpanStyle(
                                            color = linkColor,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    )
                                )
                            )
                            append("\u25B6 YouTube Video")
                            pop()
                        }
                        Text(text = annotated, style = style.copy(color = color))
                    }

                    is RenderBlock.Video -> {
                        VideoPlayer(url = block.url)
                    }
                }
                j++
            }
        }
    }
}

// =============================================================================
// Blockquote Block (left-border styled recursive content)
// =============================================================================

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BlockquoteBlock(
    content: List<RenderBlock>,
    style: TextStyle,
    color: Color,
    linkColor: Color,
    spoilerColor: Color,
    codeBackground: Color,
    onImageClick: (String) -> Unit
) {
    val borderColor = spoilerColor.copy(alpha = 0.4f)
    val quoteStyle = style.copy(
        color = spoilerColor,
        fontStyle = FontStyle.Italic
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(4.dp.toPx(), 0f),
                    end = Offset(4.dp.toPx(), size.height),
                    strokeWidth = 3.dp.toPx()
                )
            }
            .padding(start = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var j = 0
        while (j < content.size) {
            when (val block = content[j]) {
                is RenderBlock.Text -> {
                    if (block.annotatedString.isNotBlank()) {
                        Text(
                            text = block.annotatedString,
                            style = quoteStyle,
                            textAlign = block.textAlign,
                            modifier = if (block.textAlign == TextAlign.Center)
                                Modifier.fillMaxWidth() else Modifier
                        )
                    }
                }

                is RenderBlock.Image -> {
                    val imageGroup = mutableListOf(block)
                    while (j + 1 < content.size && content[j + 1] is RenderBlock.Image) {
                        j++
                        imageGroup.add(content[j] as RenderBlock.Image)
                    }
                    if (imageGroup.size > 1) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            imageGroup.forEach { img ->
                                AsyncImage(
                                    model = img.url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = imageModifier(img)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onImageClick(img.url) }
                                )
                            }
                        }
                    } else {
                        val img = imageGroup.first()
                        AsyncImage(
                            model = img.url,
                            contentDescription = null,
                            contentScale = if (img.width != null) ContentScale.Fit else ContentScale.FillWidth,
                            modifier = imageModifier(img)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onImageClick(img.url) }
                        )
                    }
                }

                is RenderBlock.Code -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(codeBackground)
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = block.code,
                            style = style.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = color
                            )
                        )
                    }
                }

                is RenderBlock.Spoiler -> {
                    SpoilerBlock(
                        content = block.children,
                        style = style,
                        color = color,
                        linkColor = linkColor,
                        spoilerColor = spoilerColor,
                        codeBackground = codeBackground,
                        onImageClick = onImageClick
                    )
                }

                is RenderBlock.Blockquote -> {
                    // Nested blockquote
                    BlockquoteBlock(
                        content = block.children,
                        style = style,
                        color = color,
                        linkColor = linkColor,
                        spoilerColor = spoilerColor,
                        codeBackground = codeBackground,
                        onImageClick = onImageClick
                    )
                }

                is RenderBlock.HorizontalRule -> {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = spoilerColor.copy(alpha = 0.5f))) {
                                append("\u2501".repeat(28))
                            }
                        },
                        style = style
                    )
                }

                is RenderBlock.YouTube -> {
                    val annotated = buildAnnotatedString {
                        pushLink(
                            LinkAnnotation.Url(
                                block.url,
                                TextLinkStyles(
                                    SpanStyle(
                                        color = linkColor,
                                        textDecoration = TextDecoration.Underline
                                    )
                                )
                            )
                        )
                        append("\u25B6 YouTube Video")
                        pop()
                    }
                    Text(text = annotated, style = style.copy(color = color))
                }

                is RenderBlock.Video -> {
                    VideoPlayer(url = block.url)
                }
            }
            j++
        }
    }
}

// =============================================================================
// Image URL Extraction (for multi-image viewer)
// =============================================================================

/** Recursively collects all image URLs from a list of [RenderBlock]s. */
private fun collectImageUrls(blocks: List<RenderBlock>): List<String> {
    val urls = mutableListOf<String>()
    for (block in blocks) {
        when (block) {
            is RenderBlock.Image -> urls.add(block.url)
            is RenderBlock.Spoiler -> urls.addAll(collectImageUrls(block.children))
            is RenderBlock.Blockquote -> urls.addAll(collectImageUrls(block.children))
            else -> { /* no images */ }
        }
    }
    return urls
}

// =============================================================================
// Image Modifier Helper
// =============================================================================

private fun imageModifier(img: RenderBlock.Image): Modifier {
    return if (img.isPercentWidth && img.width != null) {
        Modifier.fillMaxWidth(img.width / 100f)
    } else if (img.width != null) {
        Modifier.widthIn(max = img.width.dp)
    } else {
        Modifier.fillMaxWidth()
    }
}

// =============================================================================
// Data Models
// =============================================================================

internal sealed class RenderBlock {
    data class Text(
        val annotatedString: AnnotatedString,
        val textAlign: TextAlign = TextAlign.Unspecified
    ) : RenderBlock()
    data class Image(
        val url: String,
        val width: Int? = null,
        val isPercentWidth: Boolean = false,
        val linkUrl: String? = null
    ) : RenderBlock()

    data class Code(val code: String) : RenderBlock()
    data class Spoiler(val children: List<RenderBlock>) : RenderBlock()
    data class Blockquote(val children: List<RenderBlock>) : RenderBlock()
    data object HorizontalRule : RenderBlock()
    data class YouTube(val url: String) : RenderBlock()
    data class Video(val url: String) : RenderBlock()
}

// =============================================================================
// Parse Context
// =============================================================================

/**
 * Mutable context used while walking the DOM tree.
 * Accumulates inline spans into an [AnnotatedString.Builder] and flushes
 * them as [RenderBlock.Text] only when a non-text block element is encountered
 * (image, code block, spoiler, HR, YouTube, video).
 *
 * Uses a `var builder` that gets replaced with a fresh instance on flush,
 * since [AnnotatedString.Builder] has no clear/reset method.
 */
private class Ctx(
    val linkColor: Color,
    val codeBackground: Color,
    val spoilerColor: Color,
    val textAlign: TextAlign = TextAlign.Unspecified
) {
    var builder: AnnotatedString.Builder = AnnotatedString.Builder()
    var hasContent = false

    /** Flush accumulated text into a [RenderBlock.Text], if any content exists. */
    fun flushText(blocks: MutableList<RenderBlock>) {
        if (hasContent) {
            val str = builder.toAnnotatedString()
            val trimmed = trimAnnotatedString(str)
            if (trimmed.isNotBlank()) {
                blocks.add(RenderBlock.Text(trimmed, textAlign))
            }
            builder = AnnotatedString.Builder()
            hasContent = false
        }
    }

    /**
     * Ensures a paragraph break (`\n\n`) before new paragraph content.
     * No-op if builder is empty or already ends with `\n\n`.
     */
    fun ensureParagraphBreak() {
        if (!hasContent) return
        val text = builder.toAnnotatedString().text
        if (text.isEmpty()) return
        when {
            text.endsWith("\n\n") -> {}
            text.endsWith("\n") -> builder.append("\n")
            else -> builder.append("\n\n")
        }
    }

    /** Ensures a single newline at end (for list items, blockquote lines). */
    fun ensureNewline() {
        if (!hasContent) return
        val text = builder.toAnnotatedString().text
        if (text.isEmpty() || text.endsWith("\n")) return
        builder.append("\n")
    }

    fun appendNewline() {
        builder.append("\n")
        hasContent = true
    }

    fun appendText(text: String) {
        if (text.isNotEmpty()) {
            builder.append(text)
            hasContent = true
        }
    }
}

// =============================================================================
// HTML Parsing Engine (Jsoup)
// =============================================================================

/**
 * Parses server-rendered AniList HTML into a flat list of [RenderBlock]s.
 *
 * Text content flows within a single [AnnotatedString] builder as much as possible.
 * Paragraphs are separated by `\n\n`, line breaks by `\n`. Headers, blockquotes,
 * and lists are rendered via SpanStyle within the text flow.
 * Only images, code blocks, spoilers, horizontal rules, and video/YouTube embeds
 * cause a flush into a separate block.
 */
internal fun parseHtmlToBlocks(
    html: String,
    linkColor: Color,
    codeBackground: Color,
    spoilerColor: Color
): List<RenderBlock> {
    if (html.isBlank()) return emptyList()

    val doc = Jsoup.parseBodyFragment(html)
    doc.outputSettings().prettyPrint(false)

    val blocks = mutableListOf<RenderBlock>()
    val ctx = Ctx(linkColor, codeBackground, spoilerColor)

    walkChildren(doc.body(), blocks, ctx)
    ctx.flushText(blocks)

    return blocks
}

// =============================================================================
// DOM Walking — Block-Level Dispatch
// =============================================================================

/** Walks all child nodes of [parent], dispatching each to the appropriate handler. */
private fun walkChildren(
    parent: Element,
    blocks: MutableList<RenderBlock>,
    ctx: Ctx
) {
    for (node in parent.childNodes()) {
        walkNode(node, blocks, ctx)
    }
}

private fun walkNode(
    node: Node,
    blocks: MutableList<RenderBlock>,
    ctx: Ctx
) {
    when (node) {
        is TextNode -> {
            val raw = node.wholeText
            // Normalize HTML whitespace: collapse runs of whitespace to single space
            val normalized = normalizeWhitespace(raw)
            if (normalized.isNotEmpty()) {
                // Don't append leading space if builder is empty or ends with newline
                val text = builder(ctx).toAnnotatedString().text
                val trimmed = if (!ctx.hasContent || text.isEmpty() || text.endsWith('\n')) {
                    normalized.trimStart()
                } else {
                    normalized
                }
                if (trimmed.isEmpty()) return

                // Detect bare --- horizontal rule (body-level raw markdown)
                if (trimmed.trim() == "---" || trimmed.trim() == "***" || trimmed.trim() == "___") {
                    ctx.flushText(blocks)
                    blocks.add(RenderBlock.HorizontalRule)
                    return
                }

                // Suppress bare # or > prefix that appears before an element
                // (AniList generates patterns like #<ul>... or ><ul>...)
                val stripped = trimmed.trim()
                if (stripped.length <= 5 && stripped.all { it == '#' || it == '>' || it == ' ' }) {
                    // Bare markdown prefix with no actual content — suppress
                    return
                }

                // Route through parseInlineMarkdown so raw **bold**, _italic_,
                // [link](url), img(), bare URLs, etc. in body-level text nodes are properly parsed
                parseInlineMarkdown(trimmed, ctx.builder, ctx, blocks)
                ctx.hasContent = true
            }
        }
        is Element -> walkElement(node, blocks, ctx)
    }
}

/** Helper to access context builder (avoids repeated ctx.builder). */
private fun builder(ctx: Ctx): AnnotatedString.Builder = ctx.builder

private fun walkElement(
    element: Element,
    blocks: MutableList<RenderBlock>,
    ctx: Ctx
) {
    val tag = element.tagName().lowercase()

    when (tag) {
        // --- Paragraphs: insert paragraph break, render children inline ---
        "p" -> {
            ctx.ensureParagraphBreak()
            walkChildren(element, blocks, ctx)
        }

        // --- Headers: render inline with SpanStyle (stays in text flow) ---
        "h1", "h2", "h3", "h4", "h5" -> {
            ctx.ensureParagraphBreak()
            val level = tag[1].digitToInt()
            ctx.builder.withStyle(headerSpanStyle(level)) {
                renderInline(element, this, ctx, blocks)
            }
            ctx.hasContent = true
        }

        // --- Blockquotes: render as Blockquote block with children ---
        "blockquote" -> {
            ctx.flushText(blocks)
            val bqBlocks = mutableListOf<RenderBlock>()
            val bqCtx = Ctx(ctx.linkColor, ctx.codeBackground, ctx.spoilerColor)
            walkChildren(element, bqBlocks, bqCtx)
            bqCtx.flushText(bqBlocks)
            if (bqBlocks.isNotEmpty()) {
                blocks.add(RenderBlock.Blockquote(bqBlocks))
            }
        }

        // --- Horizontal rule: flush text, emit HR block ---
        "hr" -> {
            ctx.flushText(blocks)
            blocks.add(RenderBlock.HorizontalRule)
        }

        // --- Code blocks: flush text, emit Code block ---
        "pre" -> {
            ctx.flushText(blocks)
            val codeEl = element.selectFirst("code")
            val code = codeEl?.wholeText() ?: element.wholeText()
            if (code.isNotBlank()) {
                blocks.add(RenderBlock.Code(code.trim()))
            }
        }

        // --- Lists: render inline within text flow ---
        "ul" -> {
            ctx.ensureParagraphBreak()
            renderUnorderedList(element, ctx, blocks, depth = 0)
        }
        "ol" -> {
            ctx.ensureParagraphBreak()
            renderOrderedList(element, ctx, blocks, depth = 0)
        }

        // --- Images: flush text, emit Image block ---
        "img" -> {
            handleImage(element, blocks, ctx, linkUrl = null)
        }

        // --- Line break ---
        "br" -> {
            ctx.appendNewline()
        }

        // --- Center: AniList doesn't convert markdown inside <center> tags,
        //     so TextNode children contain raw markdown that needs parsing. ---
        "center" -> {
            handleCenter(element, blocks, ctx)
        }

        // --- Divs: check for YouTube, or pass through ---
        "div" -> {
            handleDiv(element, blocks, ctx)
        }

        // --- Spans: check for spoilers, otherwise pass through ---
        "span" -> {
            handleSpan(element, blocks, ctx)
        }

        // --- Video elements: <video><source src='...'></video> ---
        "video" -> {
            ctx.flushText(blocks)
            val src = element.attr("src").ifBlank {
                element.selectFirst("source")?.attr("src") ?: ""
            }
            if (src.isNotBlank()) {
                blocks.add(RenderBlock.Video(src))
            }
        }

        // --- Inline formatting (stays in text flow) ---
        "b", "strong" -> {
            ctx.builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                renderInline(element, this, ctx, blocks)
            }
            ctx.hasContent = true
        }

        "i", "em" -> {
            ctx.builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                renderInline(element, this, ctx, blocks)
            }
            ctx.hasContent = true
        }

        "del", "strike", "s" -> {
            ctx.builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                renderInline(element, this, ctx, blocks)
            }
            ctx.hasContent = true
        }

        "a" -> {
            handleAnchor(element, ctx, blocks)
        }

        "code" -> {
            // Inline code (not inside <pre>, which is handled above)
            ctx.builder.withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = ctx.codeBackground,
                    fontSize = 13.sp
                )
            ) {
                append(element.wholeText())
            }
            ctx.hasContent = true
        }

        // --- Iframe (legacy fallback) ---
        "iframe" -> {
            ctx.flushText(blocks)
            val src = element.attr("src")
            if (src.contains("youtube", ignoreCase = true) || src.contains("youtu.be", ignoreCase = true)) {
                blocks.add(RenderBlock.YouTube(src))
            } else if (src.isNotBlank()) {
                blocks.add(RenderBlock.Video(src))
            }
        }

        // --- Anything else: walk children ---
        else -> {
            walkChildren(element, blocks, ctx)
        }
    }
}

// =============================================================================
// Anchor Handling (<a> tags)
// =============================================================================

/**
 * Handles `<a>` elements:
 * 1. `<a href="..."><img ...></a>` — linked image → emit as Image block
 * 2. `<a href="...">text</a>` — hyperlink rendered inline
 * 3. `<a>text</a>` (no href) — colored text (AniList convention)
 */
private fun handleAnchor(
    element: Element,
    ctx: Ctx,
    blocks: MutableList<RenderBlock>
) {
    val href = element.attr("href")

    // Check for linked image: <a href="..."><img ...></a>
    val img = element.selectFirst("img")
    if (img != null && href.isNotBlank()) {
        handleImage(img, blocks, ctx, linkUrl = href)
        return
    }

    // Check for img() markdown in anchor text: <a href="...">img(url)</a>
    val anchorText = element.text().trim()
    val imgMdMatch = IMAGE_MD_REGEX.matchEntire(anchorText)
    if (imgMdMatch != null && href.isNotBlank()) {
        ctx.flushText(blocks)
        val imgUrl = imgMdMatch.groupValues[2]
        val sizeStr = imgMdMatch.groupValues[1]
        val isPercent = sizeStr.contains("%")
        val widthNum = sizeStr.replace(NON_DIGIT_REGEX, "").toIntOrNull()
        blocks.add(RenderBlock.Image(imgUrl, widthNum, isPercent, href))
        return
    }

    if (href.isNotBlank()) {
        // Save builder reference BEFORE pushLink — ctx.builder may be replaced
        // by flushText() during renderInline if block-level content is encountered.
        val savedBuilder = ctx.builder
        savedBuilder.pushLink(
            LinkAnnotation.Url(
                href,
                TextLinkStyles(
                    SpanStyle(
                        color = ctx.linkColor,
                        textDecoration = TextDecoration.Underline
                    )
                )
            )
        )
        renderInline(element, savedBuilder, ctx, blocks)
        savedBuilder.pop()
    } else {
        // <a> without href = colored text (AniList convention)
        ctx.builder.withStyle(SpanStyle(color = ctx.linkColor)) {
            renderInline(element, this, ctx, blocks)
        }
    }
    ctx.hasContent = true
}

// =============================================================================
// Image Handling
// =============================================================================

/**
 * Handles `<img>` elements. Flushes text and emits an Image block.
 * AniList uses: `<img width='100%' src='URL'>`, `<img width='150' src='URL'>`,
 * or `<img width='' src='URL'>`. Width can be percentage, pixels, or empty.
 */
private fun handleImage(
    element: Element,
    blocks: MutableList<RenderBlock>,
    ctx: Ctx,
    linkUrl: String?
) {
    ctx.flushText(blocks)
    val src = element.attr("src")
    if (src.isNotBlank()) {
        val widthAttr = element.attr("width")
        val isPercent = widthAttr.contains("%")
        val widthNum = widthAttr.replace(NON_DIGIT_REGEX, "").toIntOrNull()
        blocks.add(RenderBlock.Image(src, widthNum, isPercent, linkUrl))
    }
}

// =============================================================================
// Div Handling (YouTube, Generic)
// =============================================================================

/**
 * Handles `<div>` elements:
 * - `<div class='youtube' id='VIDEO_ID'></div>` — YouTube embed (empty div, ID in id attr)
 * - Generic divs: walk children as block content
 */
private fun handleDiv(
    element: Element,
    blocks: MutableList<RenderBlock>,
    ctx: Ctx
) {
    val cls = element.className().lowercase()

    // YouTube embed: <div class='youtube' id='VIDEO_ID'></div>
    if (cls.contains("youtube")) {
        ctx.flushText(blocks)
        val videoId = element.id()
        if (videoId.isNotBlank()) {
            blocks.add(RenderBlock.YouTube("https://www.youtube.com/watch?v=$videoId"))
        }
        return
    }

    // User-written spoiler div: <div rel="spoiler">content</div>
    val rel = element.attr("rel").lowercase()
    if (rel == "spoiler") {
        ctx.flushText(blocks)
        val spoilerBlocks = mutableListOf<RenderBlock>()
        val spoilerCtx = Ctx(ctx.linkColor, ctx.codeBackground, ctx.spoilerColor)
        walkChildren(element, spoilerBlocks, spoilerCtx)
        spoilerCtx.flushText(spoilerBlocks)
        if (spoilerBlocks.isNotEmpty()) {
            blocks.add(RenderBlock.Spoiler(spoilerBlocks))
        }
        return
    }

    // Generic div: walk children
    walkChildren(element, blocks, ctx)
}

// =============================================================================
// Span Handling (Spoilers)
// =============================================================================

/**
 * Handles `<span>` elements:
 * - `<span class='markdown_spoiler'><span>content</span></span>` — Spoiler
 * - Otherwise: pass through as inline content
 */
private fun handleSpan(
    element: Element,
    blocks: MutableList<RenderBlock>,
    ctx: Ctx
) {
    val cls = element.className().lowercase()

    // Spoiler: <span class='markdown_spoiler'><span>content</span></span>
    if (cls.contains("markdown_spoiler")) {
        ctx.flushText(blocks)
        val spoilerBlocks = mutableListOf<RenderBlock>()
        val spoilerCtx = Ctx(ctx.linkColor, ctx.codeBackground, ctx.spoilerColor)

        // AniList wraps spoiler content in a nested span:
        // <span class='markdown_spoiler'><span>actual content</span></span>
        val innerSpan = element.selectFirst(":root > span")
        if (innerSpan != null) {
            walkChildren(innerSpan, spoilerBlocks, spoilerCtx)
        } else {
            walkChildren(element, spoilerBlocks, spoilerCtx)
        }
        spoilerCtx.flushText(spoilerBlocks)

        if (spoilerBlocks.isNotEmpty()) {
            blocks.add(RenderBlock.Spoiler(spoilerBlocks))
        }
        return
    }

    // Regular span: render children inline
    renderInline(element, ctx.builder, ctx, blocks)
    ctx.hasContent = true
}

// =============================================================================
// Center Handling (raw markdown inside <center> tags)
// =============================================================================

/**
 * Handles `<center>` elements. AniList does NOT convert markdown inside `<center>` tags,
 * so TextNode children may contain raw markdown that needs parsing.
 * Element children are already HTML and are processed normally.
 */
private fun handleCenter(
    element: Element,
    blocks: MutableList<RenderBlock>,
    ctx: Ctx
) {
    // Flush any text accumulated before this <center> block
    ctx.flushText(blocks)

    // Create a new context with center alignment
    val centerCtx = Ctx(
        linkColor = ctx.linkColor,
        codeBackground = ctx.codeBackground,
        spoilerColor = ctx.spoilerColor,
        textAlign = TextAlign.Center
    )

    // AniList does NOT convert markdown inside <center> tags, so TextNodes
    // may contain raw markdown at any depth. We walk children recursively
    // but route all TextNodes through the markdown parser instead of
    // the normal whitespace-normalizing text path.
    walkCenterChildren(element, blocks, centerCtx)

    // Flush any remaining centered text
    centerCtx.flushText(blocks)
}

/**
 * Recursively walks children of a `<center>` element.
 * Element children that are known HTML tags (video, img, a, etc.) are dispatched
 * to [walkElement] normally. Inline formatting tags (b, i, em, strong, etc.) are
 * also dispatched to [walkElement] so that HTML-based formatting inside center is
 * preserved. For any other elements (like Jsoup-inserted wrappers), we recurse
 * into their children. TextNodes at ANY depth are parsed as raw markdown.
 */
private fun walkCenterChildren(
    parent: Element,
    blocks: MutableList<RenderBlock>,
    ctx: Ctx
) {
    for (child in parent.childNodes()) {
        when (child) {
            is TextNode -> {
                val raw = child.wholeText
                if (raw.isNotBlank()) {
                    parseMarkdownText(raw, blocks, ctx)
                }
            }
            is Element -> {
                val tag = child.tagName().lowercase()
                // Tags that are real HTML content from the API — process normally
                when (tag) {
                    "video", "img", "a", "div", "span", "br", "hr",
                    "pre", "code", "ul", "ol",
                    // Inline formatting tags (may appear if user mixes HTML inside center)
                    "b", "strong", "i", "em", "del", "strike", "s",
                    // Block-level tags that AniList may generate
                    "h1", "h2", "h3", "h4", "h5", "blockquote",
                    // Paragraph tags Jsoup may insert
                    "p" -> {
                        walkElement(child, blocks, ctx)
                    }
                    // Wrapper tags or unknown tags: recurse to find TextNodes and
                    // real elements within
                    else -> {
                        walkCenterChildren(child, blocks, ctx)
                    }
                }
            }
        }
    }
}

/**
 * Parses raw markdown text (from inside `<center>` tags) into the block/text flow.
 * Handles block-level markdown: headers (#-#####), paragraph breaks (\n\n), line breaks (\n).
 * Inline markdown within each line is handled by [parseInlineMarkdown].
 *
 * Also handles patterns where bold/italic wraps a header:
 *   `__#Header text__` → bold header
 *   `**#Header text**` → bold header
 */
private fun parseMarkdownText(
    text: String,
    blocks: MutableList<RenderBlock>,
    ctx: Ctx
) {
    val lines = text.split("\n")

    for (line in lines) {
        if (line.isBlank()) {
            ctx.ensureParagraphBreak()
            continue
        }

        val trimmedLine = line.trimStart()

        // Check for standard headers: # through #####
        val headerMatch = HEADER_REGEX.find(trimmedLine)
        if (headerMatch != null) {
            ctx.ensureParagraphBreak()
            val level = headerMatch.groupValues[1].length
            val content = headerMatch.groupValues[2]
            ctx.builder.withStyle(headerSpanStyle(level)) {
                parseInlineMarkdown(content, this, ctx, blocks)
            }
            ctx.hasContent = true
            continue
        }

        // Check for headers without space: #text (common in AniList)
        val headerNoSpaceMatch = HEADER_NO_SPACE_REGEX.find(trimmedLine)
        if (headerNoSpaceMatch != null) {
            ctx.ensureParagraphBreak()
            val level = headerNoSpaceMatch.groupValues[1].length
            val content = headerNoSpaceMatch.groupValues[2]
            ctx.builder.withStyle(headerSpanStyle(level)) {
                parseInlineMarkdown(content, this, ctx, blocks)
            }
            ctx.hasContent = true
            continue
        }

        // Check for bold-wrapped headers: __#Header__ or **#Header**
        val boldWrappedHeaderMatch = BOLD_WRAPPED_HEADER_REGEX.find(trimmedLine)
        if (boldWrappedHeaderMatch != null) {
            ctx.ensureParagraphBreak()
            val level = boldWrappedHeaderMatch.groupValues[1].length
            val content = boldWrappedHeaderMatch.groupValues[2]
            ctx.builder.withStyle(headerSpanStyle(level)) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    parseInlineMarkdown(content, this, ctx, blocks)
                }
            }
            ctx.hasContent = true
            continue
        }

        // Regular text line with inline markdown
        if (ctx.hasContent) {
            val currentText = ctx.builder.toAnnotatedString().text
            if (currentText.isNotEmpty() && !currentText.endsWith("\n")) {
                ctx.builder.append("\n")
            }
        }
        parseInlineMarkdown(line, ctx.builder, ctx, blocks)
        ctx.hasContent = true
    }
}

/**
 * Parses inline markdown text into the [builder].
 * Handles: ~~~center~~~, img(url), **bold**, __bold__, *italic*, _italic_,
 * ~~strikethrough~~, `code`, [text](url), bare URLs.
 *
 * @param blocks Optional block list for emitting Image blocks from img() markdown.
 *               When null, img() patterns are rendered as plain text.
 */
private fun parseInlineMarkdown(
    text: String,
    builder: AnnotatedString.Builder,
    ctx: Ctx,
    blocks: MutableList<RenderBlock>? = null
) {
    var i = 0
    val len = text.length

    while (i < len) {
        when {
            // AniList image markdown: img(url), img150(url), img90%(url)
            // Must be checked early so img() inside ~~~...~~~ is handled after stripping.
            blocks != null && i + 3 < len && text[i] == 'i' && text[i + 1] == 'm'
                    && text[i + 2] == 'g' && (text[i + 3] == '(' || text[i + 3].isDigit()) -> {
                val remaining = text.substring(i)
                val imgMatch = IMAGE_MD_REGEX.find(remaining)
                if (imgMatch != null && imgMatch.range.first == 0) {
                    // Flush any accumulated text before the image
                    ctx.flushText(blocks)
                    val sizeStr = imgMatch.groupValues[1]
                    val url = imgMatch.groupValues[2]
                    val isPercent = sizeStr.endsWith("%")
                    val widthNum = sizeStr.replace("%", "").toIntOrNull()
                    blocks.add(RenderBlock.Image(url, widthNum, isPercent))
                    i += imgMatch.value.length
                } else {
                    builder.append(text[i])
                    i++
                }
            }
            // Center markers: ~~~text~~~ (AniList centering syntax)
            // MUST be checked BEFORE ~~ strikethrough to avoid misinterpretation.
            i + 2 < len && text[i] == '~' && text[i + 1] == '~' && text[i + 2] == '~' -> {
                val end = text.indexOf("~~~", i + 3)
                if (end != -1) {
                    val inner = text.substring(i + 3, end)
                    parseInlineMarkdown(inner, builder, ctx, blocks)
                    i = end + 3
                } else {
                    builder.append(text[i])
                    i++
                }
            }
            // Bold+Italic: ***text***
            i + 2 < len && text[i] == '*' && text[i + 1] == '*' && text[i + 2] == '*' -> {
                val end = text.indexOf("***", i + 3)
                if (end != -1) {
                    builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        parseInlineMarkdown(text.substring(i + 3, end), this, ctx, blocks)
                    }
                    i = end + 3
                } else {
                    builder.append(text[i])
                    i++
                }
            }
            // Bold+Italic: ___text___
            i + 2 < len && text[i] == '_' && text[i + 1] == '_' && text[i + 2] == '_' -> {
                val end = text.indexOf("___", i + 3)
                if (end != -1) {
                    builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        parseInlineMarkdown(text.substring(i + 3, end), this, ctx, blocks)
                    }
                    i = end + 3
                } else {
                    builder.append(text[i])
                    i++
                }
            }
            // Bold: **text**
            i + 1 < len && text[i] == '*' && text[i + 1] == '*' -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        parseInlineMarkdown(text.substring(i + 2, end), this, ctx, blocks)
                    }
                    i = end + 2
                } else {
                    builder.append(text[i])
                    i++
                }
            }
            // Bold: __text__
            i + 1 < len && text[i] == '_' && text[i + 1] == '_' -> {
                val end = text.indexOf("__", i + 2)
                if (end != -1) {
                    builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        parseInlineMarkdown(text.substring(i + 2, end), this, ctx, blocks)
                    }
                    i = end + 2
                } else {
                    builder.append(text[i])
                    i++
                }
            }
            // Strikethrough: ~~text~~
            i + 1 < len && text[i] == '~' && text[i + 1] == '~' -> {
                val end = text.indexOf("~~", i + 2)
                if (end != -1) {
                    builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        parseInlineMarkdown(text.substring(i + 2, end), this, ctx, blocks)
                    }
                    i = end + 2
                } else {
                    builder.append(text[i])
                    i++
                }
            }
            // Inline code: `text`
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    builder.withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = ctx.codeBackground,
                            fontSize = 13.sp
                        )
                    ) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    builder.append(text[i])
                    i++
                }
            }
            // Link: [text](url)
            text[i] == '[' -> {
                val closeBracket = text.indexOf(']', i + 1)
                if (closeBracket != -1 && closeBracket + 1 < len && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val linkText = text.substring(i + 1, closeBracket)
                        val url = text.substring(closeBracket + 2, closeParen)
                        builder.pushLink(
                            LinkAnnotation.Url(
                                url,
                                TextLinkStyles(
                                    SpanStyle(
                                        color = ctx.linkColor,
                                        textDecoration = TextDecoration.Underline
                                    )
                                )
                            )
                        )
                        parseInlineMarkdown(linkText, builder, ctx, blocks)
                        builder.pop()
                        i = closeParen + 1
                    } else {
                        builder.append(text[i])
                        i++
                    }
                } else {
                    builder.append(text[i])
                    i++
                }
            }
            // Bare URL: https://... or http://...
            text[i] == 'h' && (text.startsWith("https://", i) || text.startsWith("http://", i)) -> {
                val urlMatch = BARE_URL_REGEX.find(text, i)
                if (urlMatch != null && urlMatch.range.first == i) {
                    val url = urlMatch.value
                    builder.pushLink(
                        LinkAnnotation.Url(
                            url,
                            TextLinkStyles(
                                SpanStyle(
                                    color = ctx.linkColor,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        )
                    )
                    builder.append(url)
                    builder.pop()
                    i = urlMatch.range.last + 1
                } else {
                    builder.append(text[i])
                    i++
                }
            }
            // Italic: *text*
            text[i] == '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end != -1) {
                    builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        parseInlineMarkdown(text.substring(i + 1, end), this, ctx, blocks)
                    }
                    i = end + 1
                } else {
                    builder.append(text[i])
                    i++
                }
            }
            // Italic: _text_
            text[i] == '_' -> {
                val end = text.indexOf('_', i + 1)
                if (end != -1) {
                    builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        parseInlineMarkdown(text.substring(i + 1, end), this, ctx, blocks)
                    }
                    i = end + 1
                } else {
                    builder.append(text[i])
                    i++
                }
            }
            // Regular character
            else -> {
                builder.append(text[i])
                i++
            }
        }
    }
}

// =============================================================================
// Blockquote Rendering (inline with prefix)
// =============================================================================

/**
 * Renders a `<blockquote>` inline within the text flow.
 * Uses `\u2503 ` (┃) prefix per nesting depth with italic style.
 *
 * AniList HTML pattern:
 *   `<blockquote>\n<p>text<br>more text</p>\n</blockquote>`
 * Nested: `<blockquote><blockquote><p>text</p></blockquote></blockquote>`
 */
private fun renderBlockquote(
    element: Element,
    blocks: MutableList<RenderBlock>,
    ctx: Ctx,
    depth: Int
) {
    val prefix = "\u2503 ".repeat(depth)

    for (child in element.children()) {
        val childTag = child.tagName().lowercase()
        when (childTag) {
            "blockquote" -> {
                renderBlockquote(child, blocks, ctx, depth + 1)
            }
            "p" -> {
                ctx.ensureNewline()
                ctx.builder.withStyle(
                    SpanStyle(fontStyle = FontStyle.Italic, color = ctx.spoilerColor)
                ) {
                    append(prefix)
                    renderBlockquoteInline(child, this, ctx, blocks, prefix)
                }
                ctx.hasContent = true
            }
            else -> {
                // Other elements (images, etc.) inside blockquote
                ctx.ensureNewline()
                ctx.builder.withStyle(
                    SpanStyle(fontStyle = FontStyle.Italic, color = ctx.spoilerColor)
                ) {
                    append(prefix)
                }
                ctx.hasContent = true
                walkElement(child, blocks, ctx)
            }
        }
    }
}

/**
 * Renders inline content within a blockquote paragraph.
 * `<br>` inserts newline + blockquote prefix continuation.
 */
private fun renderBlockquoteInline(
    element: Element,
    builder: AnnotatedString.Builder,
    ctx: Ctx,
    blocks: MutableList<RenderBlock>,
    prefix: String
) {
    var isFirstContent = true
    for (child in element.childNodes()) {
        when (child) {
            is TextNode -> {
                val normalized = normalizeWhitespace(child.wholeText)
                val text = if (isFirstContent) normalized.trimStart() else normalized
                if (text.isNotEmpty()) {
                    parseInlineMarkdown(text, builder, ctx, blocks)
                    isFirstContent = false
                }
            }
            is Element -> {
                val tag = child.tagName().lowercase()
                when (tag) {
                    "br" -> builder.append("\n$prefix")
                    "b", "strong" -> builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        renderBlockquoteInline(child, this, ctx, blocks, prefix)
                    }
                    "i", "em" -> builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        renderBlockquoteInline(child, this, ctx, blocks, prefix)
                    }
                    "del", "strike", "s" -> builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        renderBlockquoteInline(child, this, ctx, blocks, prefix)
                    }
                    "a" -> {
                        val href = child.attr("href")
                        if (href.isNotBlank()) {
                            builder.pushLink(
                                LinkAnnotation.Url(
                                    href,
                                    TextLinkStyles(
                                        SpanStyle(
                                            color = ctx.linkColor,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    )
                                )
                            )
                            renderBlockquoteInline(child, builder, ctx, blocks, prefix)
                            builder.pop()
                        } else {
                            builder.withStyle(SpanStyle(color = ctx.linkColor)) {
                                renderBlockquoteInline(child, this, ctx, blocks, prefix)
                            }
                        }
                    }
                    "code" -> builder.withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = ctx.codeBackground,
                            fontSize = 13.sp
                        )
                    ) {
                        append(child.wholeText())
                    }
                    "span" -> {
                        val spanCls = child.className().lowercase()
                        if (spanCls.contains("markdown_spoiler")) {
                            // Spoiler inside blockquote: break out into Spoiler block
                            ctx.hasContent = true
                            handleSpan(child, blocks, ctx)
                        } else {
                            renderBlockquoteInline(child, builder, ctx, blocks, prefix)
                        }
                    }
                    else -> renderBlockquoteInline(child, builder, ctx, blocks, prefix)
                }
                isFirstContent = false
            }
        }
    }
}

// =============================================================================
// List Rendering (inline within text flow)
// =============================================================================

/** Renders `<ul>` inline using bullet prefix. */
private fun renderUnorderedList(
    element: Element,
    ctx: Ctx,
    blocks: MutableList<RenderBlock>,
    depth: Int
) {
    val indent = "  ".repeat(depth)
    var first = true

    for (child in element.childNodes()) {
        when (child) {
            is TextNode -> {
                val raw = child.wholeText
                // Suppress bare markdown prefixes: lone # or > before content
                val trimmed = raw.trim()
                if (trimmed == "#" || trimmed == ">") continue
                val normalized = normalizeWhitespace(raw)
                if (normalized.isNotBlank()) {
                    if (ctx.hasContent) ctx.ensureNewline()
                    parseInlineMarkdown(normalized.trim(), ctx.builder, ctx, blocks)
                    ctx.hasContent = true
                    first = false
                }
            }
            is Element -> {
                val tag = child.tagName().lowercase()
                if (tag == "li") {
                    if (!first || ctx.hasContent) {
                        ctx.ensureNewline()
                    }
                    first = false
                    ctx.appendText("$indent  \u2022 ")
                    renderListItem(child, ctx, blocks, depth)
                } else {
                    // Non-<li> elements (a, strong, etc.) — render inline
                    if (ctx.hasContent) ctx.ensureNewline()
                    renderInlineForCtx(child, ctx, blocks)
                    first = false
                }
            }
        }
    }
}

/**
 * Renders `<ol>` inline using numbered prefix.
 * Supports `start` attribute: `<ol start="6">`.
 */
private fun renderOrderedList(
    element: Element,
    ctx: Ctx,
    blocks: MutableList<RenderBlock>,
    depth: Int
) {
    val indent = "  ".repeat(depth)
    val startAttr = element.attr("start")
    var idx = if (startAttr.isNotBlank()) startAttr.toIntOrNull() ?: 1 else 1
    var first = true

    for (li in element.children()) {
        if (li.tagName().lowercase() != "li") continue

        if (!first || ctx.hasContent) {
            ctx.ensureNewline()
        }
        first = false

        ctx.appendText("$indent  ${idx++}. ")
        renderListItem(li, ctx, blocks, depth)
    }
}

/** Renders content of a single `<li>` element. */
private fun renderListItem(
    li: Element,
    ctx: Ctx,
    blocks: MutableList<RenderBlock>,
    depth: Int
) {
    var isFirstContent = true
    for (child in li.childNodes()) {
        when (child) {
            is TextNode -> {
                val normalized = normalizeWhitespace(child.wholeText)
                val text = if (isFirstContent) normalized.trimStart() else normalized
                if (text.isNotEmpty()) {
                    parseInlineMarkdown(text, ctx.builder, ctx, blocks)
                    ctx.hasContent = true
                    isFirstContent = false
                }
            }
            is Element -> {
                val tag = child.tagName().lowercase()
                when (tag) {
                    "ul" -> renderUnorderedList(child, ctx, blocks, depth + 1)
                    "ol" -> renderOrderedList(child, ctx, blocks, depth + 1)
                    "img" -> handleImage(child, blocks, ctx, linkUrl = null)
                    "br" -> ctx.appendNewline()
                    "p" -> {
                        // <p> inside <li>: render inline content
                        renderInline(child, ctx.builder, ctx, blocks)
                        ctx.hasContent = true
                    }
                    else -> {
                        renderInlineForCtx(child, ctx, blocks)
                    }
                }
                isFirstContent = false
            }
        }
    }
}

// =============================================================================
// Inline Rendering
// =============================================================================

/**
 * Renders all children of [element] as inline content into [builder].
 * Handles nested inline formatting (bold, italic, links, code, etc.)
 * and breaks out of inline flow for block-level content (images, spoilers).
 *
 * IMPORTANT: All text operations use [ctx].builder instead of the [builder]
 * parameter to avoid stale references when ctx.flushText() replaces the builder.
 * The [builder] parameter is kept for API compatibility with withStyle blocks.
 */
private fun renderInline(
    element: Element,
    builder: AnnotatedString.Builder,
    ctx: Ctx,
    blocks: MutableList<RenderBlock>
) {
    for (child in element.childNodes()) {
        when (child) {
            is TextNode -> {
                val text = normalizeWhitespace(child.wholeText)
                if (text.isNotEmpty()) {
                    parseInlineMarkdown(text, ctx.builder, ctx, blocks)
                    ctx.hasContent = true
                }
            }
            is Element -> {
                val tag = child.tagName().lowercase()
                when (tag) {
                    "b", "strong" -> ctx.builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        renderInline(child, this, ctx, blocks)
                    }
                    "i", "em" -> ctx.builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        renderInline(child, this, ctx, blocks)
                    }
                    "del", "strike", "s" -> ctx.builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        renderInline(child, this, ctx, blocks)
                    }
                    "a" -> handleAnchor(child, ctx, blocks)
                    "code" -> ctx.builder.withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = ctx.codeBackground,
                            fontSize = 13.sp
                        )
                    ) {
                        append(child.wholeText())
                    }
                    "br" -> ctx.appendNewline()
                    "img" -> {
                        // Image inside inline context: break out into Image block
                        ctx.hasContent = true
                        handleImage(child, blocks, ctx, linkUrl = null)
                    }
                    "span" -> handleSpan(child, blocks, ctx)
                    // Block-level elements that can appear inside inline contexts
                    // (e.g. <center>, <div>, <p>, etc. nested in <a>, <b>, etc.)
                    // Delegate to walkElement to preserve block semantics.
                    "center", "div", "p", "ul", "ol",
                    "h1", "h2", "h3", "h4", "h5",
                    "blockquote", "hr", "pre", "video", "iframe" -> {
                        walkElement(child, blocks, ctx)
                    }
                    else -> renderInline(child, ctx.builder, ctx, blocks)
                }
                ctx.hasContent = true
            }
        }
    }
}

/**
 * Renders an element's inline content directly into the context builder.
 * Used when we need to render inline content outside a withStyle block.
 */
private fun renderInlineForCtx(
    element: Element,
    ctx: Ctx,
    blocks: MutableList<RenderBlock>
) {
    val tag = element.tagName().lowercase()
    when (tag) {
        "b", "strong" -> {
            ctx.builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                renderInline(element, this, ctx, blocks)
            }
            ctx.hasContent = true
        }
        "i", "em" -> {
            ctx.builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                renderInline(element, this, ctx, blocks)
            }
            ctx.hasContent = true
        }
        "del", "strike", "s" -> {
            ctx.builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                renderInline(element, this, ctx, blocks)
            }
            ctx.hasContent = true
        }
        "a" -> handleAnchor(element, ctx, blocks)
        "code" -> {
            ctx.builder.withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = ctx.codeBackground,
                    fontSize = 13.sp
                )
            ) {
                append(element.wholeText())
            }
            ctx.hasContent = true
        }
        "span" -> handleSpan(element, blocks, ctx)
        else -> {
            // Unknown/block-level tags: delegate to walkElement to preserve semantics
            walkElement(element, blocks, ctx)
        }
    }
}

// =============================================================================
// Whitespace Normalization
// =============================================================================

/**
 * Normalizes HTML whitespace: collapses runs of whitespace (spaces, tabs, newlines)
 * into a single space. This handles the source-level whitespace that Jsoup's
 * TextNode.wholeText preserves (indentation, newlines between tags, etc.).
 */
private fun normalizeWhitespace(text: String): String {
    return text.replace(WHITESPACE_REGEX, " ")
}

// =============================================================================
// Helpers
// =============================================================================

/** Trims leading and trailing newlines/whitespace from an AnnotatedString. */
private fun trimAnnotatedString(str: AnnotatedString): AnnotatedString {
    val text = str.text
    val start = text.indexOfFirst { it != '\n' && it != '\r' && it != ' ' }
    val end = text.indexOfLast { it != '\n' && it != '\r' && it != ' ' }
    if (start == -1 || end == -1 || start > end) return AnnotatedString("")
    if (start == 0 && end == text.length - 1) return str
    return str.subSequence(start, end + 1)
}

private fun headerSpanStyle(level: Int): SpanStyle = when (level) {
    1 -> SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    2 -> SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
    3 -> SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
    4 -> SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    5 -> SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    else -> SpanStyle(fontWeight = FontWeight.Bold)
}
