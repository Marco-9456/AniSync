package com.anisync.android.presentation.details.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.EpisodeSpan
import com.anisync.android.domain.MediaTheme
import com.anisync.android.domain.ThemeType
import com.anisync.android.domain.countCoveredEpisodes
import com.anisync.android.domain.formatEpisodeSpans
import com.anisync.android.presentation.util.bouncyClickable

/** Past this many episodes a per-episode mark is thinner than a hairline, so the bar draws spans. */
private const val TICK_LIMIT = 50

private val TILE_WIDTH = 160.dp
private val TILE_ART_HEIGHT = 90.dp
private val ROW_ART_WIDTH = 104.dp
private val ROW_ART_HEIGHT = 59.dp

/**
 * Where a theme plays across a show's run.
 *
 * Two shapes, picked from the episode count rather than a flag, because they answer the same
 * question at different scales. Up to [TICK_LIMIT] episodes each one gets its own mark, so a
 * single skipped episode is visible. Past that the marks would be sub-pixel, so each range is
 * drawn as one proportional span instead.
 *
 * A range the API left open ("13-") fades out rather than claiming episodes that have not
 * aired. No episode data at all draws the whole bar at half strength, which is the honest
 * reading of a field AnimeThemes never filled in.
 */
@Composable
fun EpisodeCoverageBar(
    spans: List<EpisodeSpan>,
    totalEpisodes: Int?,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    coveredColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
) {
    val total = totalEpisodes ?: return
    if (total <= 0) return

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)

        if (spans.isEmpty()) {
            drawRoundRect(color = coveredColor.copy(alpha = 0.4f), cornerRadius = radius)
            return@Canvas
        }

        if (total <= TICK_LIMIT) {
            val gap = 2.dp.toPx()
            val tickWidth = ((size.width - gap * (total - 1)) / total).coerceAtLeast(1f)
            for (episode in 1..total) {
                val covered = spans.any { it.contains(episode) }
                drawRoundRect(
                    color = if (covered) coveredColor else trackColor,
                    topLeft = Offset((episode - 1) * (tickWidth + gap), 0f),
                    size = Size(tickWidth, size.height),
                    cornerRadius = radius
                )
            }
            return@Canvas
        }

        drawRoundRect(color = trackColor, cornerRadius = radius)
        val minWidth = 3.dp.toPx()
        for (span in spans) {
            val last = span.end ?: total
            val startX = ((span.start - 1).toFloat() / total) * size.width
            val rawWidth = ((last - span.start + 1).toFloat() / total) * size.width
            val spanWidth = rawWidth.coerceAtLeast(minWidth)
            val x = startX.coerceAtMost((size.width - spanWidth).coerceAtLeast(0f))

            if (span.isOpen) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to coveredColor,
                            0.55f to coveredColor,
                            1f to coveredColor.copy(alpha = 0f)
                        ),
                        startX = x,
                        endX = x + spanWidth
                    ),
                    topLeft = Offset(x, 0f),
                    size = Size(spanWidth, size.height),
                    cornerRadius = radius
                )
            } else {
                drawRoundRect(
                    color = coveredColor,
                    topLeft = Offset(x, 0f),
                    size = Size(spanWidth, size.height),
                    cornerRadius = radius
                )
            }
        }
    }
}

/**
 * The picture on a theme card.
 *
 * The show's own art, dimmed and tinted by kind. AnimeThemes serves no still for a theme, and
 * decoding one out of the video would mean pulling tens of megabytes per row, so the art is
 * the cover the page has already loaded.
 */
@Composable
private fun ThemeArtwork(
    coverUrl: String?,
    slug: String,
    type: ThemeType,
    cornerRadius: Dp,
    playGlyphSize: Dp,
    modifier: Modifier = Modifier
) {
    val tint = if (type == ThemeType.OP) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tint.copy(alpha = 0.34f))
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(playGlyphSize)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(playGlyphSize * 0.55f)
            )
        }

        Surface(
            color = Color.Black.copy(alpha = 0.78f),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
        ) {
            Text(
                text = slug,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
    }
}

/** One theme in the horizontal rail on the media page. */
@Composable
fun ThemeTile(
    theme: MediaTheme,
    coverUrl: String?,
    totalEpisodes: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spans = remember(theme) { theme.episodeSpans }
    val coverageLabel = remember(spans) { formatEpisodeSpans(spans) }
    val description = if (theme.isSpoiler || spans.isEmpty()) {
        theme.songTitle.orEmpty()
    } else {
        stringResource(R.string.themes_episodes, coverageLabel)
    }

    Column(
        modifier = modifier
            .width(TILE_WIDTH)
            .clip(RoundedCornerShape(14.dp))
            .bouncyClickable(onClick = onClick, clipShape = RoundedCornerShape(14.dp))
            .semantics { contentDescription = "${theme.slug}. $description" }
    ) {
        ThemeArtwork(
            coverUrl = coverUrl,
            slug = theme.slug,
            type = theme.type,
            cornerRadius = 14.dp,
            playGlyphSize = 30.dp,
            modifier = Modifier
                .width(TILE_WIDTH)
                .height(TILE_ART_HEIGHT)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = theme.songTitle ?: theme.slug,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (theme.artists.isNotEmpty()) {
            Text(
                text = theme.artists.joinToString(", "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(8.dp))
        if (!theme.isSpoiler) {
            EpisodeCoverageBar(
                spans = spans,
                totalEpisodes = totalEpisodes,
                height = 5.dp
            )
        }
    }
}

/** One theme in the full list, where the bars of every row share a scale. */
@Composable
fun ThemeRow(
    theme: MediaTheme,
    coverUrl: String?,
    totalEpisodes: Int?,
    isSpoilerRevealed: Boolean,
    onClick: () -> Unit,
    onRevealSpoiler: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spans = remember(theme) { theme.episodeSpans }
    val hideEpisodes = theme.isSpoiler && !isSpoilerRevealed

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .bouncyClickable(onClick = onClick, clipShape = RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ThemeArtwork(
                    coverUrl = coverUrl,
                    slug = theme.slug,
                    type = theme.type,
                    cornerRadius = 10.dp,
                    playGlyphSize = 26.dp,
                    modifier = Modifier
                        .width(ROW_ART_WIDTH)
                        .height(ROW_ART_HEIGHT)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = theme.songTitle ?: theme.slug,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (theme.artists.isNotEmpty()) {
                        Text(
                            text = theme.artists.joinToString(", "),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (hideEpisodes) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.bouncyClickable(onClick = onRevealSpoiler)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.themes_spoiler_hidden),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.themes_spoiler_reveal),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = themeCoverageLabel(theme, spans),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            EpisodeCoverageBar(
                spans = if (hideEpisodes) emptyList() else spans,
                totalEpisodes = totalEpisodes,
                height = 6.dp,
                coveredColor = if (hideEpisodes) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

/**
 * The rail on the media page, with the count in the subheading and the full list behind
 * the arrow. Renders nothing at all when AnimeThemes does not list the title.
 */
@Composable
fun MediaThemesSection(
    themes: List<MediaTheme>,
    isLoading: Boolean,
    errorMessage: String?,
    coverUrl: String?,
    totalEpisodes: Int?,
    onSeeAllClick: () -> Unit,
    onThemeClick: (MediaTheme) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (themes.isEmpty() && !isLoading && errorMessage == null) return

    val horizontal = dimensionResource(R.dimen.spacing_large)

    Column(modifier = modifier.fillMaxWidth()) {
        ThemesSectionHeader(
            themes = themes,
            onSeeAllClick = onSeeAllClick.takeIf { themes.isNotEmpty() }
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacing_normal)))

        when {
            themes.isNotEmpty() -> LazyRow(
                contentPadding = PaddingValues(horizontal = horizontal),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_normal))
            ) {
                items(themes, key = { it.id }) { theme ->
                    ThemeTile(
                        theme = theme,
                        coverUrl = coverUrl,
                        totalEpisodes = totalEpisodes,
                        onClick = { onThemeClick(theme) }
                    )
                }
            }

            isLoading -> Row(
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_normal)),
                modifier = Modifier.padding(horizontal = horizontal)
            ) {
                repeat(2) { ThemeTileSkeleton() }
            }

            else -> ThemesErrorCard(
                onRetryClick = onRetryClick,
                modifier = Modifier.padding(horizontal = horizontal)
            )
        }
    }
}

@Composable
private fun ThemesSectionHeader(
    themes: List<MediaTheme>,
    onSeeAllClick: (() -> Unit)?
) {
    val openings = themes.count { it.type == ThemeType.OP }
    val endings = themes.count { it.type == ThemeType.ED }
    val subtitle = when {
        themes.isEmpty() -> null
        else -> listOfNotNull(
            pluralStringResource(R.plurals.themes_openings_count, openings, openings)
                .takeIf { openings > 0 },
            pluralStringResource(R.plurals.themes_endings_count, endings, endings)
                .takeIf { endings > 0 }
        ).joinToString("  ·  ")
    }

    com.anisync.android.presentation.components.SectionHeader(
        title = stringResource(R.string.section_themes),
        level = com.anisync.android.presentation.components.HeaderLevel.Section,
        subtitle = subtitle,
        onActionClick = onSeeAllClick
    )
}

@Composable
private fun ThemeTileSkeleton() {
    val shimmer = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(modifier = Modifier.width(TILE_WIDTH)) {
        Box(
            modifier = Modifier
                .width(TILE_WIDTH)
                .height(TILE_ART_HEIGHT)
                .clip(RoundedCornerShape(14.dp))
                .background(shimmer)
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(shimmer)
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(TILE_WIDTH * 0.55f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(shimmer)
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(shimmer)
        )
    }
}

@Composable
private fun ThemesErrorCard(
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_medium))) {
            Text(
                text = stringResource(R.string.themes_failed_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.themes_failed_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_normal)))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .bouncyClickable(onClick = onRetryClick, clipShape = RoundedCornerShape(50))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = stringResource(R.string.themes_retry),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/** "Episodes 14–22, 24 · 2 versions", or the honest fallback when the range is missing. */
@Composable
fun themeCoverageLabel(theme: MediaTheme, spans: List<EpisodeSpan>): String {
    val range = when {
        spans.isEmpty() -> stringResource(R.string.themes_all_episodes)
        else -> stringResource(R.string.themes_episodes, formatEpisodeSpans(spans))
    }
    val versions = theme.versions.size
    return if (versions > 1) {
        range + "  ·  " + pluralStringResource(R.plurals.themes_versions_count, versions, versions)
    } else {
        range
    }
}

/** "13 of 24 episodes", shown in the sheet where a number is worth more than a shape. */
@Composable
fun themeCoverageCount(spans: List<EpisodeSpan>, totalEpisodes: Int?): String? {
    if (spans.isEmpty() || totalEpisodes == null || totalEpisodes <= 0) return null
    val covered = countCoveredEpisodes(spans, totalEpisodes)
    return stringResource(R.string.themes_coverage, covered, totalEpisodes)
}
