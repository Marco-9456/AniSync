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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
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
import coil.compose.SubcomposeAsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.anisync.android.domain.LinkPreview
import com.anisync.android.domain.parser.LinkPreviewKey
import com.anisync.android.domain.parser.ParsedRichText
import com.anisync.android.domain.parser.ParserConfig
import com.anisync.android.domain.parser.RichTextAlignment
import com.anisync.android.domain.parser.RichTextBlock
import com.anisync.android.domain.parser.RichTextInline
import com.anisync.android.domain.parser.RichTextParser
import com.anisync.android.domain.parser.RichTextTextKind
import com.anisync.android.presentation.util.LocalLinkPreviewProvider
import com.anisync.android.presentation.util.rememberAniLinkRouter
import com.anisync.android.presentation.util.shimmerEffect

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
    var parsedText by remember(html) { mutableStateOf<ParsedRichText?>(null) }

    LaunchedEffect(html) {
        parsedText = RichTextParser.parse(html, ParserConfig())
    }

    parsedText?.let {
        RichTextRenderer(
            parsedData = it,
            modifier = modifier,
            style = style,
            color = color,
            linkColor = linkColor,
            codeBackground = codeBackground,
            spoilerColor = spoilerColor
        )
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

    val customUriHandler = remember(linkRouter) {
        object : androidx.compose.ui.platform.UriHandler {
            override fun openUri(uri: String) {
                linkRouter.navigate(uri)
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalUriHandler provides customUriHandler
    ) {
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
                    onLinkClick = { linkRouter.navigate(it) }
                )
            }
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
    val result = mutableListOf<RichTextBlock.AnilistLink>()
    for (block in blocks) {
        when (block) {
            is RichTextBlock.AnilistLink -> result.add(block)
            is RichTextBlock.Spoiler -> result.addAll(collectAnilistLinks(block.children))
            is RichTextBlock.Blockquote -> result.addAll(collectAnilistLinks(block.children))
            is RichTextBlock.ListBlock -> block.items.forEach { result.addAll(collectAnilistLinks(it.children)) }
            is RichTextBlock.Table -> block.rows.forEach { row ->
                row.cells.forEach { cell ->
                    result.addAll(collectAnilistLinks(cell.children))
                }
            }

            is RichTextBlock.InlineGroup -> block.children.forEach {
                if (it is RichTextBlock.AnilistLink) result.add(it)
            }

            else -> Unit
        }
    }
    return result
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
    onLinkClick: (String) -> Unit
) {
    for (block in blocks) {
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
                    Text(
                        text = block.inlines.toAnnotatedString(
                            baseStyle = style,
                            baseColor = color,
                            linkColor = linkColor,
                            codeBackground = codeBackground,
                            headingKind = block.kind
                        ),
                        style = style.copy(color = color),
                        textAlign = block.align.toTextAlign(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is RichTextBlock.Image -> {
                    RichImage(block, onImageClick, onLinkClick)
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
                                    Text(
                                        text = child.inlines.toAnnotatedString(
                                            baseStyle = style,
                                            baseColor = color,
                                            linkColor = linkColor,
                                            codeBackground = codeBackground,
                                            headingKind = child.kind
                                        ),
                                        style = style.copy(color = color),
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
                                        onLinkClick = onLinkClick
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
                                                onLinkClick = onLinkClick
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
                                    onLinkClick = onLinkClick
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
                            onLinkClick = onLinkClick
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
                            contentDescription = "YouTube Thumbnail",
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
                                contentDescription = "Play Video",
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

@Composable
private fun RichImage(
    img: RichTextBlock.Image,
    onClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    fillWidth: Boolean = false
) {
    val mod = when {
        fillWidth -> Modifier.fillMaxWidth()
        img.isPercent && img.width != null -> Modifier.fillMaxWidth(img.width / 100f)
        img.width != null -> Modifier.widthIn(max = img.width.dp)
        else -> Modifier
    }

    val isSvg =
        img.url.endsWith(".svg", ignoreCase = true) || img.url.contains("spotify-github-profile")

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(img.url)
            .crossfade(true)
            .apply {
                if (isSvg) {
                    decoderFactory(SvgDecoder.Factory())
                    allowHardware(false)
                }
            }
            .build(),
        contentDescription = null,
        contentScale = if (fillWidth || img.width != null) ContentScale.Fit else ContentScale.Inside,
        loading = {
            ImageLoadingSkeleton(
                if (img.width == null && !fillWidth) Modifier.size(48.dp) else Modifier.fillMaxWidth()
            )
        },
        modifier = mod
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                if (img.linkUrl != null) onLinkClick(img.linkUrl) else onClick(img.url)
            }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImageLoadingSkeleton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
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

private fun List<RichTextInline>.toAnnotatedString(
    baseStyle: TextStyle,
    baseColor: Color,
    linkColor: Color,
    codeBackground: Color,
    headingKind: RichTextTextKind
): AnnotatedString = buildAnnotatedString {
    withStyle(headingKind.toSpanStyle(baseStyle)) {
        appendInlines(
            inlines = this@toAnnotatedString,
            baseColor = baseColor,
            linkColor = linkColor,
            codeBackground = codeBackground
        )
    }
}

private fun AnnotatedString.Builder.appendInlines(
    inlines: List<RichTextInline>,
    baseColor: Color,
    linkColor: Color,
    codeBackground: Color
) {
    for (inline in inlines) {
        when (inline) {
            is RichTextInline.Text -> append(inline.value)
            is RichTextInline.LineBreak -> append("\n")
            is RichTextInline.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendInlines(inline.children, baseColor, linkColor, codeBackground)
            }

            is RichTextInline.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                appendInlines(inline.children, baseColor, linkColor, codeBackground)
            }

            is RichTextInline.BoldItalic -> withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                )
            ) {
                appendInlines(inline.children, baseColor, linkColor, codeBackground)
            }

            is RichTextInline.Strikethrough -> withStyle(
                SpanStyle(textDecoration = TextDecoration.LineThrough)
            ) {
                appendInlines(inline.children, baseColor, linkColor, codeBackground)
            }

            is RichTextInline.Link -> {
                pushLink(
                    LinkAnnotation.Url(
                        inline.url,
                        TextLinkStyles(style = SpanStyle(color = linkColor))
                    )
                )
                appendInlines(inline.children, baseColor, linkColor, codeBackground)
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