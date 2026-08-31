package com.anisync.android.presentation.discover.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.MediaTypeToggle
import com.anisync.android.presentation.components.MediaTypeToggleHeight
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.type.MediaType

/** The browse entry points, which are not the same on both tabs. */
enum class BrowseChip {
    SEASONAL,
    SCHEDULE,
    STUDIOS,
    GENRES,
    TOP_100,
    MANHWA,
    MANHUA,
    LIGHT_NOVELS
}

/**
 * Seasonal, Schedule and Studios describe anime and nothing else: manga has no season, no airing
 * schedule, and AniList's studio search returns animation studios. The Manga tab trades them for
 * the axes a manga reader actually browses by, both of which the search filters already model
 * (`countryOfOrigin` and `format`).
 */
fun browseChipsFor(type: MediaType): List<BrowseChip> = if (type == MediaType.ANIME) {
    listOf(
        BrowseChip.SEASONAL,
        BrowseChip.GENRES,
        BrowseChip.TOP_100,
        BrowseChip.SCHEDULE,
        BrowseChip.STUDIOS
    )
} else {
    listOf(
        BrowseChip.GENRES,
        BrowseChip.MANHWA,
        BrowseChip.MANHUA,
        BrowseChip.LIGHT_NOVELS,
        BrowseChip.TOP_100
    )
}

/**
 * The row under the search bar: an icon-only Anime/Manga switch, then the browse chips.
 *
 * The old screen spent a whole 40dp row plus its padding on a two-way toggle and left every
 * browse axis buried in the search overlay's filter sheets. This carries both in one 34dp rail,
 * matching the shape the Library screen already uses for its own list rail.
 */
@Composable
fun DiscoverBrowseRail(
    mediaType: MediaType,
    onMediaTypeChange: (MediaType) -> Unit,
    onChipClick: (BrowseChip) -> Unit,
    modifier: Modifier = Modifier
) {
    val background = MaterialTheme.colorScheme.background
    Row(
        modifier = modifier.fillMaxWidth().height(MediaTypeToggleHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MediaTypeToggle(
            selected = mediaType,
            onSelect = onMediaTypeChange,
            modifier = Modifier.padding(start = 16.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(end = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(8.dp))
                browseChipsFor(mediaType).forEach { chip ->
                    BrowseChipItem(chip = chip, onClick = { onChipClick(chip) })
                }
            }
            // The chips run under the toggle and off the right edge; both ends get a fade so the
            // rail reads as scrollable rather than clipped.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(8.dp)
                    .height(MediaTypeToggleHeight)
                    .background(Brush.horizontalGradient(listOf(background, Color.Transparent)))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(32.dp)
                    .height(MediaTypeToggleHeight)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, background)))
            )
        }
    }
}

@Composable
private fun BrowseChipItem(chip: BrowseChip, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val label = stringResource(chip.labelRes())
    Row(
        modifier = Modifier
            .height(MediaTypeToggleHeight)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .bouncyClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = label,
                clipShape = shape
            )
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = remember(chip) { chip.icon() },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

private fun BrowseChip.labelRes(): Int = when (this) {
    BrowseChip.SEASONAL -> R.string.discover_browse_seasonal
    BrowseChip.SCHEDULE -> R.string.discover_browse_schedule
    BrowseChip.STUDIOS -> R.string.discover_browse_studios
    BrowseChip.GENRES -> R.string.discover_browse_genres
    BrowseChip.TOP_100 -> R.string.discover_browse_top_100
    BrowseChip.MANHWA -> R.string.discover_browse_manhwa
    BrowseChip.MANHUA -> R.string.discover_browse_manhua
    BrowseChip.LIGHT_NOVELS -> R.string.discover_browse_light_novels
}

private fun BrowseChip.icon(): ImageVector = when (this) {
    BrowseChip.SEASONAL -> Icons.Default.CalendarMonth
    BrowseChip.SCHEDULE -> Icons.Default.Schedule
    BrowseChip.STUDIOS -> Icons.Default.Domain
    BrowseChip.GENRES -> Icons.Default.LocalOffer
    BrowseChip.TOP_100 -> Icons.Default.FormatListNumbered
    BrowseChip.MANHWA, BrowseChip.MANHUA -> Icons.Default.Public
    BrowseChip.LIGHT_NOVELS -> Icons.AutoMirrored.Filled.MenuBook
}
