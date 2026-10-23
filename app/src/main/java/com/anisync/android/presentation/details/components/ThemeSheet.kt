package com.anisync.android.presentation.details.components

import android.content.Intent
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.MediaTheme
import com.anisync.android.domain.ThemeType
import com.anisync.android.domain.ThemeVersion
import com.anisync.android.domain.ThemeVideo
import com.anisync.android.domain.VideoOverlap
import com.anisync.android.domain.formatEpisodeSpans
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.components.VideoPlayer
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import java.net.URLEncoder

/**
 * One theme, expanded.
 *
 * The video plays here rather than handing off to a browser, because AnimeThemes serves the
 * file itself and the app already carries a player for it. Sound is on from the start, which
 * is the one place in the app where that is the right default: the user tapped a song.
 *
 * A theme with more than one version lists them apart, each with its own episode range and
 * its own videos, since that is the shape of the data and the thing the collapsed row cannot
 * show.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSheet(
    theme: MediaTheme,
    totalEpisodes: Int?,
    animeSlug: String?,
    coverUrl: String?,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
) {
    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        ThemeDetail(
            theme = theme,
            totalEpisodes = totalEpisodes,
            animeSlug = animeSlug,
            coverUrl = coverUrl,
            twoColumn = false,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(R.dimen.spacing_large))
                .padding(bottom = dimensionResource(R.dimen.spacing_large))
        )
    }
}

/** Widest the clip is allowed to grow in a detail pane, which is far wider than 16:9 needs. */
private val PANE_PLAYER_MAX_WIDTH = 640.dp

/**
 * One theme's contents, laid out for the width they are given.
 *
 * In a sheet everything stacks, because a phone has one column to spend. In a detail pane
 * ([twoColumn]) the stack below the player splits: where the theme sits in the run on one side,
 * which file plays and where else to take it on the other. Same data and same state either way, so
 * a resize that swaps the layouts keeps the chosen version and video.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeDetail(
    theme: MediaTheme,
    totalEpisodes: Int?,
    animeSlug: String?,
    coverUrl: String?,
    twoColumn: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedVersionIndex by rememberSaveable(theme.id) { mutableIntStateOf(0) }
    var selectedVideoId by rememberSaveable(theme.id) { mutableStateOf<Int?>(null) }
    var audioOnly by rememberSaveable(theme.id) { mutableStateOf(false) }

    val version = theme.versions.getOrNull(selectedVersionIndex) ?: theme.versions.first()
    val video = remember(version, selectedVideoId) {
        version.videos.firstOrNull { it.id == selectedVideoId } ?: version.preferredVideo
    }
    val audio = version.audio?.takeIf { audioOnly }
    val gap = dimensionResource(R.dimen.spacing_medium)

    Column(modifier) {
        val playUrl = audio?.url ?: video?.url
        if (playUrl != null) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                VideoPlayer(
                    url = playUrl,
                    playerCache = null,
                    startMuted = false,
                    loop = false,
                    artwork = if (audio != null) {
                        { ThemeAudioArtwork(coverUrl = coverUrl, type = theme.type) }
                    } else {
                        null
                    },
                    modifier = if (twoColumn) {
                        Modifier.widthIn(max = PANE_PLAYER_MAX_WIDTH).fillMaxWidth()
                    } else {
                        Modifier.fillMaxWidth()
                    }
                )
            }
            Spacer(Modifier.height(gap))
        }

        ThemeIdentity(theme = theme)

        Spacer(Modifier.height(gap))

        // Chips inside a pane card cannot reuse the card's own tone, or they vanish into it.
        val cardChipColor = MaterialTheme.colorScheme.surfaceContainerHighest

        val onSelectVersion: (Int) -> Unit = {
            selectedVersionIndex = it
            selectedVideoId = null
            audioOnly = false
        }
        val onSelectVideo: (Int) -> Unit = {
            selectedVideoId = it
            audioOnly = false
        }
        val showSources = version.videos.size > 1 || version.audio != null

        if (twoColumn) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                DetailCard(modifier = Modifier.weight(1f)) {
                    if (theme.hasPerformerDetail) {
                        PerformersSection(theme = theme)
                        Spacer(Modifier.height(gap))
                    }
                    CoverageSection(
                        theme = theme,
                        totalEpisodes = totalEpisodes,
                        selectedVersionIndex = selectedVersionIndex,
                        onSelectVersion = onSelectVersion
                    )
                }
                DetailCard(modifier = Modifier.weight(1f)) {
                    if (showSources) {
                        VideoSection(
                            version = version,
                            selectedVideoId = video?.id.takeIf { !audioOnly },
                            audioSelected = audioOnly,
                            onSelectVideo = onSelectVideo,
                            onSelectAudio = { audioOnly = true },
                            chipColor = cardChipColor
                        )
                        Spacer(Modifier.height(gap))
                    }
                    SheetLabel(stringResource(R.string.themes_actions_label))
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
                    ThemeActions(
                        theme = theme,
                        animeSlug = animeSlug,
                        video = video,
                        wrap = true,
                        chipColor = cardChipColor
                    )
                }
            }
        } else {
            if (theme.hasPerformerDetail) {
                PerformersSection(theme = theme)
                Spacer(Modifier.height(gap))
            }
            CoverageSection(
                theme = theme,
                totalEpisodes = totalEpisodes,
                selectedVersionIndex = selectedVersionIndex,
                onSelectVersion = onSelectVersion
            )
            if (showSources) {
                Spacer(Modifier.height(gap))
                VideoSection(
                    version = version,
                    selectedVideoId = video?.id.takeIf { !audioOnly },
                    audioSelected = audioOnly,
                    onSelectVideo = onSelectVideo,
                    onSelectAudio = { audioOnly = true }
                )
            }
            Spacer(Modifier.height(gap))
            ThemeActions(theme = theme, animeSlug = animeSlug, video = video, wrap = false)
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacing_normal)))
        Text(
            text = stringResource(R.string.themes_attribution),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/** Groups a pane column into a card, so the two halves read as two things instead of one wall. */
@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_normal)),
            content = content
        )
    }
}

/**
 * Where the theme sits in the run: the versions apart when there is more than one, otherwise the
 * single span with its coverage bar.
 */
@Composable
private fun CoverageSection(
    theme: MediaTheme,
    totalEpisodes: Int?,
    selectedVersionIndex: Int,
    onSelectVersion: (Int) -> Unit
) {
    if (theme.versions.size > 1) {
        SheetLabel(stringResource(R.string.themes_versions))
        Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
        theme.versions.forEachIndexed { index, entry ->
            VersionCard(
                version = entry,
                totalEpisodes = totalEpisodes,
                isSelected = index == selectedVersionIndex,
                onClick = { onSelectVersion(index) }
            )
            if (index != theme.versions.lastIndex) {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
            }
        }
        return
    }

    SheetLabel(stringResource(R.string.themes_appears_in))
    Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
    // A show with no episode count on AniList gets no bar, so the slot goes with it.
    if (totalEpisodes != null && totalEpisodes > 0) {
        EpisodeCoverageBar(
            spans = theme.episodeSpans,
            totalEpisodes = totalEpisodes,
            height = 10.dp
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
    }
    val coverage = themeCoverageCount(theme.episodeSpans, totalEpisodes)
    Text(
        text = listOfNotNull(
            if (theme.episodeSpans.isEmpty()) {
                stringResource(R.string.themes_all_episodes)
            } else {
                pluralStringResourceOrDefault(
                    R.plurals.themes_episodes,
                    episodeQuantity(theme.episodeSpans),
                    formatEpisodeSpans(theme.episodeSpans)
                )
            },
            coverage
        ).joinToString("  ·  "),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun VideoSection(
    version: ThemeVersion,
    selectedVideoId: Int?,
    audioSelected: Boolean,
    onSelectVideo: (Int) -> Unit,
    onSelectAudio: () -> Unit,
    chipColor: Color = MaterialTheme.colorScheme.surfaceContainer
) {
    val context = LocalContext.current
    SheetLabel(stringResource(R.string.themes_video))
    Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        version.videos.forEach { candidate ->
            SourceChip(
                label = variantLabel(candidate),
                isSelected = candidate.id == selectedVideoId,
                onClick = { onSelectVideo(candidate.id) },
                containerColor = chipColor
            )
        }
        version.audio?.let { audio ->
            SourceChip(
                label = listOfNotNull(
                    stringResource(R.string.themes_variant_audio),
                    audio.sizeBytes?.let { Formatter.formatShortFileSize(context, it) }
                ).joinToString(" · "),
                isSelected = audioSelected,
                onClick = onSelectAudio,
                containerColor = chipColor
            )
        }
    }
}

/**
 * The credited voices, one per line. Only drawn when it adds to the artist line under the title:
 * the members behind a band name, or the character a voice actor sings as.
 */
@Composable
private fun PerformersSection(theme: MediaTheme) {
    SheetLabel(stringResource(R.string.themes_performers))
    Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        theme.performers.forEach { performer ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = performer.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                performer.character?.let { character ->
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.themes_performer_as, character),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    }
}

/**
 * YouTube, AnimeThemes and share. A pane column is narrower than the sheet these came from, so the
 * chips [wrap] there rather than scrolling sideways inside a card.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ThemeActions(
    theme: MediaTheme,
    animeSlug: String?,
    video: ThemeVideo?,
    wrap: Boolean,
    chipColor: Color = MaterialTheme.colorScheme.surfaceContainer
) {
    val context = LocalContext.current
    val spacing = dimensionResource(R.dimen.spacing_small)
    val chips: @Composable () -> Unit = {
        ActionChip(
            icon = Icons.Default.Search,
            label = stringResource(R.string.themes_action_youtube),
            containerColor = chipColor,
            onClick = { context.openUrl(youtubeSearchUrl(theme)) }
        )
        ActionChip(
            icon = Icons.Default.Public,
            label = stringResource(R.string.themes_action_animethemes),
            containerColor = chipColor,
            onClick = { context.openUrl(animeThemesUrl(animeSlug, theme, video)) }
        )
        ActionChip(
            icon = Icons.Default.Share,
            label = null,
            containerColor = chipColor,
            onClick = {
                val text = listOfNotNull(theme.songTitle, theme.artists.firstOrNull())
                    .joinToString(" by ")
                context.shareText(text + "\n" + animeThemesUrl(animeSlug, theme, video))
            }
        )
    }

    if (wrap) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) { chips() }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) { chips() }
    }
}

private val ACTION_CHIP_HEIGHT = 40.dp

@Composable
private fun ThemeIdentity(theme: MediaTheme) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = when (theme.type) {
                    ThemeType.OP -> stringResource(R.string.themes_opening_number, theme.sequence ?: 1)
                    ThemeType.ED -> stringResource(R.string.themes_ending_number, theme.sequence ?: 1)
                    ThemeType.IN -> theme.sequence
                        ?.let { stringResource(R.string.themes_insert_number, it) }
                        ?: stringResource(R.string.themes_insert)
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        // Only what the slot adds on top of the number, since the pill beside it already
        // says "Ending 11". Without this, ED11 and ED11-EN would read identically.
        theme.variantLabel?.let { qualifier ->
            Spacer(Modifier.width(dimensionResource(R.dimen.spacing_small)))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = qualifier,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        if (theme.isSpoiler) {
            Spacer(Modifier.width(dimensionResource(R.dimen.spacing_small)))
            SpoilerTag()
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        text = theme.songTitle ?: theme.slug,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    if (theme.artists.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = theme.artists.joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun VersionCard(
    version: ThemeVersion,
    totalEpisodes: Int?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val spans = version.episodeSpans
    Surface(
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .bouncyClickable(onClick = onClick, clipShape = RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_normal))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.themes_version_label, version.version),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (spans.isEmpty()) {
                            stringResource(R.string.themes_all_episodes)
                        } else {
                            pluralStringResourceOrDefault(
                                R.plurals.themes_episodes,
                                episodeQuantity(spans),
                                formatEpisodeSpans(spans)
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(dimensionResource(R.dimen.spacing_normal)))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
            EpisodeCoverageBar(
                spans = spans,
                totalEpisodes = totalEpisodes,
                height = 5.dp,
                coveredColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                }
            )
        }
    }
}

/** "Creditless · 1080p · Transition", enough to tell two files of one version apart. */
@Composable
private fun variantLabel(video: ThemeVideo): String = buildList {
    add(
        stringResource(
            if (video.creditless) R.string.themes_variant_creditless
            else R.string.themes_variant_with_credits
        )
    )
    video.resolution?.let { add("${it}p") }
    if (video.subbed) add(stringResource(R.string.themes_variant_subbed))
    if (video.lyrics) add(stringResource(R.string.themes_variant_lyrics))
    when (video.overlap) {
        VideoOverlap.Transition -> add(stringResource(R.string.themes_variant_transition))
        VideoOverlap.Over -> add(stringResource(R.string.themes_variant_over))
        VideoOverlap.None -> Unit
    }
}.joinToString(" · ")

@Composable
private fun SourceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else containerColor,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .bouncyClickable(onClick = onClick, clipShape = RoundedCornerShape(50))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ActionChip(
    icon: ImageVector,
    label: String?,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            // Fixed height so the icon-only chip matches the labelled ones instead of
            // sitting short and looking lifted.
            .height(ACTION_CHIP_HEIGHT)
            .clip(RoundedCornerShape(50))
            .bouncyClickable(onClick = onClick, clipShape = RoundedCornerShape(50))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            if (label != null) {
                Spacer(Modifier.width(7.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

private fun youtubeSearchUrl(theme: MediaTheme): String {
    val query = listOfNotNull(theme.songTitle, theme.artists.firstOrNull()).joinToString(" ")
    return "https://www.youtube.com/results?search_query=" +
        URLEncoder.encode(query.ifBlank { theme.slug }, "UTF-8")
}

private fun animeThemesUrl(animeSlug: String?, theme: MediaTheme, video: ThemeVideo?): String = when {
    animeSlug != null -> "https://animethemes.moe/anime/$animeSlug/${theme.slug}"
    video != null -> "https://animethemes.moe/video/${video.url.substringAfterLast('/')}"
    else -> "https://animethemes.moe"
}

private fun android.content.Context.openUrl(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    }
}

private fun android.content.Context.shareText(text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { startActivity(Intent.createChooser(intent, null)) }
}
