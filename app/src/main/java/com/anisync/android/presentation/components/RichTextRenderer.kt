package com.anisync.android.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
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
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.anisync.android.domain.LinkPreview
import com.anisync.android.domain.parser.LinkPreviewKey
import com.anisync.android.domain.parser.ParsedRichText
import com.anisync.android.domain.parser.ParserConfig
import com.anisync.android.domain.parser.RichTextAlignment
import com.anisync.android.domain.parser.RichTextBlock
import com.anisync.android.domain.parser.RichTextFloat
import com.anisync.android.domain.parser.RichTextInline
import com.anisync.android.domain.parser.RichTextParser
import com.anisync.android.domain.parser.RichTextTextKind
import com.anisync.android.presentation.util.LocalLinkPreviewProvider
import com.anisync.android.presentation.util.rememberAniLinkRouter
import com.anisync.android.presentation.util.shimmerEffect

/**
 * Process-wide LRU cache (keyed by raw HTML) of parsed rich text. A status/message body re-enters
 * composition every time its card is recycled back into view in a lazy list; without caching, each
 * re-entry re-parses the HTML and flashes the estimate→content height change — which the card's
 * animateContentSize then replays as a "settling" wiggle on every scroll-back. Seeding from here
 * lets a revisited body render at full height on the first frame, so nothing animates, and skips the
 * repeat parse. Bounded to 200 entries; android.util.LruCache is internally synchronized.
 */
private object RichTextParseCache {
    private val cache = android.util.LruCache<String, ParsedRichText>(200)
    fun get(html: String): ParsedRichText? = cache.get(html)
    fun put(html: String, parsed: ParsedRichText) {
        cache.put(html, parsed)
    }
}

@Composable
fun AsyncRichTextRenderer(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    codeBackground: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    spoilerColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    // Seed from the parse cache so a body re-entering composition (its card recycled back into view)
    // renders at full height immediately instead of re-parsing through the estimate→content height
    // change — which would otherwise replay the card's resize animation on every scroll-back. A cold
    // body still parses once, then populates the cache for next time.
    var parsedText by remember(html) { mutableStateOf(RichTextParseCache.get(html)) }

    LaunchedEffect(html) {
        if (parsedText == null) {
            val result = RichTextParser.parse(html, ParserConfig())
            RichTextParseCache.put(html, result)
            parsedText = result
        }
    }

    val estimatedMinHeight = remember(html) {
        val lineEstimate = (html.length / 50).coerceIn(1, 15)
        (lineEstimate * 20).dp
    }

    Box(
        modifier = modifier.then(
            if (parsedText == null) Modifier.heightIn(min = estimatedMinHeight) else Modifier
        )
    ) {
        parsedText?.let {
            RichTextRenderer(
                parsedData = it,
                style = style,
                color = color,
                linkColor = linkColor,
                codeBackground = codeBackground,
                spoilerColor = spoilerColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RichTextRenderer(
    parsedData: ParsedRichText,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    codeBackground: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    spoilerColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    var viewerInitialIndex by remember { mutableStateOf<Int?>(null) }
    val linkRouter = rememberAniLinkRouter()

    val previewProvider = LocalLinkPreviewProvider.current
    val previews = remember { mutableStateMapOf<LinkPreviewKey, LinkPreview>() }
    LaunchedEffect(parsedData, previewProvider) {
        if (previewProvider == null) return@LaunchedEffect
        val anilistLinks = collectAnilistLinks(parsedData.blocks)
        if (anilistLinks.isEmpty()) return@LaunchedEffect
        val fetched = previewProvider.getPreviews(anilistLinks)
        previews.putAll(fetched)
    }

    // A single, stable click listener for every inline link. Built once per router
    // identity (the router is now remembered), so the AnnotatedString link annotations
    // keep stable identity across recompositions. Previously a per-recomposition
    // UriHandler override changed the LocalUriHandler value on every recomposition,
    // tearing down the Text link pointer-input node mid-gesture and dropping taps.
    val linkListener = remember(linkRouter) {
        LinkInteractionListener { link ->
            if (link is LinkAnnotation.Clickable) linkRouter.navigate(link.tag)
        }
    }

    SelectionContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RenderBlocks(
                blocks = parsedData.blocks,
                style = style,
                color = color,
                linkColor = linkColor,
                codeBackground = codeBackground,
                spoilerColor = spoilerColor,
                previews = previews,
                onImageClick = { url ->
                    val idx = parsedData.imageUrls.indexOf(url)
                    if (idx >= 0) viewerInitialIndex = idx
                },
                onLinkClick = { linkRouter.navigate(it) },
                linkListener = linkListener
            )
        }
    }

    viewerInitialIndex?.let { index ->
        ImageViewerDialog(
            imageUrls = parsedData.imageUrls,
            initialIndex = index,
            onDismiss = { viewerInitialIndex = null }
        )
    }
}

private fun collectAnilistLinks(blocks: List<RichTextBlock>): List<RichTextBlock.AnilistLink> {
    val result = ArrayList<RichTextBlock.AnilistLink>()
    collectAnilistLinksInto(blocks, result)
    return result
}

private fun collectAnilistLinksInto(
    blocks: List<RichTextBlock>,
    out: MutableList<RichTextBlock.AnilistLink>
) {
    for (block in blocks) {
        when (block) {
            is RichTextBlock.AnilistLink -> out.add(block)
            is RichTextBlock.Spoiler -> collectAnilistLinksInto(block.children, out)
            is RichTextBlock.Blockquote -> collectAnilistLinksInto(block.children, out)
            is RichTextBlock.ListBlock -> block.items.forEach { collectAnilistLinksInto(it.children, out) }
            is RichTextBlock.Table -> block.rows.forEach { row ->
                row.cells.forEach { cell -> collectAnilistLinksInto(cell.children, out) }
            }
            is RichTextBlock.InlineGroup -> block.children.forEach {
                if (it is RichTextBlock.AnilistLink) out.add(it)
            }
            else -> Unit
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RenderBlocks(
    blocks: List<RichTextBlock>,
    style: TextStyle,
    color: Color,
    linkColor: Color,
    codeBackground: Color,
    spoilerColor: Color,
    previews: Map<LinkPreviewKey, LinkPreview>,
    onImageClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    linkListener: LinkInteractionListener
) {
    // A floated image (HTML <img align="left|right">) lets the blocks after it wrap alongside it.
    // Render the blocks before the float normally, then hand the float image + the remaining blocks
    // to RichFloatLayout: it places the wrap content beside the image and lets later content reflow
    // to full width once it clears the image's bottom.
    val floatIndex = blocks.indexOfFirst {
        it is RichTextBlock.Image && it.floatSide != RichTextFloat.None
    }
    if (floatIndex in 0 until blocks.lastIndex) {
        for (i in 0 until floatIndex) {
            RenderSingleBlock(
                blocks[i], style, color, linkColor, codeBackground, spoilerColor,
                previews, onImageClick, onLinkClick, linkListener
            )
        }
        val floatImg = blocks[floatIndex] as RichTextBlock.Image
        RichFloatLayout(
            floatSide = floatImg.floatSide,
            image = { RichImage(floatImg, onImageClick, onLinkClick) },
            flowContent = blocks.subList(floatIndex + 1, blocks.size).map { child ->
                @Composable {
                    RenderSingleBlock(
                        child, style, color, linkColor, codeBackground, spoilerColor,
                        previews, onImageClick, onLinkClick, linkListener
                    )
                }
            }
        )
        return
    }

    for (block in blocks) {
        RenderSingleBlock(
            block, style, color, linkColor, codeBackground, spoilerColor,
            previews, onImageClick, onLinkClick, linkListener
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RenderSingleBlock(
    block: RichTextBlock,
    style: TextStyle,
    color: Color,
    linkColor: Color,
    codeBackground: Color,
    spoilerColor: Color,
    previews: Map<LinkPreviewKey, LinkPreview>,
    onImageClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    linkListener: LinkInteractionListener
) {
    run {
        val blockAlignment = when (block.align.toTextAlign()) {
            TextAlign.Center -> Alignment.CenterHorizontally
            TextAlign.Right -> Alignment.End
            else -> Alignment.Start
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = blockAlignment,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (block) {
                is RichTextBlock.Text -> {
                    // A centered heading/line that ends with inline image(s) — e.g. an emoji after
                    // "…the Heavens" — can't stay centered through Compose text layout: a wrapped
                    // line holding only inline content gets left-aligned and won't sit on the
                    // previous line. For that case, peel the trailing images off and render them in
                    // a row the parent Column centers. Internal inline images stay in the text.
                    val trailingImages = block.inlines.takeLastWhile { it is RichTextInline.Image }
                    val peel = block.align == RichTextAlignment.Center &&
                        trailingImages.isNotEmpty() &&
                        trailingImages.size < block.inlines.size
                    val textInlines = if (peel) block.inlines.dropLast(trailingImages.size) else block.inlines

                    val render = textInlines.toRichInlineText(
                        baseStyle = style,
                        baseColor = color,
                        linkColor = linkColor,
                        codeBackground = codeBackground,
                        headingKind = block.kind,
                        linkListener = linkListener
                    )
                    Text(
                        text = render.text,
                        inlineContent = render.inlineContent,
                        style = block.kind.withHeadingLineHeight(style).copy(color = color),
                        textAlign = block.align.toTextAlign(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (peel) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            trailingImages.forEach { inline ->
                                val emoji = inline as RichTextInline.Image
                                val side = (emoji.width ?: 24).coerceIn(14, 72)
                                AsyncImage(
                                    model = emoji.url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(side.dp)
                                )
                            }
                        }
                    }
                }

                is RichTextBlock.Image -> {
                    RichImage(block, onImageClick, onLinkClick, scaleToWidth = true)
                }

                is RichTextBlock.InlineGroup -> {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = when (block.align.toTextAlign()) {
                            TextAlign.Center -> Arrangement.spacedBy(
                                8.dp,
                                Alignment.CenterHorizontally
                            )

                            TextAlign.Right -> Arrangement.spacedBy(8.dp, Alignment.End)
                            else -> Arrangement.spacedBy(8.dp, Alignment.Start)
                        },
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                    ) {
                        block.children.forEach { child ->
                            when (child) {
                                is RichTextBlock.Text -> {
                                    val render = child.inlines.toRichInlineText(
                                        baseStyle = style,
                                        baseColor = color,
                                        linkColor = linkColor,
                                        codeBackground = codeBackground,
                                        headingKind = child.kind,
                                        linkListener = linkListener
                                    )
                                    Text(
                                        text = render.text,
                                        inlineContent = render.inlineContent,
                                        style = child.kind.withHeadingLineHeight(style).copy(color = color),
                                        textAlign = child.align.toTextAlign(),
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }

                                is RichTextBlock.Image -> {
                                    RichImage(child, onImageClick, onLinkClick)
                                }

                                is RichTextBlock.AnilistLink -> {
                                    AniListLinkCard(
                                        block = child,
                                        preview = previews[child.previewKey],
                                        style = style,
                                        onLinkClick = onLinkClick
                                    )
                                }

                                else -> Unit
                            }
                        }
                    }
                }

                is RichTextBlock.AnilistLink -> {
                    AniListLinkCard(
                        block = block,
                        preview = previews[block.previewKey],
                        style = style,
                        onLinkClick = onLinkClick
                    )
                }

                is RichTextBlock.ListBlock -> {
                    Column(
                        modifier = Modifier.padding(start = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        block.items.forEach { item ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                if (item.bullet != null) {
                                    Text(
                                        text = item.bullet,
                                        style = style.copy(
                                            color = color,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RenderBlocks(
                                        blocks = item.children,
                                        style = style,
                                        color = color,
                                        linkColor = linkColor,
                                        codeBackground = codeBackground,
                                        spoilerColor = spoilerColor,
                                        previews = previews,
                                        onImageClick = onImageClick,
                                        onLinkClick = onLinkClick,
                                        linkListener = linkListener
                                    )
                                }
                            }
                        }
                    }
                }

                is RichTextBlock.Table -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .drawBehind {
                                drawRoundRect(
                                    color = spoilerColor.copy(alpha = 0.2f),
                                    size = size,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())
                                )
                            }
                    ) {
                        block.rows.forEachIndexed { rowIndex, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .drawBehind {
                                        if (rowIndex < block.rows.size - 1) {
                                            drawLine(
                                                color = spoilerColor.copy(alpha = 0.2f),
                                                start = Offset(0f, size.height),
                                                end = Offset(size.width, size.height),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        }
                                    }
                            ) {
                                row.cells.forEachIndexed { cellIndex, cell ->
                                    val cellHorizontalAlignment = when (cell.align) {
                                        RichTextAlignment.Center -> Alignment.Center
                                        RichTextAlignment.End -> Alignment.CenterEnd
                                        else -> Alignment.CenterStart
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .drawBehind {
                                                if (cellIndex < row.cells.size - 1) {
                                                    drawLine(
                                                        color = spoilerColor.copy(alpha = 0.2f),
                                                        start = Offset(size.width, 0f),
                                                        end = Offset(size.width, size.height),
                                                        strokeWidth = 1.dp.toPx()
                                                    )
                                                }
                                            }
                                            .background(
                                                if (cell.isHeader) spoilerColor.copy(alpha = 0.05f)
                                                else Color.Transparent
                                            )
                                            .padding(8.dp),
                                        contentAlignment = cellHorizontalAlignment
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            RenderBlocks(
                                                blocks = cell.children,
                                                style = style.copy(
                                                    fontWeight = if (cell.isHeader) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = color,
                                                linkColor = linkColor,
                                                codeBackground = codeBackground,
                                                spoilerColor = spoilerColor,
                                                previews = previews,
                                                onImageClick = onImageClick,
                                                onLinkClick = onLinkClick,
                                                linkListener = linkListener
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is RichTextBlock.CodeBlock -> {
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
                                color = if (codeBackground.luminance() > 0.5f) Color.Black else Color.White
                            )
                        )
                    }
                }

                is RichTextBlock.Spoiler -> {
                    var revealed by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(spoilerColor.copy(alpha = if (revealed) 0.05f else 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { revealed = !revealed }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (revealed) "Spoiler" else "Spoiler, click to view",
                                style = style.copy(
                                    color = spoilerColor.copy(alpha = 0.8f),
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        AnimatedVisibility(
                            visible = revealed,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    start = 12.dp,
                                    end = 12.dp,
                                    bottom = 12.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RenderBlocks(
                                    blocks = block.children,
                                    style = style,
                                    color = color,
                                    linkColor = linkColor,
                                    codeBackground = codeBackground,
                                    spoilerColor = spoilerColor,
                                    previews = previews,
                                    onImageClick = onImageClick,
                                    onLinkClick = onLinkClick,
                                    linkListener = linkListener
                                )
                            }
                        }
                    }
                }

                is RichTextBlock.Blockquote -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                            .background(spoilerColor.copy(alpha = 0.05f))
                            .drawBehind {
                                drawLine(
                                    color = spoilerColor.copy(alpha = 0.4f),
                                    start = Offset(4.dp.toPx(), 0f),
                                    end = Offset(4.dp.toPx(), size.height),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RenderBlocks(
                            blocks = block.children,
                            style = style.copy(fontStyle = FontStyle.Italic),
                            color = color,
                            linkColor = linkColor,
                            codeBackground = codeBackground,
                            spoilerColor = spoilerColor,
                            previews = previews,
                            onImageClick = onImageClick,
                            onLinkClick = onLinkClick,
                            linkListener = linkListener
                        )
                    }
                }

                is RichTextBlock.HorizontalRule -> {
                    val mod = if (block.widthPercent != null) {
                        Modifier.fillMaxWidth(block.widthPercent / 100f)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                    HorizontalDivider(
                        modifier = mod.padding(vertical = 8.dp),
                        color = spoilerColor.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                }

                is RichTextBlock.YouTube -> {
                    val videoId = remember(block.videoIdOrUrl) {
                        extractYouTubeVideoId(block.videoIdOrUrl)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onLinkClick("https://www.youtube.com/watch?v=$videoId") }
                    ) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("https://img.youtube.com/vi/$videoId/hqdefault.jpg")
                                .crossfade(true)
                                .build(),
                            contentDescription = stringResource(R.string.cd_youtube_thumbnail),
                            contentScale = ContentScale.Crop,
                            loading = { ImageLoadingSkeleton(Modifier.fillMaxWidth()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(R.string.cd_play_video),
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                is RichTextBlock.Video -> {
                    VideoPlayer(
                        url = block.url,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Upper bound for an explicit image width from AniList markdown. Malformed markup such as
 * `img2147483647(url)` parses into a huge [RichTextBlock.Image.width]; feeding that straight
 * into [Modifier.widthIn] overflows Compose's `Constraints` (max ~262143 px) and crashes the
 * whole layout pass. 3000 dp is far past any real screen yet safe at every density.
 */
private const val MAX_RICH_IMAGE_WIDTH_DP = 3000

/**
 * Lays out a floated image with the following content wrapping beside it, then reflowing to full
 * width once it clears the image's bottom — a block-granular approximation of CSS `float` (Compose
 * text can't reflow around a float mid-paragraph, but per-block width switching matches AniList for
 * the short captions/headings that typically sit beside a profile image).
 */
@Composable
private fun RichFloatLayout(
    floatSide: RichTextFloat,
    image: @Composable () -> Unit,
    flowContent: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    gap: Dp = 8.dp
) {
    SubcomposeLayout(modifier = modifier.fillMaxWidth()) { constraints ->
        val totalWidth = constraints.maxWidth
        val gapPx = gap.roundToPx()

        val imagePlaceable = subcompose("float-image", image).firstOrNull()
            ?.measure(Constraints(maxWidth = totalWidth))
        val iw = imagePlaceable?.width ?: 0
        val ih = imagePlaceable?.height ?: 0

        val reducedWidth = (totalWidth - iw - gapPx).coerceAtLeast(1)
        val besideX = if (floatSide == RichTextFloat.End) 0 else iw + gapPx
        val imageX = if (floatSide == RichTextFloat.End) (totalWidth - iw).coerceAtLeast(0) else 0

        val placeables = ArrayList<Placeable>(flowContent.size)
        val xs = ArrayList<Int>(flowContent.size)
        val ys = ArrayList<Int>(flowContent.size)

        var y = 0
        var index = 0
        // Beside phase: wrap items in the narrowed column until they pass the image's bottom.
        while (index < flowContent.size && y < ih) {
            if (index > 0) y += gapPx
            val p = subcompose(index, flowContent[index]).first()
                .measure(Constraints(maxWidth = reducedWidth))
            placeables.add(p); xs.add(besideX); ys.add(y)
            y += p.height
            index++
        }
        // Below phase: the rest clears the image and spans the full width.
        var belowY = maxOf(y, ih)
        while (index < flowContent.size) {
            if (index > 0) belowY += gapPx
            val p = subcompose(index, flowContent[index]).first()
                .measure(Constraints(maxWidth = totalWidth))
            placeables.add(p); xs.add(0); ys.add(belowY)
            belowY += p.height
            index++
        }

        val height = maxOf(belowY, ih, y)
        layout(totalWidth, height) {
            imagePlaceable?.place(imageX, 0)
            for (k in placeables.indices) placeables[k].place(xs[k], ys[k])
        }
    }
}

@Composable
private fun RichImage(
    img: RichTextBlock.Image,
    onClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    fillWidth: Boolean = false,
    // Standalone block images should display AT their declared width (scaled up if the source is
    // smaller), matching AniList. Grid/inline images keep it as a cap only, so the FlowRow can wrap
    // several per row instead of forcing each to full width.
    scaleToWidth: Boolean = false
) {
    val mod = when {
        fillWidth -> Modifier.fillMaxWidth()
        // Clamp malformed widths: a percent fraction must stay in 0f..1f, and an absolute
        // width must stay within Constraints limits — see MAX_RICH_IMAGE_WIDTH_DP.
        img.isPercent && img.width != null ->
            Modifier.fillMaxWidth((img.width / 100f).coerceIn(0f, 1f))
        img.width != null ->
            Modifier.widthIn(max = img.width.coerceIn(0, MAX_RICH_IMAGE_WIDTH_DP).dp)
                .let { if (scaleToWidth) it.fillMaxWidth() else it }
        // No width hint: fill the available width so the image always has a concrete width to
        // render into. A vector source with no intrinsic size would otherwise collapse to 0×0.
        else -> Modifier.fillMaxWidth()
    }

    // Resolve whether this URL is actually an SVG (by content type — badge/widget services serve
    // image/svg+xml from extension-less URLs). SVGs are routed to a WebView because Coil's
    // AndroidSVG backend can't render foreignObject/CSS-animated widgets or nested-SVG logos.
    val initialKind = remember(img.url) { RichSvgResolver.quickKind(img.url) }
    val kind by produceState(initialKind, img.url) {
        if (value is RichImgKind.Loading) value = RichSvgResolver.resolve(img.url)
    }

    when (val resolved = kind) {
        is RichImgKind.Svg -> {
            // Size the WebView to the badge's natural width (capped) so a small badge isn't
            // stretched to full width; the centered Column then centers it and the tap target
            // matches the badge instead of spanning the row. An explicit width/percent on the
            // source still wins; only width-less SVGs fall back to the intrinsic width.
            val svgMod = when {
                fillWidth -> Modifier.fillMaxWidth()
                img.isPercent && img.width != null ->
                    Modifier.fillMaxWidth((img.width / 100f).coerceIn(0f, 1f))
                img.width != null ->
                    Modifier.widthIn(max = img.width.coerceIn(0, MAX_RICH_IMAGE_WIDTH_DP).dp)
                        .fillMaxWidth()
                resolved.naturalWidthDp != null ->
                    Modifier.widthIn(
                        max = resolved.naturalWidthDp
                            .coerceIn(1f, MAX_RICH_IMAGE_WIDTH_DP.toFloat()).dp
                    ).fillMaxWidth()
                else -> Modifier.fillMaxWidth()
            }
            RichSvgView(
                svg = resolved,
                linkUrl = img.linkUrl,
                onLinkClick = onLinkClick,
                modifier = svgMod
                    .aspectRatio(resolved.aspectRatio)
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        RichImgKind.Loading -> {
            ImageLoadingSkeleton(modifier = mod, aspectRatio = null)
        }

        RichImgKind.Raster -> {
            val placeholderAspectRatio =
                if (img.width != null && img.height != null && img.height > 0) {
                    img.width.toFloat() / img.height.toFloat()
                } else {
                    null
                }
            val isSmallUnknown = img.width == null && !fillWidth

            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(img.url)
                    .crossfade(true)
                    // Kept as an offline-safe fallback: if the SVG probe failed (e.g. no network)
                    // a simple SVG still decodes here. No-op for raster sources.
                    .decoderFactory(SvgDecoder.Factory())
                    .build(),
                contentDescription = null,
                // FillWidth reliably scales the image to the box width regardless of an unbounded
                // height — needed so a tall standalone image (e.g. a 400x600 gif declared width=500)
                // displays at full width like AniList instead of collapsing to a thumbnail.
                contentScale = when {
                    scaleToWidth -> ContentScale.FillWidth
                    fillWidth || img.width != null -> ContentScale.Fit
                    else -> ContentScale.Inside
                },
                loading = {
                    ImageLoadingSkeleton(
                        modifier = if (isSmallUnknown) Modifier.size(48.dp) else Modifier.fillMaxWidth(),
                        aspectRatio = if (isSmallUnknown) null else placeholderAspectRatio
                    )
                },
                modifier = mod
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (img.linkUrl != null) onLinkClick(img.linkUrl) else onClick(img.url)
                    }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImageLoadingSkeleton(
    modifier: Modifier = Modifier,
    aspectRatio: Float? = 16f / 9f
) {
    val sizedModifier = if (aspectRatio != null) {
        modifier.aspectRatio(aspectRatio)
    } else {
        modifier.heightIn(min = 120.dp)
    }
    Box(
        modifier = sizedModifier
            .clip(RoundedCornerShape(8.dp))
            .shimmerEffect(),
        contentAlignment = Alignment.Center
    ) {
        ContainedLoadingIndicator(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            indicatorColor = MaterialTheme.colorScheme.primary
        )
    }
}

private fun RichTextAlignment.toTextAlign(): TextAlign = when (this) {
    RichTextAlignment.Start -> TextAlign.Start
    RichTextAlignment.Center -> TextAlign.Center
    RichTextAlignment.End -> TextAlign.Right
    RichTextAlignment.Justify -> TextAlign.Justify
}

private class RichInlineText(
    val text: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>
)

/**
 * Make the space immediately before an inline [RichTextInline.Image] non-breaking, so a trailing
 * emoji stays on the same line as the word it follows (e.g. "Heavens 🙂") instead of wrapping to a
 * line of its own — which, when it contains only inline content, fails to honor center alignment.
 */
private fun List<RichTextInline>.bindInlineImagesToText(): List<RichTextInline> {
    val result = ArrayList<RichTextInline>(size)
    for (inline in this) {
        val processed = when (inline) {
            is RichTextInline.Bold -> RichTextInline.Bold(inline.children.bindInlineImagesToText())
            is RichTextInline.Italic -> RichTextInline.Italic(inline.children.bindInlineImagesToText())
            is RichTextInline.BoldItalic -> RichTextInline.BoldItalic(inline.children.bindInlineImagesToText())
            is RichTextInline.Strikethrough -> RichTextInline.Strikethrough(inline.children.bindInlineImagesToText())
            is RichTextInline.Link -> RichTextInline.Link(inline.url, inline.children.bindInlineImagesToText())
            else -> inline
        }
        if (processed is RichTextInline.Image && result.isNotEmpty()) {
            result[result.lastIndex] = result.last().withNonBreakingTrailingSpace()
        }
        result.add(processed)
    }
    return result
}

private fun RichTextInline.withNonBreakingTrailingSpace(): RichTextInline = when (this) {
    is RichTextInline.Text ->
        if (value.endsWith(' ')) RichTextInline.Text(value.dropLast(1) + ' ') else this
    is RichTextInline.Bold ->
        if (children.isEmpty()) this
        else RichTextInline.Bold(children.dropLast(1) + children.last().withNonBreakingTrailingSpace())
    is RichTextInline.Italic ->
        if (children.isEmpty()) this
        else RichTextInline.Italic(children.dropLast(1) + children.last().withNonBreakingTrailingSpace())
    is RichTextInline.BoldItalic ->
        if (children.isEmpty()) this
        else RichTextInline.BoldItalic(children.dropLast(1) + children.last().withNonBreakingTrailingSpace())
    is RichTextInline.Strikethrough ->
        if (children.isEmpty()) this
        else RichTextInline.Strikethrough(children.dropLast(1) + children.last().withNonBreakingTrailingSpace())
    is RichTextInline.Link ->
        if (children.isEmpty()) this
        else RichTextInline.Link(url, children.dropLast(1) + children.last().withNonBreakingTrailingSpace())
    else -> this
}

private fun List<RichTextInline>.toRichInlineText(
    baseStyle: TextStyle,
    baseColor: Color,
    linkColor: Color,
    codeBackground: Color,
    headingKind: RichTextTextKind,
    linkListener: LinkInteractionListener
): RichInlineText {
    val inlineContent = LinkedHashMap<String, InlineTextContent>()
    val prepared = bindInlineImagesToText()
    val text = buildAnnotatedString {
        withStyle(headingKind.toSpanStyle(baseStyle)) {
            appendInlines(
                inlines = prepared,
                baseColor = baseColor,
                linkColor = linkColor,
                codeBackground = codeBackground,
                linkListener = linkListener,
                inlineContent = inlineContent
            )
        }
    }
    return RichInlineText(text, inlineContent)
}

private fun AnnotatedString.Builder.appendInlines(
    inlines: List<RichTextInline>,
    baseColor: Color,
    linkColor: Color,
    codeBackground: Color,
    linkListener: LinkInteractionListener,
    inlineContent: MutableMap<String, InlineTextContent>
) {
    for (inline in inlines) {
        when (inline) {
            is RichTextInline.Text -> append(inline.value)
            is RichTextInline.LineBreak -> append("\n")
            is RichTextInline.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendInlines(inline.children, baseColor, linkColor, codeBackground, linkListener, inlineContent)
            }

            is RichTextInline.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                appendInlines(inline.children, baseColor, linkColor, codeBackground, linkListener, inlineContent)
            }

            is RichTextInline.BoldItalic -> withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                )
            ) {
                appendInlines(inline.children, baseColor, linkColor, codeBackground, linkListener, inlineContent)
            }

            is RichTextInline.Strikethrough -> withStyle(
                SpanStyle(textDecoration = TextDecoration.LineThrough)
            ) {
                appendInlines(inline.children, baseColor, linkColor, codeBackground, linkListener, inlineContent)
            }

            is RichTextInline.Link -> {
                // Clickable annotation with a stable listener — the tap is delivered by the
                // annotation itself rather than via a global LocalUriHandler override, so
                // recompositions never tear down the link's gesture node. The url rides along
                // as the annotation tag and is routed by AniLinkRouter on click.
                pushLink(
                    LinkAnnotation.Clickable(
                        tag = inline.url,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            ),
                            pressedStyle = SpanStyle(
                                color = linkColor.copy(alpha = 0.6f),
                                textDecoration = TextDecoration.Underline
                            )
                        ),
                        linkInteractionListener = linkListener
                    )
                )
                appendInlines(inline.children, baseColor, linkColor, codeBackground, linkListener, inlineContent)
                pop()
            }

            is RichTextInline.InlineCode -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground,
                        color = if (codeBackground.luminance() > 0.5f) Color.Black else Color.White,
                        fontSize = 13.sp
                    )
                ) {
                    append(inline.code)
                }
            }

            is RichTextInline.Image -> {
                // Emoji-sized inline image: register an InlineTextContent box sized to the
                // requested px (≈sp) so it flows with the surrounding text on the same line.
                val id = "inline-img-${inlineContent.size}"
                val side = (inline.width ?: 24).coerceIn(14, 72)
                inlineContent[id] = InlineTextContent(
                    Placeholder(
                        width = side.sp,
                        height = side.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    // Plain AsyncImage (not SubcomposeAsyncImage): a SubcomposeLayout inside text
                    // inline content can mis-measure and drop the box out of the line flow.
                    AsyncImage(
                        model = inline.url,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // Use a non-breaking space as the placeholder char so the line breaker never
                // inserts a break before the emoji box — it stays on its preceding word's line
                // (and thus honors the heading's center alignment) instead of wrapping alone.
                appendInlineContent(id, " ")
            }
        }
    }
}

private fun RichTextTextKind.toSpanStyle(base: TextStyle): SpanStyle = when (this) {
    RichTextTextKind.Paragraph -> SpanStyle()
    RichTextTextKind.Heading1 -> SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    RichTextTextKind.Heading2 -> SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
    RichTextTextKind.Heading3 -> SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
    RichTextTextKind.Heading4 -> SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    RichTextTextKind.Heading5 -> SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
}

/**
 * Heading font sizes are applied per-span via [toSpanStyle], but a paragraph's [lineHeight] stays
 * at the base body style (~20sp). A 28sp heading in a 20sp line box overlaps when it wraps and even
 * overflows into the next block. Scale the line height to the heading size (≈1.2×) so wrapped
 * headings lay out cleanly. Paragraphs keep the base line height.
 */
private fun RichTextTextKind.withHeadingLineHeight(base: TextStyle): TextStyle = when (this) {
    RichTextTextKind.Paragraph -> base
    RichTextTextKind.Heading1 -> base.copy(lineHeight = 34.sp)
    RichTextTextKind.Heading2 -> base.copy(lineHeight = 30.sp)
    RichTextTextKind.Heading3 -> base.copy(lineHeight = 26.sp)
    RichTextTextKind.Heading4 -> base.copy(lineHeight = 24.sp)
    RichTextTextKind.Heading5 -> base.copy(lineHeight = 22.sp)
}

private fun extractYouTubeVideoId(value: String): String {
    var id = value
    while (id.contains("v=") || id.contains("youtu.be/") || id.contains("embed/")) {
        id = when {
            id.contains("v=") -> id.substringAfterLast("v=")
            id.contains("youtu.be/") -> id.substringAfterLast("youtu.be/")
            id.contains("embed/") -> id.substringAfterLast("embed/")
            else -> id
        }
    }
    id = id.substringBefore("&").substringBefore("?")
    val clean = id.replace(Regex("[^a-zA-Z0-9_-]"), "")
    return if (clean.length >= 11) clean.takeLast(11) else clean
}