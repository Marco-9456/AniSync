package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.CommentNode
import com.anisync.android.ui.theme.LocalAvatarShape

/**
 * Breadcrumb strip showing the drill-down navigation chain.
 * Each entry in [breadcrumbs] represents a comment the user "drilled into".
 * Tapping the back arrow pops the last entry; tapping a breadcrumb navigates to that level.
 */
@Composable
fun FoldedAncestorStrip(
    breadcrumbs: List<CommentNode>,
    onNavigateBack: () -> Unit,
    onNavigateToLevel: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val threadDepthDescription = stringResource(R.string.cd_thread_depth, breadcrumbs.size)
    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .semantics {
                contentDescription = threadDepthDescription
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Back button
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.cd_go_back_one_level),
                modifier = Modifier
                    .size(28.dp)
                    .clip(LocalAvatarShape.current)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = LocalAvatarShape.current
                    )
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onNavigateBack)
                    .padding(4.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Breadcrumb trail
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // "Root" entry — tapping goes back to full view
                Text(
                    text = stringResource(R.string.all),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onNavigateToLevel(0) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )

                breadcrumbs.forEachIndexed { index, comment ->
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )

                    val isLast = index == breadcrumbs.lastIndex
                    Text(
                        text = comment.authorName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                        color = if (isLast) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .then(
                                if (!isLast) Modifier.clickable {
                                    onNavigateToLevel(index + 1)
                                } else Modifier
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            // Depth level badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "×${breadcrumbs.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
