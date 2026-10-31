package com.anisync.android.presentation.share.studio

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.EmptyState
import com.anisync.android.presentation.components.MediaArtworkThemeOverride
import com.anisync.android.presentation.components.ProfileColorThemeOverride
import com.anisync.android.presentation.components.SegmentedTabGroup
import com.anisync.android.presentation.share.cards.CardTone
import com.anisync.android.presentation.share.cards.Render
import com.anisync.android.presentation.share.cards.fixedHeight
import com.anisync.android.presentation.share.cards.ShareSubject
import com.anisync.android.presentation.share.toShareBitmap
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.ui.theme.LocalAppThemeSpec
import com.anisync.android.ui.theme.paletteRoles
import com.anisync.android.ui.theme.rememberSeededColorScheme
import com.anisync.android.ui.theme.shareCardColorScheme
import com.anisync.android.util.ShareUtils
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.launch

@Composable
fun ShareStudioScreen(
    coverColor: String?,
    profileColor: String?,
    onClose: () -> Unit,
    viewModel: ShareStudioViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val amoled by LocalAppSettings.current.amoledEnabled.collectAsStateWithLifecycle()
    val appDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val appTone = when {
        !appDark -> CardTone.LIGHT
        amoled -> CardTone.PURE_BLACK
        else -> CardTone.DARK
    }
    LaunchedEffect(appTone) { viewModel.onAction(ShareStudioAction.FollowAppTone(appTone)) }

    // Read before the page palette below replaces it, so the App swatch is the app's own palette.
    val appSpec = LocalAppThemeSpec.current
    val tone = state.spec.tone
    val dark = tone != CardTone.LIGHT
    val appScheme = shareCardColorScheme(dark = dark, amoled = tone == CardTone.PURE_BLACK)
    val appLight = shareCardColorScheme(dark = false)
    val cardScheme = cardScheme(state.swatch, tone, appScheme)
    val looks = state.swatches.map { swatch ->
        val seed = swatch.seed
        if (seed == null) {
            SwatchLook(appLight.primary, appScheme.primaryContainer, appScheme.onPrimaryContainer)
        } else {
            val roles = remember(seed, dark) { paletteRoles(seed, dark, PaletteStyle.TonalSpot) }
            SwatchLook(swatch.source, roles.primaryContainer(), roles.onPrimaryContainer())
        }
    }

    MediaArtworkThemeOverride(coverColor) {
        ProfileColorThemeOverride(profileColor) {
            CompositionLocalProvider(LocalAppThemeSpec provides appSpec) {
                StudioContent(
                    state = state,
                    looks = looks,
                    cardScheme = cardScheme,
                    onAction = viewModel::onAction,
                    onClose = onClose,
                )
            }
        }
    }
}

@Composable
private fun cardScheme(swatch: StudioSwatch?, tone: CardTone, appScheme: ColorScheme): ColorScheme {
    val seed = swatch?.seed ?: return appScheme
    return rememberSeededColorScheme(seed, tone != CardTone.LIGHT, tone == CardTone.PURE_BLACK, PaletteStyle.TonalSpot)
}

@Composable
private fun StudioContent(
    state: ShareStudioUiState,
    looks: List<SwatchLook>,
    cardScheme: ColorScheme,
    onAction: (ShareStudioAction) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    var busy by remember { mutableStateOf(false) }
    val subject = state.subject

    fun capture(block: suspend (ImageBitmap) -> Unit) {
        if (busy || layer.size.width == 0) return
        scope.launch {
            busy = true
            try {
                block(layer.toShareBitmap())
            } catch (_: Throwable) {
                // The card left composition while it was being read, the viewer can tap again.
            } finally {
                busy = false
            }
        }
    }

    fun save() = capture { bitmap ->
        val saved = ShareUtils.saveCardToGallery(context, bitmap)
        if (saved) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        Toast.makeText(
            context,
            resources.getString(if (saved) R.string.share_saved_to_gallery else R.string.share_save_failed),
            Toast.LENGTH_SHORT,
        ).show()
    }

    val storageAccess = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) save() else Toast.makeText(context, R.string.share_save_needs_storage, Toast.LENGTH_SHORT).show()
    }

    val actions = StudioActions(
        busy = busy,
        onSave = {
            if (ShareUtils.saveNeedsStoragePermission(context)) {
                storageAccess.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                save()
            }
        },
        onCopy = {
            subject?.link?.let { ShareUtils.copyText(context, it) }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            Toast.makeText(context, R.string.share_copied, Toast.LENGTH_SHORT).show()
        },
        onShare = {
            capture { bitmap ->
                val text = listOfNotNull(state.spec.caption.trim().ifBlank { null }, subject?.link).joinToString("\n\n")
                ShareUtils.shareBitmap(context, bitmap, text.ifBlank { null })
            }
        },
    )

    val wide = LocalAdaptiveInfo.current.isExpandedOrWider
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                StudioStage(
                    state = state,
                    cardScheme = cardScheme,
                    layer = layer,
                    onAction = onAction,
                    wide = true,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                StudioControlsPane(state, looks, cardScheme, onAction, onClose, actions)
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                StudioHeader(state.subject?.name, onAction, onClose, closeAtStart = true)
                StudioStage(
                    state = state,
                    cardScheme = cardScheme,
                    layer = layer,
                    onAction = onAction,
                    wide = false,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                if (subject != null) {
                    PanelSelector(state.panel, onAction, Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(PhonePanelHeight)
                            .padding(top = 12.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        StudioPanelContent(state.panel, state, looks, cardScheme, onAction)
                    }
                    StudioActionRow(actions, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }
    }
}

private class StudioActions(val busy: Boolean, val onSave: () -> Unit, val onCopy: () -> Unit, val onShare: () -> Unit)

@Composable
private fun StudioHeader(
    subjectName: String?,
    onAction: (ShareStudioAction) -> Unit,
    onClose: () -> Unit,
    closeAtStart: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = if (closeAtStart) 8.dp else 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (closeAtStart) {
            CloseButton(onClose)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.share_action_share),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subjectName != null) {
                Text(
                    text = subjectName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        TextButton(onClick = { onAction(ShareStudioAction.Reset) }) {
            Text(stringResource(R.string.reset), style = MaterialTheme.typography.labelLarge)
        }
        if (!closeAtStart) {
            Spacer(Modifier.width(8.dp))
            CloseButton(onClose)
        }
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit) {
    FilledIconButton(
        onClick = onClose,
        modifier = Modifier.size(40.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_share_close), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun PanelSelector(panel: StudioPanel, onAction: (ShareStudioAction) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    SegmentedTabGroup(
        options = StudioPanel.entries,
        selected = panel,
        onSelect = { onAction(ShareStudioAction.SelectPanel(it)) },
        label = { option ->
            stringResource(
                when (option) {
                    StudioPanel.LAYOUT -> R.string.share_panel_layout
                    StudioPanel.COLOR -> R.string.share_panel_color
                    StudioPanel.BACKGROUND -> R.string.share_panel_background
                    StudioPanel.DETAILS -> R.string.share_panel_details
                }
            )
        },
        modifier = modifier,
        fillEqually = true,
        colors = ToggleButtonDefaults.colors(
            containerColor = scheme.surfaceContainerHigh,
            contentColor = scheme.onSurface,
        ),
        labelWeight = FontWeight.Medium,
        contentPadding = PaddingValues(horizontal = 6.dp),
    )
}

/** The card in the middle, its neighbours peeking either side, dots and the swipe hint below. */
@Composable
private fun StudioStage(
    state: ShareStudioUiState,
    cardScheme: ColorScheme,
    layer: GraphicsLayer,
    onAction: (ShareStudioAction) -> Unit,
    wide: Boolean,
    modifier: Modifier = Modifier,
) {
    val subject = state.subject
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        when {
            subject != null -> StagePager(subject, state, cardScheme, layer, onAction, wide)
            state.failed -> EmptyState(
                icon = Icons.Outlined.ErrorOutline,
                title = stringResource(R.string.empty_generic_title),
                description = stringResource(R.string.error_failed_to_load),
            )
            else -> AppCircularProgressIndicator()
        }
    }
}

@Composable
private fun StagePager(
    subject: ShareSubject,
    state: ShareStudioUiState,
    cardScheme: ColorScheme,
    layer: GraphicsLayer,
    onAction: (ShareStudioAction) -> Unit,
    wide: Boolean,
) {
    val templates = state.templates
    val pager = rememberPagerState(initialPage = state.templateIndex) { templates.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager.settledPage) {
        if (pager.settledPage != state.templateIndex) onAction(ShareStudioAction.SelectTemplate(pager.settledPage))
    }
    LaunchedEffect(state.templateIndex) {
        if (pager.currentPage != state.templateIndex) pager.animateScrollToPage(state.templateIndex)
    }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .then(if (wide) Modifier.statusBarsPadding().navigationBarsPadding() else Modifier)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val target = when (event.key) {
                    Key.DirectionLeft -> pager.currentPage - 1
                    Key.DirectionRight -> pager.currentPage + 1
                    else -> return@onPreviewKeyEvent false
                }
                if (target in templates.indices) scope.launch { pager.animateScrollToPage(target) }
                true
            }
    ) {
        val hintArea = if (wide) 88.dp else 64.dp
        val cardArea = maxHeight - hintArea - if (wide) 56.dp else 18.dp
        // Pages are as wide as the card shows, so the neighbours' edges peek in from the sides.
        val cardHeight = state.spec.size.fixedHeight ?: ChatPreviewHeight
        val shown = minOf(CardPreviewWidth, CardPreviewWidth * (cardArea / cardHeight), maxWidth - 64.dp)
        val peek = (maxWidth - shown) / 2
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(if (wide) 56.dp else 18.dp))
            HorizontalPager(
                state = pager,
                contentPadding = PaddingValues(horizontal = peek.coerceAtLeast(16.dp)),
                pageSpacing = if (wide) 60.dp else 56.dp,
                modifier = Modifier.fillMaxWidth().height(cardArea),
            ) { page ->
                val template = templates[page]
                val description = stringResource(R.string.a11y_share_card_page, stringResource(template.label), page + 1, templates.size)
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = if (page == pager.currentPage) 1f else NeighbourAlpha }
                        .clearAndSetSemantics { contentDescription = description },
                    contentAlignment = if (wide) Alignment.TopCenter else Alignment.Center,
                ) {
                    val shape = RoundedCornerShape(if (wide) 28.dp else 20.dp)
                    ShareCaptureArea(
                        layer = if (page == pager.currentPage) layer else null,
                        modifier = Modifier
                            .dropShadow(shape, Shadow(radius = 33.dp, color = Color.Black.copy(alpha = 0.55f), offset = DpOffset(0.dp, 14.dp)))
                            .clip(shape),
                    ) {
                        MaterialTheme(colorScheme = cardScheme) {
                            template.Render(subject, state.spec)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            PagerDots(count = templates.size, current = pager.currentPage, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(if (wide) R.string.share_swipe_hint_keys else R.string.share_swipe_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(if (wide) 46.dp else 14.dp))
        }
    }
}

private val CardPreviewWidth = 360.dp
private val ChatPreviewHeight = 420.dp
private const val NeighbourAlpha = 0.45f

@Composable
private fun PagerDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    if (count < 2) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(if (index == current) 22.dp else 8.dp, 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (index == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}

@Composable
private fun StudioControlsPane(
    state: ShareStudioUiState,
    looks: List<SwatchLook>,
    cardScheme: ColorScheme,
    onAction: (ShareStudioAction) -> Unit,
    onClose: () -> Unit,
    actions: StudioActions,
) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        Modifier
            .width(ControlsPaneWidth)
            .fillMaxHeight()
            .background(surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            StudioHeader(state.subject?.name, onAction, onClose, closeAtStart = false)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                if (state.subject != null) {
                    StudioPanel.entries.forEach { panel -> StudioPanelContent(panel, state, looks, cardScheme, onAction) }
                }
            }
        }
        if (state.subject != null) {
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(40.dp).background(Brush.verticalGradient(listOf(surface.copy(alpha = 0f), surface))))
                StudioActionRow(
                    actions,
                    Modifier
                        .background(surface)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
}

private val ControlsPaneWidth = 544.dp

@Composable
private fun StudioActionRow(actions: StudioActions, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StudioAction(Icons.Filled.Download, stringResource(R.string.share_action_save), actions.busy, primary = false, onClick = actions.onSave)
        StudioAction(Icons.Filled.ContentCopy, stringResource(R.string.share_action_copy_link), busy = false, primary = false, onClick = actions.onCopy)
        StudioAction(Icons.Filled.Share, stringResource(R.string.share_action_share), actions.busy, primary = true, onClick = actions.onShare)
    }
}

@Composable
private fun RowScope.StudioAction(icon: ImageVector, label: String, busy: Boolean, primary: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        enabled = !busy,
        modifier = Modifier.weight(1f).height(56.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) scheme.primary else scheme.surfaceContainerHigh,
            contentColor = if (primary) scheme.onPrimary else scheme.onSurface,
        ),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

