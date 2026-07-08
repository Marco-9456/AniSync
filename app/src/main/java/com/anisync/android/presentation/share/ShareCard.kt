package com.anisync.android.presentation.share

import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.util.ShareUtils
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.launch

/**
 * Fixed capture width for every share card — a portrait-leaning social card that reads
 * well as a chat/story preview. Cards size their height to content.
 */
val ShareCardWidth = 340.dp

/** Rounded outer shape shared by all share cards (matches the app's 28dp hero radius). */
internal val ShareCardShape = RoundedCornerShape(28.dp)

/** Slightly larger radius for the Square/Story backdrop frame the card sits inside. */
internal val ShareCardShapeFramed = RoundedCornerShape(32.dp)

/**
 * Share customizer entry point: previews a [card] exactly as it will be shared and lets the user
 * **customize** it live — theme, aspect (compact / square / story), an optional baked caption, and
 * (when [supportsPrivacy]) whether the score/progress show. Then it offers: save the PNG to the
 * gallery, copy the [link] to the clipboard, or open the system share sheet with the image (the
 * user's caption + [link] ride along as the share text).
 *
 * Preview, controls, caption and actions form one scrolling flow — the card scrolls along rather
 * than pinning or resizing (no size jump when the keyboard opens).
 *
 * The live preview doubles as the load gate: Coil covers are decoded by the time the user acts, so
 * the export is never blank. [seedColor] (artwork color) enables the COVER theme; [templates]
 * populates the style picker.
 *
 * Presented as a full-screen dialog whose window blurs the app behind it, with the customizer
 * floating on a translucent, theme-tinted veil. Tapping the veil dismisses; taps on the content
 * are consumed. Falls back to a heavier dim when window blur is unavailable.
 */
@Composable
fun ShareImageSheet(
    onDismiss: () -> Unit,
    link: String? = null,
    seedColor: Color? = null,
    supportsPrivacy: Boolean = false,
    templates: List<ShareCardTemplate> = emptyList(),
    templateLabel: (@Composable (ShareCardTemplate) -> String)? = null,
    card: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val view = LocalView.current
        val context = LocalContext.current
        // Cross-window blur is a device capability (OEMs/power-saving disable it); when it's off,
        // FLAG_BLUR_BEHIND is silently ignored, so compensate with a much heavier veil + dim.
        var blurActive by remember { mutableStateOf(false) }
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                context.getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true
            blurActive = canBlur
            if (window != null) {
                if (canBlur) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.attributes = window.attributes.apply { blurBehindRadius = 90 }
                    window.setDimAmount(0.2f)
                } else {
                    window.setDimAmount(0.45f)
                }
            }
            onDispose { }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                // Theme-tinted veil over the blur so text keeps contrast on any wallpaper/screen.
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = if (blurActive) 0.5f else 0.88f)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .systemBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            // One scrolling flow (card included); width-capped so tablets get a centered column,
            // not edge-to-edge controls.
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    // Consume taps so interacting with the controls never falls through to dismiss.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShareCustomizerContent(
                    onDismiss = onDismiss,
                    link = link,
                    seedColor = seedColor,
                    supportsPrivacy = supportsPrivacy,
                    templates = templates,
                    templateLabel = templateLabel,
                    previewMaxHeight = 420.dp,
                    card = card,
                )
            }
        }
    }
}

/**
 * The customizer itself — preview, controls, caption and actions in **one scrolling flow** (the
 * card scrolls along like any other element; nothing pins or resizes when the keyboard opens —
 * focusing the caption simply scrolls it into view). Shared verbatim by both presentation hosts,
 * which wrap it in their own scroll container.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ShareCustomizerContent(
    onDismiss: () -> Unit,
    link: String?,
    seedColor: Color?,
    supportsPrivacy: Boolean,
    templates: List<ShareCardTemplate>,
    templateLabel: (@Composable (ShareCardTemplate) -> String)?,
    previewMaxHeight: Dp,
    card: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val controller = rememberCaptureController()
    var busy by remember { mutableStateOf(false) }
    var config by remember {
        mutableStateOf(ShareCardConfig(template = templates.firstOrNull() ?: ShareCardTemplate.STANDARD))
    }

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

    // The caption baked onto the card also rides along as the shared text, above the link.
    val shareText = listOfNotNull(config.caption.trim().ifBlank { null }, link).joinToString("\n\n")
        .ifBlank { null }

    ShareCaptureArea(
        controller = controller,
        config = config,
        seedColor = seedColor,
        maxPreviewHeight = previewMaxHeight,
        card = card,
    )

            Spacer(Modifier.height(16.dp))

            ShareCustomizeControls(
                config = config,
                onConfig = { config = it },
                coverAvailable = seedColor != null,
                supportsPrivacy = supportsPrivacy,
                templates = templates,
                templateLabel = templateLabel,
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = config.caption,
                onValueChange = { config = config.copy(caption = it.take(80)) },
                label = { Text(stringResource(R.string.share_caption_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SheetAction(
                    icon = Icons.Filled.Download,
                    label = stringResource(R.string.share_action_save),
                    busy = busy,
                    container = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    runAction { bmp ->
                        val ok = ShareUtils.saveCardToGallery(context, bmp)
                        if (ok) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                    busy = busy,
                    container = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    // Copy just the link — no capture needed; pastes cleanly into the Feed composer.
                    link?.let { ShareUtils.copyText(context, it) }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    Toast.makeText(context, R.string.share_copied, Toast.LENGTH_SHORT).show()
                }
                SheetAction(
                    icon = Icons.Filled.Share,
                    label = stringResource(R.string.share_action_share),
                    busy = busy,
                    container = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    runAction { bmp ->
                        ShareUtils.shareBitmap(context, bmp, shareText)
                        onDismiss()
                    }
                }
            }
}

/** One icon-over-label action tile in the share sheet's action row. Shows a spinner while busy. */
@Composable
private fun RowScope.SheetAction(
    icon: ImageVector,
    label: String,
    busy: Boolean,
    container: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = !busy,
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
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(4.dp))
                Text(text = label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
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
 * through. Renders the optional user [caption] then the shared AniSync footer, so every exported
 * image is captioned + attributed.
 *
 * [handle] is the AniList username shown in the footer (omitted when unknown).
 */
@Composable
fun ShareCardScaffold(
    modifier: Modifier = Modifier,
    handle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val caption = LocalShareCardConfig.current.caption.trim()
    Column(
        modifier = modifier
            .width(ShareCardWidth)
            .clip(ShareCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        content()
        if (caption.isNotBlank()) ShareCardCaption(caption)
        ShareCardFooter(handle = handle)
    }
}

/** The user's baked one-liner, set just above the footer. */
@Composable
private fun ShareCardCaption(caption: String) {
    Text(
        text = caption,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 16.dp)
    )
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
