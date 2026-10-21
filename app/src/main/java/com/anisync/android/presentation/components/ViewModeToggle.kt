package com.anisync.android.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.rememberHapticFeedback

/**
 * List and grid as the two segments [MediaTypeToggle] draws, so the switch pinned at the start of
 * a search rail is the same control the Library rail opens on: icons alone on a phone, the words
 * spelled out where the window has room.
 */
@Composable
fun ViewModeToggle(
    isGrid: Boolean,
    onGridChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    val showLabels = !LocalAdaptiveInfo.current.isCompact
    val segmentModifier = if (showLabels) {
        Modifier
    } else {
        Modifier.width(ConnectedToggleDefaults.IconOnlyWidth)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ConnectedToggleDefaults.Spacing)
    ) {
        ConnectedToggleSegment(
            icon = Icons.Outlined.ViewAgenda,
            label = stringResource(R.string.library_view_list),
            selected = !isGrid,
            leading = true,
            showLabel = showLabels,
            modifier = segmentModifier,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onGridChange(false)
            }
        )
        ConnectedToggleSegment(
            icon = Icons.Outlined.GridView,
            label = stringResource(R.string.library_view_grid),
            selected = isGrid,
            leading = false,
            showLabel = showLabels,
            modifier = segmentModifier,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onGridChange(true)
            }
        )
    }
}
