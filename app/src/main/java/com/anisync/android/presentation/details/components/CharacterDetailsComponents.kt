package com.anisync.android.presentation.details.components

import com.anisync.android.ui.theme.emphasis
import com.anisync.android.domain.url

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.presentation.components.AnimatedFavoriteButton
import com.anisync.android.presentation.components.ReadMoreToggle
import com.anisync.android.presentation.components.TranslateIconButton
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.rememberCopyToClipboard
import java.text.NumberFormat
import java.util.Locale

/**
 * Full-width hero image for character/staff details.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailHeroImage(
    imageUrl: String?,
    contentDescription: String,
    id: Int,
    onImageClick: () -> Unit = {},
    backdropUrl: String? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    transitionKey: String = TransitionKeys.characterImage(id)
) {
    val bannerImageUrl = backdropUrl ?: imageUrl
    val portraitShape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
    val imageModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        val spatialSpec = AppMotion.rememberSpatialSpec()
        with(sharedTransitionScope) {
            Modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = transitionKey),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> spatialSpec },
                    clipInOverlayDuringTransition = OverlayClip(portraitShape)
                )
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(bottom = 24.dp)
    ) {
        // Background Banner
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(bannerImageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .graphicsLayer {
                    scaleX = 1.1f
                    scaleY = 1.1f
                }
                .then(if (backdropUrl == null) Modifier.blur(24.dp) else Modifier)
        )

        // Dark gradient overlay for the banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Overlapping Portrait — matches MediaCover styling: same rounded shape
        // as the source thumbnail (corner_radius_large) so sharedBounds clip
        // morph is smooth, no border.
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(start = 24.dp, top = 220.dp)
                .width(140.dp)
                .aspectRatio(5f / 7f)
                .then(imageModifier)
                .clip(portraitShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onImageClick)
        )
    }
}

/**
 * Name card with tint showing name, native name, alternative names and favorites count.
 *
 * The primary [name] gets a direct long-press-to-copy gesture (full string copy
 * since it's a single short line). Native name and alternative names are wrapped
 * in a [SelectionContainer] so users can copy a portion (or all via "Select all").
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NameCard(
    name: String,
    nativeName: String?,
    alternativeNames: List<String> = emptyList(),
    favourites: Int?,
    isFavourite: Boolean = false,
    onFavouriteClick: (() -> Unit)? = null,
    nameClipLabel: String = stringResource(R.string.clip_label_character_name),
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(32.dp)
    val copyToClipboard = rememberCopyToClipboard()
    val copyLabel = stringResource(R.string.a11y_action_copy)
    val copiedNameMessage = stringResource(R.string.copied_name)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(cardShape),
        shape = cardShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = {
                        copyToClipboard(nameClipLabel, name, copiedNameMessage)
                    },
                    onLongClickLabel = copyLabel
                )
            )
            if (nativeName != null || alternativeNames.isNotEmpty()) {
                SelectionContainer {
                    Column {
                        nativeName?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        if (alternativeNames.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.also_known_as, alternativeNames.joinToString(", ")),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            favourites?.let { favs ->
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedFavoriteButton(
                            isFavorite = isFavourite,
                            onClick = { onFavouriteClick?.invoke() },
                            iconSize = 18.dp,
                            activeColor = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = NumberFormat.getNumberInstance(Locale.getDefault()).format(favs),
                            style = MaterialTheme.typography.labelLarge.emphasis(),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Attributes card showing key-value rows with dividers.
 */
@Composable
fun AttributesCard(
    attributes: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    if (attributes.isEmpty()) return

    val cardShape = RoundedCornerShape(24.dp)
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = cardShape,
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
    ) {
        SelectionContainer {
            Column(modifier = Modifier.padding(16.dp)) {
                attributes.forEachIndexed { index, (key, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.4f)
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium.emphasis(),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.6f)
                        )
                    }
                    if (index < attributes.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun VoiceActorCard(
    name: String,
    language: String?,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copyToClipboard = rememberCopyToClipboard()
    val copyLabel = stringResource(R.string.a11y_action_copy)
    val staffClipLabel = stringResource(R.string.clip_label_staff_name)
    val copiedNameMessage = stringResource(R.string.copied_name)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = { copyToClipboard(staffClipLabel, name, copiedNameMessage) },
            onLongClickLabel = copyLabel
        )
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(140f / 200f)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium.emphasis(),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        language?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape
            ) {
                Text(
                    text = it.uppercase(),
                    style = MaterialTheme.typography.labelSmall.emphasis(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun FeaturedMediaItem(
    mediaId: Int,
    coverUrl: String?,
    cover: com.anisync.android.domain.CoverImage? = null,
    title: String,
    role: String?,
    year: Int?,
    type: String? = null,
    onClick: () -> Unit,
    transitionPrefix: String = TransitionKeys.CHARACTER,
    fillCell: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(16.dp)
    val spatialSpec = if (sharedTransitionScope != null) {
        AppMotion.rememberSpatialSpec()
    } else {
        null
    }
    val coverKey = TransitionKeys.cover(transitionPrefix, mediaId)
    val titleKey = TransitionKeys.title(transitionPrefix, mediaId)

    // Fill the grid cell at a uniform 3:4 cover ratio in a See-all grid; keep the natural
    // fixed width in the horizontal preview rail so cards scroll side by side.
    val widthModifier = if (fillCell) Modifier.fillMaxWidth() else Modifier.width(120.dp)
    val coverSizeModifier = if (fillCell) {
        Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
    } else {
        Modifier
            .height(160.dp)
            .fillMaxWidth()
    }

    val cardModifier = if (
        sharedTransitionScope != null &&
        animatedVisibilityScope != null &&
        spatialSpec != null
    ) {
        with(sharedTransitionScope) {
            modifier
                .then(widthModifier)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = coverKey),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> spatialSpec },
                    clipInOverlayDuringTransition = OverlayClip(cardShape)
                )
                .clip(cardShape)
                .clickable(onClick = onClick)
        }
    } else {
        modifier
            .then(widthModifier)
            .clip(cardShape)
            .clickable(onClick = onClick)
    }

    val titleModifier = if (
        sharedTransitionScope != null &&
        animatedVisibilityScope != null &&
        spatialSpec != null
    ) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = titleKey),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> spatialSpec },
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
            )
        }
    } else {
        Modifier
    }

    Column(
        modifier = cardModifier
    ) {
        AsyncImage(
            model = cover.url() ?: coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = coverSizeModifier
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.height(8.dp))
        val roleText = buildString {
            role?.let { append(it.replace("_", " ")) }
            year?.let {
                if (isNotEmpty()) append(" ")
                append("($it)")
            }
        }
        if (roleText.isNotEmpty()) {
            Text(
                text = roleText,
                style = MaterialTheme.typography.labelSmall.emphasis(),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.emphasis(),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = titleModifier
        )
        if (type != null) {
            Text(
                text = type,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.emphasis(),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpandableBiography(html: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val cardShape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_extra_large))
    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .animateContentSize(
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
            )
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_medium))) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "BIOGRAPHY",
                    style = MaterialTheme.typography.labelSmall.emphasis(),
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                TranslateIconButton(
                    text = html,
                    iconSize = 18.dp,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))

            Box(
                modifier = if (!expanded) Modifier
                    .height(100.dp)
                    .clip(RoundedCornerShape(0.dp))
                else Modifier
            ) {
                com.anisync.android.presentation.components.AsyncRichTextRenderer(
                    html = html,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_normal)))

            ReadMoreToggle(
                expanded = expanded,
                onToggle = { expanded = !expanded }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpandableSynopsis(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val cardShape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_extra_large))
    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .animateContentSize(
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
            )
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_medium))) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.section_synopsis),
                    style = MaterialTheme.typography.labelSmall.emphasis(),
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                TranslateIconButton(
                    text = text,
                    iconSize = 18.dp,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))

            // Rich renderer so inline AniList links in anime/manga (and character)
            // descriptions are clickable. Collapse via a clipped height box, mirroring the
            // biography composable above — the renderer already wraps its content in a
            // SelectionContainer, so wrapping again here would nest (and crash) one.
            Box(
                modifier = if (!expanded) Modifier
                    .height(110.dp)
                    .clip(RoundedCornerShape(0.dp))
                else Modifier
            ) {
                com.anisync.android.presentation.components.AsyncRichTextRenderer(
                    html = text,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_normal)))

            ReadMoreToggle(
                expanded = expanded,
                onToggle = { expanded = !expanded }
            )
        }
    }
}
