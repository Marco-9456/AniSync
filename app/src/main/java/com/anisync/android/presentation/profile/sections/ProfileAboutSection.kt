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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.AsyncRichTextRenderer
import com.anisync.android.presentation.components.EmptyState
import com.anisync.android.presentation.components.formatRelativeTimeSeconds
import com.anisync.android.presentation.util.copyOnLongPress
import com.anisync.android.presentation.util.formatMonthYear
import com.anisync.android.presentation.util.openUrl
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.presentation.util.rememberCopyToClipboard
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.ui.theme.emphasis
import com.anisync.android.util.AniListUrls
import java.text.DateFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

/** B1's About, the bio in full with the names and details AniList keeps beside it. */
@Composable
fun ProfileAboutContent(
    profile: UserProfile,
    isOwnProfile: Boolean,
    onEditAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (profile.about.isNullOrBlank()) {
            EmptyState(
                icon = Icons.Default.Person,
                title = stringResource(if (isOwnProfile) R.string.profile_about_empty_own else R.string.profile_about_empty),
                description = stringResource(R.string.profile_about_empty_body),
                actionLabel = if (isOwnProfile) stringResource(R.string.profile_about_write) else null,
                actionIcon = if (isOwnProfile) Icons.Default.Edit else null,
                onAction = if (isOwnProfile) onEditAbout else null,
                modifier = Modifier.height(320.dp)
            )
        } else {
            AsyncRichTextRenderer(
                html = profile.about,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        ProfileAboutExtras(profile)
    }
}

/** What AniList keeps beside the bio: the names the profile went by, then its details. */
@Composable
fun ProfileAboutExtras(profile: UserProfile, modifier: Modifier = Modifier, horizontalPadding: Dp = 16.dp) {
    Column(modifier = modifier) {
        if (profile.previousNames.isNotEmpty()) PreviousNames(profile, horizontalPadding)
        AboutDetails(profile, horizontalPadding)
    }
}

@Composable
private fun AboutHeading(title: String, horizontalPadding: Dp, subtitle: String? = null) {
    Row(modifier = Modifier.padding(start = horizontalPadding, end = horizontalPadding, top = 24.dp, bottom = 12.dp)) {
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
private fun PreviousNames(profile: UserProfile, horizontalPadding: Dp) {
    val locale = LocalConfiguration.current.locales[0]
    val latest = profile.previousNames.mapNotNull { it.changedAt }.maxOrNull()
    val changed = latest?.let {
        val month = formatMonthYear(YearMonth.from(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())), locale)
        stringResource(R.string.profile_previous_names_changed, month)
    }
    Column {
        AboutHeading(stringResource(R.string.profile_previous_names), horizontalPadding, changed)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = horizontalPadding)
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
private fun AboutDetails(profile: UserProfile, horizontalPadding: Dp) {
    val context = LocalContext.current
    val copy = rememberCopyToClipboard()
    val clipLabel = stringResource(R.string.profile_username)
    val copiedMessage = stringResource(R.string.profile_username_copied, profile.name)
    val url = AniListUrls.userUrl(profile.name)
    val locale = LocalConfiguration.current.locales[0]
    val dates = remember(locale) { DateFormat.getDateInstance(DateFormat.LONG, locale) }
    Column {
        AboutHeading(stringResource(R.string.profile_details), horizontalPadding)
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(horizontal = horizontalPadding)
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

/**
 * Semi-connected Bio card: a preview bubble above a floating menu-item bar carrying About metadata
 * and Read more / Open. When the bio is empty, only the standalone About bar renders so previous
 * names and profile details stay reachable.
 */
@Composable
internal fun BioPreview(
    about: String?,
    previousNamesCount: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (about.isNullOrBlank()) {
        BioAboutBar(
            previousNamesCount = previousNamesCount,
            truncated = false,
            standalone = true,
            onClick = onOpen,
            modifier = modifier
        )
        return
    }

    // Decided in a single measure pass so the header never draws at one height and settles at
    // another when scrolling back into view.
    SubcomposeLayout(modifier = modifier.fillMaxWidth()) { constraints ->
        val width = constraints.maxWidth
        val gapPx = BioBubbleGap.roundToPx()

        val previewPlaceable = subcompose(BioSlot.Preview) {
            BioPreviewBubble(about = about, onOpen = onOpen)
        }.first().measure(Constraints.fixedWidth(width))

        val isTruncated = previewPlaceable[BioTruncatedLine] == 1

        val footerPlaceable = subcompose(BioSlot.Footer) {
            BioAboutBar(
                previousNamesCount = previousNamesCount,
                truncated = isTruncated,
                standalone = false,
                onClick = onOpen
            )
        }.first().measure(Constraints.fixedWidth(width))

        layout(width, previewPlaceable.height + gapPx + footerPlaceable.height) {
            previewPlaceable.placeRelative(0, 0)
            footerPlaceable.placeRelative(0, previewPlaceable.height + gapPx)
        }
    }
}

private enum class BioSlot { Preview, Footer }

private val BioTruncatedLine = HorizontalAlignmentLine(::minOf)
private val BioPreviewHeight = 128.dp
private val BioFadeHeight = 56.dp
private val BioBubbleGap = 4.dp
private val BioOuterRadius = 20.dp
private val BioInnerRadius = 6.dp
private val BioTopBubbleShape = RoundedCornerShape(
    topStart = BioOuterRadius,
    topEnd = BioOuterRadius,
    bottomStart = BioInnerRadius,
    bottomEnd = BioInnerRadius
)
private val BioBottomBubbleShape = RoundedCornerShape(
    topStart = BioInnerRadius,
    topEnd = BioInnerRadius,
    bottomStart = BioOuterRadius,
    bottomEnd = BioOuterRadius
)
private val BioStandaloneShape = RoundedCornerShape(BioOuterRadius)

@Composable
private fun BioPreviewBubble(about: String, onOpen: () -> Unit) {
    val container = MaterialTheme.colorScheme.surfaceContainerLow
    val haptic = rememberHapticFeedback()
    Layout(
        contents = listOf(
            { AsyncRichTextRenderer(html = about, modifier = Modifier.fillMaxWidth()) },
            {
                Box(
                    modifier = Modifier.background(
                        Brush.verticalGradient(listOf(container.copy(alpha = 0f), container))
                    )
                )
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(BioTopBubbleShape)
            .background(container)
            .clickable(role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onOpen()
            }
    ) { (bodyMeasurables, fadeMeasurables), constraints ->
        val width = constraints.maxWidth
        val horizontalInset = 16.dp.roundToPx()
        val verticalInset = 14.dp.roundToPx()
        val maxHeight = BioPreviewHeight.roundToPx()

        val body = bodyMeasurables.first().measure(
            Constraints(maxWidth = (width - 2 * horizontalInset).coerceAtLeast(0))
        )
        val naturalHeight = body.height + 2 * verticalInset
        val truncated = naturalHeight > maxHeight
        val height = if (truncated) maxHeight else naturalHeight
        val fade = if (truncated) {
            fadeMeasurables.first().measure(Constraints.fixed(width, BioFadeHeight.roundToPx()))
        } else {
            null
        }

        layout(width, height, alignmentLines = mapOf(BioTruncatedLine to if (truncated) 1 else 0)) {
            body.placeRelative(horizontalInset, verticalInset)
            fade?.placeRelative(0, height - fade.height)
        }
    }
}

@Composable
private fun BioAboutBar(
    previousNamesCount: Int,
    truncated: Boolean,
    standalone: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    val aboutLabel = stringResource(R.string.profile_about_label)
    val subtitle = if (previousNamesCount > 0) {
        pluralStringResourceOrDefault(R.plurals.profile_previous_names_count, previousNamesCount, previousNamesCount)
    } else {
        stringResource(R.string.profile_bio_details)
    }
    val actionLabel = stringResource(if (truncated) R.string.profile_read_more else R.string.profile_bio_open)
    val titleStyle = MaterialTheme.typography.labelLarge
    val metaStyle = MaterialTheme.typography.bodyMedium
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = if (standalone) BioStandaloneShape else BioBottomBubbleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(titleStyle.toSpanStyle().copy(color = onSurface)) {
                            append(aboutLabel)
                        }
                        withStyle(metaStyle.toSpanStyle().copy(color = onSurfaceVariant)) {
                            append("  ·  ")
                            append(subtitle)
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge.emphasis(),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
