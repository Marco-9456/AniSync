package com.anisync.android.presentation.details.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.RelatedMedia
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.formatAsTitle

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RelationItem(
    relation: RelatedMedia,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val imageShape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
    
    Column(
        modifier = modifier
            .width(dimensionResource(R.dimen.character_item_width))
            .clip(imageShape)
            .clickable(onClick = onClick)
            .padding(bottom = dimensionResource(R.dimen.spacing_small))
    ) {
        // Apply shared element transition if scopes are provided
        val imageModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            val spatialSpec = AppMotion.rememberSpatialSpec()
            with(sharedTransitionScope) {
                Modifier
                    .height(dimensionResource(R.dimen.character_image_height))
                    .fillMaxWidth()
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = TransitionKeys.relationCover(relation.id)),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spatialSpec },
                        clipInOverlayDuringTransition = OverlayClip(imageShape)
                    )
                    .clip(imageShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            }
        } else {
            Modifier
                .height(dimensionResource(R.dimen.character_image_height))
                .fillMaxWidth()
                .clip(imageShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        }
        
        AsyncImage(
            model = relation.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = imageModifier
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
        Text(
            text = relation.relationType.formatAsTitle() ?: relation.relationType,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = relation.titleUserPreferred,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
