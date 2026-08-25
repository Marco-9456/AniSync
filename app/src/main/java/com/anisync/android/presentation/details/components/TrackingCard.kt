package com.anisync.android.presentation.details.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.domain.ExternalLink
import com.anisync.android.domain.ExternalLinkType
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaDetails
import com.anisync.android.presentation.components.iconRes
import com.anisync.android.presentation.components.toIndicatorKind
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.formatCountdownAdaptive
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.presentation.util.toIcon
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.listIndicatorColor

/** The statuses the quick menu offers, in the order the library tabs use. */
private val QUICK_STATUSES = listOf(
    LibraryStatus.CURRENT,
    LibraryStatus.PLANNING,
    LibraryStatus.COMPLETED,
    LibraryStatus.PAUSED,
    LibraryStatus.DROPPED
)

/**
 * The entry tracker: the one control the details page exists to serve.
 *
 * Replaces the Favourite + Share pair that used to lead the page (both moved to the app bar) and
 * the floating action button that hid list management behind two taps. Status, progress and the
 * increment/decrement pair are the same behaviour the library card already offers, so a title
 * reads and writes the same way from either surface.
 *
 * Off the list it collapses to the two actions that matter: add it, or plan it in one tap.
 */
@Composable
fun TrackingCard(
    details: MediaDetails,
    onStatusSelect: (LibraryStatus) -> Unit,
    onProgressChange: (Int) -> Unit,
    onEditClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isManga = details.type == MediaType.MANGA
    val total = if (isManga) details.chapters else details.episodes

    if (details.listEntryId == null) {
        NotTrackedActions(
            mediaType = details.type,
            onStatusSelect = onStatusSelect,
            modifier = modifier
        )
        return
    }

    val haptic = rememberHapticFeedback()
    val progress = details.listProgress ?: 0
    val status = details.listStatus ?: LibraryStatus.CURRENT

    Surface(
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_extra_large)),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_medium))) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(
                    status = status,
                    mediaType = details.type,
                    onSelect = onStatusSelect,
                    onRemove = onRemoveClick
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (total != null) {
                        stringResource(R.string.details_progress_of, progress, total)
                    } else {
                        stringResource(
                            if (isManga) R.string.details_progress_read
                            else R.string.details_progress_watched,
                            progress
                        )
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_normal)))

            // A determinate bar needs a denominator. Long-running series (One Piece and friends)
            // carry no episode count on AniList, so say so rather than draw a bar against a guess.
            if (total != null && total > 0) {
                val animated by animateFloatAsState(
                    targetValue = (progress.toFloat() / total).coerceIn(0f, 1f),
                    label = "TrackingProgress"
                )
                LinearProgressIndicator(
                    progress = { animated },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            } else {
                Text(
                    text = stringResource(
                        if (isManga) R.string.details_no_chapter_total
                        else R.string.details_no_episode_total
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_normal)))

            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))) {
                StepperButton(
                    icon = Icons.Default.Remove,
                    label = stringResource(R.string.a11y_action_decrement_progress),
                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                    content = MaterialTheme.colorScheme.onSurface,
                    enabled = progress > 0,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onProgressChange(progress - 1)
                    }
                )
                StepperButton(
                    icon = Icons.Default.Add,
                    label = stringResource(R.string.a11y_action_increment_progress),
                    container = MaterialTheme.colorScheme.primary,
                    content = MaterialTheme.colorScheme.onPrimary,
                    enabled = total == null || progress < total,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onProgressChange(progress + 1)
                    }
                )
                Surface(
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large)),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .bouncyClickable(
                            onClick = onEditClick,
                            role = Role.Button,
                            clipShape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
                        Text(
                            text = stringResource(R.string.edit_entry),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotTrackedActions(
    mediaType: MediaType?,
    onStatusSelect: (LibraryStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_normal))
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Button(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
                Text(
                    text = stringResource(R.string.details_add_to_list),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            // The same menu the status pill opens. Adding a title is picking a list, so it should
            // cost one menu, not the whole edit sheet with scores, dates and rewatch counts.
            ListStatusMenu(
                expanded = expanded,
                onDismiss = { expanded = false },
                selected = null,
                mediaType = mediaType,
                onSelect = onStatusSelect
            )
        }
        OutlinedButton(
            onClick = { onStatusSelect(LibraryStatus.PLANNING) },
            modifier = Modifier.height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
            Text(
                text = stringResource(R.string.details_quick_plan),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * The status chip and the menu it opens. Anchoring the five statuses to the control they change
 * is what lets the floating action button go: it carried the same list, three taps away from the
 * value it was describing.
 */
@Composable
private fun StatusPill(
    status: LibraryStatus,
    mediaType: MediaType?,
    onSelect: (LibraryStatus) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = listIndicatorColor(status.toIndicatorKind())
    val haptic = rememberHapticFeedback()

    Box {
        Surface(
            shape = RoundedCornerShape(15.dp),
            color = colors.container,
            modifier = Modifier.bouncyClickable(
                onClick = { expanded = true },
                role = Role.Button,
                onClickLabel = stringResource(R.string.cd_change_list_status),
                clipShape = RoundedCornerShape(15.dp)
            )
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.toLabel(mediaType),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.content
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_tiny)))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.content
                )
            }
        }

        ListStatusMenu(
            expanded = expanded,
            onDismiss = { expanded = false },
            selected = status,
            mediaType = mediaType,
            onSelect = onSelect,
            onRemove = onRemove
        )
    }
}

/**
 * The list picker, shared by the status pill and the Add to list button.
 *
 * Built from the Material 3 expressive menu API rather than a hand-rolled popup: a grouped
 * container, per-position item shapes (rounder at the ends of the group, tighter between them) and
 * the component's own selected treatment. Only the palette is ours, so the selected row carries the
 * list's colour pair, the same one the library indicator uses, and names the list by hue as well as
 * by word.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ListStatusMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    selected: LibraryStatus?,
    mediaType: MediaType?,
    onSelect: (LibraryStatus) -> Unit,
    onRemove: (() -> Unit)? = null
) {
    val haptic = rememberHapticFeedback()

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        // The group is the surface in an expressive menu, so the popup behind it gets out of the
        // way. Left opaque, its 4dp corners show through the group's rounder ones.
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        val groupCount = if (onRemove != null) 2 else 1
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = groupCount)) {
            QUICK_STATUSES.forEachIndexed { index, option ->
                val isSelected = option == selected
                val listColors = listIndicatorColor(option.toIndicatorKind())
                DropdownMenuItem(
                    selected = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onDismiss()
                        onSelect(option)
                    },
                    text = { Text(option.toLabel(mediaType)) },
                    shapes = MenuDefaults.itemShape(index, QUICK_STATUSES.size),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(option.toIndicatorKind().iconRes()),
                            contentDescription = null,
                            modifier = Modifier.size(MenuDefaults.LeadingIconSize)
                        )
                    },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(MenuDefaults.TrailingIconSize)
                            )
                        }
                    } else null,
                    colors = MenuDefaults.selectableItemColors(
                        selectedContainerColor = listColors.container,
                        selectedTextColor = listColors.content,
                        selectedLeadingIconColor = listColors.content,
                        selectedTrailingIconColor = listColors.content
                    )
                )
            }
        }

        // Its own group rather than a divider: leaving the list is a different kind of act from
        // moving between lists, and the gap says so without a rule across the menu.
        if (onRemove != null) {
            Spacer(modifier = Modifier.height(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 1, count = 2)) {
                DropdownMenuItem(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onDismiss()
                        onRemove()
                    },
                    text = { Text(stringResource(R.string.details_remove_from_list)) },
                    shape = MenuDefaults.standaloneItemShape,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(MenuDefaults.LeadingIconSize)
                        )
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.error,
                        leadingIconColor = MaterialTheme.colorScheme.error
                    )
                )
            }
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
    Surface(
        shape = shape,
        color = if (enabled) container else container.copy(alpha = 0.4f),
        modifier = Modifier
            .width(68.dp)
            .height(44.dp)
            .then(
                if (enabled) {
                    Modifier.bouncyClickable(
                        onClick = onClick,
                        role = Role.Button,
                        onClickLabel = label,
                        clipShape = shape
                    )
                } else Modifier
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) content else content.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * Countdown to the next episode, lifted out of the Information carousel where it could be parked
 * off screen behind a horizontal scroll. It answers the first question a viewer of an airing
 * series has, so it sits directly under the tracker.
 */
@Composable
fun NextEpisodeStrip(
    details: MediaDetails,
    modifier: Modifier = Modifier
) {
    val airing = details.nextAiringEpisode ?: return
    val remaining = rememberLiveCountdownSeconds(airing.airingAt, airing.timeUntilAiring)
    if (remaining <= 0) return

    Surface(
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large)),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_normal)))
            Text(
                text = stringResource(
                    R.string.details_next_episode_in,
                    airing.episode,
                    formatCountdownAdaptive(remaining)
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Default,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Streaming links, above the fold. They used to be the last block of the Overview tab, which put
 * "where do I watch this" behind the whole page.
 */
@Composable
fun WatchOnRow(
    externalLinks: List<ExternalLink>,
    mediaType: MediaType?,
    modifier: Modifier = Modifier
) {
    val streaming = remember(externalLinks) {
        externalLinks.filter { it.type == ExternalLinkType.STREAMING }
    }
    if (streaming.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            text = stringResource(
                if (mediaType == MediaType.MANGA) R.string.subsection_reading
                else R.string.subsection_streaming
            ),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_large))
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = dimensionResource(R.dimen.spacing_large)
            ),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
        ) {
            items(streaming.size, key = { streaming[it].id }) { index ->
                ExternalLinkChip(streaming[index])
            }
        }
    }
}
