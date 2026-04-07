package com.anisync.android.presentation.forum.components

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.domain.ForumCategory
import com.anisync.android.domain.ForumThread

private const val DEBUG_PERFORMANCE = false

@Composable
fun ForumThreadCard(
    thread: ForumThread,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSaved: Boolean = false,
    onSaveClick: (() -> Unit)? = null,
    isSubscribed: Boolean = false,
    onSubscribeClick: (() -> Unit)? = null,
    onUserClick: (String) -> Unit = {}
) {
    // PERFORMANCE METRICS: Track recomposition counts
    if (DEBUG_PERFORMANCE) {
        val recompositionCount = remember { arrayOf(0) }
        SideEffect {
            recompositionCount[0]++
            Log.d(
                "Performance",
                "ForumThreadCard (ID: ${thread.id}) recomposed ${recompositionCount[0]} times"
            )
        }
    }

    ForumThreadCardContent(
        thread = thread,
        onClick = onClick,
        isSaved = isSaved,
        onSaveClick = onSaveClick,
        isSubscribed = isSubscribed,
        onSubscribeClick = onSubscribeClick,
        onUserClick = onUserClick,
        modifier = modifier
    )
}

@Composable
private fun ForumThreadCardContent(
    thread: ForumThread,
    onClick: () -> Unit,
    isSaved: Boolean,
    onSaveClick: (() -> Unit)?,
    isSubscribed: Boolean,
    onSubscribeClick: (() -> Unit)?,
    onUserClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            // PERFORMANCE NOTE: `animateContentSize` can sometimes cause jank in deep LazyColumns
            // if triggered too often. Kept for UX polish, but worth monitoring on low-end devices.
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Top Row: Author Row + Action Icons
            // This row spans the full width, untouched by the thumbnail
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AuthorRow(
                    name = thread.authorName,
                    avatarUrl = thread.authorAvatarUrl,
                    timestampSeconds = thread.createdAt,
                    modifier = Modifier.weight(1f), // Constrains author row so it doesn't push icons off-screen
                    onUserClick = onUserClick
                )

                // Action Icons Row
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onSubscribeClick != null) {
                        IconButton(onClick = onSubscribeClick) {
                            Icon(
                                imageVector = if (isSubscribed) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone,
                                contentDescription = if (isSubscribed) "Unsubscribe" else "Subscribe",
                                tint = if (isSubscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    if (onSaveClick != null) {
                        IconButton(onClick = onSaveClick) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isSaved) "Unsave" else "Save",
                                tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Middle Row: Title, Last Reply Info, and Image Thumbnail
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Title and Last Reply Column
                Column(modifier = Modifier.weight(1f)) {
                    // Title row with sticky/locked indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        if (thread.isSticky) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = "Pinned",
                                modifier = Modifier
                                    .padding(top = 2.dp, end = 6.dp)
                                    .size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (thread.isLocked) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "Locked",
                                modifier = Modifier
                                    .padding(top = 2.dp, end = 6.dp)
                                    .size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = thread.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Last reply info (Only renders if data is present)
                    if (thread.replyUserName != null && thread.repliedAt != null) {
                        Spacer(Modifier.height(8.dp))
                        AuthorRow(
                            name = "Last reply by ${thread.replyUserName}",
                            avatarUrl = thread.replyUserAvatarUrl,
                            timestampSeconds = thread.repliedAt,
                            avatarSize = 16.dp,
                            modifier = Modifier.padding(start = if (thread.isSticky || thread.isLocked) 24.dp else 0.dp),
                            onUserClick = { onUserClick(thread.replyUserName!!) }
                        )
                    }
                }

                // Media thumbnail
                if (thread.mediaCoverUrl != null) {
                    Spacer(Modifier.width(16.dp))
                    AsyncImage(
                        model = thread.mediaCoverUrl,
                        contentDescription = thread.mediaTitle ?: "Linked media",
                        modifier = Modifier
                            .size(width = 56.dp, height = 80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            // Added a background color so it acts as a visible placeholder skeleton
                            // before the image loads and inside Compose Previews.
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Bottom Row: Stats row & Categories
            // This row also spans the full width now
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatBadge(
                        icon = Icons.Default.ChatBubbleOutline,
                        value = thread.replyCount,
                        contentDescription = "${thread.replyCount} replies"
                    )
                    StatBadge(
                        icon = Icons.Outlined.FavoriteBorder,
                        value = thread.likeCount,
                        contentDescription = "${thread.likeCount} likes",
                        tint = if (thread.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatBadge(
                        icon = Icons.Default.RemoveRedEye,
                        value = thread.viewCount,
                        contentDescription = "${thread.viewCount} views"
                    )
                }

                // Category chips
                if (thread.categories.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        // Switched from weighted children to a horizontally scrollable row.
                        // This guarantees chips will maintain their readable width. If they exceed
                        // the remaining space next to stats, the user can naturally scroll them.
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        thread.categories.take(2).forEach { category ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer.copy(
                                            alpha = 0.6f
                                        )
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ForumThreadCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author Row Skeleton
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonLine(
                    fraction = 0f,
                    height = 24.dp,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                SkeletonLine(fraction = 0.3f, height = 14.dp)
            }
            Spacer(Modifier.height(16.dp))
            // Title Skeleton
            SkeletonLine(fraction = 0.9f, height = 20.dp)
            Spacer(Modifier.height(6.dp))
            SkeletonLine(fraction = 0.6f, height = 20.dp)
            Spacer(Modifier.height(24.dp))
            // Footer Skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SkeletonLine(fraction = 0.4f, height = 16.dp)
                SkeletonLine(fraction = 0.2f, height = 16.dp)
            }
        }
    }
}


// ============================================================================
// --- Dependencies & Mocks (Ensures the file is self-contained & compiles) ---
// ============================================================================

@Composable
fun AuthorRow(
    name: String,
    avatarUrl: String?,
    timestampSeconds: Long,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 24.dp,
    onUserClick: ((String) -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.then(
            if (onUserClick != null) Modifier.clickable { onUserClick(name) } else Modifier
        )
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar of $name",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(avatarSize * 0.7f)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // This weight(1f, fill=false) constraint prevents long names from taking all
            // the horizontal space and squashing the timestamp to 0-width (which caused vertical text stacking).
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(6.dp))
        val formattedTime = remember(timestampSeconds) { formatRelativeTime(timestampSeconds) }
        Text(
            text = "• $formattedTime",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false // Prevents characters from stacking into a vertical block
        )
    }
}

/**
 * Formats a Unix timestamp (in seconds) as a human-readable relative time string.
 */
private fun formatRelativeTime(timestampSeconds: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestampSeconds
    return when {
        diff < 0 -> "just now"
        diff < 60 -> "just now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        diff < 604800 -> "${diff / 86400}d ago"
        diff < 2592000 -> "${diff / 604800}w ago"
        diff < 31536000 -> "${diff / 2592000}mo ago"
        else -> "${diff / 31536000}y ago"
    }
}

@Composable
fun StatBadge(
    icon: ImageVector,
    value: Int,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false // Safely ensuring numbers are forced to stay on a single line
        )
    }
}

@Composable
fun SkeletonLine(fraction: Float, height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(fraction.takeIf { it > 0f } ?: 1f)
            .height(height)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    )
}

// ============================================================================
// --- Jetpack Compose Previews ---
// ============================================================================

@Preview(showBackground = true, backgroundColor = 0xFFF3F4F6)
@Composable
fun PreviewForumThreadCard_Normal() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            ForumThreadCard(
                thread = ForumThread(
                    id = 1,
                    title = "What is your favorite Anime of the Season so far? Let's discuss!",
                    body = null,
                    authorId = 1,
                    authorName = "OtakuSenpai",
                    authorAvatarUrl = null,
                    createdAt = 1680000000,
                    updatedAt = 1680000000,
                    replyCount = 142,
                    likeCount = 56,
                    viewCount = 1205,
                    isLiked = false,
                    isSubscribed = false,
                    isLocked = false,
                    siteUrl = null,
                    categories = listOf(ForumCategory(1, "Discussion"), ForumCategory(2, "Anime"))
                ),
                onClick = {},
                onSaveClick = {},
                onSubscribeClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F4F6)
@Composable
fun PreviewForumThreadCard_StickyAndMedia() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            ForumThreadCard(
                thread = ForumThread(
                    id = 2,
                    title = "[Megathread] Jujutsu Kaisen Season 2 Episode 18 Discussion",
                    body = null,
                    authorId = 0,
                    authorName = "AutoMod",
                    authorAvatarUrl = null,
                    createdAt = 1680000000,
                    updatedAt = 1680000000,
                    isSticky = true,
                    isLocked = true,
                    isSubscribed = false,
                    siteUrl = null,
                    replyUserName = "GojoFan99",
                    repliedAt = 1680001000,
                    replyCount = 890,
                    likeCount = 340,
                    isLiked = true,
                    viewCount = 15000,
                    mediaCoverUrl = "https://example.com/mock.jpg", // Coil will gracefully fail or show placeholder
                    categories = listOf(ForumCategory(3, "Episode"), ForumCategory(4, "Spoilers"))
                ),
                onClick = {},
                isSaved = true,
                onSaveClick = {},
                onSubscribeClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F4F6)
@Composable
fun PreviewForumThreadCard_Skeleton() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            ForumThreadCardSkeleton()
        }
    }
}