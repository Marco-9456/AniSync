package com.anisync.android.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.domain.MediaReview
import com.anisync.android.type.ReviewRating
import com.anisync.android.ui.theme.emphasis
import androidx.compose.ui.res.stringResource
import java.text.DateFormat
import java.util.Date

/** Score → traffic-light colour, shared by every review surface. */
fun reviewScoreColor(score: Int): Color = when {
    score >= 75 -> Color(0xFF4CAF50) // Green
    score >= 50 -> Color(0xFFFFC107) // Amber
    else -> Color(0xFFFF5722)        // Red-orange
}

/** Star + `score/100` on a solid score-coloured pill — overlaid on the review banner. */
@Composable
fun ReviewScorePill(
    score: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(reviewScoreColor(score), RoundedCornerShape(percent = 50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "$score/100",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 13.sp
        )
    }
}

/**
 * The author row shared by [com.anisync.android.presentation.review.ReviewDetailScreen] and the
 * media-details review sheet: a soft container holding the reviewer (avatar + name + date) on the
 * start and a segmented up/down helpful-count pill on the end. The pill is display-only; actual
 * voting lives in [ReviewVoteActions].
 */
@Composable
fun ReviewAuthorBar(
    review: MediaReview,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onUserClick(review.userName) }
                .padding(end = 8.dp)
        ) {
            UserAvatar(
                url = review.userAvatarUrl,
                contentDescription = null,
                size = 40.dp
            )
            // Ellipsize instead of wrapping: in a narrow detail pane the vote pill squeezes this
            // column, and an unconstrained date used to wrap one character per line.
            Column {
                Text(
                    text = review.userName,
                    style = MaterialTheme.typography.titleSmall.emphasis(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val dateStr = remember(review.createdAt) {
                    if (review.createdAt > 0) {
                        DateFormat.getDateInstance().format(Date(review.createdAt * 1000L))
                    } else ""
                }
                if (dateStr.isNotEmpty()) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        ReviewVoteCountPill(
            rating = review.rating,
            ratingAmount = review.ratingAmount
        )
    }
}

/**
 * Segmented up/down helpful-count chip: filled thumb-up + count, a hairline divider, then
 * thumb-down + count. Display-only; shared by the review card footer and [ReviewAuthorBar].
 */
@Composable
fun ReviewVoteCountPill(
    rating: Int,
    ratingAmount: Int,
    modifier: Modifier = Modifier
) {
    val upCount = rating.coerceAtLeast(0)
    val downCount = (ratingAmount - rating).coerceAtLeast(0)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VoteCount(icon = Icons.Filled.ThumbUp, count = upCount)
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(18.dp)
                .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
        )
        VoteCount(icon = Icons.Filled.ThumbDown, count = downCount)
    }
}

@Composable
private fun VoteCount(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int
) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = formatCount(count),
            style = MaterialTheme.typography.labelMedium.emphasis(),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * The two expanding pill buttons pinned to the bottom of a review surface. The user's current vote
 * is reflected by filling the matching pill (primary for helpful, error for not-helpful); tapping
 * the active pill again clears the vote.
 */
@Composable
fun ReviewVoteActions(
    userRating: String?,
    onRate: (ReviewRating) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUpVoted = userRating == "UP_VOTE"
    val isDownVoted = userRating == "DOWN_VOTE"

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VotePill(
                icon = Icons.Filled.ThumbUp,
                contentDescription = stringResource(R.string.cd_like_review),
                selected = isUpVoted,
                container = MaterialTheme.colorScheme.primary,
                onContainer = MaterialTheme.colorScheme.onPrimary,
                onClick = {
                    onRate(if (isUpVoted) ReviewRating.NO_VOTE else ReviewRating.UP_VOTE)
                },
                modifier = Modifier.weight(1f)
            )
            VotePill(
                icon = Icons.Filled.ThumbDown,
                contentDescription = stringResource(R.string.cd_dislike_review),
                selected = isDownVoted,
                container = MaterialTheme.colorScheme.error,
                onContainer = MaterialTheme.colorScheme.onError,
                onClick = {
                    onRate(if (isDownVoted) ReviewRating.NO_VOTE else ReviewRating.DOWN_VOTE)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun VotePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selected: Boolean,
    container: Color,
    onContainer: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) container else MaterialTheme.colorScheme.surfaceContainerHighest
    val fg = if (selected) onContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = fg,
            modifier = Modifier.size(22.dp)
        )
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${(count / 100_000) / 10.0}M"
    count >= 1_000 -> "${(count / 100) / 10.0}k"
    else -> count.toString()
}
