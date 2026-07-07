package com.anisync.android.presentation.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaDetails
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.domain.formatCommunityScore
import com.anisync.android.domain.url
import com.anisync.android.presentation.util.formatCompactNumber
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.LocalExpressiveTypography

/**
 * Shareable card for a single media entry: banner with the title and community score, cover,
 * studio, popularity/favourites, top genres, and an **adaptive** footer strip that reflects the
 * viewer's own list state — progress while watching, a neutral count while planning/completed,
 * or the media's release status (incl. upcoming/TBA) when it isn't on their list at all.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaShareCard(
    details: MediaDetails,
    scoreFormat: ScoreFormat,
    handle: String? = null,
    modifier: Modifier = Modifier,
) {
    val config = LocalShareCardConfig.current
    val isManga = details.type == MediaType.MANGA
    val coverUrl = details.coverUrl ?: details.cover.url()
    // Accent comes from the card's own scheme so it tracks the selected theme; under the COVER
    // theme primary is already artwork-seeded, so the poster tint survives exactly there.
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    // Community average, rendered in the viewer's own score scale; suppressed by the privacy toggle.
    val scoreText = if (config.showScore) formatCommunityScore(details.score, scoreFormat) else null
    val showScoreStar = scoreFormat != ScoreFormat.POINT_5 && scoreFormat != ScoreFormat.POINT_3

    if (config.template == ShareCardTemplate.HERO) {
        MediaHeroCard(
            details = details,
            isManga = isManga,
            coverUrl = coverUrl,
            scoreText = scoreText,
            showScoreStar = showScoreStar,
            showProgress = config.showProgress,
            handle = handle,
            modifier = modifier,
        )
        return
    }

    ShareCardScaffold(modifier = modifier, handle = handle) {
        ShareCardBannerBox(
            bannerUrl = details.bannerUrl ?: coverUrl,
            height = 168.dp
        ) {
            if (scoreText != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accent)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showScoreStar) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = onAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = scoreText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = onAccent
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                val meta = listOfNotNull(
                    details.format,
                    details.seasonYear?.toString() ?: details.year?.toString()
                ).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(
                        text = meta.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = details.titleUserPreferred,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(accent)
        )

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row {
                Box(
                    modifier = Modifier
                        .width(84.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (coverUrl != null) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    val studioName = details.studio?.name ?: details.studios.firstOrNull()?.name
                    if (!studioName.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = studioName,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        details.popularity?.takeIf { it > 0 }?.let {
                            MiniStat(
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                value = formatCompactNumber(it),
                                label = stringResource(R.string.share_media_popularity),
                                accent = accent
                            )
                        }
                        details.favourites?.takeIf { it > 0 }?.let {
                            MiniStat(
                                icon = Icons.Filled.Favorite,
                                value = formatCompactNumber(it),
                                label = stringResource(R.string.share_media_favourites),
                                accent = accent
                            )
                        }
                    }
                }
            }

            val genres = details.genres.take(3)
            if (genres.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genres.forEach { genre -> ShareChip(genre) }
                }
            }

            MediaStatusStrip(details = details, isManga = isManga, showProgress = config.showProgress)
        }
    }
}

/**
 * The "Poster" media template: the cover art full-bleed at 2:3 with the title, meta and genres set
 * into its bottom scrim, then the same adaptive status strip. A bolder, artwork-first alternative to
 * the standard stat-sheet card.
 */
@Composable
private fun MediaHeroCard(
    details: MediaDetails,
    isManga: Boolean,
    coverUrl: String?,
    scoreText: String?,
    showScoreStar: Boolean,
    showProgress: Boolean,
    handle: String?,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    ShareCardScaffold(modifier = modifier, handle = handle) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.88f)
                        )
                    )
            )
            if (scoreText != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accent)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showScoreStar) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = onAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = scoreText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = onAccent
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val meta = listOfNotNull(
                    details.format,
                    details.seasonYear?.toString() ?: details.year?.toString(),
                    details.studio?.name ?: details.studios.firstOrNull()?.name
                ).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(
                        text = meta.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = details.titleUserPreferred,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                val genres = details.genres.take(3)
                if (genres.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        genres.forEach { genre ->
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        Box(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            MediaStatusStrip(details = details, isManga = isManga, showProgress = showProgress)
        }
    }
}

/** Icon + value + eyebrow, used for the popularity / favourites figures. */
@Composable
private fun MiniStat(icon: ImageVector, value: String, label: String, accent: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The adaptive footer strip. Left is the headline (viewer's list status when tracked, else the
 * media's release status); right is the fitting detail (progress, a neutral count, airing episode,
 * or a release year / TBA). Never shows "0 / 25" for a planned-but-unstarted entry.
 */
@Composable
private fun MediaStatusStrip(details: MediaDetails, isManga: Boolean, showProgress: Boolean) {
    val status = details.listStatus
    val onList = status != null && status != LibraryStatus.UNKNOWN
    val total = if (isManga) details.chapters else details.episodes
    val unit = stringResource(if (isManga) R.string.share_media_chapters else R.string.share_media_episodes)

    val leftLabel: String
    val rightText: String?
    if (onList) {
        // The viewer's own tracked status — a filled, coloured pill.
        leftLabel = stringResource(listStatusLabel(status!!, isManga))
        val progress = details.listProgress ?: 0
        // The progress toggle hides the personal "3 / 25" figure but keeps the status label.
        rightText = if (!showProgress) null else when (status) {
            LibraryStatus.CURRENT, LibraryStatus.REPEATING, LibraryStatus.PAUSED, LibraryStatus.DROPPED ->
                "$progress / ${total?.toString() ?: "?"} $unit"
            LibraryStatus.COMPLETED, LibraryStatus.PLANNING ->
                total?.takeIf { it > 0 }?.let { "$it $unit" }
            else -> null
        }
    } else {
        // Not on the viewer's list — the show's own airing state, worded and styled so it never
        // reads as a personal "Completed"/tracked status.
        leftLabel = stringResource(mediaReleaseLabel(details.status))
        rightText = when (details.status) {
            "FINISHED" -> total?.takeIf { it > 0 }?.let { "$it $unit" }
            "RELEASING" -> details.nextAiringEpisode?.let {
                stringResource(R.string.share_media_ep_number, it.episode)
            } ?: total?.takeIf { it > 0 }?.let { "$it $unit" }
            "NOT_YET_RELEASED" ->
                (details.seasonYear ?: details.year)?.toString() ?: stringResource(R.string.share_media_tba)
            else -> total?.takeIf { it > 0 }?.let { "$it $unit" }
        }
    }

    val container = if (onList) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHighest
    val onContainer = if (onList) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = leftLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = onContainer
        )
        if (rightText != null) {
            Spacer(Modifier.weight(1f))
            Text(
                text = rightText,
                style = MaterialTheme.typography.labelLarge,
                color = onContainer.copy(alpha = 0.85f)
            )
        }
    }
}

private fun listStatusLabel(status: LibraryStatus, isManga: Boolean): Int = when (status) {
    LibraryStatus.CURRENT -> if (isManga) R.string.status_reading else R.string.status_watching
    LibraryStatus.PLANNING -> R.string.status_planning
    LibraryStatus.COMPLETED -> R.string.status_completed
    LibraryStatus.DROPPED -> R.string.status_dropped
    LibraryStatus.PAUSED -> R.string.status_paused
    LibraryStatus.REPEATING -> if (isManga) R.string.status_rereading else R.string.status_rewatching
    LibraryStatus.UNKNOWN -> R.string.status_planning
}

private fun mediaReleaseLabel(status: String): Int = when (status) {
    "RELEASING" -> R.string.share_media_status_airing
    "FINISHED" -> R.string.share_media_status_finished
    "NOT_YET_RELEASED" -> R.string.share_media_status_upcoming
    "CANCELLED" -> R.string.share_media_status_cancelled
    "HIATUS" -> R.string.share_media_status_hiatus
    else -> R.string.share_media_status_finished
}

/** Parses AniList's `#RRGGBB` cover color; null when absent or malformed. */
internal fun parseCoverColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: IllegalArgumentException) {
        null
    }
}
