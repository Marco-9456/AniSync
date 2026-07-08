package com.anisync.android.presentation.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.SegmentedTabGroup
import com.anisync.android.ui.theme.shareCardColorScheme
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.CaptureController
import kotlin.math.roundToInt

/** Card color palette for the exported image, independent of the app's active theme. */
enum class ShareCardTheme { AUTO, LIGHT, DARK, AMOLED, COVER }

/** Canvas aspect. COMPACT = content-height chat preview; SQUARE/STORY sit the card on a backdrop. */
enum class ShareCardFormat { COMPACT, SQUARE, STORY }

/** Per-card layout variant. Only the media card ships a second (HERO) template today. */
enum class ShareCardTemplate { STANDARD, HERO }

/** User-tunable state for a share card, owned by [ShareImageSheet] and read by the cards. */
@Immutable
data class ShareCardConfig(
    val theme: ShareCardTheme = ShareCardTheme.AUTO,
    val format: ShareCardFormat = ShareCardFormat.COMPACT,
    val template: ShareCardTemplate = ShareCardTemplate.STANDARD,
    val showScore: Boolean = true,
    val showProgress: Boolean = true,
    val caption: String = "",
)

/** Lets the scaffold and cards read live customization without threading it through every param. */
val LocalShareCardConfig = compositionLocalOf { ShareCardConfig() }

/** Extra width the framed formats add around the [ShareCardWidth] card for backdrop margins. */
private val FrameMargin = 20.dp

/** Fixed export width in px. Density is overridden so the PNG is this crisp on every device. */
private const val ExportWidthPx = 1080f

/** Preview cap so the card + controls both stay on screen without scrolling the card away. */
private val PreviewMaxHeight = 280.dp

/**
 * Renders [card] exactly as it will be exported — under the chosen [ShareCardTheme], inside the
 * chosen [ShareCardFormat] frame — and attaches the [controller] so the sheet can snapshot it.
 *
 * The capture node is laid out under an **overridden density** so it always measures to
 * [ExportWidthPx] px wide (crisp regardless of the device's real density), then scaled *down* for
 * on-screen display via [scaledToFit]. The capture reads the node's own high-res layer, so the
 * export stays full resolution while the preview fits the sheet.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ShareCaptureArea(
    controller: CaptureController,
    config: ShareCardConfig,
    seedColor: Color?,
    modifier: Modifier = Modifier,
    maxPreviewHeight: Dp = PreviewMaxHeight,
    card: @Composable () -> Unit,
) {
    val displayDensity = LocalDensity.current
    val frameWidthDp: Dp = if (config.format == ShareCardFormat.COMPACT) {
        ShareCardWidth
    } else {
        ShareCardWidth + FrameMargin * 2
    }
    val captureDensity = ExportWidthPx / frameWidthDp.value
    val maxPreviewHeightPx = with(displayDensity) { maxPreviewHeight.toPx() }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val availWidthPx = constraints.maxWidth.toFloat()
        // Never upscale past the card's natural dp size; otherwise fit within the sheet's width/height.
        val naturalScale = (frameWidthDp.value * displayDensity.density) / ExportWidthPx

        CompositionLocalProvider(
            LocalDensity provides Density(captureDensity, displayDensity.fontScale)
        ) {
            Box(
                Modifier
                    .scaledToFit(naturalScale, availWidthPx, maxPreviewHeightPx)
                    .capturable(controller)
            ) {
                ShareCardThemeScope(config.theme, seedColor) {
                    CompositionLocalProvider(LocalShareCardConfig provides config) {
                        ShareFrame(config.format, frameWidthDp, card)
                    }
                }
            }
        }
    }
}

/**
 * Measures the child at its natural (export-resolution) size, then places it scaled so it fits
 * within [maxWidthPx] × [maxHeightPx] without ever exceeding [baseScale]. The captured node keeps
 * its full pixel size — only the on-screen placement shrinks.
 */
private fun Modifier.scaledToFit(baseScale: Float, maxWidthPx: Float, maxHeightPx: Float): Modifier =
    layout { measurable, _ ->
        val placeable = measurable.measure(Constraints())
        val fit = minOf(
            baseScale,
            maxWidthPx / placeable.width.coerceAtLeast(1),
            maxHeightPx / placeable.height.coerceAtLeast(1),
        )
        layout((placeable.width * fit).roundToInt(), (placeable.height * fit).roundToInt()) {
            placeable.placeWithLayer(0, 0) {
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = fit
                scaleY = fit
            }
        }
    }

/**
 * Wraps [content] in a [MaterialTheme] carrying the card's own color scheme when the user picks a
 * non-[ShareCardTheme.AUTO] look. COVER seeds a MaterialKolor scheme from the artwork [seedColor],
 * in the app's current light/dark polarity; with no seed it falls through to AUTO.
 */
@Composable
fun ShareCardThemeScope(theme: ShareCardTheme, seedColor: Color?, content: @Composable () -> Unit) {
    if (theme == ShareCardTheme.AUTO || (theme == ShareCardTheme.COVER && seedColor == null)) {
        content()
        return
    }
    val appDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val scheme = when (theme) {
        ShareCardTheme.LIGHT -> shareCardColorScheme(dark = false)
        ShareCardTheme.DARK -> shareCardColorScheme(dark = true)
        ShareCardTheme.AMOLED -> shareCardColorScheme(dark = true, amoled = true)
        ShareCardTheme.COVER -> shareCardColorScheme(dark = appDark, seed = seedColor)
        ShareCardTheme.AUTO -> shareCardColorScheme(dark = appDark) // unreachable; keeps when exhaustive
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}

/**
 * Sits [card] on a full-bleed backdrop for the SQUARE / STORY formats: a soft theme-derived gradient
 * (so it reads right in light, dark and AMOLED) with the card floating above it on a shadow. COMPACT
 * renders the card bare so its rounded corners stay transparent for chat bubbles.
 *
 * The gradient is built from the *card's* scheme (this runs inside [ShareCardThemeScope]), so it
 * tracks the chosen theme automatically — no image, no blur, nothing to leak into the capture.
 */
@Composable
private fun ShareFrame(
    format: ShareCardFormat,
    frameWidth: Dp,
    card: @Composable () -> Unit,
) {
    if (format == ShareCardFormat.COMPACT) {
        card()
        return
    }
    val scheme = MaterialTheme.colorScheme
    val minHeight = if (format == ShareCardFormat.SQUARE) frameWidth else frameWidth * 16f / 9f
    Box(
        modifier = Modifier
            .width(frameWidth)
            .heightIn(min = minHeight)
            .clip(ShareCardShapeFramed)
            .background(Brush.verticalGradient(listOf(scheme.surfaceBright, scheme.surfaceDim))),
        contentAlignment = Alignment.Center,
    ) {
        // Soft lift, not a heavy drop: reduced elevation + translucent shadow colors, otherwise
        // the card reads as heavily outlined on the light gradient.
        Box(
            Modifier
                .padding(FrameMargin)
                .shadow(
                    elevation = 8.dp,
                    shape = ShareCardShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.35f),
                )
        ) {
            card()
        }
    }
}

/**
 * The customization row shown under the live preview: theme + format (+ template) pickers and the
 * per-card privacy toggles, all built on the app's [SegmentedTabGroup]. [coverAvailable] gates the
 * COVER theme (needs an artwork seed); [templates] drives the style picker; [supportsPrivacy] the
 * score/progress toggles. [templateLabel] renames the style options per card type (Card/Poster,
 * Stats/Recap, Grid/Ranked); null falls back to the generic labels.
 */
@Composable
fun ShareCustomizeControls(
    config: ShareCardConfig,
    onConfig: (ShareCardConfig) -> Unit,
    coverAvailable: Boolean,
    supportsPrivacy: Boolean,
    templates: List<ShareCardTemplate>,
    modifier: Modifier = Modifier,
    templateLabel: (@Composable (ShareCardTemplate) -> String)? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ControlRow(label = stringResource(R.string.share_customize_theme)) {
            val themes = remember(coverAvailable) {
                buildList {
                    addAll(listOf(ShareCardTheme.AUTO, ShareCardTheme.LIGHT, ShareCardTheme.DARK, ShareCardTheme.AMOLED))
                    if (coverAvailable) add(ShareCardTheme.COVER)
                }
            }
            SegmentedTabGroup(
                options = themes,
                selected = config.theme,
                onSelect = { onConfig(config.copy(theme = it)) },
                label = { themeLabel(it) },
            )
        }

        ControlRow(label = stringResource(R.string.share_customize_format)) {
            SegmentedTabGroup(
                options = ShareCardFormat.entries,
                selected = config.format,
                onSelect = { onConfig(config.copy(format = it)) },
                label = { formatLabel(it) },
                fillEqually = true,
            )
        }

        if (templates.size > 1) {
            ControlRow(label = stringResource(R.string.share_customize_template)) {
                SegmentedTabGroup(
                    options = templates,
                    selected = config.template,
                    onSelect = { onConfig(config.copy(template = it)) },
                    label = { templateLabel?.invoke(it) ?: defaultTemplateLabel(it) },
                    fillEqually = true,
                )
            }
        }

        if (supportsPrivacy) {
            ControlRow(label = stringResource(R.string.share_customize_show)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToggleChip(
                        selected = config.showScore,
                        label = stringResource(R.string.share_toggle_score),
                        onToggle = { onConfig(config.copy(showScore = it)) },
                    )
                    ToggleChip(
                        selected = config.showProgress,
                        label = stringResource(R.string.share_toggle_progress),
                        onToggle = { onConfig(config.copy(showProgress = it)) },
                    )
                }
            }
        }
    }
}

/** Small labelled group: an eyebrow over its control [content]. */
@Composable
private fun ControlRow(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/** A leading-check FilterChip used as an on/off privacy toggle. */
@Composable
private fun ToggleChip(selected: Boolean, label: String, onToggle: (Boolean) -> Unit) {
    FilterChip(
        selected = selected,
        onClick = { onToggle(!selected) },
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null) }
        } else null,
    )
}

@Composable
private fun themeLabel(theme: ShareCardTheme): String = stringResource(
    when (theme) {
        ShareCardTheme.AUTO -> R.string.share_theme_auto
        ShareCardTheme.LIGHT -> R.string.share_theme_light
        ShareCardTheme.DARK -> R.string.share_theme_dark
        ShareCardTheme.AMOLED -> R.string.share_theme_amoled
        ShareCardTheme.COVER -> R.string.share_theme_cover
    }
)

@Composable
private fun formatLabel(format: ShareCardFormat): String = stringResource(
    when (format) {
        ShareCardFormat.COMPACT -> R.string.share_format_compact
        ShareCardFormat.SQUARE -> R.string.share_format_square
        ShareCardFormat.STORY -> R.string.share_format_story
    }
)

@Composable
private fun defaultTemplateLabel(template: ShareCardTemplate): String = stringResource(
    when (template) {
        ShareCardTemplate.STANDARD -> R.string.share_template_standard
        ShareCardTemplate.HERO -> R.string.share_template_hero
    }
)
