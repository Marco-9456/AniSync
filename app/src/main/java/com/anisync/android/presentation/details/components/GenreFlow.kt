package com.anisync.android.presentation.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenreFlow(genres: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        genres.forEach { genre ->
            // Non-clickable Surface - no misleading interaction affordances
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = CircleShape
            ) {
                Text(
                    text = genre,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_normal), vertical = 6.dp)
                )
            }
        }
    }
}
