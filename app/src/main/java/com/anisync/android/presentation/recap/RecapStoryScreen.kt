package com.anisync.android.presentation.recap

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.recap.RecapPage
import com.anisync.android.domain.recap.isCard
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.EmptyState
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.navigation.ShareStudio
import com.anisync.android.presentation.share.cards.CardMark
import com.anisync.android.presentation.share.cards.CardSpec
import com.anisync.android.presentation.share.cards.RecapMonthTemplate
import com.anisync.android.presentation.share.cards.RecapYearTemplate
import com.anisync.android.presentation.share.cards.cardText
import com.anisync.android.presentation.share.studio.ShareCaptureArea
import com.anisync.android.presentation.share.studio.ShareStudioSubject
import com.anisync.android.presentation.share.toShareBitmap
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.data.CardSize
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.presentation.util.rememberAnimationsEnabled
import com.anisync.android.ui.theme.resolveDarkTheme
import com.anisync.android.ui.theme.shareCardColorScheme
import com.anisync.android.util.ShareUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

/** How long a page stays before the story moves on. */
private const val PAGE_MILLIS = 8_000

/** The progress of a page that does not run on a timer. */
private val FullProgress: () -> Float = { 1f }

/** The frame every page is drawn at. */
private val FrameWidth = 448.dp
private val FrameHeight = 997.dp

/** The largest font scale the story's fixed frame takes. */
private const val MAX_FONT_SCALE = 1.15f

/** How far down a swipe has to travel to close the story. */
private val CloseDistance = 120.dp

@Composable
fun RecapStoryScreen(
    onClose: () -> Unit,
    onOpenStudio: (ShareStudio) -> Unit,
    onOpenStatistics: () -> Unit,
    viewModel: RecapStoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val onAction = viewModel::onAction
    LifecycleEventEffect(Lifecycle.Event.ON_START) { onAction(RecapStoryAction.Visible(true)) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { onAction(RecapStoryAction.Visible(false)) }
    val close = {
        onAction(RecapStoryAction.Visible(false))
        onClose()
    }
    BackHandler(onBack = close)

    val story = state.story
    if (story == null) {
        StoryPlaceholder(state, onAction, close)
        return
    }
    CompositionLocalProvider(LocalAppScheme provides MaterialTheme.colorScheme) {
        StoryStage(state, story, onAction, close, onOpenStudio) {
            onAction(RecapStoryAction.Visible(false))
            onAction(RecapStoryAction.SeeStatistics)
            onOpenStatistics()
        }
    }
}

/** The app's own scheme, which pages without a seed wear while every other page wears its own. */
private val LocalAppScheme = staticCompositionLocalOf<ColorScheme?> { null }

@Composable
private fun StoryPlaceholder(state: RecapStoryUiState, onAction: (RecapStoryAction) -> Unit, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            state.loading -> AppCircularProgressIndicator(Modifier.align(Alignment.Center))
            state.failed -> EmptyState(
                icon = Icons.Default.Public,
                title = stringResource(R.string.forum_empty_failed_title),
                description = stringResource(if (state.rateLimited) R.string.profile_rate_limited_error else R.string.recap_failed_body),
                actionLabel = stringResource(R.string.retry),
                actionIcon = Icons.Default.Refresh,
                onAction = { onAction(RecapStoryAction.Retry) },
                emblemShape = RoundedCornerShape(22.dp),
                modifier = Modifier.align(Alignment.Center),
            )
            else -> EmptyState(
                icon = Icons.Default.AutoAwesomeMotion,
                title = stringResource(R.string.recap_missing_title),
                description = stringResource(R.string.recap_missing_body),
                emblemShape = RoundedCornerShape(22.dp),
                modifier = Modifier.align(Alignment.Center),
            )
        }
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_recap_close), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/**
 * A page's scheme: its seed in the app's own theme, light or dark, AMOLED and palette style as the
 * viewer set them, the way a title's page wears its cover. A page without a seed wears the app's.
 */
@Composable
private fun pageScheme(seed: Color?): ColorScheme {
    val themeMode by LocalAppSettings.current.themeMode.collectAsStateWithLifecycle()
    return recapScheme(seed, LocalAppScheme.current ?: MaterialTheme.colorScheme, themeMode.resolveDarkTheme())
}

@Composable
private fun StoryStage(
    state: RecapStoryUiState,
    story: StoryModel,
    onAction: (RecapStoryAction) -> Unit,
    onClose: () -> Unit,
    onOpenStudio: (ShareStudio) -> Unit,
    onOpenStatistics: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pageLayer = rememberGraphicsLayer()
    val cardLayer = rememberGraphicsLayer()
    val autoAdvance = rememberAutoAdvance()
    val page = story.pages[state.page]
    val scheme = pageScheme(story.seeds[state.page])

    val songPending by rememberUpdatedState(state.songPending)
    val progress = remember(state.page, state.replays) { Animatable(0f) }
    LaunchedEffect(state.page, state.replays, state.isStopped, autoAdvance) {
        if (!autoAdvance || page.isCard || state.isStopped) return@LaunchedEffect
        if (state.page == 0) snapshotFlow { songPending }.first { !it }
        val remaining = ((1f - progress.value) * PAGE_MILLIS).roundToInt().coerceAtLeast(1)
        progress.animateTo(1f, tween(remaining, easing = LinearEasing))
        onAction(RecapStoryAction.Next)
    }

    var busy by remember { mutableStateOf(false) }
    fun shareImage(make: suspend () -> ImageBitmap) {
        if (busy) return
        scope.launch {
            busy = true
            try {
                ShareUtils.shareBitmap(context, make(), story.card.link)
            } catch (_: Throwable) {
                // The layer left composition while it was read, the viewer can tap again.
            } finally {
                busy = false
            }
        }
    }
    fun saveCard() {
        if (busy || cardLayer.size.width == 0) return
        scope.launch {
            busy = true
            try {
                val saved = ShareUtils.saveCardToGallery(context, cardLayer.toShareBitmap())
                Toast.makeText(context, if (saved) R.string.share_saved_to_gallery else R.string.share_save_failed, Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {
            } finally {
                busy = false
            }
        }
    }
    val storageAccess = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) saveCard() else Toast.makeText(context, R.string.share_save_needs_storage, Toast.LENGTH_SHORT).show()
    }

    val actions = PageActions(
        onStart = { onAction(RecapStoryAction.Next) },
        onCustomize = {
            onOpenStudio(
                ShareStudio(
                    subject = ShareStudioSubject.RECAP.name,
                    id = if (story.isPreview) PREVIEW_STUDIO_ID else 0,
                    periodKey = story.period.key,
                    coverColor = story.coverColor,
                )
            )
        },
        onSaveCard = {
            if (ShareUtils.saveNeedsStoragePermission(context)) storageAccess.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else saveCard()
        },
        onShareCard = { if (cardLayer.size.width > 0) shareImage { cardLayer.toShareBitmap() } },
        onSeeStatistics = onOpenStatistics,
        onReplay = { onAction(RecapStoryAction.Replay) },
    )
    val onSharePage = { shareImage { storyCrop(context, pageLayer.toShareBitmap(), scheme) } }

    val canvas: @Composable () -> Unit = {
        StoryCanvas(
            state = state,
            story = story,
            page = page,
            progress = if (!autoAdvance || page.isCard) FullProgress else { { progress.value } },
            pageLayer = pageLayer,
            cardLayer = cardLayer,
            actions = actions,
            onAction = onAction,
            onClose = onClose,
            onSharePage = onSharePage,
        )
    }

    MaterialTheme(colorScheme = scheme) {
        BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (LocalAdaptiveInfo.current.isExpandedOrWider) {
                TabletStage(story, state, onAction, maxHeightDp = maxHeight.value, canvas = canvas)
            } else {
                val scale = frameScale(maxWidth, maxHeight)
                val bar = (maxWidth - FrameWidth * scale) / 2
                if (bar > 0.dp) {
                    RecapStageBackdrop(page, story)
                    // The bars beside a frame narrower than the screen turn pages like its own sides.
                    Box(
                        Modifier.fillMaxSize().pointerInput(bar) {
                            val barPx = bar.toPx()
                            detectTapGestures { offset ->
                                when {
                                    offset.x < barPx -> onAction(RecapStoryAction.Previous)
                                    offset.x > size.width - barPx -> onAction(RecapStoryAction.Next)
                                }
                            }
                        }
                    )
                }
                Box(Modifier.align(Alignment.Center).requiredSize(FrameWidth * scale, FrameHeight * scale)) {
                    ScaledFrame(scale, canvas)
                }
            }
        }
    }
}

/**
 * How far the frame scales on a phone. It fits the screen, except that a screen a little wider than
 * the frame gets the frame at its full width, losing a sliver of the top and bottom edges, which hold
 * only background, rather than thin bars down both sides.
 */
private fun frameScale(width: Dp, height: Dp): Float {
    val fit = min(width / FrameWidth, height / FrameHeight)
    val byWidth = width / FrameWidth
    val overflow = FrameHeight * byWidth - height
    return if (overflow > 0.dp && overflow <= height * MAX_EDGE_CROP) byWidth else fit
}

/** The most of the frame's height a wide screen may crop, split between its top and bottom. */
private const val MAX_EDGE_CROP = 0.04f

/**
 * Draws [content] on the 448 by 997 frame, scaled by [scale] so it fits the space it is given. The
 * frame's boxes have fixed sizes, so a large system font scale is capped where the text still fits.
 */
@Composable
private fun ScaledFrame(scale: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density * scale, density.fontScale.coerceAtMost(MAX_FONT_SCALE))) {
        Box(Modifier.requiredSize(FrameWidth, FrameHeight)) { content() }
    }
}

/**
 * One story page as a Story share card: the page at [width], in the colours it wears in the story,
 * centred and cropped to [height] and marked the way Share this page crops and marks it.
 */
@Composable
fun RecapSlide(story: StoryModel, page: RecapPage, width: Dp, height: Dp) {
    val themeMode by LocalAppSettings.current.themeMode.collectAsStateWithLifecycle()
    val dark = themeMode.resolveDarkTheme()
    val scheme = recapScheme(story.seeds.getOrNull(story.pages.indexOf(page)), shareCardColorScheme(dark), dark)
    val density = LocalDensity.current
    val scale = width / FrameWidth
    MaterialTheme(colorScheme = scheme) {
        Box(Modifier.size(width, height).clipToBounds(), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalDensity provides Density(density.density * scale, density.fontScale.coerceAtMost(MAX_FONT_SCALE))) {
                Box(Modifier.requiredSize(FrameWidth, FrameHeight)) {
                    RecapPageBackground(page, story)
                    RecapPageContent(page, story, song = null, actions = StillPageActions) {}
                }
            }
            CardMark(Modifier.align(Alignment.BottomEnd).padding(width / 18), size = width / 15)
        }
    }
}

/** A slide in the share studio is a picture, so its buttons do nothing. */
private val StillPageActions = PageActions({}, {}, {}, {}, {}, {})

/** WT1: the page centred over its blurred artwork, with arrows beside it. */
@Composable
private fun TabletStage(
    story: StoryModel,
    state: RecapStoryUiState,
    onAction: (RecapStoryAction) -> Unit,
    maxHeightDp: Float,
    canvas: @Composable () -> Unit,
) {
    val pageHeight = (maxHeightDp - 40f).dp
    val scale = pageHeight / FrameHeight
    val pageWidth = FrameWidth * scale
    Box(Modifier.fillMaxSize()) {
        ArtworkBackdrop(story.rankedCovers.firstOrNull(), Modifier.fillMaxSize(), Backdrop.STAGE)
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f)))
        Box(
            Modifier
                .align(Alignment.Center)
                .size(pageWidth, pageHeight)
                .dropShadow(RoundedCornerShape(28.dp), Shadow(radius = 48.dp, color = Color.Black.copy(alpha = 0.5f), offset = DpOffset(0.dp, 20.dp)))
                .clip(RoundedCornerShape(28.dp))
        ) {
            ScaledFrame(scale, canvas)
        }
        val arrowOffset = pageWidth / 2 + 32.dp + 28.dp
        if (state.page > 0) {
            PageArrow(Icons.Default.ChevronLeft, stringResource(R.string.cd_recap_previous), Modifier.align(Alignment.Center).offset(x = -arrowOffset)) {
                onAction(RecapStoryAction.Previous)
            }
        }
        if (state.page < story.pages.lastIndex) {
            PageArrow(Icons.Default.ChevronRight, stringResource(R.string.cd_recap_next), Modifier.align(Alignment.Center).offset(x = arrowOffset)) {
                onAction(RecapStoryAction.Next)
            }
        }
    }
}

@Composable
private fun PageArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(scheme.onSurface.copy(alpha = 0.14f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = scheme.onSurface, modifier = Modifier.size(28.dp))
    }
}

/** The page, its background and content recorded for sharing, under the story's chrome. */
@Composable
private fun StoryCanvas(
    state: RecapStoryUiState,
    story: StoryModel,
    page: RecapPage,
    progress: () -> Float,
    pageLayer: GraphicsLayer,
    cardLayer: GraphicsLayer,
    actions: PageActions,
    onAction: (RecapStoryAction) -> Unit,
    onClose: () -> Unit,
    onSharePage: () -> Unit,
) {
    val density = LocalDensity.current
    var drag by remember { mutableFloatStateOf(0f) }
    val closeAt = with(density) { CloseDistance.toPx() }
    val nextLabel = stringResource(R.string.cd_recap_next)
    val previousLabel = stringResource(R.string.cd_recap_previous)
    val pageLabel = stringResource(R.string.a11y_recap_page, count(state.page + 1), count(story.pages.size))
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = drag.coerceAtLeast(0f) }
            .semantics {
                stateDescription = pageLabel
                customActions = listOf(
                    CustomAccessibilityAction(nextLabel) { onAction(RecapStoryAction.Next); true },
                    CustomAccessibilityAction(previousLabel) { onAction(RecapStoryAction.Previous); true },
                )
            }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(story) {
                    var held = false
                    detectTapGestures(
                        onPress = {
                            tryAwaitRelease()
                            if (held) {
                                held = false
                                onAction(RecapStoryAction.Hold(false))
                            }
                        },
                        onLongPress = {
                            held = true
                            onAction(RecapStoryAction.Hold(true))
                        },
                        onTap = { offset ->
                            if (offset.x < size.width / 3f) onAction(RecapStoryAction.Previous) else onAction(RecapStoryAction.Next)
                        },
                    )
                }
                .pointerInput(story) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (drag > closeAt) onClose()
                            drag = 0f
                        },
                        onDragCancel = { drag = 0f },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            drag = (drag + amount).coerceAtLeast(0f)
                        },
                    )
                }
                .drawWithContent {
                    pageLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(pageLayer)
                }
        ) {
            AnimatedContent(
                targetState = state.page,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
                label = "recap_page",
            ) { index ->
                val shown = story.pages[index]
                MaterialTheme(colorScheme = pageScheme(story.seeds[index])) {
                    Box(Modifier.fillMaxSize()) {
                        RecapPageBackground(shown, story)
                        RecapPageContent(shown, story, state.song, actions) {
                            StoryCard(story, cardLayer)
                        }
                    }
                }
            }
        }
        StoryChrome(state, story, page, progress, onAction, onClose, onSharePage)
    }
}

/** The recap's Story card, scaled into the 315 by 560 slot the card pages give it. */
@Composable
private fun StoryCard(story: StoryModel, layer: GraphicsLayer) {
    Box(
        Modifier
            .fillMaxSize()
            .dropShadow(RoundedCornerShape(24.dp), Shadow(radius = 36.dp, color = Color.Black.copy(alpha = 0.5f), offset = DpOffset(0.dp, 16.dp)))
            .clip(RoundedCornerShape(24.dp))
    ) {
        ShareCaptureArea(layer, Modifier.fillMaxSize()) {
            val spec = CardSpec(size = CardSize.STORY)
            if (story.isYear) RecapYearTemplate.Card(story.card, spec) else RecapMonthTemplate.Card(story.card, spec)
        }
    }
}

@Composable
private fun StoryChrome(
    state: RecapStoryUiState,
    story: StoryModel,
    page: RecapPage,
    progress: () -> Float,
    onAction: (RecapStoryAction) -> Unit,
    onClose: () -> Unit,
    onSharePage: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val ink = scheme.onSurface
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.offset(16.dp, 60.dp).width(416.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val bar = ink.copy(alpha = 0.95f)
            story.pages.indices.forEach { index ->
                // Read while drawing, so the running page redraws its bar without recomposing the story.
                Box(
                    Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(ink.copy(alpha = 0.28f)).drawBehind {
                        val fill = when {
                            index < state.page -> 1f
                            index == state.page -> progress()
                            else -> 0f
                        }
                        if (fill > 0f) drawRoundRect(bar, size = Size(size.width * fill, size.height), cornerRadius = CornerRadius(2.dp.toPx()))
                    }
                )
            }
        }
        Row(
            Modifier.offset(16.dp, 76.dp).width(416.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            UserAvatar(contentDescription = null, size = 31.46.dp, url = story.avatarUrl)
            Column(Modifier.weight(1f)) {
                val title = if (story.isYear) {
                    stringResource(R.string.recap_story_year_title, story.period.key)
                } else {
                    stringResource(R.string.recap_story_month_title, monthName((story.period as com.anisync.android.domain.recap.RecapPeriod.Month).month.month))
                }
                Text(title, style = cardText(14, 20, FontWeight.Bold), color = ink, maxLines = 1)
                state.song?.let { song ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = ink.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                        val playing = song.artist?.let { stringResource(R.string.filter_chip_value, song.title, it) } ?: song.title
                        Text(
                            text = if (song.muted) stringResource(R.string.recap_now_playing_muted, playing) else playing,
                            style = cardText(12, 16, FontWeight.Medium),
                            color = ink.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Row(Modifier.offset(x = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                state.song?.let { song ->
                    ChromeButton(
                        icon = if (song.muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        description = stringResource(if (song.muted) R.string.cd_recap_unmute else R.string.cd_recap_mute),
                    ) { onAction(RecapStoryAction.ToggleMute) }
                }
                ChromeButton(
                    icon = if (state.paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    description = stringResource(if (state.paused) R.string.cd_recap_play else R.string.cd_recap_pause),
                ) { onAction(RecapStoryAction.TogglePause) }
                ChromeButton(Icons.Default.Close, stringResource(R.string.cd_recap_close), onClose)
            }
        }
        if (!page.isCard) {
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 880.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ink.copy(alpha = 0.14f))
                    .clickable(onClick = onSharePage)
                    .padding(start = 16.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.recap_share_page), style = cardText(14, 20, FontWeight.Bold), color = ink)
            }
        }
    }
}

@Composable
private fun ChromeButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
    }
}

/** False while TalkBack explores by touch or animations are off, when a page waits for the reader. */
@Composable
private fun rememberAutoAdvance(): Boolean {
    val context = LocalContext.current
    var exploring by remember { mutableStateOf(touchExploration(context)) }
    DisposableEffect(context) {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { exploring = it }
        manager?.addTouchExplorationStateChangeListener(listener)
        onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
    }
    return !exploring && rememberAnimationsEnabled()
}

private fun touchExploration(context: Context): Boolean =
    (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)?.isTouchExplorationEnabled == true

/**
 * A 9:16 picture of a page: the middle of the 448 by 997 frame, where every page keeps its content,
 * scaled to 1080 by 1920 with the AniSync mark in its corner.
 */
private fun storyCrop(context: Context, page: ImageBitmap, scheme: ColorScheme): ImageBitmap {
    val bitmap = page.asAndroidBitmap()
    // A software canvas cannot read a hardware bitmap, so that one is copied once.
    val source = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
    val cropHeight = (source.width * 16f / 9f).roundToInt().coerceAtMost(source.height)
    val top = ((source.height - cropHeight) / 2).coerceAtLeast(0)
    val out = createBitmap(STORY_WIDTH, STORY_HEIGHT)
    val canvas = Canvas(out)
    canvas.drawBitmap(source, Rect(0, top, source.width, top + cropHeight), Rect(0, 0, STORY_WIDTH, STORY_HEIGHT), Paint(Paint.FILTER_BITMAP_FLAG))
    if (source !== bitmap) source.recycle()
    val mark = STORY_WIDTH / 15
    val left = STORY_WIDTH - mark - STORY_WIDTH / 18
    val markTop = STORY_HEIGHT - mark - STORY_WIDTH / 18
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = scheme.primary.toArgb() }
    canvas.drawCircle(left + mark / 2f, markTop + mark / 2f, mark / 2f, paint)
    ContextCompat.getDrawable(context, R.drawable.ic_brand_glyph)?.mutate()?.let { glyph ->
        glyph.setTint(scheme.onPrimary.toArgb())
        glyph.setBounds(left, markTop, left + mark, markTop + mark)
        glyph.draw(canvas)
    }
    return out.asImageBitmap()
}

private const val STORY_WIDTH = 1080
private const val STORY_HEIGHT = 1920

/** The id a studio route carries for a Developer Tools preview rather than a stored recap. */
const val PREVIEW_STUDIO_ID = -1
