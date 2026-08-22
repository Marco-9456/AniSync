package com.anisync.android.presentation.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ContextualFlowRowOverflow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.anisync.android.ui.theme.LocalExpressiveTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Displays content metadata (Genres and Tags) in a unified section.
 *
 * Every tag carries AniList's rank, 0 to 100, meaning how much of that tag this title
 * has. The chip prints it and maps it onto four size and shade steps, so the section
 * can be skimmed without reading a single number. Collapsed holds
 * [COLLAPSED_TAG_ROWS] wrapped rows whatever the tag count and offers the rest behind
 * a "+N more" chip; expanded groups by the family in front of the hyphen in the
 * AniList category. Spoilers sit behind one shutter, built like the one
 * `RichTextRenderer` draws for a `~! !~` span.
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

    val rankedTags = remember(tags) {
        tags.filter { it.rank != null }.sortedByDescending { it.rank }
    }

    val spoilerTags = remember(rankedTags) {
        rankedTags.filter { it.isMediaSpoiler || it.isGeneralSpoiler }
    }

    val regularTags = remember(rankedTags) {
        rankedTags.filter { !it.isMediaSpoiler && !it.isGeneralSpoiler }
    }

    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.label_categories),
            level = HeaderLevel.Section
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

        if (genres.isNotEmpty()) {
            MetadataGroup(title = stringResource(R.string.label_genres)) {
                GenreCloud(genres = genres, onGenreClick = onGenreClick)
            }
        }

        if (regularTags.isNotEmpty()) {
            if (genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
            }
            TagsGroup(
                tags = regularTags,
                expanded = expanded,
                onToggleExpanded = { expanded = !expanded },
                onTagClick = onTagClick
            )
        }

        if (spoilerTags.isNotEmpty()) {
            if (genres.isNotEmpty() || regularTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
            }
            SpoilerShutter(
                tags = spoilerTags,
                expanded = expanded,
                onTagClick = onTagClick
            )
        }
    }
}

/** Rows the collapsed cloud keeps, whether the title has six tags or forty. */
private const val COLLAPSED_TAG_ROWS = 3

@Composable
private fun MetadataGroup(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.spacing_large)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            if (count != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = count.toString(),
                    style = LocalExpressiveTypography.current.numericMono.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.weight(1f))
                trailing()
            }
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreCloud(
    genres: List<String>,
    onGenreClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.spacing_large)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        genres.forEach { genre ->
            GenreChip(genre = genre, onClick = { onGenreClick(genre) })
        }
    }
}

@Composable
private fun TagsGroup(
    tags: List<Tag>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onTagClick: (Tag) -> Unit,
    modifier: Modifier = Modifier
) {
    val families = remember(tags) { groupByFamily(tags) }
    val sidePadding = dimensionResource(R.dimen.spacing_large)

    MetadataGroup(
        modifier = modifier,
        title = stringResource(R.string.label_tags),
        count = tags.size,
        trailing = { ExpandAction(expanded = expanded, onClick = onToggleExpanded) }
    ) {
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = sidePadding),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                families.forEach { (family, familyTags) ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FamilyHeader(family = family, count = familyTags.size)
                        TagCloud(
                            tags = familyTags,
                            expanded = true,
                            onTagClick = onTagClick
                        )
                    }
                }
            }
        } else {
            TagCloud(
                tags = tags,
                expanded = false,
                onTagClick = onTagClick,
                maxLines = COLLAPSED_TAG_ROWS,
                onOverflowClick = onToggleExpanded,
                modifier = Modifier.padding(horizontal = sidePadding)
            )
        }
    }
}

@Composable
private fun FamilyHeader(family: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = family,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = count.toString(),
            style = LocalExpressiveTypography.current.numericMono.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagCloud(
    tags: List<Tag>,
    expanded: Boolean,
    onTagClick: (Tag) -> Unit,
    modifier: Modifier = Modifier,
    isSpoiler: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    onOverflowClick: (() -> Unit)? = null
) {
    // ContextualFlowRow, not FlowRow: the "+N more" chip needs shownItemCount during
    // composition, and FlowRow only settles that count in the draw phase. Compose marks
    // both this and FlowRow's overflow as no longer maintained, so if either is removed
    // the count has to be measured by hand rather than swapped for the plain FlowRow.
    ContextualFlowRow(
        itemCount = tags.size,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxLines = maxLines,
        overflow = if (onOverflowClick != null) {
            ContextualFlowRowOverflow.expandIndicator {
                MoreTagsChip(
                    count = totalItemCount - shownItemCount,
                    onClick = onOverflowClick
                )
            }
        } else {
            ContextualFlowRowOverflow.Clip
        }
    ) { index ->
        TagChip(
            tag = tags[index],
            expanded = expanded,
            isSpoiler = isSpoiler,
            onTagClick = onTagClick
        )
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

/**
 * The chip the rest of the section is built from. Rank picks the height, the type step
 * and the tint strength; the hue only says whether this is a spoiler. The shape, the
 * hairline and the 12dp padding are unchanged from the chip that shipped before.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagChip(
    tag: Tag,
    expanded: Boolean,
    isSpoiler: Boolean,
    onTagClick: (Tag) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val expressive = LocalExpressiveTypography.current
    val rank = tag.rank ?: 0
    val tier = tierOf(rank)
    val step = stepFor(tier, expanded)

    val hue = if (isSpoiler) colorScheme.error else colorScheme.primary
    val labelColor = if (isSpoiler) {
        colorScheme.error.copy(alpha = step.labelAlpha)
    } else {
        when (tier) {
            TagTier.Defining, TagTier.Strong -> colorScheme.onSurface
            TagTier.Present -> colorScheme.onSurfaceVariant
            TagTier.Faint -> colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        }
    }
    val rankLabel = stringResource(R.string.a11y_tag_rank, tag.name, rank)

    val chip = @Composable {
        Surface(
            modifier = modifier
                .height(step.height)
                .semantics(mergeDescendants = true) { contentDescription = rankLabel },
            shape = RoundedCornerShape(8.dp),
            color = hue.copy(alpha = step.fillAlpha),
            border = BorderStroke(1.dp, hue.copy(alpha = step.borderAlpha))
        ) {
            Row(
                modifier = Modifier
                    .clickable { onTagClick(tag) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = step.labelSize,
                        fontWeight = step.labelWeight
                    ),
                    color = labelColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.tags_rank_percent, rank),
                    style = expressive.numericMono.copy(fontSize = step.rankSize),
                    color = labelColor.copy(alpha = labelColor.alpha * 0.7f)
                )
            }
        }
    }

    val description = tag.description
    if (!description.isNullOrBlank()) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = {
                PlainTooltip {
                    Text(text = description)
                }
            },
            state = rememberTooltipState(),
            enableUserInput = true
        ) {
            chip()
        }
    } else {
        chip()
    }
}

/** The overflow affordance the collapsed cloud ends on. Does the same as "Expand". */
@Composable
private fun MoreTagsChip(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .height(26.dp)
            .dashedRoundedBorder(color = primary.copy(alpha = 0.34f))
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = pluralStringResource(R.plurals.tags_more, count, count),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = primary
        )
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = primary
        )
    }
}

/** The trailing control on the Tags header, styled like [SectionHeader]'s own action. */
@Composable
private fun ExpandAction(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val angle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "tags_expand_chevron"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = stringResource(
                if (expanded) R.string.tags_collapse else R.string.tags_expand
            ),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = angle },
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * One shutter for the whole spoiler group, not one per chip.
 *
 * Container, tints and animation follow the `~! !~` block `RichTextRenderer` already
 * draws, so a spoiler behaves the same here as it does in a review or a description:
 * a neutral container that says what it is, dims once the decision is made, and opens
 * on a single tap. The red lives on the badge and on the chips inside, never on the
 * container, because the container is a control and not a warning.
 */
@Composable
private fun SpoilerShutter(
    tags: List<Tag>,
    expanded: Boolean,
    onTagClick: (Tag) -> Unit,
    modifier: Modifier = Modifier
) {
    var revealed by rememberSaveable { mutableStateOf(false) }
    val shutterColor = MaterialTheme.colorScheme.onSurfaceVariant
    val angle by animateFloatAsState(
        targetValue = if (revealed) 180f else 0f,
        label = "spoiler_chevron"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.spacing_large))
            .clip(RoundedCornerShape(8.dp))
            .background(shutterColor.copy(alpha = if (revealed) 0.05f else 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClickLabel = stringResource(
                        if (revealed) R.string.cd_tags_hide_spoilers
                        else R.string.cd_tags_show_spoilers
                    )
                ) { revealed = !revealed }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpoilerTag()
            Text(
                text = if (revealed) {
                    pluralStringResource(R.plurals.tags_spoiler_revealed, tags.size, tags.size)
                } else {
                    pluralStringResource(R.plurals.tags_spoiler_hidden, tags.size, tags.size)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                color = shutterColor.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = angle },
                tint = shutterColor.copy(alpha = 0.8f)
            )
        }
        AnimatedVisibility(
            visible = revealed,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            TagCloud(
                tags = tags,
                expanded = expanded,
                isSpoiler = true,
                onTagClick = onTagClick,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            )
        }
    }
}

private enum class TagTier { Defining, Strong, Present, Faint }

private fun tierOf(rank: Int): TagTier = when {
    rank >= 80 -> TagTier.Defining
    rank >= 60 -> TagTier.Strong
    rank >= 40 -> TagTier.Present
    else -> TagTier.Faint
}

/** Height, type and tint for one rank tier, in one of the section's two states. */
@Immutable
private data class TagChipStep(
    val height: Dp,
    val labelSize: TextUnit,
    val labelWeight: FontWeight,
    val rankSize: TextUnit,
    val fillAlpha: Float,
    val borderAlpha: Float,
    val labelAlpha: Float
)

private fun stepFor(tier: TagTier, expanded: Boolean): TagChipStep = when (tier) {
    TagTier.Defining -> TagChipStep(
        height = if (expanded) 36.dp else 30.dp,
        labelSize = if (expanded) 14.sp else 13.sp,
        labelWeight = FontWeight.Bold,
        rankSize = if (expanded) 13.sp else 12.sp,
        fillAlpha = 0.28f,
        borderAlpha = 0.60f,
        labelAlpha = 1f
    )

    TagTier.Strong -> TagChipStep(
        height = if (expanded) 32.dp else 28.dp,
        labelSize = if (expanded) 13.sp else 12.sp,
        labelWeight = FontWeight.SemiBold,
        rankSize = if (expanded) 12.sp else 11.sp,
        fillAlpha = 0.17f,
        borderAlpha = 0.36f,
        labelAlpha = 1f
    )

    TagTier.Present -> TagChipStep(
        height = if (expanded) 30.dp else 26.dp,
        labelSize = if (expanded) 12.sp else 11.sp,
        labelWeight = FontWeight.Medium,
        rankSize = 11.sp,
        fillAlpha = 0.10f,
        borderAlpha = 0.20f,
        labelAlpha = 0.9f
    )

    TagTier.Faint -> TagChipStep(
        height = if (expanded) 28.dp else 24.dp,
        labelSize = 11.sp,
        labelWeight = FontWeight.Medium,
        rankSize = if (expanded) 11.sp else 10.sp,
        fillAlpha = 0.05f,
        borderAlpha = 0.10f,
        labelAlpha = 0.75f
    )
}

/**
 * AniList sends categories like "Cast-Traits" and "Theme-Fantasy". Only the part in
 * front of the hyphen names a family anyone recognises, and grouping by it is what
 * turns a wall of thirteen cast tags into one labelled block.
 */
private fun tagFamily(category: String): String = category.substringBefore('-').trim()

/** Families ordered by their own top rank; [tags] is expected to be rank-sorted already. */
private fun groupByFamily(tags: List<Tag>): List<Pair<String, List<Tag>>> =
    tags.groupBy { tagFamily(it.category) }
        .toList()
        .sortedByDescending { (_, familyTags) -> familyTags.firstOrNull()?.rank ?: 0 }

private fun Modifier.dashedRoundedBorder(
    color: Color,
    cornerRadius: Dp = 8.dp,
    strokeWidth: Dp = 1.dp
) = drawBehind {
    val width = strokeWidth.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(width / 2, width / 2),
        size = Size(size.width - width, size.height - width),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(
            width = width,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(4.dp.toPx(), 3.dp.toPx())
            )
        )
    )
}

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
