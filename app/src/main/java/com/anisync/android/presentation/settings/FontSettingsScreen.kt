package com.anisync.android.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.data.AppSettings
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import com.anisync.android.ui.theme.FontAxes
import com.anisync.android.ui.theme.TypeCategory
import com.anisync.android.ui.theme.TypographyAxisConfig
import com.anisync.android.ui.theme.TypographyOverrides
import kotlin.math.roundToInt

/**
 * Developer "font playground" — per-category live control of the five Google Sans Flex
 * variable-font axes (weight, width, optical size, slant, rounded).
 *
 * A category chip row (All / Display / Headline / Title / Body / Label) picks the target; the
 * sliders below edit that category's axes app-wide in real time. "All" writes one axis value
 * into every category at once. The preview strip is pinned in the top bar's `belowBar` slot so
 * it tracks the collapsing bar and never slides underneath it. Reset clears every override.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FontSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val overrides = uiState.fontPlayground.overrides
    val lazyListState = rememberLazyListState()
    // null == the "All" shortcut.
    var selectedCategory by rememberSaveable { mutableStateOf<TypeCategory?>(null) }

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
        belowBar = { PreviewStrip(category = selectedCategory) },
    ) { topContentPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = topContentPadding + 12.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CategorySelector(
                    selected = selectedCategory,
                    onSelect = { selectedCategory = it },
                )
            }
            item {
                AxisSliderCard(
                    overrides = overrides,
                    category = selectedCategory,
                    onAction = viewModel::onAction,
                )
            }
        }
    }
}

/**
 * Preview strip pinned under the app bar — a single sample styled with the selected category's
 * role, so it reflects every axis edit live. Reads `MaterialTheme.typography`, which `AppTheme`
 * rebuilds whenever an override changes.
 */
@Composable
private fun PreviewStrip(category: TypeCategory?) {
    val typography = MaterialTheme.typography
    val (sample, style) = when (category) {
        null -> stringResource(R.string.font_playground_title) to typography.displaySmall
        TypeCategory.DISPLAY -> stringResource(R.string.font_category_display) to typography.displayMedium
        TypeCategory.HEADLINE -> stringResource(R.string.font_category_headline) to typography.headlineMedium
        TypeCategory.TITLE -> stringResource(R.string.font_category_title) to typography.titleLarge
        TypeCategory.BODY -> stringResource(R.string.font_category_body) to typography.bodyLarge
        TypeCategory.LABEL -> stringResource(R.string.font_category_label) to typography.labelLarge
    }
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Text(
            text = sample,
            style = style,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySelector(
    selected: TypeCategory?,
    onSelect: (TypeCategory?) -> Unit,
) {
    // null entry = "All".
    val entries: List<Pair<TypeCategory?, String>> = listOf(
        null to stringResource(R.string.font_category_all),
        TypeCategory.DISPLAY to stringResource(R.string.font_category_display),
        TypeCategory.HEADLINE to stringResource(R.string.font_category_headline),
        TypeCategory.TITLE to stringResource(R.string.font_category_title),
        TypeCategory.BODY to stringResource(R.string.font_category_body),
        TypeCategory.LABEL to stringResource(R.string.font_category_label),
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        entries.forEach { (category, label) ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun AxisSliderCard(
    overrides: TypographyOverrides,
    category: TypeCategory?,
    onAction: (SettingsAction) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            FontPlaygroundAxis.entries.forEach { axis ->
                val value = axisValue(overrides, category, axis)
                FontAxisSlider(
                    label = stringResource(axis.labelRes),
                    value = value,
                    valueRange = axis.range,
                    valueText = axis.format(value),
                    onValueChange = { v ->
                        if (category == null) {
                            onAction(SettingsAction.SetFontAxisAll(axis, v))
                        } else {
                            onAction(SettingsAction.SetFontAxis(category, axis, v))
                        }
                    },
                )
            }
        }
    }
}

/**
 * One labelled axis row: the axis name on the left, a tabular value chip on the right, and an
 * expressive [Slider] underneath.
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

// --- axis metadata + value resolution -------------------------------------------------------

private val FontPlaygroundAxis.labelRes: Int
    get() = when (this) {
        FontPlaygroundAxis.WEIGHT -> R.string.font_axis_weight
        FontPlaygroundAxis.WIDTH -> R.string.font_axis_width
        FontPlaygroundAxis.OPTICAL_SIZE -> R.string.font_axis_optical_size
        FontPlaygroundAxis.SLANT -> R.string.font_axis_slant
        FontPlaygroundAxis.ROUNDNESS -> R.string.font_axis_rounded
    }

private val FontPlaygroundAxis.range: ClosedFloatingPointRange<Float>
    get() = when (this) {
        FontPlaygroundAxis.WEIGHT -> AppSettings.MIN_FONT_WEIGHT..AppSettings.MAX_FONT_WEIGHT
        FontPlaygroundAxis.WIDTH -> AppSettings.MIN_FONT_WIDTH..AppSettings.MAX_FONT_WIDTH
        FontPlaygroundAxis.OPTICAL_SIZE -> AppSettings.MIN_FONT_OPSZ..AppSettings.MAX_FONT_OPSZ
        FontPlaygroundAxis.SLANT -> AppSettings.MIN_FONT_SLANT..AppSettings.MAX_FONT_SLANT
        FontPlaygroundAxis.ROUNDNESS -> AppSettings.MIN_FONT_ROUNDNESS..AppSettings.MAX_FONT_ROUNDNESS
    }

private val FontPlaygroundAxis.default: Float
    get() = when (this) {
        FontPlaygroundAxis.WEIGHT -> AppSettings.DEFAULT_FONT_WEIGHT
        FontPlaygroundAxis.WIDTH -> AppSettings.DEFAULT_FONT_WIDTH
        FontPlaygroundAxis.OPTICAL_SIZE -> AppSettings.DEFAULT_FONT_OPSZ
        FontPlaygroundAxis.SLANT -> AppSettings.DEFAULT_FONT_SLANT
        FontPlaygroundAxis.ROUNDNESS -> AppSettings.DEFAULT_FONT_ROUNDNESS
    }

/** Slant shows one decimal; the other axes are whole numbers. */
private fun FontPlaygroundAxis.format(value: Float): String =
    if (this == FontPlaygroundAxis.SLANT) {
        ((value * 10f).roundToInt() / 10f).toString()
    } else {
        value.roundToInt().toString()
    }

private fun categoryPreset(category: TypeCategory): FontAxes = when (category) {
    TypeCategory.DISPLAY -> TypographyAxisConfig.display
    TypeCategory.HEADLINE -> TypographyAxisConfig.headline
    TypeCategory.TITLE -> TypographyAxisConfig.title
    TypeCategory.BODY -> TypographyAxisConfig.body
    TypeCategory.LABEL -> TypographyAxisConfig.label
}

/**
 * The value a slider should show: the category's override if set, else its `TypographyAxisConfig`
 * preset, else the axis default. For the "All" shortcut ([category] == null) there is no single
 * source of truth, so the neutral axis default is shown until the user drags.
 */
private fun axisValue(
    overrides: TypographyOverrides,
    category: TypeCategory?,
    axis: FontPlaygroundAxis,
): Float {
    if (category == null) return axis.default
    val override = overrides.forCategory(category)
    val preset = categoryPreset(category)
    return when (axis) {
        FontPlaygroundAxis.WEIGHT -> override.weight ?: preset.weight
        FontPlaygroundAxis.WIDTH -> override.width ?: preset.width ?: axis.default
        FontPlaygroundAxis.OPTICAL_SIZE -> override.opticalSize ?: preset.opticalSize ?: axis.default
        FontPlaygroundAxis.SLANT -> override.slant ?: preset.slant ?: axis.default
        FontPlaygroundAxis.ROUNDNESS -> override.roundness ?: preset.roundness ?: axis.default
    }
}
