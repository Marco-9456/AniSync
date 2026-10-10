package com.anisync.android.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.settings.SettingsChoiceTile
import com.anisync.android.ui.theme.AppPaletteStyles
import com.anisync.android.ui.theme.toDescription
import com.anisync.android.ui.theme.toDisplayName
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme

/**
 * Every palette style drawn as a small piece of UI in the colors it would produce from [seedColor],
 * so a style can be judged by eye instead of by its name. Listed in [AppPaletteStyles] order, which is
 * also the order the preference stores them by.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteStyleSheet(
    seedColor: Color,
    isDarkMode: Boolean,
    selectedStyle: PaletteStyle,
    onStyleSelected: (PaletteStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.setting_color_style),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.setting_color_style_sheet_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(20.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(AppPaletteStyles, key = { it.toDisplayName() }) { style ->
                    PaletteStyleCard(
                        style = style,
                        seedColor = seedColor,
                        isDarkMode = isDarkMode,
                        selected = style == selectedStyle,
                        onClick = { onStyleSelected(style) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteStyleCard(
    style: PaletteStyle,
    seedColor: Color,
    isDarkMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = remember(style, seedColor, isDarkMode) {
        dynamicColorScheme(
            seedColor = seedColor,
            isDark = isDarkMode,
            isAmoled = false,
            style = style,
            specVersion = ColorSpec.SpecVersion.SPEC_2026
        )
    }
    SettingsChoiceTile(selected = selected, onClick = onClick) {
        Column(
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MiniUi(scheme)
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = style.toDisplayName(),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    text = style.toDescription(),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** An avatar, two lines of text and a FAB, each in the role it would take in the real app. */
@Composable
private fun MiniUi(scheme: ColorScheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(scheme.surfaceContainer, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(28.dp).background(scheme.primary, CircleShape))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.fillMaxWidth(0.85f).height(8.dp).background(scheme.secondary, CircleShape))
            Box(Modifier.fillMaxWidth(0.5f).height(8.dp).background(scheme.tertiary, CircleShape))
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(28.dp).background(scheme.primaryContainer, RoundedCornerShape(8.dp)))
    }
}
