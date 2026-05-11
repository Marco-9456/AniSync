package com.anisync.android.presentation.discover.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaType

/**
 * Horizontal "All / Format1 / Format2 / …" chip row used by Section grids.
 * The full advanced-search UI lives in `AdvancedSearchScreen`.
 */
@Composable
fun SearchFiltersRow(
    mediaType: MediaType,
    selectedFormat: MediaFormat?,
    onFormatSelected: (MediaFormat?) -> Unit,
    modifier: Modifier = Modifier
) {
    val formats = when (mediaType) {
        MediaType.ANIME -> listOf(
            MediaFormat.TV,
            MediaFormat.MOVIE,
            MediaFormat.OVA,
            MediaFormat.SPECIAL,
            MediaFormat.ONA
        )
        MediaType.MANGA -> listOf(
            MediaFormat.MANGA,
            MediaFormat.NOVEL,
            MediaFormat.ONE_SHOT
        )
        else -> emptyList()
    }

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFormat == null,
            onClick = { onFormatSelected(null) },
            label = { Text(stringResource(R.string.all)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        formats.forEach { format ->
            FilterChip(
                selected = selectedFormat == format,
                onClick = { onFormatSelected(format) },
                label = { Text(format.toDisplayString()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

private fun MediaFormat.toDisplayString(): String = when (this) {
    MediaFormat.TV -> "TV"
    MediaFormat.TV_SHORT -> "TV Short"
    MediaFormat.MOVIE -> "Movie"
    MediaFormat.SPECIAL -> "Special"
    MediaFormat.OVA -> "OVA"
    MediaFormat.ONA -> "ONA"
    MediaFormat.MUSIC -> "Music"
    MediaFormat.MANGA -> "Manga"
    MediaFormat.NOVEL -> "Novel"
    MediaFormat.ONE_SHOT -> "One-shot"
    else -> this.name
}
