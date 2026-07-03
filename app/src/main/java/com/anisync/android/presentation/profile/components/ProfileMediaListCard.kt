package com.anisync.android.presentation.profile.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.url
import com.anisync.android.presentation.components.MetaChip
import com.anisync.android.presentation.components.NoteBottomSheet
import com.anisync.android.presentation.components.NotePreview
import com.anisync.android.presentation.components.ScoreChip
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.toColor
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaType
import com.anisync.android.util.getTitle
import java.time.Instant
import java.time.ZoneOffset

private val CoverWidth = 58.dp

/**
 * Compact, read-only row for another user's list entry on their profile (#78). Modelled on the
 * library's [com.anisync.android.presentation.library.components.LibraryListCard] but smaller and
 * non-interactive: it trades the progress bar and adjusters for a flow of metadata chips (status,
 * score, progress, format, rewatches) and a single-line preview of the user's freeform note.
 *
 * The card's height is driven by its text column (the poster is a fixed-aspect thumbnail, inset and
 * centred), so wrapping chips never clip the note. The note shows one line; when it overflows it
 * becomes tappable and opens the full text in a bottom sheet, since these are someone else's notes.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileMediaListCard(
    entry: LibraryEntry,
    mediaType: MediaType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    // Favourites mode: AniList doesn't surface the viewer's list status/score/progress on
    // favourites, so those chips (and the note) are hidden and the card shows only objective
    // media data — type + release year.
    showViewerListData: Boolean = true,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    transitionPrefix: String = TransitionKeys.LIBRARY
) {
    val spatialSpec = AppMotion.rememberSpatialSpec()
    val title = entry.getTitle(titleLanguage)
    val total = if (mediaType == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes
    // Release year for favourites mode (mediaStartDate is a UTC epoch, see mapFuzzyDateToLong).
    val releaseYear = entry.mediaStartDate?.let {
        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).year
    }
    val cacheKey = TransitionKeys.imageCacheKey(transitionPrefix, entry.mediaId) +
        "-" + com.anisync.android.domain.LocalCoverQuality.current.name +
        TransitionKeys.coverVersion(entry.cover.url() ?: entry.coverUrl)

    var showNoteSheet by remember(entry.id) { mutableStateOf(false) }

    val coverShape = RoundedCornerShape(10.dp)
    val coverSharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = TransitionKeys.cover(transitionPrefix, entry.mediaId)
                ),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> spatialSpec },
                clipInOverlayDuringTransition = OverlayClip(coverShape)
            )
        }
    } else {
        Modifier
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .bouncyClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = stringResource(R.string.a11y_action_open_details, title),
                clipShape = RoundedCornerShape(14.dp)
            )
    ) {
        // The text column sets the card height; the poster is a fixed 0.7-aspect thumbnail, inset and
        // vertically centred, so it always shows the full cover (never stretched or over-cropped) no
        // matter how many chip rows or how long the note line is.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 10.dp, top = 10.dp, bottom = 10.dp)
                    .width(CoverWidth)
                    .aspectRatio(0.7f)
                    .then(coverSharedModifier)
                    .clip(coverShape)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(entry.cover.url() ?: entry.coverUrl)
                        .crossfade(true)
                        .placeholderMemoryCacheKey(cacheKey)
                        .memoryCacheKey(cacheKey)
                        .build(),
                    contentDescription = stringResource(R.string.a11y_media_poster, title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 12.dp, top = 9.dp, bottom = 9.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (showViewerListData) {
                        MetaChip(
                            label = entry.status.toLabel(mediaType),
                            accent = entry.status.toColor()
                        )
                        ScoreChip(score = entry.score, format = entry.scoreFormat)
                        if (entry.progress > 0 || (total ?: 0) > 0) {
                            MetaChip(label = progressLabel(entry.progress, total, mediaType))
                        }
                    }
                    entry.format?.takeIf { it != MediaFormat.UNKNOWN__ }?.let {
                        MetaChip(label = it.toLabel())
                    }
                    if (showViewerListData && entry.rewatches > 0) {
                        MetaChip(
                            label = entry.rewatches.toString(),
                            leadingIcon = Icons.Default.Repeat
                        )
                    }
                    if (!showViewerListData) {
                        releaseYear?.let { MetaChip(label = it.toString()) }
                    }
                }

                if (showViewerListData && !entry.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    NotePreview(
                        note = entry.notes,
                        onOpen = { showNoteSheet = true }
                    )
                }
            }
        }
    }

    if (showNoteSheet && !entry.notes.isNullOrBlank()) {
        NoteBottomSheet(
            note = entry.notes,
            heading = title,
            onDismiss = { showNoteSheet = false }
        )
    }
}

/** Progress as "Ep 12 / 24", trimming the total when it's unknown. */
private fun progressLabel(progress: Int, total: Int?, mediaType: MediaType): String {
    val unit = if (mediaType == MediaType.MANGA) "Ch" else "Ep"
    return if ((total ?: 0) > 0) "$unit $progress / $total" else "$unit $progress"
}
