package com.anisync.android.presentation.details.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay

/** Past this many episodes a per-episode mark is thinner than a hairline, so the bar draws spans. */
private const val TICK_LIMIT = 50

private val TILE_WIDTH = 160.dp
private val TILE_ART_HEIGHT = 90.dp
private val ROW_ART_WIDTH = 104.dp
private val ROW_ART_HEIGHT = 59.dp

/** Room left around a long slug ("OP1-EN4Kids") so it ellipsises inside the artwork. */
private val BADGE_INSET = 16.dp

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
    artWidth: Dp,
    modifier: Modifier = Modifier,
    /** False in the full list, where the row already carries a play button the glyph would sit under. */
    showPlayGlyph: Boolean = true
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

        if (showPlayGlyph) {
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
        }

        Surface(
            color = Color.Black.copy(alpha = 0.78f),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                // A cap, not a width: "OP1" stays tight and only a long slug is trimmed.
                .widthIn(max = artWidth - BADGE_INSET)
        ) {
            Text(
                text = slug,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
    val description = if (spans.isEmpty()) {
        theme.songTitle.orEmpty()
    } else {
        pluralStringResource(R.plurals.themes_episodes, episodeQuantity(spans), coverageLabel)
    }

    // The ripple is rounded at the top to match the artwork and left square at the bottom, so
    // the corner curve does not bite the first and last mark off the episode bar.
    val rippleShape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)

    Column(
        modifier = modifier
            .width(TILE_WIDTH)
            .bouncyClickable(onClick = onClick, clipShape = rippleShape)
            .semantics { contentDescription = "${theme.slug}. $description" }
    ) {
        ThemeArtwork(
            coverUrl = coverUrl,
            slug = theme.slug,
            type = theme.type,
            cornerRadius = 14.dp,
            playGlyphSize = 30.dp,
            artWidth = TILE_WIDTH,
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
        // Always drawn, even with no artist recorded. An absent line would make this tile
        // shorter than its neighbours, and a LazyRow takes its height from the tallest visible
        // item, so the whole section would resize as it scrolled.
        Text(
            text = theme.artists.joinToString(", "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        EpisodeCoverageBar(
            spans = spans,
            totalEpisodes = totalEpisodes,
            height = 5.dp
        )
    }
}

/** One theme in the full list, where the bars of every row share a scale. */
@Composable
fun ThemeRow(
    theme: MediaTheme,
    coverUrl: String?,
    totalEpisodes: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val spans = remember(theme) { theme.episodeSpans }

    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        shape = RoundedCornerShape(16.dp),
        // A detail pane keeps one theme on screen, so the row it came from has to stay marked.
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
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
                    artWidth = ROW_ART_WIDTH,
                    showPlayGlyph = false,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = themeCoverageLabel(theme, spans),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (theme.isSpoiler) {
                            Spacer(Modifier.width(8.dp))
                            SpoilerTag()
                        }
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
                spans = spans,
                totalEpisodes = totalEpisodes,
                height = 6.dp
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
    retryAfterSeconds: Long? = null,
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
                message = errorMessage,
                retryAfterSeconds = retryAfterSeconds,
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
    message: String?,
    retryAfterSeconds: Long?,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // A rate limit is the one failure with a known end, so it counts down and holds the
    // button rather than inviting a tap that will fail the same way.
    var remaining by remember(retryAfterSeconds) { mutableLongStateOf(retryAfterSeconds ?: 0L) }
    LaunchedEffect(retryAfterSeconds) {
        while (remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
    }
    val waiting = remaining > 0

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
                text = message ?: stringResource(R.string.themes_failed_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_normal)))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .then(
                        if (waiting) Modifier
                        else Modifier.bouncyClickable(
                            onClick = onRetryClick,
                            clipShape = RoundedCornerShape(50)
                        )
                    )
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
                        text = if (waiting) {
                            stringResource(R.string.themes_retry_in, remaining)
                        } else {
                            stringResource(R.string.themes_retry)
                        },
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
        else -> pluralStringResource(
            R.plurals.themes_episodes,
            episodeQuantity(spans),
            formatEpisodeSpans(spans)
        )
    }
    val versions = theme.versions.size
    return if (versions > 1) {
        range + "  ·  " + pluralStringResource(R.plurals.themes_versions_count, versions, versions)
    } else {
        range
    }
}

/**
 * Marks a theme AnimeThemes flagged as spoiling the show.
 *
 * A warning, not a cover. What spoils is the theme itself, its visuals and often its lyrics,
 * so hiding the episode range would withhold the one thing that is safe to read. Drawn as a
 * tonal pill so it sits in the same family as the number and qualifier pills rather than
 * shouting over the row.
 */
@Composable
fun SpoilerTag(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.themes_spoiler_tag),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onErrorContainer,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** 1 when the theme plays over a single episode, so the label can say "Episode" not "Episodes". */
fun episodeQuantity(spans: List<EpisodeSpan>): Int {
    val only = spans.singleOrNull() ?: return 2
    return if (only.end == only.start) 1 else 2
}

/** "13 of 24 episodes", shown in the sheet where a number is worth more than a shape. */
@Composable
fun themeCoverageCount(spans: List<EpisodeSpan>, totalEpisodes: Int?): String? {
    if (spans.isEmpty() || totalEpisodes == null || totalEpisodes <= 0) return null
    val covered = countCoveredEpisodes(spans, totalEpisodes)
    return stringResource(R.string.themes_coverage, covered, totalEpisodes)
}
