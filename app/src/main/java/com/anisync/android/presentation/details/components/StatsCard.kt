package com.anisync.android.presentation.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import com.anisync.android.domain.MediaDetails
import com.anisync.android.presentation.util.formatAsTitle
import com.anisync.android.type.MediaType

@Composable
fun StatsCard(details: MediaDetails) {
    val isManga = details.type == MediaType.MANGA
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_extra_large)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(R.dimen.spacing_medium)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                label = if (isManga) stringResource(R.string.stat_chapters) else stringResource(R.string.stat_episodes),
                value = if (isManga) "${details.chapters ?: "?"}" else "${details.episodes ?: "?"}"
            )
            VerticalDivider(Modifier.height(dimensionResource(R.dimen.spacing_extra_large)), color = MaterialTheme.colorScheme.outlineVariant)
            StatItem(
                label = stringResource(R.string.stat_status),
                value = details.status.formatAsTitle() ?: details.status
            )
            VerticalDivider(Modifier.height(dimensionResource(R.dimen.spacing_extra_large)), color = MaterialTheme.colorScheme.outlineVariant)
            StatItem(
                label = stringResource(R.string.stat_source),
                value = stringResource(R.string.source_original) // Replace with actual source if available in MediaDetails
            )
        }
    }
}
