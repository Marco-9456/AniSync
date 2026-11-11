package com.anisync.android.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.LibraryPriority
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.presentation.util.toIconRes
import com.anisync.android.presentation.util.toLabel

/** One cell of a [CoverBadgeRibbon]. */
@Immutable
data class CoverBadge(
    @DrawableRes val icon: Int,
    val container: Color,
    val content: Color,
    val contentDescription: String?,
    /** A count printed after the icon, such as how many times a title was rewatched. */
    val label: String? = null
)

/**
 * The stack of marks welded into a cover's top-left corner.
 *
 * Only the last cell rounds its bottom-end corner; anything above it stays square, so two marks
 * fuse into one shape instead of reading as two stickers stuck on the artwork. Stating the rule by
 * position rather than by badge means a third mark later inherits it without touching this.
 *
 * Order is fixed and not by importance: notes has been on this corner since #75 and moving it would
 * shift a mark people already read.
 */
@Composable
fun CoverBadgeRibbon(
    badges: List<CoverBadge>,
    modifier: Modifier = Modifier,
    iconSize: Dp = 14.dp,
    cellPadding: Dp = 4.dp
) {
    if (badges.isEmpty()) return
    // Cells share the widest one's width, so a cell with a count keeps the stack one shape.
    Column(modifier = modifier.width(IntrinsicSize.Max)) {
        badges.forEachIndexed { index, badge ->
            Surface(
                shape = if (index == badges.lastIndex) {
                    RoundedCornerShape(bottomEnd = 8.dp)
                } else {
                    RoundedCornerShape(0.dp)
                },
                color = badge.container,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(cellPadding)
                        .then(
                            if (badge.label != null && badge.contentDescription != null) {
                                Modifier.clearAndSetSemantics { contentDescription = badge.contentDescription }
                            } else {
                                Modifier
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(badge.icon),
                        contentDescription = badge.contentDescription.takeIf { badge.label == null },
                        tint = badge.content,
                        modifier = Modifier.size(iconSize)
                    )
                    if (badge.label != null) {
                        Text(
                            text = badge.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badge.content,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/** The notes mark: this entry carries a note worth spotting without opening it (#75). */
@Composable
fun notesBadge(): CoverBadge = CoverBadge(
    icon = R.drawable.ic_note_stack_24px,
    container = MaterialTheme.colorScheme.primaryContainer,
    content = MaterialTheme.colorScheme.onPrimaryContainer,
    contentDescription = stringResource(R.string.a11y_has_notes)
)

/**
 * The priority mark, or null for [LibraryPriority.LOW].
 *
 * Low is also what an entry that has never carried a priority reads as, so marking it would tag a
 * whole library and say nothing.
 */
@Composable
fun priorityBadge(priority: LibraryPriority): CoverBadge? {
    if (priority == LibraryPriority.LOW) return null
    val high = priority == LibraryPriority.HIGH
    return CoverBadge(
        icon = priority.toIconRes(),
        container = if (high) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        content = if (high) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        contentDescription = stringResource(R.string.a11y_priority, priority.toLabel())
    )
}

/**
 * The rewatch mark: how many times the title was started over, which AniList's own lists show next
 * to the title. Null for a title never repeated.
 */
@Composable
fun rewatchBadge(rewatches: Int, isManga: Boolean): CoverBadge? {
    if (rewatches <= 0) return null
    return CoverBadge(
        icon = R.drawable.ic_list_repeating,
        container = MaterialTheme.colorScheme.tertiaryContainer,
        content = MaterialTheme.colorScheme.onTertiaryContainer,
        contentDescription = pluralStringResourceOrDefault(
            if (isManga) R.plurals.a11y_reread_count else R.plurals.a11y_rewatch_count,
            rewatches,
            rewatches
        ),
        label = rewatches.toString()
    )
}

/** The ribbon a library cover carries, in fixed order. Empty when there is nothing to say. */
@Composable
fun coverBadges(
    hasNotes: Boolean,
    priority: LibraryPriority? = null,
    rewatches: Int = 0,
    isManga: Boolean = false
): List<CoverBadge> =
    buildList {
        if (hasNotes) add(notesBadge())
        priority?.let { level -> priorityBadge(level)?.let(::add) }
        rewatchBadge(rewatches, isManga)?.let(::add)
    }
