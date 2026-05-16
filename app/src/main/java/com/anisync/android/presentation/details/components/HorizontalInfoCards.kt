package com.anisync.android.presentation.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.MediaDetails
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader

import com.anisync.android.presentation.util.formatAsTitle
import com.anisync.android.presentation.util.formatCountdownAdaptive
import com.anisync.android.type.MediaType
import kotlinx.coroutines.delay

/**
 * Displays media information in a scrollable horizontal list with pill-style cards.
 * Order: Status, Episodes, Duration, Season, Aired, Source, Studio
 */
@Composable
fun HorizontalInfoCards(
    details: MediaDetails,
    modifier: Modifier = Modifier,
    onStudioClick: (Int) -> Unit = {}
) {
    val infoItems = buildInfoItems(details, onStudioClick)

    if (infoItems.isEmpty()) return

    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.section_information),
            level = HeaderLevel.Section
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

        LazyRow(
            contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(infoItems, key = { it.label }) { item ->
                InfoPill(
                    icon = item.icon,
                    iconResId = item.iconResId,
                    label = item.label,
                    value = item.value,
                    iconTint = item.iconTint,
                    isStatus = item.isStatus,
                    onClick = item.onClick
                )
            }
        }
    }
}

private data class InfoItem(
    val icon: ImageVector? = null,
    val iconResId: Int? = null,
    val label: String,
    val value: String,
    val iconTint: Color,
    val isStatus: Boolean = false,
    val onClick: (() -> Unit)? = null
)

@Composable
private fun buildInfoItems(
    details: MediaDetails,
    onStudioClick: (Int) -> Unit
): List<InfoItem> {
    val items = mutableListOf<InfoItem>()

    // 1. Status
    val statusValue = if (details.status.equals("NOT_YET_RELEASED", ignoreCase = true)) {
        stringResource(R.string.media_status_upcoming)
    } else {
        details.status.formatAsTitle() ?: stringResource(R.string.unknown)
    }
    items.add(
        InfoItem(
            icon = MediaDetailsIcons.getStatusIcon(details.status),
            label = stringResource(R.string.stat_status),
            value = statusValue,
            iconTint = MediaDetailsIcons.getStatusColor(details.status),
            isStatus = true
        )
    )

    // 1b. Airing countdown (only when next episode known)
    details.nextAiringEpisode?.let { airing ->
        val remaining = rememberLiveCountdownSeconds(airing.airingAt, airing.timeUntilAiring)
        if (remaining > 0) {
            items.add(
                InfoItem(
                    icon = Icons.Default.HourglassBottom,
                    label = stringResource(R.string.stat_next_episode),
                    value = stringResource(
                        R.string.airing_countdown_value,
                        airing.episode,
                        formatCountdownAdaptive(remaining)
                    ),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            )
        }
    }

    // 2. Episodes
    val episodesLabel = if (details.type == MediaType.MANGA) {
        stringResource(R.string.stat_chapters)
    } else {
        stringResource(R.string.stat_episodes)
    }
    val episodesValue = if (details.type == MediaType.MANGA) {
        details.chapters?.let { "$it" } ?: stringResource(R.string.unknown)
    } else {
        when {
            details.episodes != null -> "${details.episodes}"
            details.nextAiringEpisode != null -> "${details.nextAiringEpisode.episode - 1}"
            else -> "?"
        }
    }
    // Append unit suffix based on type
    val fullEpisodesValue = if (episodesValue != "?") {
        if (details.type == MediaType.MANGA) "$episodesValue Chs" else "$episodesValue Eps"
    } else {
        stringResource(R.string.unknown)
    }

    items.add(
        InfoItem(
            icon = MediaDetailsIcons.getEpisodesIcon(details.type),
            label = episodesLabel,
            value = fullEpisodesValue,
            iconTint = MaterialTheme.colorScheme.primary
        )
    )

    // 3. Duration
    details.duration?.let { duration ->
        items.add(
            InfoItem(
                icon = Icons.Default.AccessTime,
                label = stringResource(R.string.stat_duration),
                value = "$duration mins",
                iconTint = MaterialTheme.colorScheme.secondary
            )
        )
    }

    // 4. Season
    details.seasonYear?.let { year ->
        val seasonValue = details.season?.let { season ->
            "${season.lowercase().replaceFirstChar { it.uppercase() }} $year"
        } ?: year.toString()

        if (MediaDetailsIcons.useCustomSeasonIcon(details.season)) {
            items.add(
                InfoItem(
                    iconResId = MediaDetailsIcons.getSeasonIconResId(details.season),
                    label = stringResource(R.string.stat_season),
                    value = seasonValue,
                    iconTint = MediaDetailsIcons.getSeasonColor(details.season)
                )
            )
        } else {
            items.add(
                InfoItem(
                    icon = MediaDetailsIcons.getSeasonIcon(details.season)!!,
                    label = stringResource(R.string.stat_season),
                    value = seasonValue,
                    iconTint = MediaDetailsIcons.getSeasonColor(details.season)
                )
            )
        }
    }

    // 5. Aired Date (Start - End)
    val airedDateValue = when {
        details.startDate != null && details.endDate != null -> "${details.startDate} - ${details.endDate}"
        details.startDate != null -> details.startDate
        else -> null
    }
    airedDateValue?.let { aired ->
        items.add(
            InfoItem(
                icon = Icons.Default.DateRange,
                label = stringResource(R.string.stat_aired),
                value = aired,
                iconTint = MaterialTheme.colorScheme.primary
            )
        )
    }

    // 6. Source
    val sourceValue = details.source?.let { getSourceDisplayName(it) } ?: stringResource(R.string.unknown)
    items.add(
        InfoItem(
            icon = MediaDetailsIcons.getSourceIcon(),
            label = stringResource(R.string.stat_source),
            value = sourceValue,
            iconTint = MaterialTheme.colorScheme.tertiary
        )
    )

    // 6. Studio
    details.studio?.let { studio ->
        items.add(
            InfoItem(
                icon = Icons.Default.Business,
                label = stringResource(R.string.stat_studio),
                value = studio.name,
                iconTint = Color(0xFFFF9800), // Orange
                onClick = { onStudioClick(studio.id) }
            )
        )
    }

    return items
}

@Composable
private fun InfoPill(
    icon: ImageVector?,
    iconResId: Int?,
    label: String,
    value: String,
    iconTint: Color,
    isStatus: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = Modifier.height(56.dp) // Slightly taller for better touch target
    val cardShape = RoundedCornerShape(16.dp)
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = cardShape,
            colors = cardColors
        ) {
            InfoPillContent(icon, iconResId, label, value, iconTint)
        }
        return
    }
    Card(
        modifier = cardModifier,
        shape = cardShape,
        colors = cardColors
    ) {
        InfoPillContent(icon, iconResId, label, value, iconTint)
    }
}

@Composable
private fun InfoPillContent(
    icon: ImageVector?,
    iconResId: Int?,
    label: String,
    value: String,
    iconTint: Color
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = iconTint.copy(alpha = 0.12f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (iconResId != null) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

/**
 * Live ticker that returns seconds remaining until [airingAt] (unix seconds, absolute).
 * Uses absolute time so the value stays accurate across cache reads and backgrounding.
 * [fallbackSeconds] is used only if airingAt is in the past relative to device clock
 * (e.g., clock skew vs. server-snapshot time).
 */
@Composable
private fun rememberLiveCountdownSeconds(airingAt: Long, fallbackSeconds: Int): Int {
    var secs by remember(airingAt) {
        val now = System.currentTimeMillis() / 1000
        val initial = (airingAt - now).toInt()
        mutableIntStateOf(if (initial > 0) initial else fallbackSeconds.coerceAtLeast(0))
    }
    LaunchedEffect(airingAt) {
        while (secs > 0) {
            delay(1_000)
            val now = System.currentTimeMillis() / 1000
            secs = ((airingAt - now).coerceAtLeast(0)).toInt()
        }
    }
    return secs
}

@Composable
private fun getSourceDisplayName(source: String): String {
    return when (source.uppercase()) {
        "ORIGINAL" -> stringResource(R.string.source_original)
        "MANGA" -> stringResource(R.string.source_manga)
        "LIGHT_NOVEL" -> stringResource(R.string.source_light_novel)
        "VISUAL_NOVEL" -> stringResource(R.string.source_visual_novel)
        "VIDEO_GAME" -> stringResource(R.string.source_video_game)
        "OTHER" -> stringResource(R.string.source_other)
        "NOVEL" -> stringResource(R.string.source_novel)
        "DOUJINSHI" -> stringResource(R.string.source_doujinshi)
        "ANIME" -> stringResource(R.string.source_anime)
        "WEB_NOVEL" -> stringResource(R.string.source_web_novel)
        "LIVE_ACTION" -> stringResource(R.string.source_live_action)
        "GAME" -> stringResource(R.string.source_game)
        "COMIC" -> stringResource(R.string.source_comic)
        "MULTIMEDIA_PROJECT" -> stringResource(R.string.source_multimedia_project)
        "PICTURE_BOOK" -> stringResource(R.string.source_picture_book)
        else -> source.replace("_", " ").lowercase()
            .replaceFirstChar { it.uppercase() }
    }
}