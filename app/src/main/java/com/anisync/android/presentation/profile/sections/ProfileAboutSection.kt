package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.AsyncRichTextRenderer
import com.anisync.android.presentation.components.EmptyState
import com.anisync.android.presentation.components.formatRelativeTimeSeconds
import com.anisync.android.presentation.util.copyOnLongPress
import com.anisync.android.presentation.util.openUrl
import com.anisync.android.presentation.util.rememberCopyToClipboard
import com.anisync.android.ui.theme.emphasis
import com.anisync.android.util.AniListUrls
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

fun LazyListScope.profileAboutPage(
    profile: UserProfile,
    isOwnProfile: Boolean,
    onEditAbout: () -> Unit
) {
    item(key = "about_bio", contentType = "about_bio") {
        if (profile.about.isNullOrBlank()) {
            EmptyState(
                icon = Icons.Default.Person,
                title = stringResource(if (isOwnProfile) R.string.profile_about_empty_own else R.string.profile_about_empty),
                description = stringResource(R.string.profile_about_empty_body),
                actionLabel = if (isOwnProfile) stringResource(R.string.profile_about_write) else null,
                actionIcon = if (isOwnProfile) Icons.Default.Edit else null,
                onAction = if (isOwnProfile) onEditAbout else null,
                modifier = Modifier.height(360.dp)
            )
        } else {
            AsyncRichTextRenderer(
                html = profile.about,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
    if (profile.previousNames.isNotEmpty()) {
        item(key = "about_previous_names", contentType = "about_section") {
            PreviousNames(profile)
        }
    }
    item(key = "about_details", contentType = "about_section") {
        AboutDetails(profile)
    }
}

@Composable
private fun AboutHeading(title: String, subtitle: String? = null) {
    Row(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(2.dp),
            modifier = Modifier
                .width(4.dp)
                .height(if (subtitle != null) 40.dp else 24.dp)
        ) {}
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviousNames(profile: UserProfile) {
    val latest = profile.previousNames.mapNotNull { it.changedAt }.maxOrNull()
    val changed = latest?.let {
        val month = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())
            .format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
        stringResource(R.string.profile_previous_names_changed, month)
    }
    Column {
        AboutHeading(stringResource(R.string.profile_previous_names), changed)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            profile.previousNames.forEach { previous ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.copyOnLongPress(
                        text = previous.name,
                        clipLabel = stringResource(R.string.profile_username),
                        copiedMessage = stringResource(R.string.profile_username_copied, previous.name)
                    )
                ) {
                    Text(
                        text = previous.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutDetails(profile: UserProfile) {
    val context = LocalContext.current
    val copy = rememberCopyToClipboard()
    val clipLabel = stringResource(R.string.profile_username)
    val copiedMessage = stringResource(R.string.profile_username_copied, profile.name)
    val url = AniListUrls.userUrl(profile.name)
    val dates = remember { DateFormat.getDateInstance(DateFormat.LONG) }
    Column {
        AboutHeading(stringResource(R.string.profile_details))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                profile.createdAt?.let {
                    DetailRow(Icons.Default.CalendarMonth, stringResource(R.string.profile_joined, dates.format(Date(it))))
                }
                profile.activeAt?.let {
                    DetailRow(
                        Icons.Default.Schedule,
                        stringResource(R.string.profile_active, formatRelativeTimeSeconds(LocalResources.current, it / 1000L))
                    )
                }
                DetailRow(
                    icon = Icons.Default.Person,
                    text = profile.name,
                    detail = stringResource(R.string.profile_username_hint),
                    actionIcon = Icons.Default.ContentCopy,
                    actionLabel = stringResource(R.string.profile_copy_username),
                    onAction = { copy(clipLabel, profile.name, copiedMessage) },
                    modifier = Modifier.copyOnLongPress(text = profile.name, clipLabel = clipLabel, copiedMessage = copiedMessage)
                )
                DetailRow(
                    icon = Icons.Default.Link,
                    text = url.removePrefix("https://"),
                    detail = stringResource(R.string.profile_open_on_anilist),
                    actionIcon = Icons.AutoMirrored.Filled.OpenInNew,
                    actionLabel = stringResource(R.string.profile_open_on_anilist),
                    onAction = { openUrl(context, url) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    actionIcon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail != null) {
                Text(text = detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (actionIcon != null && onAction != null) {
            IconButton(onClick = onAction) {
                Icon(actionIcon, contentDescription = actionLabel)
            }
        }
    }
}

@Composable
internal fun BioPreview(about: String, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val container = MaterialTheme.colorScheme.surfaceContainerLow
    // A bio of a few lines reads as plain text, a longer one is cut short on a card. Decided while
    // measuring, so the header never draws at one height and then settles at another: that jump
    // moved everything under it each time the header scrolled back into view.
    Layout(
        contents = listOf(
            { Box(modifier = Modifier.background(container)) },
            { AsyncRichTextRenderer(html = about, modifier = Modifier.fillMaxWidth()) },
            { Box(modifier = Modifier.background(Brush.verticalGradient(listOf(container.copy(alpha = 0f), container)))) },
            {
                FilledTonalButton(onClick = onOpen) {
                    Text(stringResource(R.string.profile_read_more), style = MaterialTheme.typography.labelLarge.emphasis())
                }
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onOpen)
    ) { (card, text, fade, button), constraints ->
        val width = constraints.maxWidth
        val inset = 16.dp.roundToPx()
        val body = text.first().measure(Constraints(maxWidth = (width - 2 * inset).coerceAtLeast(0)))
        if (body.height <= BioShortLimit.roundToPx()) {
            return@Layout layout(width, body.height) { body.placeRelative(0, 0) }
        }
        val height = BioPreviewHeight.roundToPx()
        val cardPlaceable = card.first().measure(Constraints.fixed(width, height))
        val fadePlaceable = fade.first().measure(Constraints.fixed(width, 72.dp.roundToPx()))
        val buttonPlaceable = button.first().measure(Constraints())
        val margin = 12.dp.roundToPx()
        layout(width, height) {
            cardPlaceable.placeRelative(0, 0)
            body.placeRelative(inset, 14.dp.roundToPx())
            fadePlaceable.placeRelative(0, height - fadePlaceable.height)
            buttonPlaceable.placeRelative(width - buttonPlaceable.width - margin, height - buttonPlaceable.height - margin)
        }
    }
}

private val BioPreviewHeight = 148.dp
private val BioShortLimit = 88.dp
