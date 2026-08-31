package com.anisync.android.presentation.discover.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.DiscoverSection
import com.anisync.android.presentation.components.AppModalBottomSheet
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Drag Discover's rails into the order you want, and switch off the ones you never read.
 *
 * The order is per media type, not per screen. The tabs do not carry the same set (the airing
 * timeline exists only for anime, Releasing now only for manga), so one shared order could not
 * describe both, and the two audiences rarely want the same thing first anyway.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderSectionsSheet(
    visible: Boolean,
    sections: List<DiscoverSection>,
    hiddenSections: Set<DiscoverSection>,
    onDismiss: () -> Unit,
    onReorder: (List<DiscoverSection>) -> Unit,
    onVisibilityChanged: (DiscoverSection, Boolean) -> Unit,
    onReset: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (!visible) return

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        val haptics = LocalHapticFeedback.current
        val localOrder = remember { sections.toMutableStateList() }
        LaunchedEffect(sections) {
            if (localOrder.toList() != sections) {
                localOrder.clear()
                localOrder.addAll(sections)
            }
        }

        val lazyListState = rememberLazyListState()
        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
            localOrder.apply {
                add(to.index - HEADER_ITEM_COUNT, removeAt(from.index - HEADER_ITEM_COUNT))
            }
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = lazyListState,
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp)
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.discover_reorder_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onReset) {
                        Text(
                            text = stringResource(R.string.discover_reorder_reset),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.discover_reorder_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
            }

            items(localOrder, key = { it.id }) { section ->
                ReorderableItem(reorderableState, key = section.id) { isDragging ->
                    val isHidden = section in hiddenSections
                    val elevation by animateDpAsState(
                        if (isDragging) 6.dp else 0.dp,
                        label = "discover_section_drag"
                    )
                    Surface(
                        shadowElevation = elevation,
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh
                        else MaterialTheme.colorScheme.background,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = stringResource(R.string.cd_reorder),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .draggableHandle(
                                        onDragStarted = {
                                            haptics.performHapticFeedback(
                                                HapticFeedbackType.GestureThresholdActivate
                                            )
                                        },
                                        onDragStopped = {
                                            haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                            onReorder(localOrder.toList())
                                        }
                                    )
                            )

                            Spacer(Modifier.width(14.dp))

                            Text(
                                text = stringResource(section.titleRes()),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                imageVector = if (isHidden) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = stringResource(
                                    if (isHidden) R.string.discover_section_show
                                    else R.string.discover_section_hide
                                ),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable(
                                        role = Role.Switch,
                                        onClick = { onVisibilityChanged(section, isHidden) }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Header block before the draggable rows, which the drag indices have to account for. */
private const val HEADER_ITEM_COUNT = 1

fun DiscoverSection.titleRes(): Int = when (this) {
    DiscoverSection.TRENDING -> R.string.discover_section_trending
    DiscoverSection.AIRING_TODAY -> R.string.discover_section_airing_today
    DiscoverSection.RELEASING_NOW -> R.string.discover_section_releasing_now
    DiscoverSection.POPULAR -> R.string.discover_section_popular
    DiscoverSection.NOT_YET_RELEASED -> R.string.discover_section_not_yet_released
    DiscoverSection.NEWLY_ADDED -> R.string.discover_section_newly_added
    DiscoverSection.REVIEWS -> R.string.discover_section_reviews
}
