package com.anisync.android.presentation.details.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.domain.ExternalLink
import com.anisync.android.domain.ExternalLinkType
import com.anisync.android.domain.MediaDetails
import com.anisync.android.domain.Tag
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.util.bouncyCombinedClickable
import com.anisync.android.presentation.util.formatAsTitle
import com.anisync.android.presentation.util.rememberCopyToClipboard
import com.anisync.android.type.MediaType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Displays content metadata (Genres and Tags) in a unified section.
 * Organized into subsections: Genres, Tags, and Spoilers.
 */
@Composable
fun ContentMetadataSection(
    genres: List<String>,
    tags: List<Tag>,
    modifier: Modifier = Modifier,
    onGenreClick: (String) -> Unit = {},
    onTagClick: (Tag) -> Unit = {}
) {
    if (genres.isEmpty() && tags.isEmpty()) return

    val sortedTags = remember(tags) {
        tags.filter { it.rank != null }.sortedByDescending { it.rank }
    }

    val spoilerTags = remember(sortedTags) {
        sortedTags.filter { it.isMediaSpoiler || it.isGeneralSpoiler }
    }

    val regularTags = remember(sortedTags) {
        sortedTags.filter { !it.isMediaSpoiler && !it.isGeneralSpoiler }
    }

    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.label_categories),
            level = HeaderLevel.Section
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

        // 1. Genres Subsection
        if (genres.isNotEmpty()) {
            MetadataGroup(
                title = stringResource(R.string.label_genres),
                items = genres,
                keySelector = { it }
            ) { genre ->
                GenreChip(genre = genre, onClick = { onGenreClick(genre) })
            }
        }

        // 2. Regular Tags Subsection
        if (regularTags.isNotEmpty()) {
            if (genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
            }
            MetadataGroup(
                title = stringResource(R.string.label_tags),
                items = regularTags,
                keySelector = { it.name }
            ) { tag ->
                TagChip(tag = tag, isSpoiler = false, onTagClick = onTagClick)
            }
        }

        // 3. Spoiler Tags Subsection
        if (spoilerTags.isNotEmpty()) {
            if (genres.isNotEmpty() || regularTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
            }
            MetadataGroup(
                title = stringResource(R.string.label_spoilers),
                titleColor = MaterialTheme.colorScheme.error,
                items = spoilerTags,
                keySelector = { it.name }
            ) { tag ->
                TagChip(tag = tag, isSpoiler = true, onTagClick = onTagClick)
            }
        }
    }
}

@Composable
private fun <T> MetadataGroup(
    title: String,
    items: List<T>,
    keySelector: (T) -> Any,
    titleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable (T) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = titleColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_large))
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = items,
                key = keySelector
            ) { item ->
                content(item)
            }
        }
    }
}

@Composable
private fun GenreChip(
    genre: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(32.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
    ) {
        Box(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = genre,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagChip(
    tag: Tag,
    isSpoiler: Boolean,
    onTagClick: (Tag) -> Unit,
    modifier: Modifier = Modifier
) {
    // State to toggle spoiler visibility
    var isVisible by remember { mutableStateOf(!isSpoiler) }

    val colorScheme = MaterialTheme.colorScheme
    val tagColors = rememberTagColors(tag.category, colorScheme, isSpoiler)

    val shape = RoundedCornerShape(8.dp)

    // Tooltip state
    val tooltipState = rememberTooltipState()

    // Only show tooltip if:
    // 1. Tag has a description
    // 2. For spoilers: only when revealed (isVisible == true)
    val showTooltip = tag.description?.isNotBlank() == true && (!isSpoiler || isVisible)

    val tagContent = @Composable {
        Surface(
            modifier = modifier
                .height(32.dp)
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            shape = shape,
            color = tagColors.containerColor.copy(alpha = 0.12f),
            border = BorderStroke(
                1.dp,
                tagColors.borderColor.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .clip(shape)
                    // Hidden spoilers reveal on first tap; revealed tags (spoiler or
                    // not) open the tag search on the next one.
                    .clickable {
                        if (isSpoiler && !isVisible) isVisible = true else onTagClick(tag)
                    }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AnimatedContent(
                    targetState = isVisible,
                    label = "spoiler_reveal"
                ) { visible ->
                    if (visible) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = tagColors.textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isSpoiler) {
                                // Chip body now opens the tag search, so re-hiding
                                // moved onto a dedicated trailing eye target.
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = stringResource(R.string.label_spoiler),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { isVisible = false }
                                        .padding(4.dp)
                                        .size(14.dp),
                                    tint = tagColors.textColor
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = tagColors.textColor
                            )
                            Text(
                                text = stringResource(R.string.label_spoiler),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = tagColors.textColor
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTooltip) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = {
                PlainTooltip {
                    Text(text = tag.description ?: "")
                }
            },
            state = tooltipState,
            enableUserInput = true
        ) {
            tagContent()
        }
    } else {
        tagContent()
    }
}

@Composable
private fun rememberTagColors(
    category: String,
    colorScheme: androidx.compose.material3.ColorScheme,
    isSpoiler: Boolean
): TagColors {
    if (isSpoiler) {
        return TagColors(
            containerColor = colorScheme.error,
            borderColor = colorScheme.error,
            textColor = colorScheme.error
        )
    }

    return when (category.lowercase()) {
        "themes" -> TagColors(
            containerColor = colorScheme.primary,
            borderColor = colorScheme.primary,
            textColor = colorScheme.primary
        )

        "demographics" -> TagColors(
            containerColor = colorScheme.secondary,
            borderColor = colorScheme.secondary,
            textColor = colorScheme.secondary
        )

        "genre" -> TagColors(
            containerColor = colorScheme.tertiary,
            borderColor = colorScheme.tertiary,
            textColor = colorScheme.tertiary
        )

        "cast", "setting" -> TagColors(
            containerColor = colorScheme.secondaryContainer,
            borderColor = colorScheme.onSecondaryContainer,
            textColor = colorScheme.onSurface
        )

        "technical" -> TagColors(
            containerColor = colorScheme.outline,
            borderColor = colorScheme.outline,
            textColor = colorScheme.onSurfaceVariant
        )

        "content-warning", "explicit-content" -> TagColors(
            containerColor = colorScheme.error,
            borderColor = colorScheme.error,
            textColor = colorScheme.error
        )

        else -> TagColors(
            containerColor = colorScheme.surfaceVariant,
            borderColor = colorScheme.outline,
            textColor = colorScheme.onSurface
        )
    }
}

private data class TagColors(
    val containerColor: Color,
    val borderColor: Color,
    val textColor: Color
)

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

@Composable
fun ExternalLinksSection(
    externalLinks: List<ExternalLink>,
    mediaType: MediaType?,
    modifier: Modifier = Modifier
) {
    val streamingLinks = remember(externalLinks) {
        externalLinks.filter { it.type == ExternalLinkType.STREAMING }
    }

    val otherLinks = remember(externalLinks) {
        externalLinks.filter { it.type != ExternalLinkType.STREAMING }
    }

    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.section_external_links),
            level = HeaderLevel.Section
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

        if (streamingLinks.isNotEmpty()) {
            Text(
                text = stringResource(
                    if (mediaType == MediaType.MANGA) R.string.subsection_reading
                    else R.string.subsection_streaming
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_large))
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
            LazyRow(
                contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
            ) {
                items(streamingLinks, key = { it.id }) { link ->
                    ExternalLinkChip(link)
                }
            }
        }

        if (otherLinks.isNotEmpty()) {
            if (streamingLinks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
            }
            Text(
                text = stringResource(R.string.subsection_external),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_large))
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
            LazyRow(
                contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
            ) {
                items(otherLinks, key = { it.id }) { link ->
                    ExternalLinkChip(link)
                }
            }
        }
    }
}

@Composable
fun ExternalLinkChip(
    link: ExternalLink,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val copyToClipboard = rememberCopyToClipboard()
    val copyLabel = stringResource(R.string.a11y_action_copy)
    val linkClipLabel = stringResource(R.string.clip_label_external_link, link.site)
    val copiedLinkMessage = stringResource(R.string.copied_link, link.site)
    val scope = rememberCoroutineScope()

    var confirmationTrigger by remember { mutableIntStateOf(0) }

    val confirmationScale by animateFloatAsState(
        targetValue = if (confirmationTrigger > 0) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = 0.4f,
            stiffness = 800f
        ),
        label = "ConfirmationBounce"
    )

    val chipColor = remember(link.color) {
        link.color?.let { colorHex ->
            try {
                Color(android.graphics.Color.parseColor(colorHex))
            } catch (_: Exception) {
                null
            }
        }
    }

    val labelText = remember(link) {
        val info = listOfNotNull(link.language, link.notes)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        if (info.isNotEmpty()) "${link.site} ($info)" else link.site
    }

    Surface(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .graphicsLayer {
                scaleX = confirmationScale
                scaleY = confirmationScale
            }
            .bouncyCombinedClickable(
                onClick = {
                    link.url?.let { url ->
                        try {
                            uriHandler.openUri(url)
                        } catch (_: Exception) {
                            // Ignore error
                        }
                    }
                },
                onLongClick = {
                    link.url?.let { url ->
                        copyToClipboard(linkClipLabel, url, copiedLinkMessage)
                        confirmationTrigger++
                        scope.launch {
                            delay(150)
                            confirmationTrigger = 0
                        }
                    }
                },
                onLongClickLabel = copyLabel
            ),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (link.icon != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(link.icon)
                        .allowHardware(false)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = chipColor?.let { ColorFilter.tint(it) }
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = chipColor ?: MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = labelText,
                style = LocalTextStyle.current.copy(
                    fontSize = MaterialTheme.typography.labelLarge.fontSize
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenreFlow(genres: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        genres.forEach { genre ->
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

object MediaDetailsIcons {

    fun getFormatIcon(format: String?, type: MediaType?): ImageVector {
        if (type == MediaType.MANGA) {
            return Icons.Filled.Book
        }
        return when (format?.uppercase()) {
            "MOVIE" -> Icons.Filled.Movie
            "TV" -> Icons.Filled.LiveTv
            "OVA", "ONA" -> Icons.Filled.Videocam
            "SPECIAL" -> Icons.Filled.Videocam
            "MUSIC" -> Icons.Filled.PlayCircle
            else -> Icons.Filled.LiveTv
        }
    }

    fun getStatusIcon(status: String?): ImageVector {
        return when (status?.uppercase()) {
            "FINISHED", "COMPLETED" -> Icons.Filled.CheckCircle
            "RELEASING", "HIATUS" -> Icons.Filled.PlayCircle
            "NOT_YET_RELEASED" -> Icons.Filled.Schedule
            "CANCELLED" -> Icons.Filled.Close
            else -> Icons.Filled.CheckCircle
        }
    }

    fun getStatusColor(status: String?): Color {
        return when (status?.uppercase()) {
            "FINISHED", "COMPLETED" -> Color(0xFF4CAF50)
            "RELEASING" -> Color(0xFF2196F3)
            "NOT_YET_RELEASED" -> Color(0xFFFFC107)
            "CANCELLED" -> Color(0xFFF44336)
            else -> Color.Gray
        }
    }

    fun getSeasonIconResId(season: String?): Int {
        return when (season?.uppercase()) {
            "FALL" -> com.anisync.android.R.drawable.temp_preferences_eco_24px
            "SPRING" -> com.anisync.android.R.drawable.psychiatry_24px
            else -> 0
        }
    }

    fun getSeasonIcon(season: String?): ImageVector? {
        return when (season?.uppercase()) {
            "SUMMER" -> Icons.Filled.WbSunny
            "WINTER" -> Icons.Filled.AcUnit
            else -> Icons.Filled.Schedule
        }
    }

    fun useCustomSeasonIcon(season: String?): Boolean {
        return season?.uppercase() in listOf("FALL", "SPRING")
    }

    fun getSeasonColor(season: String?): Color {
        return when (season?.uppercase()) {
            "FALL" -> Color(0xFFFF9800)
            "SUMMER" -> Color(0xFFFFA000)
            "SPRING" -> Color(0xFF4CAF50)
            "WINTER" -> Color(0xFF03A9F4)
            else -> Color.Gray
        }
    }

    fun getSeasonContentDescriptionResId(season: String?): Int? {
        return when (season?.uppercase()) {
            "FALL" -> R.string.season_fall
            "SUMMER" -> R.string.season_summer
            "SPRING" -> R.string.season_spring
            "WINTER" -> R.string.season_winter
            else -> null
        }
    }

    fun getEpisodesIcon(type: MediaType?): ImageVector {
        return if (type == MediaType.MANGA) {
            Icons.AutoMirrored.Filled.MenuBook
        } else {
            Icons.Filled.FormatListNumbered
        }
    }

    fun getSourceIcon(): ImageVector {
        return Icons.Filled.Star
    }
}
