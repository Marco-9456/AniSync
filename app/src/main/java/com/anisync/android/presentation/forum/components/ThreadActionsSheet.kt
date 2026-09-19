package com.anisync.android.presentation.forum.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.anisync.android.R
import com.anisync.android.domain.ForumThread
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.components.formatRelativeTimeSeconds
import com.anisync.android.presentation.util.bouncyClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalResources

/**
 * Per-thread actions, in the shape the Feed's post sheet already set: an identifying header so it
 * is obvious which thread is about to change, then one row per action.
 *
 * Every row maps to something the app can already do. Editing and deleting are deliberately absent
 * — they live on the thread screen, where the viewer's ownership is already resolved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadActionsSheet(
    thread: ForumThread,
    isSaved: Boolean,
    onSubscribe: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val url = thread.siteUrl ?: "https://anilist.co/forum/thread/${thread.id}"

    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                UserAvatar(
                    url = thread.authorAvatarUrl,
                    contentDescription = null,
                    size = 40.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = thread.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            thread.categories.take(2).forEach { append(it.name).append(" · ") }
                            append(thread.authorName)
                            append(" · ")
                            append(
                                formatRelativeTimeSeconds(
                                    LocalResources.current,
                                    thread.createdAt
                                )
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
            Spacer(Modifier.height(8.dp))

            SheetAction(
                icon = if (thread.isSubscribed) {
                    Icons.Filled.Notifications
                } else {
                    Icons.Outlined.NotificationsNone
                },
                label = stringResource(
                    if (thread.isSubscribed) R.string.forum_action_unsubscribe
                    else R.string.forum_action_subscribe
                ),
                subtitle = stringResource(R.string.forum_action_subscribe_sub),
                onClick = { onSubscribe(); onDismiss() }
            )
            SheetAction(
                icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                label = stringResource(
                    if (isSaved) R.string.forum_action_unsave else R.string.forum_action_save
                ),
                subtitle = stringResource(R.string.forum_action_save_sub),
                onClick = { onSave(); onDismiss() }
            )
            SheetAction(
                icon = Icons.Default.Share,
                label = stringResource(R.string.forum_action_share),
                subtitle = null,
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                    onDismiss()
                }
            )
            SheetAction(
                icon = Icons.Default.ContentCopy,
                label = stringResource(R.string.forum_action_copy_link),
                subtitle = null,
                onClick = {
                    context.getSystemService<ClipboardManager>()
                        ?.setPrimaryClip(ClipData.newPlainText(thread.title, url))
                    onDismiss()
                }
            )
            SheetAction(
                icon = Icons.Default.Link,
                label = stringResource(R.string.forum_action_open_anilist),
                subtitle = stringResource(R.string.forum_action_open_anilist_sub),
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    onDismiss()
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(
                onClick = onClick,
                onClickLabel = label,
                clipShape = RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
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
