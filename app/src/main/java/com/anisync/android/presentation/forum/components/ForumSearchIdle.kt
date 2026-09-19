package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.ForumThread
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.components.formatRelativeTimeSeconds
import com.anisync.android.presentation.forum.components.shared.formatCount
import com.anisync.android.presentation.forum.forumCategoryTabs
import com.anisync.android.presentation.util.bouncyClickable

/**
 * What the search screen shows before anything is typed.
 *
 * The shipped screen put one grey sentence in the middle of an empty window. The categories were
 * already behind a sheet of seventeen full-width pills, so they move here as a wrapped chip grid
 * and the hot list fills the rest. Both are data the screen already had.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ForumSearchIdle(
    trending: List<ForumThread>,
    onCategoryClick: (Int?) -> Unit,
    onThreadClick: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        SectionHeader(
            title = stringResource(R.string.forum_browse_by_category),
            level = HeaderLevel.Section
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            forumCategoryTabs.filter { it.id != null }.forEach { tab ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape,
                    modifier = Modifier
                        .height(36.dp)
                        .bouncyClickable(
                            onClick = { onCategoryClick(tab.id) },
                            clipShape = CircleShape
                        )
                ) {
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
        }

        if (trending.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SectionHeader(
                title = stringResource(R.string.forum_trending_discussions),
                level = HeaderLevel.Section
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                trending.forEach { thread ->
                    TrendingRow(
                        thread = thread,
                        onClick = { onThreadClick(thread.id, thread.title) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendingRow(thread: ForumThread, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onClick, clipShape = RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = thread.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    thread.categories.firstOrNull()?.let { category ->
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = stringResource(
                            R.string.forum_replies_count,
                            thread.replyCount.formatCount()
                        ) + " · " + formatRelativeTimeSeconds(
                            LocalResources.current,
                            thread.repliedAt ?: thread.createdAt
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            UserAvatar(
                url = thread.authorAvatarUrl,
                contentDescription = null,
                size = 24.dp
            )
        }
    }
}
