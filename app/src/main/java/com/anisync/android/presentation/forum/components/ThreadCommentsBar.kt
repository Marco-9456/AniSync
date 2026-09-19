package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.forum.components.shared.formatCount
import com.anisync.android.presentation.util.bouncyClickable

private val OuterCorner = 17.dp
private val SeamCorner = 7.dp

/**
 * How many comments there are, where you are in them, and how they are ordered — one 56dp row.
 *
 * The shipped screen spread this over five pieces of furniture stacked to roughly a quarter of a
 * phone screen: a "Comments" section heading, two borderless filter chips that were invisible when
 * unselected, a comment count, a page indicator and a Jump button. Two of them said the same thing,
 * and the Jump button sat where the reply button covered it.
 *
 * [commentsOnPage] counts what is loaded below, replies included. The thread's lifetime total is
 * already implied by "of 284 pages", and printing it here described a list the user was not
 * looking at.
 */
@Composable
fun ThreadCommentsBar(
    commentsOnPage: Int,
    currentPage: Int,
    lastPage: Int,
    isOldestFirst: Boolean,
    onJumpToPage: () -> Unit,
    onSortChange: (oldestFirst: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (lastPage > 1) {
                            Modifier
                                .bouncyClickable(
                                    onClick = onJumpToPage,
                                    role = Role.Button,
                                    clipShape = RoundedCornerShape(10.dp)
                                )
                                .padding(vertical = 4.dp)
                        } else {
                            Modifier
                        }
                    )
            ) {
                Text(
                    text = stringResource(
                        R.string.forum_comments_count_short,
                        commentsOnPage.formatCount()
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (lastPage > 1) {
                    Text(
                        text = stringResource(R.string.forum_page_of, currentPage, lastPage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                SortSegment(
                    label = stringResource(R.string.forum_sort_oldest_short),
                    selected = isOldestFirst,
                    leading = true,
                    onClick = { onSortChange(true) }
                )
                SortSegment(
                    label = stringResource(R.string.forum_sort_newest_short),
                    selected = !isOldestFirst,
                    leading = false,
                    onClick = { onSortChange(false) }
                )
            }
        }
    }
}

/**
 * The same connected-group shapes the rest of the app uses for a binary choice, at the smaller
 * height this row needs. Two borderless chips 12dp apart read as two independent filters.
 */
@Composable
private fun SortSegment(
    label: String,
    selected: Boolean,
    leading: Boolean,
    onClick: () -> Unit
) {
    val shape: Shape = when {
        selected -> CircleShape
        leading -> RoundedCornerShape(
            topStart = OuterCorner,
            bottomStart = OuterCorner,
            topEnd = SeamCorner,
            bottomEnd = SeamCorner
        )

        else -> RoundedCornerShape(
            topStart = SeamCorner,
            bottomStart = SeamCorner,
            topEnd = OuterCorner,
            bottomEnd = OuterCorner
        )
    }
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        shape = shape,
        modifier = Modifier
            .height(34.dp)
            .bouncyClickable(onClick = onClick, role = Role.Tab, clipShape = shape)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
