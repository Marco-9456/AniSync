package com.anisync.android.presentation.details.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.CharacterInfo
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.bouncyClickable

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CharacterItem(
    character: CharacterInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val imageShape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
    
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .width(dimensionResource(R.dimen.character_item_width))
            .clip(imageShape)
            .bouncyClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = stringResource(R.string.a11y_action_open_details, character.nameUserPreferred)
            )
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
                        sharedContentState = rememberSharedContentState(key = TransitionKeys.characterImage(character.id)),
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
            model = character.imageUrl,
            contentDescription = stringResource(R.string.a11y_character_image, character.nameUserPreferred),
            contentScale = ContentScale.Crop,
            modifier = imageModifier
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
        Text(
            text = character.nameUserPreferred,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = character.role,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start
        )
    }
}
