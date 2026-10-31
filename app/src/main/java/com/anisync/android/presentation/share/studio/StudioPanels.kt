package com.anisync.android.presentation.share.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.data.CardSize
import com.anisync.android.presentation.components.SegmentedTabGroup
import com.anisync.android.presentation.share.cards.CardBackground
import com.anisync.android.presentation.share.cards.CardSpec
import com.anisync.android.presentation.share.cards.CardTone
import com.anisync.android.presentation.share.cards.ShareSubject
import java.util.Locale
import kotlin.math.roundToInt

/** What the Color panel needs to draw each swatch the way the card will wear it. */
class SwatchLook(val source: Color, val container: Color, val onContainer: Color)

@Composable
fun StudioPanelContent(
    panel: StudioPanel,
    state: ShareStudioUiState,
    looks: List<SwatchLook>,
    cardScheme: ColorScheme,
    onAction: (ShareStudioAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (panel) {
        StudioPanel.LAYOUT -> LayoutPanel(state, onAction, modifier)
        StudioPanel.COLOR -> ColorPanel(state, looks, onAction, modifier)
        StudioPanel.BACKGROUND -> BackgroundPanel(state, looks, cardScheme, onAction, modifier)
        StudioPanel.DETAILS -> DetailsPanel(state, onAction, modifier)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(LocalConfiguration.current.locales[0] ?: Locale.getDefault()),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LayoutPanel(state: ShareStudioUiState, onAction: (ShareStudioAction) -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(R.string.share_section_size))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardSize.entries.forEach { size ->
                SizeTile(size, selected = state.spec.size == size, onClick = { onAction(ShareStudioAction.SelectSize(size)) }, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(2.dp))
        SectionLabel(stringResource(R.string.share_section_template))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.templates.forEachIndexed { index, template ->
                FilterChip(
                    selected = index == state.templateIndex,
                    onClick = { onAction(ShareStudioAction.SelectTemplate(index)) },
                    label = { Text(stringResource(template.label)) },
                    leadingIcon = if (index == state.templateIndex) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun SizeTile(size: CardSize, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val content = if (selected) scheme.onPrimaryContainer else scheme.onSurface
    val hint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant
    val (label, sub) = when (size) {
        CardSize.CHAT -> R.string.share_template_standard to R.string.share_size_chat_hint
        CardSize.SQUARE -> R.string.share_format_square to R.string.share_size_ratio_square
        CardSize.PORTRAIT -> R.string.share_size_portrait to R.string.share_size_ratio_portrait
        CardSize.STORY -> R.string.share_format_story to R.string.share_size_ratio_story
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) scheme.primaryContainer else scheme.surfaceContainer,
        modifier = modifier.semantics { this.selected = selected },
    ) {
        Column(
            Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val (offset, frame) = when (size) {
                CardSize.CHAT -> (8.dp to 4.dp) to (20.dp to 26.dp)
                CardSize.SQUARE -> (5.dp to 4.dp) to (26.dp to 26.dp)
                CardSize.PORTRAIT -> (7.dp to 3.dp) to (22.dp to 28.dp)
                CardSize.STORY -> (9.dp to 1.dp) to (18.dp to 32.dp)
            }
            Box(Modifier.size(36.dp, 34.dp)) {
                Box(
                    Modifier
                        .offset(offset.first, offset.second)
                        .size(frame.first, frame.second)
                        .border(2.dp, if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant, RoundedCornerShape(5.dp))
                )
            }
            Text(stringResource(label), style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
            Text(stringResource(sub), style = MaterialTheme.typography.labelSmall, color = hint.copy(alpha = 0.8f), maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColorPanel(state: ShareStudioUiState, looks: List<SwatchLook>, onAction: (ShareStudioAction) -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(
            stringResource(
                if (state.subject is ShareSubject.Media || state.subject is ShareSubject.Review) {
                    R.string.share_section_palette_cover
                } else {
                    R.string.share_section_palette_picture
                }
            )
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = if (state.swatches.size >= FullSwatchRow) Arrangement.SpaceBetween else Arrangement.spacedBy(16.dp),
        ) {
            state.swatches.forEachIndexed { index, swatch ->
                val look = looks.getOrNull(index) ?: return@forEachIndexed
                SwatchItem(
                    label = stringResource(swatch.label),
                    look = look,
                    selected = index == state.swatchIndex,
                    onClick = { onAction(ShareStudioAction.SelectSwatch(index)) },
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        SectionLabel(stringResource(R.string.share_section_tone))
        SegmentedTabGroup(
            options = CardTone.entries,
            selected = state.spec.tone,
            onSelect = { onAction(ShareStudioAction.SelectTone(it)) },
            label = { tone ->
                stringResource(
                    when (tone) {
                        CardTone.DARK -> R.string.share_theme_dark
                        CardTone.LIGHT -> R.string.share_theme_light
                        CardTone.PURE_BLACK -> R.string.share_tone_pure_black
                    }
                )
            },
            fillEqually = true,
            colors = ToggleButtonDefaults.colors(
                containerColor = scheme.surfaceContainerHigh,
                contentColor = scheme.onSurface,
                checkedContainerColor = scheme.secondaryContainer,
                checkedContentColor = scheme.onSecondaryContainer,
            ),
        )
    }
}

@Composable
private fun SwatchItem(label: String, look: SwatchLook, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val description = if (selected) stringResource(R.string.a11y_share_swatch_selected, label) else label
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
            modifier = Modifier
                .size(56.dp)
                .semantics {
                    contentDescription = description
                    role = Role.RadioButton
                    this.selected = selected
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (selected) {
                    Box(Modifier.requiredSize(64.dp).border(2.dp, scheme.primary, RoundedCornerShape(20.dp)))
                }
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(look.container),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(24.dp).background(look.source))
                    if (selected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = look.onContainer, modifier = Modifier.size(20.dp))
                    } else {
                        Box(Modifier.size(20.dp).clip(CircleShape).background(look.container))
                    }
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun BackgroundPanel(
    state: ShareStudioUiState,
    looks: List<SwatchLook>,
    cardScheme: ColorScheme,
    onAction: (ShareStudioAction) -> Unit,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(R.string.share_panel_background))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardBackground.entries.forEach { background ->
                val enabled = background != CardBackground.CLEAR || state.clearAllowed
                val selected = state.spec.background == background
                Surface(
                    onClick = { onAction(ShareStudioAction.SelectBackground(background)) },
                    enabled = enabled,
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) scheme.primaryContainer else scheme.surfaceContainer,
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (enabled) 1f else DisabledAlpha)
                        .semantics { this.selected = selected },
                ) {
                    Column(
                        Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        BackgroundSwatch(background, looks, cardScheme)
                        Text(
                            text = stringResource(
                                when (background) {
                                    CardBackground.ARTWORK_BLUR -> R.string.share_background_blur
                                    CardBackground.GRADIENT -> R.string.share_background_gradient
                                    CardBackground.SOLID -> R.string.share_background_solid
                                    CardBackground.CLEAR -> R.string.share_background_clear
                                }
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        if (!state.clearAllowed) {
            Text(
                text = stringResource(R.string.share_background_clear_note),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        Column(
            Modifier.alpha(if (state.dimAllowed) 1f else DisabledAlpha),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.share_dim), style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
                Text(
                    text = stringResource(R.string.share_dim_value, (state.spec.dim * 100).roundToInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
            Slider(
                value = state.spec.dim,
                onValueChange = { onAction(ShareStudioAction.SetDim(it)) },
                enabled = state.dimAllowed,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BackgroundSwatch(background: CardBackground, looks: List<SwatchLook>, card: ColorScheme) {
    val shape = RoundedCornerShape(10.dp)
    val modifier = Modifier.size(44.dp, 32.dp)
    when (background) {
        CardBackground.ARTWORK_BLUR -> {
            val first = looks.firstOrNull()?.source?.takeIf { it.isSpecified() } ?: card.primary
            val second = looks.getOrNull(1)?.source?.takeIf { it.isSpecified() } ?: card.secondary
            Box(modifier.clip(shape).background(Brush.horizontalGradient(listOf(first, second))))
        }
        CardBackground.GRADIENT -> Box(modifier.clip(shape).background(Brush.verticalGradient(listOf(card.primaryContainer, card.surface))))
        CardBackground.SOLID -> Box(modifier.clip(shape).background(card.surfaceContainerHigh))
        CardBackground.CLEAR -> {
            val stroke = MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier.drawBehind {
                    val width = 1.5.dp.toPx()
                    drawRoundRect(
                        color = stroke,
                        cornerRadius = CornerRadius(10.dp.toPx()),
                        style = Stroke(width = width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                    )
                }
            )
        }
    }
}

private fun Color.isSpecified(): Boolean = this != Color.Unspecified

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsPanel(state: ShareStudioUiState, onAction: (ShareStudioAction) -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val subject = state.subject ?: return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(R.string.share_section_details))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            subject.details.forEach { detail ->
                val selected = state.spec.shows(detail)
                FilterChip(
                    selected = selected,
                    onClick = { onAction(ShareStudioAction.ToggleDetail(detail)) },
                    label = { Text(stringResource(detail.label)) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                )
            }
        }
        TextField(
            value = state.spec.caption,
            onValueChange = { onAction(ShareStudioAction.SetCaption(it)) },
            label = { Text(stringResource(R.string.share_caption)) },
            placeholder = { Text(stringResource(R.string.share_caption_hint)) },
            singleLine = true,
            trailingIcon = {
                Text(
                    text = stringResource(R.string.share_caption_counter, state.spec.caption.length, CardSpec.CAPTION_LIMIT),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            },
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = scheme.surfaceContainerHigh,
                unfocusedContainerColor = scheme.surfaceContainerHigh,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val FullSwatchRow = 6
private const val DisabledAlpha = 0.38f

/** The panel height on a phone, so switching panels never moves the card. */
val PhonePanelHeight: Dp = 218.dp
