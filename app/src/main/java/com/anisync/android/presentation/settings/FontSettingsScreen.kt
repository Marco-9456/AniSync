package com.anisync.android.presentation.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.data.AppSettings
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import kotlin.math.roundToInt

/**
 * Developer "font playground" — live sliders for the five Google Sans Flex variable-font axes
 * (weight, width, optical size, slant, rounded). A 1:1 take on the design prototype: a sticky
 * primary-coloured preview card over a scrolling card of expressive sliders.
 *
 * Moving any slider activates a global [com.anisync.android.ui.theme.FontAxisOverrides] that
 * re-renders the whole app's typography in real time; the top-bar Reset action clears it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FontSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val font = uiState.fontPlayground
    val lazyListState = rememberLazyListState()

    CollapsingTopBarScaffold(
        title = stringResource(R.string.font_playground_title),
        onBackClick = onBackClick,
        modifier = modifier,
        scrollableState = lazyListState,
        enableEnterAnimation = true,
        actions = {
            IconButton(onClick = { viewModel.onAction(SettingsAction.ResetFontAxes) }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.font_playground_reset),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    ) { topContentPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = topContentPadding,
                bottom = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding() + 24.dp,
            ),
        ) {
            // Sticky preview card — pinned under the collapsing bar while the sliders scroll.
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                ) {
                    PreviewCard()
                }
            }

            item {
                SettingsCard(
                    font = font,
                    onAction = viewModel::onAction,
                )
            }
        }
    }
}

/**
 * The primary-coloured preview card. Its three texts use [MaterialTheme.typography], so they
 * track the live axis overrides automatically. Text content is scratch — editable, not saved.
 */
@Composable
private fun PreviewCard() {
    var displayText by rememberSaveable { mutableStateOf("Expressive") }
    var headlineText by rememberSaveable { mutableStateOf("Material Design 3") }
    var bodyText by rememberSaveable {
        mutableStateOf(
            "Google Sans Flex allows deep customization through variable axes. Tap here to " +
                "edit this text and see how weight, width, optical size, slant, and " +
                "roundedness interact.",
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PreviewField(
                value = displayText,
                onValueChange = { displayText = it },
                textStyle = MaterialTheme.typography.displayLarge,
            )
            PreviewField(
                value = headlineText,
                onValueChange = { headlineText = it },
                textStyle = MaterialTheme.typography.headlineLarge,
            )
            PreviewField(
                value = bodyText,
                onValueChange = { bodyText = it },
                textStyle = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun PreviewField(
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle.copy(color = onPrimary),
        cursorBrush = SolidColor(onPrimary),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SettingsCard(
    font: FontPlaygroundUiState,
    onAction: (SettingsAction) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            FontAxisSlider(
                label = stringResource(R.string.font_axis_weight),
                value = font.weight,
                valueRange = AppSettings.MIN_FONT_WEIGHT..AppSettings.MAX_FONT_WEIGHT,
                valueText = font.weight.roundToInt().toString(),
                onValueChange = { onAction(SettingsAction.SetFontAxis(FontPlaygroundAxis.WEIGHT, it)) },
            )
            FontAxisSlider(
                label = stringResource(R.string.font_axis_width),
                value = font.width,
                valueRange = AppSettings.MIN_FONT_WIDTH..AppSettings.MAX_FONT_WIDTH,
                valueText = font.width.roundToInt().toString(),
                onValueChange = { onAction(SettingsAction.SetFontAxis(FontPlaygroundAxis.WIDTH, it)) },
            )
            FontAxisSlider(
                label = stringResource(R.string.font_axis_optical_size),
                value = font.opticalSize,
                valueRange = AppSettings.MIN_FONT_OPSZ..AppSettings.MAX_FONT_OPSZ,
                valueText = font.opticalSize.roundToInt().toString(),
                onValueChange = { onAction(SettingsAction.SetFontAxis(FontPlaygroundAxis.OPTICAL_SIZE, it)) },
            )
            FontAxisSlider(
                label = stringResource(R.string.font_axis_slant),
                value = font.slant,
                valueRange = AppSettings.MIN_FONT_SLANT..AppSettings.MAX_FONT_SLANT,
                valueText = ((font.slant * 10f).roundToInt() / 10f).toString(),
                onValueChange = { onAction(SettingsAction.SetFontAxis(FontPlaygroundAxis.SLANT, it)) },
            )
            FontAxisSlider(
                label = stringResource(R.string.font_axis_rounded),
                value = font.roundness,
                valueRange = AppSettings.MIN_FONT_ROUNDNESS..AppSettings.MAX_FONT_ROUNDNESS,
                valueText = font.roundness.roundToInt().toString(),
                onValueChange = { onAction(SettingsAction.SetFontAxis(FontPlaygroundAxis.ROUNDNESS, it)) },
            )
        }
    }
}

/**
 * One labelled axis row: the axis name on the left, a tabular value chip on the right, and an
 * expressive [Slider] underneath. Mirrors the prototype's `setting-group`.
 */
@Composable
private fun FontAxisSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.padding(horizontal = 4.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        )
    }
}
