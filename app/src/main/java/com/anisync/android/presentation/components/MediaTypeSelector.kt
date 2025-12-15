package com.anisync.android.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.anisync.android.R
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.type.MediaType

/**
 * A shared Anime/Manga toggle selector component.
 *
 * Follows state hoisting pattern: events go up (onSelect), state comes down (selected).
 * Uses Material 3 ToggleButton with connected button shapes and includes
 * scale animation and haptic feedback for enhanced UX.
 *
 * @param selected The currently selected MediaType
 * @param onSelect Callback when a media type is selected
 * @param modifier Modifier for the component (first optional param per Compose guidelines)
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediaTypeSelector(
    selected: MediaType,
    onSelect: (MediaType) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        ToggleButton(
            checked = selected == MediaType.ANIME,
            onCheckedChange = {
                haptic.click()
                onSelect(MediaType.ANIME)
            },
            modifier = Modifier.weight(1f),
            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
        ) {
            val scale by animateFloatAsState(
                targetValue = if (selected == MediaType.ANIME) 1.1f else 1f,
                animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
                label = "AnimeScale"
            )
            Text(
                text = stringResource(R.string.media_type_anime),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.scale(scale)
            )
        }
        ToggleButton(
            checked = selected == MediaType.MANGA,
            onCheckedChange = {
                haptic.click()
                onSelect(MediaType.MANGA)
            },
            modifier = Modifier.weight(1f),
            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
        ) {
            val scale by animateFloatAsState(
                targetValue = if (selected == MediaType.MANGA) 1.1f else 1f,
                animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
                label = "MangaScale"
            )
            Text(
                text = stringResource(R.string.media_type_manga),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.scale(scale)
            )
        }
    }
}
