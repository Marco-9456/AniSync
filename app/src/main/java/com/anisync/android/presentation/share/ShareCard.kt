package com.anisync.android.presentation.share

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.util.ShareUtils
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.launch

/**
 * Fixed capture width for every share card — a portrait-leaning social card that reads
 * well as a chat/story preview. Cards size their height to content.
 */
val ShareCardWidth = 340.dp

/** Rounded outer shape shared by all share cards (matches the app's 28dp hero radius). */
private val ShareCardShape = RoundedCornerShape(28.dp)

/**
 * Bottom sheet that previews a [card] exactly as it will be shared, then offers: save the PNG to
 * the gallery, copy the [caption] link to the clipboard, or open the system share sheet with the
 * PNG. Showing the live card first doubles as the load gate — Coil covers are decoded by the time
 * the user acts, so the exported image is never blank.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ShareImageSheet(
    onDismiss: () -> Unit,
    caption: String? = null,
    card: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = rememberCaptureController()
    var busy by remember { mutableStateOf(false) }

    // Capture once per tap, then run [block] over the bitmap. Guards against concurrent taps.
    fun runAction(block: suspend (androidx.compose.ui.graphics.ImageBitmap) -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            try {
                block(controller.captureAsync().await())
            } catch (_: Throwable) {
                // Node left composition mid-capture — keep the sheet open for a retry.
            } finally {
                busy = false
            }
        }
    }

    com.anisync.android.presentation.components.AppModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.share_image_preview_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Height-capped + scrollable so a tall card still leaves the action row visible.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .width(ShareCardWidth)
                        .clip(ShareCardShape)
                        .capturable(controller)
                ) {
                    card()
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SheetAction(
                    icon = Icons.Filled.Download,
                    label = stringResource(R.string.share_action_save),
                    enabled = !busy,
                    container = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    runAction { bmp ->
                        val ok = ShareUtils.saveCardToGallery(context, bmp)
                        Toast.makeText(
                            context,
                            context.getString(
                                if (ok) R.string.share_saved_to_gallery else R.string.share_save_failed
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                SheetAction(
                    icon = Icons.Filled.ContentCopy,
                    label = stringResource(R.string.share_action_copy),
                    enabled = !busy,
                    container = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    // Copy just the link — no capture needed; pastes cleanly into the Feed composer.
                    caption?.let { ShareUtils.copyText(context, it) }
                    Toast.makeText(context, R.string.share_copied, Toast.LENGTH_SHORT).show()
                }
                SheetAction(
                    icon = Icons.Filled.Share,
                    label = stringResource(R.string.share_action_share),
                    enabled = !busy,
                    container = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    runAction { bmp ->
                        ShareUtils.shareBitmap(context, bmp, caption)
                        onDismiss()
                    }
                }
            }
        }
    }
}

/** One icon-over-label action tile in the share sheet's action row. */
@Composable
private fun RowScope.SheetAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    container: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = container,
        contentColor = contentColor,
        modifier = Modifier
            .weight(1f)
            .height(64.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

/**
 * Banner box shared by the share cards: a cropped [bannerUrl] behind a bottom-weighted scrim,
 * with [content] overlaid (title, badges). Falls back to a solid accent when no banner exists.
 */
@Composable
fun ShareCardBannerBox(
    bannerUrl: String?,
    height: Dp,
    modifier: Modifier = Modifier,
    scrimAlpha: Float = 0.72f,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier.fillMaxWidth().height(height)) {
        if (bannerUrl != null) {
            AsyncImage(
                model = bannerUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.10f),
                        1f to Color.Black.copy(alpha = scrimAlpha)
                    )
                )
        )
        content()
    }
}

/**
 * Opaque, rounded, self-contained surface every share card sits on. Opaque matters: the PNG
 * is composited over arbitrary chat/story backgrounds, so no theme transparency may leak
 * through. Ends with the shared AniSync footer so every exported image is attributed.
 *
 * [handle] is the AniList username shown in the footer (omitted when unknown).
 */
@Composable
fun ShareCardScaffold(
    modifier: Modifier = Modifier,
    handle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .width(ShareCardWidth)
            .clip(ShareCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        content()
        ShareCardFooter(handle = handle)
    }
}

/** AniSync wordmark + monochrome mark + optional @handle. Kept consistent across all cards. */
@Composable
private fun ShareCardFooter(handle: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_monochrome),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Fixed brand name so exported cards always read "AniSync", even from
            // preview/debug builds where app_name carries a suffix.
            Text(
                text = stringResource(R.string.share_card_brand),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (!handle.isNullOrBlank()) {
                Text(
                    text = "@$handle",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = stringResource(R.string.share_card_source),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * One value+label cell for the stat rows. [value] is the big figure, [label] the eyebrow
 * beneath it. Weighted by the caller so a row of tiles splits the width evenly.
 */
@Composable
fun ShareStatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Rounded secondary-container pill for genre tags. Shared by the media and stats cards. */
@Composable
fun ShareChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
