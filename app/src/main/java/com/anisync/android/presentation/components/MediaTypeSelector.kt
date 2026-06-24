package com.anisync.android.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import com.anisync.android.type.MediaType

/**
 * Shared Anime/Manga toggle — a thin wrapper over [SegmentedTabGroup] using its
 * equal-width, two-option layout.
 *
 * Follows state hoisting: events go up (onSelect), state comes down (selected).
 *
 * @param selected The currently selected MediaType
 * @param onSelect Callback when a media type is selected
 * @param modifier Modifier for the component (first optional param per Compose guidelines)
 */
@Composable
fun MediaTypeSelector(
    selected: MediaType,
    onSelect: (MediaType) -> Unit,
    modifier: Modifier = Modifier
) {
    SegmentedTabGroup(
        options = remember { listOf(MediaType.ANIME, MediaType.MANGA) },
        selected = selected,
        onSelect = onSelect,
        label = { type ->
            stringResource(
                if (type == MediaType.ANIME) R.string.media_type_anime else R.string.media_type_manga
            )
        },
        modifier = modifier,
        icon = { type ->
            if (type == MediaType.ANIME) Icons.Default.Tv else Icons.AutoMirrored.Filled.MenuBook
        },
        fillEqually = true,
        labelStyle = MaterialTheme.typography.titleSmall
    )
}
