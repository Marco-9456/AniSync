package com.anisync.android.presentation.components

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A reusable fullscreen image viewer dialog with horizontal paging, pinch-to-zoom,
 * page counter, and download support.
 *
 * @param imageUrls The list of image URLs to display in the pager.
 * @param initialIndex The index of the image to show first.
 * @param onDismiss Called when the dialog should be dismissed.
 */
@Composable
fun ImageViewerDialog(
    imageUrls: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { imageUrls.size }
    )
    var isZoomed by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F0F))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !isZoomed,
                pageSpacing = 16.dp
            ) { page ->
                val isCurrentPage = pagerState.currentPage == page

                ZoomableImage(
                    imageUrl = imageUrls[page],
                    isCurrentPage = isCurrentPage,
                    onTap = onDismiss,
                    onZoomChanged = { zoomed -> isZoomed = zoomed }
                )
            }

            AnimatedVisibility(
                visible = !isZoomed,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${imageUrls.size}",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        IconButton(
                            onClick = { downloadImage(context, imageUrls[pagerState.currentPage]) },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download"
                            )
                        }
                    }
                }
            }
        }
    }
}

// Holder to avoid triggering recomposition when updating the Job reference
private class AnimationJobHolder {
    var job: Job? = null
}

/**
 * A zoomable image with pinch-to-zoom, double-tap-to-zoom, and pan support.
 * Unconsumed pan gestures are safely delegated to the parent HorizontalPager.
 */
@Composable
private fun ZoomableImage(
    imageUrl: String,
    isCurrentPage: Boolean,
    onTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val animHolder = remember { AnimationJobHolder() }

    val isZoomed by remember { derivedStateOf { scale > 1.01f } }

    LaunchedEffect(isZoomed) {
        onZoomChanged(isZoomed)
    }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage && scale > 1.01f) {
            animHolder.job?.cancel()
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { if (scale <= 1.01f) onTap() },
                    onDoubleTap = {
                        animHolder.job?.cancel()
                        animHolder.job = scope.launch {
                            val startScale = scale
                            val startOx = offsetX
                            val startOy = offsetY
                            val targetScale = if (scale > 1.01f) 1f else 3f

                            animate(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 300)
                            ) { value, _ ->
                                scale = lerp(startScale, targetScale, value)
                                offsetX = lerp(startOx, 0f, value)
                                offsetY = lerp(startOy, 0f, value)
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    animHolder.job?.cancel()

                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()

                        val isPinch = event.changes.size > 1
                        val isCurrentlyZoomed = scale > 1f

                        if (isPinch || isCurrentlyZoomed) {
                            scale = (scale * zoom).coerceIn(1f, 5f)

                            if (scale > 1f) {
                                val maxX = (size.width * (scale - 1)) / 2f
                                val maxY = (size.height * (scale - 1)) / 2f
                                offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }

                            event.changes.forEach {
                                if (it.positionChanged()) {
                                    it.consume()
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = "Zoomable Image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
        )
    }
}

/** Downloads an image to the device's Downloads folder using [DownloadManager]. */
private fun downloadImage(context: Context, imageUrl: String) {
    try {
        val uri = imageUrl.toUri()
        val fileName = uri.lastPathSegment?.let { segment ->
            if (segment.contains('.')) segment
            else "$segment.jpg"
        } ?: "image_${System.currentTimeMillis()}.jpg"

        val request = DownloadManager.Request(uri)
            .setTitle(fileName)
            .setDescription("Downloading high-resolution image...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(DownloadManager::class.java)
        dm?.enqueue(request)

        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.e("ImageViewerError", "Failed to download image", e)
        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
    }
}