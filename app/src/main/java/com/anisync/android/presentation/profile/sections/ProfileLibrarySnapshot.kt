package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.anisync.android.R
import com.anisync.android.domain.AnimeStatusCounts
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.util.formatDecimal
import com.anisync.android.ui.theme.LocalExpressiveTypography
import com.anisync.android.ui.theme.emphasis

/**
 * A single, glanceable "library at a glance" card for the profile Overview tab.
 *
 * Replaces the old full-size [com.anisync.android.presentation.statistics.HeroDashboard] copy that
 * duplicated the Stats tab. Per m3.material.io/foundations (hierarchy + grouping) it groups the
 * highest-signal library numbers into one neutral [surfaceContainerHigh] container, then surfaces
 * the anime list status mix — previously computed but never shown — as a segmented bar with a
 * legend. The card is display-only; the section header's expand button is the route into full Stats.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileLibrarySnapshot(
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    val statuses = remember(profile.animeStatusCounts) { profile.animeStatusCounts.toSegments() }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
            // The huge stat numbers share a single size derived from the longest value, so a
            // four-digit library count or a decimal mean never silently clips while the three
            // columns keep an even editorial rhythm.
            val animeValue = profile.animeCount.toString()
            val mangaValue = profile.mangaCount.toString()
            val meanValue = profile.meanScore.roundToInt().toString()
            val maxValueLen = maxOf(animeValue.length, mangaValue.length, meanValue.length)
            val valueStyle = snapshotNumericStyle(
                base = LocalExpressiveTypography.current.statNumericMedium,
                maxLen = maxValueLen
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetricColumn(
                    value = animeValue,
                    label = stringResource(R.string.media_type_anime),
                    valueStyle = valueStyle,
                    caption = stringResource(R.string.snapshot_days, formatDecimal(profile.daysWatched)),
                    modifier = Modifier.weight(1f)
                )
                MetricColumn(
                    value = mangaValue,
                    label = stringResource(R.string.media_type_manga),
                    valueStyle = valueStyle,
                    caption = stringResource(R.string.snapshot_chapters, formatThousands(profile.chaptersRead)),
                    modifier = Modifier.weight(1f)
                )
                MetricColumn(
                    value = meanValue,
                    label = stringResource(R.string.statistics_mean_score),
                    valueStyle = valueStyle,
                    captionIcon = true,
                    modifier = Modifier.weight(1f)
                )
            }

            if (statuses.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(20.dp))

                StatusBar(statuses)
                Spacer(Modifier.height(14.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    statuses.forEach { segment ->
                        StatusLegendItem(segment)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(
    value: String,
    label: String,
    valueStyle: TextStyle,
    modifier: Modifier = Modifier,
    caption: String? = null,
    captionIcon: Boolean = false
) {
    val expressive = LocalExpressiveTypography.current
    Column(modifier = modifier) {
        Text(
            text = value,
            style = valueStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label.uppercase(),
            style = expressive.statLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        when {
            caption != null -> Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            captionIcon -> Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun StatusBar(segments: List<StatusSegment>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(CircleShape),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        segments.forEach { segment ->
            Box(
                modifier = Modifier
                    .weight(segment.count.toFloat())
                    .fillMaxHeight()
                    .background(segment.color())
            )
        }
    }
}

@Composable
private fun StatusLegendItem(segment: StatusSegment) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(segment.color(), CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(segment.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = segment.count.toString(),
            style = MaterialTheme.typography.labelMedium.emphasis(),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** One slice of the anime-list status mix, with its M3 color role and i18n label. */
private data class StatusSegment(
    val labelRes: Int,
    val count: Int,
    val colorRole: StatusColorRole
)

private enum class StatusColorRole { PRIMARY, SECONDARY, TERTIARY, PRIMARY_CONTAINER, OUTLINE }

@Composable
private fun StatusSegment.color(): Color = when (colorRole) {
    StatusColorRole.PRIMARY -> MaterialTheme.colorScheme.primary
    StatusColorRole.SECONDARY -> MaterialTheme.colorScheme.secondary
    StatusColorRole.TERTIARY -> MaterialTheme.colorScheme.tertiary
    StatusColorRole.PRIMARY_CONTAINER -> MaterialTheme.colorScheme.primaryContainer
    StatusColorRole.OUTLINE -> MaterialTheme.colorScheme.outline
}

/**
 * Maps the five anime statuses to fixed labels + color roles in a lifecycle order
 * (watching -> completed -> planning -> paused -> dropped). Zero-count statuses are
 * dropped so neither the bar nor the legend renders an empty slice. Color roles mirror
 * [com.anisync.android.presentation.statistics.StatusDistributionDonut] for app-wide
 * consistency.
 */
private fun AnimeStatusCounts.toSegments(): List<StatusSegment> = listOf(
    StatusSegment(R.string.status_watching, watching, StatusColorRole.PRIMARY),
    StatusSegment(R.string.status_completed, completed, StatusColorRole.TERTIARY),
    StatusSegment(R.string.status_planning, planning, StatusColorRole.SECONDARY),
    StatusSegment(R.string.status_paused, onHold, StatusColorRole.PRIMARY_CONTAINER),
    StatusSegment(R.string.status_dropped, dropped, StatusColorRole.OUTLINE)
).filter { it.count > 0 }

private fun formatThousands(value: Int): String =
    if (value >= 1000) "%,d".format(value) else value.toString()

/**
 * Scales the shared stat-number size down for longer values so a four- or five-digit count
 * (or a decimal) stays on one line inside its third of the card. Returns [base] (40sp) for the
 * common one-to-three character case.
 */
private fun snapshotNumericStyle(base: TextStyle, maxLen: Int): TextStyle = when {
    maxLen >= 5 -> base.copy(fontSize = 26.sp, lineHeight = 30.sp)
    maxLen == 4 -> base.copy(fontSize = 32.sp, lineHeight = 36.sp)
    else -> base
}
