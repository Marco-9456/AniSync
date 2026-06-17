package com.anisync.android.presentation.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One discrete card/section of a dashboard. [key] is used as the lazy-list item key on compact
 * (single-column) so scroll position and item animations stay stable; [content] is the section
 * composable, which is expected to manage its own horizontal insets (the existing statistics and
 * profile sections all self-pad to 16.dp).
 */
class DashboardSection(
    val key: String,
    val content: @Composable () -> Unit,
)

/**
 * How many columns a section dashboard should flow into at the current width. Single column on a
 * phone (keeps the lazy, full-bleed stack), two on medium/expanded, three on very wide windows.
 * Charts stay readable because each column is still >= ~360dp at these breakpoints.
 */
@Composable
fun dashboardColumns(): Int = when (LocalAdaptiveInfo.current.widthClass) {
    WidthSizeClass.COMPACT -> 1
    WidthSizeClass.MEDIUM -> 2
    WidthSizeClass.EXPANDED -> 2
    WidthSizeClass.LARGE -> 3
}

/**
 * Lays [sections] out as a multi-column dashboard: sections are distributed round-robin across
 * [columns] equal-weight columns, each an independently-packed vertical stack. Round-robin keeps
 * column heights roughly balanced without measuring (true masonry) and preserves a stable mapping.
 *
 * Sections keep their own internal 16.dp horizontal padding, which becomes the inter-column gutter
 * (32.dp) and outer margin (16.dp) — no extra arrangement spacing is added. Collapses to a plain
 * vertical stack when [columns] <= 1.
 */
@Composable
fun StatsDashboardGrid(
    sections: List<DashboardSection>,
    columns: Int,
    modifier: Modifier = Modifier,
    verticalSpacing: Dp = 24.dp,
) {
    if (columns <= 1) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        ) {
            sections.forEach { it.content() }
        }
        return
    }
    Row(modifier = modifier.fillMaxWidth()) {
        for (col in 0 until columns) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            ) {
                sections.forEachIndexed { index, section ->
                    if (index % columns == col) section.content()
                }
            }
        }
    }
}
