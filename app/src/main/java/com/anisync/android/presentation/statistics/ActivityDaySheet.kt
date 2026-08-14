package com.anisync.android.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.domain.ActivityMediaType
import com.anisync.android.domain.UserActivity
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.profile.components.MediaTypeLabel
import com.anisync.android.ui.theme.emphasis
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val CoverWidth = 42.dp
private val CoverHeight = 56.dp

/** Keeps a status row, whose tile is square, the same height as the rows carrying cover art. */
private val RowMinHeight = 72.dp

/**
 * What actually happened on one day of the week breakdown.
 *
 * The bars come from AniList's precomputed per-day counts, which carry no detail at all, so the
 * list behind a day is a separate fetch of that day's activity feed. Counts can therefore differ
 * from the bar: `activityHistory` counts every kind of activity, this asks for list, text and
 * message activities only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActivityDaySheet(
    userId: Int,
    day: ActivityDay,
    onDismiss: () -> Unit,
    onOpenActivity: (UserActivity) -> Unit,
    viewModel: ActivityDayViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val activities by viewModel.activities.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(userId, day.date) { viewModel.load(userId, day) }

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        ActivityDaySheetContent(
            date = day.date,
            activities = activities,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onOpenActivity = onOpenActivity
        )
    }
}

@Composable
internal fun ActivityDaySheetContent(
    date: LocalDate,
    activities: List<UserActivity>,
    isLoading: Boolean,
    errorMessage: String?,
    onOpenActivity: (UserActivity) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f)
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                text = date.format(dateFormatter),
                style = MaterialTheme.typography.titleLarge.emphasis()
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = pluralStringResource(
                    R.plurals.statistics_activity_count, activities.size, activities.size
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when {
            isLoading && activities.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppCircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            errorMessage != null && activities.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            activities.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.statistics_activity_day_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activities, key = { it.id }) { activity ->
                        ActivityDayRow(activity, onClick = { onOpenActivity(activity) })
                    }
                }
            }
        }
    }
}

/**
 * One entry of the day. Every kind of activity keeps the same skeleton — leading slot, title, one
 * line of meta, time — so a day that mixes list updates with a written status still scans as one
 * list. Only the leading slot and the trailing tag change.
 */
@Composable
private fun ActivityDayRow(activity: UserActivity, onClick: () -> Unit) {
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val time = remember(activity.timestamp) {
        Instant.ofEpochSecond(activity.timestamp).atZone(ZoneId.systemDefault()).toLocalTime()
    }
    val excerpt = remember(activity.id) { statusExcerpt(activity.bodyMarkdown ?: activity.text) }
    val title = activity.mediaTitle.ifBlank {
        excerpt ?: stringResource(R.string.statistics_activity_day_posted)
    }
    val meta = activityMeta(activity, hasExcerpt = excerpt != null)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActivityLeading(activity)
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (meta.isNotEmpty() || activity.mediaType != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    activity.mediaType?.let { type ->
                        Text(
                            text = "  ·  ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        MediaTypeLabel(type)
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))
        Text(
            text = time.format(timeFormatter),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Cover art where there is any, the media kind where AniList has no cover, and a distinct tile for
 * a written status, which has no media at all. The slot keeps its width either way so the titles
 * below each other stay aligned.
 */
@Composable
private fun ActivityLeading(activity: UserActivity) {
    val context = LocalContext.current
    val cover = activity.mediaCoverUrl
    when {
        cover != null -> AsyncImage(
            model = ImageRequest.Builder(context).data(cover).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = CoverWidth, height = CoverHeight)
                .clip(RoundedCornerShape(8.dp))
        )
        activity.mediaType != null -> Box(
            modifier = Modifier
                .size(width = CoverWidth, height = CoverHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (activity.mediaType == ActivityMediaType.MANGA) {
                    Icons.AutoMirrored.Rounded.MenuBook
                } else {
                    Icons.Rounded.Tv
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        else -> Box(
            modifier = Modifier
                .size(CoverWidth)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Notes,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** "Watched episode 5" for a list update, otherwise what kind of post it was. */
@Composable
private fun activityMeta(activity: UserActivity, hasExcerpt: Boolean): String {
    val status = activity.status
    if (!status.isNullOrBlank()) {
        val capitalised = status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val progress = activity.progress
        return if (progress.isNullOrBlank()) capitalised else "$capitalised $progress"
    }
    // Without an excerpt the title already says the post is a status, and repeating it under
    // itself reads like a bug.
    return if (hasExcerpt) stringResource(R.string.statistics_activity_day_posted) else ""
}

private val EmbedMarkup = Regex("""(img|youtube|webm|video)\d*\([^)]*\)""", RegexOption.IGNORE_CASE)
private val LinkMarkup = Regex("""\[([^\]]*)]\([^)]*\)""")
private val HtmlTag = Regex("""<[^>]+>""")
private val BareUrl = Regex("""https?://\S+""")
private val LeftoverMarkup = Regex("""[#*_>`~]+""")

/**
 * The readable opening of a status post, for use as a row title.
 *
 * AniList posts routinely open with an image embed, a link or a spoiler marker, none of which say
 * anything in one line, so the markup is stripped before the first surviving line is taken. A post
 * that is nothing but an image has no excerpt at all, and the row falls back to naming the kind.
 */
internal fun statusExcerpt(body: String?): String? {
    if (body.isNullOrBlank()) return null
    return body
        .replace(EmbedMarkup, " ")
        .replace(LinkMarkup, "$1")
        .replace(HtmlTag, " ")
        .replace(BareUrl, " ")
        .replace(LeftoverMarkup, " ")
        .lineSequence()
        .map { it.replace(Regex("""\s+"""), " ").trim() }
        .firstOrNull { it.isNotEmpty() }
}
