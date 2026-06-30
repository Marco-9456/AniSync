package com.anisync.android.presentation.details.components

import com.anisync.android.ui.theme.emphasis
import com.anisync.android.domain.url

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.CharacterInfo
import com.anisync.android.domain.CharacterMedia
import com.anisync.android.domain.RecommendedMedia
import com.anisync.android.domain.RelatedMedia
import com.anisync.android.domain.StaffInfo
import com.anisync.android.domain.VoicedCharacter
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.bouncyCombinedClickable
import com.anisync.android.presentation.util.formatAsTitle
import com.anisync.android.presentation.util.rememberCopyToClipboard
import com.anisync.android.util.getName
import com.anisync.android.util.getTitle

/**
 * Portrait aspect for character/staff cards (matches the 100×140 fixed-cell dimens).
 * Used by the See-all grids so cells fill their column at a uniform ratio instead of
 * a fixed width that floats inside a wider adaptive cell (#83).
 */
private const val PERSON_PORTRAIT_ASPECT = 5f / 7f

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CharacterItem(
    character: CharacterInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillCell: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val imageShape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
    val copyToClipboard = rememberCopyToClipboard()
    val copyLabel = stringResource(R.string.a11y_action_copy)
    val nameClipLabel = stringResource(R.string.clip_label_character_name)
    val copiedNameMessage = stringResource(R.string.copied_name)

    // In a grid cell, fill the column width and hold a uniform portrait aspect; in the
    // horizontal preview rail, keep the natural fixed width so items scroll side by side.
    val widthModifier = if (fillCell) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.width(dimensionResource(R.dimen.character_item_width))
    }
    val imageSizeModifier = if (fillCell) {
        Modifier
            .fillMaxWidth()
            .aspectRatio(PERSON_PORTRAIT_ASPECT)
    } else {
        Modifier
            .height(dimensionResource(R.dimen.character_image_height))
            .fillMaxWidth()
    }

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .then(widthModifier)
            .clip(imageShape)
            .bouncyCombinedClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = stringResource(
                    R.string.a11y_action_open_details,
                    character.nameUserPreferred
                ),
                onLongClickLabel = copyLabel,
                onLongClick = {
                    copyToClipboard(nameClipLabel, character.nameUserPreferred, copiedNameMessage)
                }
            )
            .padding(bottom = dimensionResource(R.dimen.spacing_small))
    ) {
        val imageModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            val spatialSpec = AppMotion.rememberSpatialSpec()
            with(sharedTransitionScope) {
                imageSizeModifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = TransitionKeys.characterImage(
                                character.id
                            )
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spatialSpec },
                        clipInOverlayDuringTransition = OverlayClip(imageShape)
                    )
                    .clip(imageShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            }
        } else {
            imageSizeModifier
                .clip(imageShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        }

        AsyncImage(
            model = character.imageUrl,
            contentDescription = stringResource(
                R.string.a11y_character_image,
                character.nameUserPreferred
            ),
            contentScale = ContentScale.Crop,
            modifier = imageModifier
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
        Text(
            text = character.nameUserPreferred,
            style = MaterialTheme.typography.labelMedium.emphasis(),
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun StaffItem(
    staff: StaffInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillCell: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val imageShape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
    val copyToClipboard = rememberCopyToClipboard()
    val copyLabel = stringResource(R.string.a11y_action_copy)
    val nameClipLabel = stringResource(R.string.clip_label_staff_name)
    val copiedNameMessage = stringResource(R.string.copied_name)

    val roleText = staff.role.takeIf { it.isNotBlank() }
        ?: staff.primaryOccupations.firstOrNull().orEmpty()

    // In a grid cell, fill the column width and hold a uniform portrait aspect; in the
    // horizontal preview rail, keep the natural fixed width so items scroll side by side.
    val widthModifier = if (fillCell) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.width(dimensionResource(R.dimen.character_item_width))
    }
    val imageSizeModifier = if (fillCell) {
        Modifier
            .fillMaxWidth()
            .aspectRatio(PERSON_PORTRAIT_ASPECT)
    } else {
        Modifier
            .height(dimensionResource(R.dimen.character_image_height))
            .fillMaxWidth()
    }

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .then(widthModifier)
            .clip(imageShape)
            .bouncyCombinedClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = stringResource(
                    R.string.a11y_action_open_details,
                    staff.nameUserPreferred
                ),
                onLongClickLabel = copyLabel,
                onLongClick = {
                    copyToClipboard(nameClipLabel, staff.nameUserPreferred, copiedNameMessage)
                }
            )
            .padding(bottom = dimensionResource(R.dimen.spacing_small))
    ) {
        val imageModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            val spatialSpec = AppMotion.rememberSpatialSpec()
            with(sharedTransitionScope) {
                imageSizeModifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = TransitionKeys.staffImage(staff.id)
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spatialSpec },
                        clipInOverlayDuringTransition = OverlayClip(imageShape)
                    )
                    .clip(imageShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            }
        } else {
            imageSizeModifier
                .clip(imageShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        }

        AsyncImage(
            model = staff.imageUrl,
            contentDescription = stringResource(
                R.string.a11y_staff_image,
                staff.nameUserPreferred
            ),
            contentScale = ContentScale.Crop,
            modifier = imageModifier
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
        Text(
            text = staff.nameUserPreferred,
            style = MaterialTheme.typography.labelMedium.emphasis(),
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.primary
        )
        if (roleText.isNotEmpty()) {
            Text(
                text = roleText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MediaRoleItem(
    media: CharacterMedia,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = media.cover.url() ?: media.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            media.voiceActors.firstOrNull()?.let { va ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    AsyncImage(
                        model = va.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }

        Text(
            text = media.titleUserPreferred,
            style = MaterialTheme.typography.labelMedium.emphasis(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )

        Text(
            text = media.type?.name ?: stringResource(R.string.unknown),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RelationItem(
    relation: RelatedMedia,
    onClick: () -> Unit,
    transitionPrefix: String = TransitionKeys.MEDIA_DETAILS,
    modifier: Modifier = Modifier,
    fillCell: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val imageShape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))

    val widthModifier = if (fillCell) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.width(dimensionResource(R.dimen.character_item_width))
    }
    val imageSizeModifier = if (fillCell) {
        Modifier
            .fillMaxWidth()
            .aspectRatio(PERSON_PORTRAIT_ASPECT)
    } else {
        Modifier
            .height(dimensionResource(R.dimen.character_image_height))
            .fillMaxWidth()
    }

    Column(
        modifier = modifier
            .then(widthModifier)
            .clip(imageShape)
            .clickable(onClick = onClick)
            .padding(bottom = dimensionResource(R.dimen.spacing_small))
    ) {
        val imageModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            val spatialSpec = AppMotion.rememberSpatialSpec()
            with(sharedTransitionScope) {
                imageSizeModifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = TransitionKeys.cover(transitionPrefix, relation.id)
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spatialSpec },
                        clipInOverlayDuringTransition = OverlayClip(imageShape)
                    )
                    .clip(imageShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            }
        } else {
            imageSizeModifier
                .clip(imageShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        }

        AsyncImage(
            model = relation.cover.url() ?: relation.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = imageModifier
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
        Text(
            text = relation.relationType.formatAsTitle() ?: relation.relationType,
            style = MaterialTheme.typography.labelSmall.emphasis(),
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

@Composable
fun RecommendationItem(
    recommendation: RecommendedMedia,
    onClick: () -> Unit,
    onRate: (isUpvote: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    fillCell: Boolean = false
) {
    val imageShape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
    val isUpvoted = recommendation.userRating == "RATE_UP"
    val isDownvoted = recommendation.userRating == "RATE_DOWN"

    val widthModifier = if (fillCell) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.width(dimensionResource(R.dimen.character_item_width))
    }
    val imageSizeModifier = if (fillCell) {
        Modifier
            .fillMaxWidth()
            .aspectRatio(PERSON_PORTRAIT_ASPECT)
    } else {
        Modifier
            .height(dimensionResource(R.dimen.character_image_height))
            .fillMaxWidth()
    }

    Column(
        modifier = modifier
            .then(widthModifier)
            .clip(imageShape)
            .bouncyClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = recommendation.titleUserPreferred
            )
            .padding(bottom = dimensionResource(R.dimen.spacing_small))
    ) {
        Box(
            modifier = imageSizeModifier
                .clip(imageShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = recommendation.cover.url() ?: recommendation.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
        Text(
            text = recommendation.format?.formatAsTitle() ?: "",
            style = MaterialTheme.typography.labelSmall.emphasis(),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = recommendation.titleUserPreferred,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onRate(true) },
                modifier = Modifier.size(28.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (isUpvoted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.ThumbUp,
                    contentDescription = stringResource(R.string.cd_like),
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = "${recommendation.rating}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = when {
                    recommendation.rating > 0 -> MaterialTheme.colorScheme.primary
                    recommendation.rating < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            IconButton(
                onClick = { onRate(false) },
                modifier = Modifier.size(28.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (isDownvoted) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.ThumbDown,
                    contentDescription = stringResource(R.string.cd_dislike),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VoicedCharacterItem(
    voicedCharacter: VoicedCharacter,
    titleLanguage: com.anisync.android.data.TitleLanguage = com.anisync.android.data.TitleLanguage.ROMAJI,
    onCharacterClick: () -> Unit,
    onMediaClick: (Int) -> Unit,
    transitionPrefix: String = TransitionKeys.STAFF,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(8.dp)
    val copyToClipboard = rememberCopyToClipboard()
    val copyLabel = stringResource(R.string.a11y_action_copy)
    val nameClipLabel = stringResource(R.string.clip_label_character_name)
    val copiedNameMessage = stringResource(R.string.copied_name)
    val voicedName = voicedCharacter.getName(titleLanguage)
    val titleSpatialSpec = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        AppMotion.rememberSpatialSpec()
    } else {
        null
    }
    val characterImageModifier = if (
        sharedTransitionScope != null &&
        animatedVisibilityScope != null
    ) {
        val characterSpatialSpec = AppMotion.rememberSpatialSpec()
        with(sharedTransitionScope) {
            Modifier
                .width(56.dp)
                .wrapContentHeight()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = TransitionKeys.characterImage(voicedCharacter.characterId)
                    ),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> characterSpatialSpec },
                    clipInOverlayDuringTransition = OverlayClip(cardShape)
                )
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        }
    } else {
        Modifier
            .width(56.dp)
            .wrapContentHeight()
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Character header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = onCharacterClick,
                        onLongClick = {
                            copyToClipboard(nameClipLabel, voicedName, copiedNameMessage)
                        },
                        onLongClickLabel = copyLabel
                    )
                    .padding(4.dp)
            ) {
                AsyncImage(
                    model = voicedCharacter.characterImageUrl,
                    contentDescription = voicedName,
                    contentScale = ContentScale.FillWidth,
                    modifier = characterImageModifier
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = voicedName,
                    style = MaterialTheme.typography.titleMedium.emphasis(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (voicedCharacter.mediaAppearances.isNotEmpty()) {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse All" else "Expand All",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    if (voicedCharacter.mediaAppearances.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                alpha = 0.5f
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Media appearances
                    voicedCharacter.mediaAppearances.forEach { appearance ->
                        val rowModifier = if (
                            sharedTransitionScope != null &&
                            animatedVisibilityScope != null &&
                            titleSpatialSpec != null
                        ) {
                            with(sharedTransitionScope) {
                                Modifier.sharedBounds(
                                    sharedContentState = rememberSharedContentState(
                                        key = TransitionKeys.title(transitionPrefix, appearance.mediaId)
                                    ),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    boundsTransform = { _, _ -> titleSpatialSpec },
                                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                                )
                            }
                        } else {
                            Modifier
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .clickable { onMediaClick(appearance.mediaId) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            appearance.startYear?.let { year ->
                                Text(
                                    text = year.toString(),
                                    style = MaterialTheme.typography.labelLarge.emphasis(),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(48.dp)
                                )
                            }
                            Text(
                                text = appearance.getTitle(titleLanguage),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = rowModifier
                            )
                        }
                    }
                }
            }
        }
    }
}
