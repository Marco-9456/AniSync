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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.anisync.android.domain.parser.ParsedRichText
import com.anisync.android.domain.parser.ParserConfig
import com.anisync.android.domain.parser.RichTextBlock
import com.anisync.android.domain.parser.RichTextParser
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
    var parsedText by remember { mutableStateOf<ParsedRichText?>(null) }

    LaunchedEffect(html, linkColor, codeBackground, spoilerColor) {
        val config = ParserConfig(linkColor, codeBackground, spoilerColor)
        parsedText = RichTextParser.parse(html, config)
    }

    parsedText?.let {
        RichTextRenderer(it, modifier, style, color, spoilerColor)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RichTextRenderer(
    parsedData: ParsedRichText,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    spoilerColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    var viewerInitialIndex by remember { mutableStateOf<Int?>(null) }
    val uriHandler = LocalUriHandler.current

    SelectionContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            RenderBlocks(
                blocks = parsedData.blocks,
                style = style,
                color = color,
                spoilerColor = spoilerColor,
                onImageClick = { url ->
                    val idx = parsedData.imageUrls.indexOf(url)
                    if (idx >= 0) viewerInitialIndex = idx
                },
                onLinkClick = { uriHandler.openUri(it) }
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RenderBlocks(
    blocks: List<RichTextBlock>,
    style: TextStyle,
    color: Color,
    spoilerColor: Color,
    onImageClick: (String) -> Unit,
    onLinkClick: (String) -> Unit
) {
    for (block in blocks) {
        val blockAlignment = when (block.align) {
            TextAlign.Center -> Alignment.CenterHorizontally
            TextAlign.Right -> Alignment.End
            else -> Alignment.Start
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = blockAlignment,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            when (block) {
                is RichTextBlock.Text -> {
                    Text(
                        text = block.text,
                        style = style.copy(color = color),
                        textAlign = block.align,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is RichTextBlock.Image -> {
                    RichImage(block, onImageClick, onLinkClick)
                }

                is RichTextBlock.ImageGroup -> {
                    FlowRow(
                        horizontalArrangement = when (block.align) {
                            TextAlign.Center -> Arrangement.Center
                            TextAlign.Right -> Arrangement.End
                            else -> Arrangement.spacedBy(6.dp)
                        },
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        block.images.forEach { RichImage(it, onImageClick, onLinkClick) }
                    }
                }

                is RichTextBlock.AnilistLink -> {
                    // Beautiful Placeholder Card for AniList API Embeds
                    // TODO: Replace with a component that fetches title/coverImage using block.id from your API
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .clickable { onLinkClick(block.url) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val typeColor = when (block.type.lowercase()) {
                            "anime" -> Color(0xFF3DB4F2)
                            "manga" -> Color(0xFFF2A33D)
                            "character" -> Color(0xFFE03D51)
                            "staff" -> Color(0xFF8F56C0)
                            "user" -> Color(0xFF4CAF50)
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(typeColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = block.type.take(1).uppercase(),
                                style = style.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = typeColor
                                )
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AniList ${block.type.replaceFirstChar { it.uppercase() }} #${block.id}",
                                style = style.copy(fontWeight = FontWeight.Bold, color = typeColor),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Tap to view on AniList",
                                style = style.copy(
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                is RichTextBlock.ListBlock -> {
                    Column(
                        modifier = Modifier.padding(start = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                Column(modifier = Modifier.weight(1f)) {
                                    RenderBlocks(
                                        item.children,
                                        style,
                                        color,
                                        spoilerColor,
                                        onImageClick,
                                        onLinkClick
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
                                            .background(if (cell.isHeader) spoilerColor.copy(alpha = 0.05f) else Color.Transparent)
                                            .padding(8.dp)
                                    ) {
                                        RenderBlocks(
                                            blocks = cell.children,
                                            style = style.copy(fontWeight = if (cell.isHeader) FontWeight.Bold else FontWeight.Normal),
                                            color = color,
                                            spoilerColor = spoilerColor,
                                            onImageClick = onImageClick,
                                            onLinkClick = onLinkClick
                                        )
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
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        val isLightBackground =
                            MaterialTheme.colorScheme.surfaceContainerHighest.luminance() > 0.5f
                        val textColor = if (isLightBackground) Color.Black else Color.White

                        Text(
                            text = block.code,
                            style = style.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = textColor
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
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RenderBlocks(
                                    block.children,
                                    style,
                                    color,
                                    spoilerColor,
                                    onImageClick,
                                    onLinkClick
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
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RenderBlocks(
                            block.children,
                            style.copy(fontStyle = FontStyle.Italic),
                            color,
                            spoilerColor,
                            onImageClick,
                            onLinkClick
                        )
                    }
                }

                is RichTextBlock.HorizontalRule -> {
                    val mod =
                        if (block.widthPercent != null) Modifier.fillMaxWidth(block.widthPercent / 100f) else Modifier.fillMaxWidth()
                    HorizontalDivider(
                        modifier = mod.padding(vertical = 8.dp),
                        color = spoilerColor.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                }

                is RichTextBlock.YouTube -> {
                    val videoId = remember(block.videoId) {
                        var id = block.videoId

                        // AniList's API sometimes dumps raw URLs into the ID field if a user formatted it weirdly inside a spoiler.
                        // We loop to aggressively strip all YouTube domains and parameters until we isolate the core ID.
                        while (id.contains("v=") || id.contains("youtu.be/") || id.contains("embed/")) {
                            if (id.contains("v=")) id = id.substringAfterLast("v=")
                            else if (id.contains("youtu.be/")) id =
                                id.substringAfterLast("youtu.be/")
                            else if (id.contains("embed/")) id = id.substringAfterLast("embed/")
                        }

                        id = id.substringBefore("&").substringBefore("?")

                        // YouTube IDs are strictly 11 characters (base64-encoded 64-bit ints).
                        // This regex and length check strips away leftover markdown syntax, quotes, or HTML artifacts.
                        val cleanId = id.replace(Regex("[^a-zA-Z0-9_-]"), "")
                        if (cleanId.length >= 11) cleanId.takeLast(11) else cleanId
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
                            loading = { ImageLoadingSkeleton() },
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
    onLinkClick: (String) -> Unit
) {
    val mod = if (img.isPercent && img.width != null) Modifier.fillMaxWidth(img.width / 100f)
    else if (img.width != null) Modifier.widthIn(max = img.width.dp)
    else Modifier.fillMaxWidth()

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(img.url)
            .crossfade(true)
            .build(),
        contentDescription = null,
        contentScale = if (img.width != null) ContentScale.Fit else ContentScale.FillWidth,
        loading = { ImageLoadingSkeleton() },
        modifier = mod
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                if (img.linkUrl != null) onLinkClick(img.linkUrl) else onClick(img.url)
            }
    )
}

/**
 * Shimmer skeleton with MD3 Expressive shape-morphing loading indicator.
 * Shows while images in threads and comments are loading — gives users
 * a clear visual cue that content will appear in this area.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImageLoadingSkeleton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
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