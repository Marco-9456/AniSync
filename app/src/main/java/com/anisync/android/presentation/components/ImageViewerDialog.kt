package com.anisync.android.presentation.components

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/**
 * A reusable fullscreen image viewer dialog with horizontal paging, page counter,
 * and download support.
 *
 * Displays a pageable gallery of images in a fullscreen dialog. The user can swipe
 * between images, dismiss by tapping the background or back button, and download
 * the currently visible image.
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
                .background(Color.Black.copy(alpha = 0.50f))
        ) {
            // Image pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(indication = null, interactionSource = null) { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageUrls[page],
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                indication = null,
                                interactionSource = null
                            ) { /* consume click to prevent dismiss */ }
                    )
                }
            }

            // Top bar with back button, page counter, and download
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "${pagerState.currentPage + 1} of ${imageUrls.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = {
                        downloadImage(context, imageUrls[pagerState.currentPage])
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/** Downloads an image to the device's Downloads folder using [DownloadManager]. */
private fun downloadImage(context: Context, imageUrl: String) {
    try {
        val uri = Uri.parse(imageUrl)
        val fileName = uri.lastPathSegment?.let { segment ->
            if (segment.contains('.')) segment
            else "$segment.jpg"
        } ?: "image_${System.currentTimeMillis()}.jpg"

        val request = DownloadManager.Request(uri)
            .setTitle(fileName)
            .setDescription("Downloading image...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(DownloadManager::class.java)
        dm?.enqueue(request)

        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
    }
}
