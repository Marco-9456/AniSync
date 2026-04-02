package com.anisync.android.presentation.details.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.domain.CharacterDetails
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.TransitionKeys

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CharacterHeaderSection(
    character: CharacterDetails,
    onPosterClick: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val posterShape = RoundedCornerShape(12.dp)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        // Banner Background (Using character image cropped/zoomed as banner)
        // Decorative image - contentDescription null as it duplicates the poster
        AsyncImage(
            model = character.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        // Gradient Scrim (Top)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Gradient Scrim (Bottom blend)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
                .offset(y = 120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Hard cut gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .offset(y = 140.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // Cover Image (Poster) with shared element transition
        val cardModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            val spatialSpec = AppMotion.rememberSpatialSpec()
            with(sharedTransitionScope) {
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp)
                    .width(130.dp)
                    .height(190.dp)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = TransitionKeys.characterImage(character.id)),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spatialSpec },
                        clipInOverlayDuringTransition = OverlayClip(posterShape)
                    )
            }
        } else {
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp)
                .width(130.dp)
                .height(190.dp)
        }
        
        Card(
            modifier = cardModifier,
            shape = posterShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(character.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.a11y_character_image, character.name),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onPosterClick)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpandableCharacterSynopsis(text: AnnotatedString) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var revealedSpoilers by rememberSaveable { mutableStateOf(setOf<Int>()) }

    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = effectsSpec,
        label = "ArrowRotation"
    )

    val spoilerBackground = MaterialTheme.colorScheme.surfaceVariant
    val spoilerRevealedColor = MaterialTheme.colorScheme.onSurfaceVariant

    val displayText = remember(text, revealedSpoilers) {
        val spoilers = text.getStringAnnotations(tag = "SPOILER", start = 0, end = text.length)
            .sortedBy { it.start }

        if (spoilers.isEmpty()) return@remember text

        buildAnnotatedString {
            var lastIndex = 0

            for (spoiler in spoilers) {
                if (spoiler.start > lastIndex) {
                    append(text.subSequence(lastIndex, spoiler.start))
                }

                val annotation = spoiler
                val spoilerIndex = annotation.item.toIntOrNull() ?: -1
                val isRevealed = spoilerIndex in revealedSpoilers
                val spoilerContent = text.substring(annotation.start, annotation.end)

                val listener = LinkInteractionListener {
                    revealedSpoilers = if (spoilerIndex in revealedSpoilers) {
                        revealedSpoilers - spoilerIndex
                    } else {
                        revealedSpoilers + spoilerIndex
                    }
                }

                addLink(
                    LinkAnnotation.Clickable(
                        tag = "SPOILER",
                        linkInteractionListener = listener
                    ),
                    start = this.length,
                    end = this.length + spoilerContent.length
                )

                withStyle(
                    SpanStyle(
                        background = spoilerBackground,
                        color = if (isRevealed) spoilerRevealedColor else spoilerBackground
                    )
                ) {
                    append(spoilerContent)
                }

                lastIndex = spoiler.end
            }

            if (lastIndex < text.length) {
                append(text.subSequence(lastIndex, text.length))
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box {
                Text(
                    text = displayText,
                    modifier = Modifier.clickable { expanded = !expanded },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    ),
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { expanded = !expanded }
            ) {
                Text(
                    text = if (expanded) "Show less" else "Read more",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = arrowRotation }
                )
            }
        }
    }
}

@Composable
fun NameSection(character: CharacterDetails) {
    Column {
        Text(
            text = character.name,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        character.nativeName?.let { native ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = native,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Favourites Badge
        character.favourites?.let { favs ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$favs Favourites",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun CharacterStatsCard(character: CharacterDetails, attributes: List<Pair<String, String>>) {
    fun findAttr(key: String): String? = attributes.find { it.first.equals(key, ignoreCase = true) }?.second

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val gender = character.gender.takeUnless { it.isNullOrEmpty() || it == "?" } ?: findAttr("Gender") ?: "?"
            StatItem(label = "Gender", value = gender, modifier = Modifier.weight(1f))
            
            VerticalDivider(Modifier.height(32.dp), color = MaterialTheme.colorScheme.outlineVariant)
            
            // Remove leading/trailing dashes from age (e.g., "17-" or "-17" -> "17") while preserving ranges like "16-17"
            val ageRaw = character.age?.trim('-')?.trim()
            val age = ageRaw.takeUnless { it.isNullOrEmpty() || it == "?" } ?: findAttr("Age") ?: "?"
            StatItem(label = "Age", value = age, modifier = Modifier.weight(1f))
            
            VerticalDivider(Modifier.height(32.dp), color = MaterialTheme.colorScheme.outlineVariant)
            
            val blood = character.bloodType.takeUnless { it.isNullOrEmpty() || it == "?" } ?: findAttr("Blood Type") ?: findAttr("Blood") ?: "?"
            StatItem(label = "Blood", value = blood, modifier = Modifier.weight(1f))
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
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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

@Composable
fun CharacterInfoSection(attributes: List<Pair<String, String>>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            attributes.forEachIndexed { index, (key, value) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.4f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.6f)
                        )
                    }
                }
                if (index < attributes.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpandableSynopsis(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    // Use spring physics from motionScheme for consistent feel
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    // Animated arrow rotation (0° collapsed → 180° expanded)
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = effectsSpec,
        label = "ArrowRotation"
    )

    // Using Surface for better elevation handling
    Surface(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_extra_large)),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
            )
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_medium))) {
            Text(
                stringResource(R.string.section_synopsis),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))

            // Text Content with crossfade effect
            Box {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_normal)))

            // Interaction hint with animated arrow
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (expanded) stringResource(R.string.synopsis_show_less) else stringResource(R.string.synopsis_read_more),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(dimensionResource(R.dimen.spacing_tiny)))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_size_tiny))
                        .graphicsLayer { rotationZ = arrowRotation }
                )
            }
        }
    }
}
