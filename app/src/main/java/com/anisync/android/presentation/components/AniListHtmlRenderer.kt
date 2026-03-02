package com.anisync.android.presentation.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

// =============================================================================
// Pre-compiled Regex Patterns
// =============================================================================
// PERF: Regex compilation is expensive. These were previously compiled inside
// hot functions (normalizeWhitespace per TextNode, parseMarkdownText per line,
// handleImage per image). Hoisting them to top-level vals eliminates redundant
// compilation and reduces CPU time in the parsing hot-path.

/** Collapses runs of whitespace to a single space (used in normalizeWhitespace). */
private val WHITESPACE_REGEX = Regex("[\\s]+")

/** Standard markdown header: "# text" through "##### text". */
private val HEADER_REGEX = Regex("^(#{1,5})\\s+(.*)")

/** Header without space: "#text" (common in AniList markdown). */
private val HEADER_NO_SPACE_REGEX = Regex("^(#{1,5})([^#\\s].*)")

/** Bold wrapping a header: "__#Header__" or "**#Header**". */
private val BOLD_WRAPPED_HEADER_REGEX =
    Regex("^(?:\\*\\*|__)(#{1,5})\\s*(.*?)(?:\\*\\*|__)\\s*$")

/** Strips non-digit characters from an image width attribute. */
private val NON_DIGIT_REGEX = Regex("[^0-9]")

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
@OptIn(ExperimentalLayoutApi::class)
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

    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.clipToBounds(),
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
                                        .clickable { fullscreenImageUrl = img.url }
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
                                .clickable { fullscreenImageUrl = img.url }
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
                        onImageClick = { fullscreenImageUrl = it }
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

                is RenderBlock.Blockquote -> {
                    BlockquoteBlock(
                        content = block.children,
                        style = style,
                        color = color,
                        linkColor = linkColor,
                        spoilerColor = spoilerColor,
                        codeBackground = codeBackground,
                        onImageClick = { fullscreenImageUrl = it }
                    )
                }
            }
            i++
        }
    }

    fullscreenImageUrl?.let { imageUrl ->
        FullscreenImageDialog(
            imageUrl = imageUrl,
            onDismiss = { fullscreenImageUrl = null }
        )
    }
}

// =============================================================================
// Spoiler Block (click-to-reveal)
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
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
                }
                j++
            }
        }
    }
}

// =============================================================================
// Blockquote Block (left border bar + background + italic text)
// =============================================================================

/**
 * Renders a blockquote with AniList-style visual treatment:
 * - Left vertical border bar (3dp wide)
 * - Subtle dark background
 * - Italic text (AniList convention: "quoted text will always appear italic")
 *
 * Nested blockquotes are handled recursively — a [RenderBlock.Blockquote]
 * inside [content] produces a nested [BlockquoteBlock] with compounding visual
 * indentation.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    val borderColor = spoilerColor.copy(alpha = 0.5f)
    val bgColor = spoilerColor.copy(alpha = 0.08f)
    // AniList blockquote text is always italic
    val italicStyle = style.copy(fontStyle = FontStyle.Italic)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .drawBehind {
                // Draw the left border bar
                drawRect(
                    color = borderColor,
                    topLeft = Offset.Zero,
                    size = Size(3.dp.toPx(), size.height)
                )
            }
            .padding(start = 12.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            var j = 0
            while (j < content.size) {
                when (val block = content[j]) {
                    is RenderBlock.Text -> {
                        if (block.annotatedString.isNotBlank()) {
                            Text(
                                text = block.annotatedString,
                                style = italicStyle.copy(color = color),
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
                                style = italicStyle.copy(
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
                            style = italicStyle,
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
                            style = italicStyle
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
                        Text(text = annotated, style = italicStyle.copy(color = color))
                    }

                    is RenderBlock.Video -> {
                        VideoPlayer(url = block.url)
                    }

                    is RenderBlock.Blockquote -> {
                        // Nested blockquote — recurse for compounding indentation
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
                }
                j++
            }
        }
    }
}

// =============================================================================
// Fullscreen Image Viewer
// =============================================================================

@Composable
private fun FullscreenImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(indication = null, interactionSource = null) { onDismiss() }
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }

            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
                    .clickable(indication = null, interactionSource = null) { /* consume */ }
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

// =============================================================================
// Video Player (Media3 ExoPlayer — autoplay, loop, muted, with controls)
// =============================================================================

/**
 * Inline video player using Media3 [ExoPlayer] with [PlayerView] for native
 * playback controls (play/pause, seek bar, progress). Includes a mute/unmute
 * toggle overlay. Starts muted, looping, and autoplaying — matching AniList's
 * webm() behavior.
 *
 * The ExoPlayer instance is remembered by URL so it survives recompositions
 * (e.g., when the user scrolls comments). It is released in [DisposableEffect.onDispose].
 */
@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(url: String) {
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(true) }

    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(url) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    controllerShowTimeoutMs = 2500
                    controllerAutoShow = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Mute/Unmute toggle
        IconButton(
            onClick = {
                isMuted = !isMuted
                exoPlayer.volume = if (isMuted) 0f else 1f
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(32.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
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
    data object HorizontalRule : RenderBlock()
    data class YouTube(val url: String) : RenderBlock()
    data class Video(val url: String) : RenderBlock()
    data class Blockquote(val children: List<RenderBlock>) : RenderBlock()
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
 *
 * PERF: Tracks [trailingNewlineCount] to avoid calling [AnnotatedString.Builder.toAnnotatedString]
 * just to check the last character(s). Previously, ensureParagraphBreak(), ensureNewline(),
 * walkNode(), and parseMarkdownText() each called toAnnotatedString() which builds a full
 * copy of accumulated text + all spans + all link annotations — O(n) per call. With trailing
 * newline tracking, these checks become O(1).
 */
private class Ctx(
    val linkColor: Color,
    val codeBackground: Color,
    val spoilerColor: Color,
    val textAlign: TextAlign = TextAlign.Unspecified
) {
    var builder: AnnotatedString.Builder = AnnotatedString.Builder()
    var hasContent = false

    // PERF: Tracks how many consecutive newlines trail the current builder content.
    // Updated by appendText(), appendNewline(), and markNonNewlineContent().
    // Eliminates the need for toAnnotatedString() in ensureParagraphBreak/ensureNewline.
    var trailingNewlineCount = 0
        private set

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
            trailingNewlineCount = 0
        }
    }

    /**
     * Ensures a paragraph break (`\n\n`) before new paragraph content.
     * No-op if builder is empty or already ends with `\n\n`.
     *
     * PERF: Uses [trailingNewlineCount] instead of toAnnotatedString().
     */
    fun ensureParagraphBreak() {
        if (!hasContent) return
        when {
            trailingNewlineCount >= 2 -> { /* already has paragraph break */ }
            trailingNewlineCount == 1 -> {
                builder.append("\n")
                trailingNewlineCount = 2
            }
            else -> {
                builder.append("\n\n")
                trailingNewlineCount = 2
            }
        }
    }

    /**
     * Ensures a single newline at end (for list items, blockquote lines).
     *
     * PERF: Uses [trailingNewlineCount] instead of toAnnotatedString().
     */
    fun ensureNewline() {
        if (!hasContent) return
        if (trailingNewlineCount > 0) return
        builder.append("\n")
        trailingNewlineCount = 1
    }

    fun appendNewline() {
        builder.append("\n")
        hasContent = true
        trailingNewlineCount++
    }

    fun appendText(text: String) {
        if (text.isNotEmpty()) {
            builder.append(text)
            hasContent = true
            // Count trailing newlines in the appended text
            trailingNewlineCount = 0
            for (i in text.length - 1 downTo 0) {
                if (text[i] == '\n') trailingNewlineCount++
                else break
            }
        }
    }

    /**
     * Call after non-newline content is appended directly to [builder] (e.g., inside
     * withStyle blocks). Resets [trailingNewlineCount] to 0 since we know the
     * appended content is not a newline.
     */
    fun markNonNewlineContent() {
        hasContent = true
        trailingNewlineCount = 0
    }
}

// =============================================================================
// HTML Parsing Engine (Jsoup)
// =============================================================================

/**
 * Parses server-rendered AniList HTML into a flat list of [RenderBlock]s.
 *
 * Text content flows within a single [AnnotatedString] builder as much as possible.
 * Paragraphs are separated by `\n\n`, line breaks by `\n`. Headers and lists are
 * rendered via SpanStyle within the text flow.
 * Images, code blocks, spoilers, blockquotes, horizontal rules, and video/YouTube
 * embeds break out into separate composables.
 */
internal fun parseHtmlToBlocks(
    html: String,
    linkColor: Color,
    codeBackground: Color,
    spoilerColor: Color
): List<RenderBlock> {
    if (html.isBlank()) return emptyList()

    // PERF: Measure total parse time for performance monitoring.
    // Uses try/catch around android.util.Log so it doesn't crash in JVM unit tests
    // where the Android framework is not available. In release builds, ProGuard/R8
    // strips DEBUG-level logs.
    val startNanos = System.nanoTime()

    val doc = Jsoup.parseBodyFragment(html)
    doc.outputSettings().prettyPrint(false)

    val blocks = mutableListOf<RenderBlock>()
    val ctx = Ctx(linkColor, codeBackground, spoilerColor)

    walkChildren(doc.body(), blocks, ctx)
    ctx.flushText(blocks)

    val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000.0
    try {
        android.util.Log.d(
            "AniListHtmlRenderer",
            "parseHtmlToBlocks: ${blocks.size} blocks in %.2fms (html length=${html.length})".format(elapsedMs)
        )
    } catch (_: RuntimeException) {
        // android.util.Log is unavailable in JVM unit tests — silently ignore.
    }

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
            // Normalize HTML whitespace: collapse runs of whitespace to single space
            val normalized = normalizeWhitespace(node.wholeText)
            if (normalized.isNotEmpty()) {
                // Don't append leading space if builder is empty or ends with newline.
                // PERF: Uses trailingNewlineCount instead of toAnnotatedString().
                val trimmed = if (!ctx.hasContent || ctx.trailingNewlineCount > 0) {
                    normalized.trimStart()
                } else {
                    normalized
                }
                ctx.appendText(trimmed)
            }
        }
        is Element -> walkElement(node, blocks, ctx)
    }
}

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
        // FIX: Pass markdownText = true so raw markdown like __bold__ inside headers
        // is parsed. AniList's API converts ### to <h3> but may leave __ as raw text.
        "h1", "h2", "h3", "h4", "h5" -> {
            ctx.ensureParagraphBreak()
            val level = tag[1].digitToInt()
            ctx.builder.withStyle(headerSpanStyle(level)) {
                renderInline(element, this, ctx, blocks, markdownText = true)
            }
            ctx.markNonNewlineContent()
        }

        // --- Blockquotes: flush text, parse children into a separate Blockquote block ---
        // Blockquotes are rendered as a distinct composable with a left border bar,
        // subtle background, and italic text (matching AniList's styling).
        "blockquote" -> {
            renderBlockquote(element, blocks, ctx)
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
            ctx.markNonNewlineContent()
        }

        "i", "em" -> {
            ctx.builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                renderInline(element, this, ctx, blocks)
            }
            ctx.markNonNewlineContent()
        }

        "del", "strike", "s" -> {
            ctx.builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                renderInline(element, this, ctx, blocks)
            }
            ctx.markNonNewlineContent()
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
            ctx.markNonNewlineContent()
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

    if (href.isNotBlank()) {
        ctx.builder.pushLink(
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
        renderInline(element, ctx.builder, ctx, blocks)
        ctx.builder.pop()
    } else {
        // <a> without href = colored text (AniList convention)
        ctx.builder.withStyle(SpanStyle(color = ctx.linkColor)) {
            renderInline(element, this, ctx, blocks)
        }
    }
    ctx.markNonNewlineContent()
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
    ctx.markNonNewlineContent()
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
 *
 * FIX: `<p>` is handled by recursing with [walkCenterChildren] instead of dispatching
 * to [walkElement]. Previously, `<p>` inside `<center>` was dispatched to walkElement →
 * walkChildren → walkNode, where TextNodes were appended verbatim without markdown
 * parsing. This caused `<center><p>_Test_</p></center>` to render as literal "_Test_"
 * instead of italic "Test". Now, `<p>` recurses so its TextNode children still go
 * through [parseMarkdownText] for proper italic/bold/link rendering.
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
                when (tag) {
                    // Block-level tags that need special handling inside center:
                    // <p> recurses through walkCenterChildren so nested TextNodes
                    // still get markdown parsing.
                    "p" -> {
                        ctx.ensureParagraphBreak()
                        walkCenterChildren(child, blocks, ctx)
                    }
                    // Tags that are real HTML content from the API — process normally
                    "video", "img", "a", "div", "span", "br", "hr",
                    "pre", "code", "ul", "ol",
                    // Inline formatting tags (may appear if user mixes HTML inside center)
                    "b", "strong", "i", "em", "del", "strike", "s",
                    // Block-level tags that AniList may generate
                    "h1", "h2", "h3", "h4", "h5", "blockquote" -> {
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
                parseInlineMarkdown(content, this, ctx)
            }
            ctx.markNonNewlineContent()
            continue
        }

        // Check for headers without space: #text (common in AniList)
        val headerNoSpaceMatch = HEADER_NO_SPACE_REGEX.find(trimmedLine)
        if (headerNoSpaceMatch != null) {
            ctx.ensureParagraphBreak()
            val level = headerNoSpaceMatch.groupValues[1].length
            val content = headerNoSpaceMatch.groupValues[2]
            ctx.builder.withStyle(headerSpanStyle(level)) {
                parseInlineMarkdown(content, this, ctx)
            }
            ctx.markNonNewlineContent()
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
                    parseInlineMarkdown(content, this, ctx)
                }
            }
            ctx.markNonNewlineContent()
            continue
        }

        // Regular text line with inline markdown.
        // PERF: Uses trailingNewlineCount instead of toAnnotatedString().
        if (ctx.hasContent && ctx.trailingNewlineCount == 0) {
            ctx.appendNewline()
        }
        parseInlineMarkdown(line, ctx.builder, ctx)
        ctx.markNonNewlineContent()
    }
}

/**
 * Parses inline markdown text into the [builder].
 * Handles: **bold**, __bold__, *italic*, _italic_, ~~strikethrough~~,
 * `code`, [text](url).
 */
private fun parseInlineMarkdown(
    text: String,
    builder: AnnotatedString.Builder,
    ctx: Ctx
) {
    var i = 0
    val len = text.length

    while (i < len) {
        when {
            // Bold+Italic: ***text***
            i + 2 < len && text[i] == '*' && text[i + 1] == '*' && text[i + 2] == '*' -> {
                val end = text.indexOf("***", i + 3)
                if (end != -1) {
                    builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        parseInlineMarkdown(text.substring(i + 3, end), this, ctx)
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
                        parseInlineMarkdown(text.substring(i + 2, end), this, ctx)
                    }
                    i = end + 2
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
                        parseInlineMarkdown(text.substring(i + 3, end), this, ctx)
                    }
                    i = end + 3
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
                        parseInlineMarkdown(text.substring(i + 2, end), this, ctx)
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
                        parseInlineMarkdown(text.substring(i + 2, end), this, ctx)
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
                        parseInlineMarkdown(linkText, builder, ctx)
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
            // Italic: *text*
            text[i] == '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end != -1) {
                    builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        parseInlineMarkdown(text.substring(i + 1, end), this, ctx)
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
                        parseInlineMarkdown(text.substring(i + 1, end), this, ctx)
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
 * Renders a `<blockquote>` as a separate [RenderBlock.Blockquote].
 *
 * Flushes any accumulated text, parses the blockquote's children into a
 * separate list of [RenderBlock]s, and emits a [RenderBlock.Blockquote].
 * Nested `<blockquote>` elements naturally produce nested [RenderBlock.Blockquote]
 * blocks since walkElement dispatches them back here.
 *
 * The composable rendering applies the visual styling: left border bar,
 * subtle background, and italic text — matching AniList's blockquote appearance.
 */
private fun renderBlockquote(
    element: Element,
    blocks: MutableList<RenderBlock>,
    ctx: Ctx
) {
    // Flush any text accumulated before the blockquote
    ctx.flushText(blocks)

    // Parse blockquote children into a separate block list
    val childBlocks = mutableListOf<RenderBlock>()
    val childCtx = Ctx(ctx.linkColor, ctx.codeBackground, ctx.spoilerColor)

    walkChildren(element, childBlocks, childCtx)
    childCtx.flushText(childBlocks)

    if (childBlocks.isNotEmpty()) {
        blocks.add(RenderBlock.Blockquote(childBlocks))
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
                // Raw text directly in <ul> (e.g. <ul>**bold text:**)
                val text = normalizeWhitespace(child.wholeText).trim()
                if (text.isNotEmpty()) {
                    if (!first || ctx.hasContent) {
                        ctx.ensureNewline()
                    }
                    parseInlineMarkdown(text, ctx.builder, ctx)
                    ctx.hasContent = true
                    first = false
                }
            }
            is Element -> {
                val tag = child.tagName().lowercase()
                when (tag) {
                    "li" -> {
                        if (!first || ctx.hasContent) {
                            ctx.ensureNewline()
                        }
                        first = false
                        ctx.appendText("$indent  \u2022 ")
                        renderListItem(child, ctx, blocks, depth)
                    }
                    "ul" -> renderUnorderedList(child, ctx, blocks, depth + 1)
                    "ol" -> renderOrderedList(child, ctx, blocks, depth + 1)
                    else -> {
                        // Other elements directly in <ul>
                        renderInlineForCtx(child, ctx, blocks)
                        first = false
                    }
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

    for (child in element.childNodes()) {
        when (child) {
            is TextNode -> {
                // Raw text directly in <ol>
                val text = normalizeWhitespace(child.wholeText).trim()
                if (text.isNotEmpty()) {
                    if (!first || ctx.hasContent) {
                        ctx.ensureNewline()
                    }
                    parseInlineMarkdown(text, ctx.builder, ctx)
                    ctx.hasContent = true
                    first = false
                }
            }
            is Element -> {
                val tag = child.tagName().lowercase()
                when (tag) {
                    "li" -> {
                        if (!first || ctx.hasContent) {
                            ctx.ensureNewline()
                        }
                        first = false
                        ctx.appendText("$indent  ${idx++}. ")
                        renderListItem(child, ctx, blocks, depth)
                    }
                    "ul" -> renderUnorderedList(child, ctx, blocks, depth + 1)
                    "ol" -> renderOrderedList(child, ctx, blocks, depth + 1)
                    else -> {
                        renderInlineForCtx(child, ctx, blocks)
                        first = false
                    }
                }
            }
        }
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
                    // Parse inline markdown (bold, italic, links, etc.) from raw text
                    parseInlineMarkdown(text, ctx.builder, ctx)
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
 */
private fun renderInline(
    element: Element,
    builder: AnnotatedString.Builder,
    ctx: Ctx,
    blocks: MutableList<RenderBlock>,
    markdownText: Boolean = false
) {
    for (child in element.childNodes()) {
        when (child) {
            is TextNode -> {
                val text = normalizeWhitespace(child.wholeText)
                if (text.isNotEmpty()) {
                    if (markdownText) {
                        parseInlineMarkdown(text, builder, ctx)
                    } else {
                        builder.append(text)
                    }
                }
            }
            is Element -> {
                val tag = child.tagName().lowercase()
                when (tag) {
                    "b", "strong" -> builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        renderInline(child, this, ctx, blocks, markdownText)
                    }
                    "i", "em" -> builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        renderInline(child, this, ctx, blocks, markdownText)
                    }
                    "del", "strike", "s" -> builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        renderInline(child, this, ctx, blocks, markdownText)
                    }
                    "center" -> {
                        // <center> inside inline context (e.g. inside <h1>):
                        // enable markdown parsing for text nodes
                        renderInline(child, builder, ctx, blocks, true)
                    }
                    "a" -> {
                        val href = child.attr("href")
                        val img = child.selectFirst("img")
                        if (img != null && href.isNotBlank()) {
                            // Linked image: break out of inline flow
                            ctx.hasContent = true
                            handleImage(img, blocks, ctx, linkUrl = href)
                        } else if (href.isNotBlank()) {
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
                            // Don't parse markdown inside <a> — content is already the link text
                            renderInline(child, builder, ctx, blocks, false)
                            builder.pop()
                        } else {
                            builder.withStyle(SpanStyle(color = ctx.linkColor)) {
                                renderInline(child, this, ctx, blocks, markdownText)
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
                        // Never parse markdown inside <code>
                        append(child.wholeText())
                    }
                    "br" -> builder.append("\n")
                    "img" -> {
                        // Image inside inline context: break out into Image block
                        ctx.hasContent = true
                        handleImage(child, blocks, ctx, linkUrl = null)
                    }
                    "span" -> {
                        val cls = child.className().lowercase()
                        if (cls.contains("markdown_spoiler")) {
                            ctx.hasContent = true
                            handleSpan(child, blocks, ctx)
                        } else {
                            renderInline(child, builder, ctx, blocks, markdownText)
                        }
                    }
                    else -> renderInline(child, builder, ctx, blocks, markdownText)
                }
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
            renderInline(element, ctx.builder, ctx, blocks)
            ctx.hasContent = true
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
