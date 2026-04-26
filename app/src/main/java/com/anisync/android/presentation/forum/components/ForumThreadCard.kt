package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
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
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 1. HEADER: Author, Post Time, Badges, and Top Actions
            ThreadHeader(
                thread = thread,
                isSaved = isSaved,
                onSaveClick = onSaveClick,
                isSubscribed = isSubscribed,
                onSubscribeClick = onSubscribeClick,
                onUserClick = onUserClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. BODY: Title, Tags, and Media Cover
            ThreadBody(thread = thread)

            Spacer(modifier = Modifier.height(16.dp))

            // 3. FOOTER: Last Reply info & Engagement Stats
            ThreadFooter(
                thread = thread,
                onUserClick = onUserClick
            )
        }
    }
}

@Composable
private fun ThreadHeader(
    thread: ForumThread,
    isSaved: Boolean,
    onSaveClick: (() -> Unit)?,
    isSubscribed: Boolean,
    onSubscribeClick: (() -> Unit)?,
    onUserClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Author Avatar
        UserAvatar(
            url = thread.authorAvatarUrl,
            contentDescription = thread.authorName,
            size = 36.dp,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onUserClick(thread.authorName) }
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Author Info & Status Badges
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = thread.authorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clickable { onUserClick(thread.authorName) }
                )

                // Contextual Badges
                if (thread.isSticky) {
                    StatusBadge(
                        icon = Icons.Default.PushPin,
                        text = "Pinned",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                if (thread.isLocked) {
                    StatusBadge(
                        icon = Icons.Default.Lock,
                        text = "Locked",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Text(
                text = formatRelativeTimeSeconds(thread.createdAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Quick Actions
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (onSubscribeClick != null) {
                ActionIconButton(
                    icon = if (isSubscribed) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone,
                    contentDescription = if (isSubscribed) "Unsubscribe" else "Subscribe",
                    isActive = isSubscribed,
                    activeColor = MaterialTheme.colorScheme.primary,
                    onClick = onSubscribeClick
                )
            }
            if (onSaveClick != null) {
                ActionIconButton(
                    icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isSaved) "Unsave" else "Save",
                    isActive = isSaved,
                    activeColor = MaterialTheme.colorScheme.primary,
                    onClick = onSaveClick
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThreadBody(thread: ForumThread) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Main Title
            Text(
                text = thread.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Categories / Tags
            if (thread.categories.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    thread.categories.forEach { category ->
                        CategoryChip(name = category.name)
                    }
                }
            }
        }

        // Optional Media Cover Thumbnail
        if (thread.mediaCoverUrl != null) {
            Spacer(Modifier.width(16.dp))
            AsyncImage(
                model = thread.mediaCoverUrl,
                contentDescription = thread.mediaTitle ?: "Media cover",
                modifier = Modifier
                    .width(84.dp)
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun ThreadFooter(
    thread: ForumThread,
    onUserClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Last Reply Section (Collapses gracefully on small screens)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (thread.replyUserName != null && thread.repliedAt != null) {
                Surface(
                    onClick = { onUserClick(thread.replyUserName!!) },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        UserAvatar(
                            url = thread.replyUserAvatarUrl,
                            contentDescription = thread.replyUserName,
                            size = 18.dp,
                            modifier = Modifier.clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Last by ${thread.replyUserName} • ${
                                formatRelativeTimeSeconds(
                                    thread.repliedAt
                                )
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Engagement Metrics
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                icon = Icons.Outlined.ChatBubbleOutline,
                value = thread.replyCount
            )
            StatItem(
                icon = if (thread.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                value = thread.likeCount,
                tint = if (thread.isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            StatItem(
                icon = Icons.Outlined.RemoveRedEye,
                value = thread.viewCount
            )
        }
    }
}

// ============================================================================
// --- Reusable Micro-Components ---
// ============================================================================

@Composable
private fun StatusBadge(
    icon: ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(containerColor)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(bottom = 1.dp) // Optical alignment
        )
    }
}

@Composable
private fun ActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun CategoryChip(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: Int,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = formatStatValue(value),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun UserAvatar(
    url: String?,
    contentDescription: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(size * 0.7f)
            )
        }
    }
}

// ============================================================================
// --- Enhanced Skeleton Loader ---
// ============================================================================

@Composable
fun ForumThreadCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Skeleton
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBox(width = 36.dp, height = 36.dp, shape = CircleShape)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonBox(width = 100.dp, height = 14.dp)
                    Spacer(Modifier.height(8.dp))
                    SkeletonBox(width = 60.dp, height = 10.dp)
                }
                SkeletonBox(width = 32.dp, height = 32.dp, shape = CircleShape)
            }

            Spacer(Modifier.height(16.dp))

            // Body Skeleton
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonBox(
                        width = Dp.Infinity,
                        height = 20.dp,
                        modifier = Modifier.fillMaxWidth(0.95f)
                    )
                    Spacer(Modifier.height(8.dp))
                    SkeletonBox(
                        width = Dp.Infinity,
                        height = 20.dp,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    )

                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonBox(width = 70.dp, height = 24.dp, shape = RoundedCornerShape(8.dp))
                        SkeletonBox(width = 90.dp, height = 24.dp, shape = RoundedCornerShape(8.dp))
                    }
                }

                Spacer(Modifier.width(16.dp))
                // Thumbnail Skeleton
                SkeletonBox(width = 84.dp, height = 112.dp, shape = RoundedCornerShape(12.dp))
            }

            Spacer(Modifier.height(20.dp))

            // Footer Skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Last reply skeleton
                SkeletonBox(width = 160.dp, height = 26.dp, shape = RoundedCornerShape(50))

                // Stats skeleton
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SkeletonBox(width = 36.dp, height = 16.dp)
                    SkeletonBox(width = 36.dp, height = 16.dp)
                    SkeletonBox(width = 36.dp, height = 16.dp)
                }
            }
        }
    }
}

@Composable
private fun SkeletonBox(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(percent = 50)
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    )
}

// ============================================================================
// --- Domain Mocks & Helpers ---
// ============================================================================

data class ForumThread(
    val id: Int,
    val title: String,
    val body: String?,
    val authorId: Int,
    val authorName: String,
    val authorAvatarUrl: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val replyUserName: String? = null,
    val replyUserAvatarUrl: String? = null,
    val repliedAt: Long? = null,
    val replyCount: Int,
    val likeCount: Int,
    val viewCount: Int,
    val isLiked: Boolean,
    val isSubscribed: Boolean,
    val isLocked: Boolean,
    val isSticky: Boolean = false,
    val siteUrl: String?,
    val mediaTitle: String? = null,
    val mediaCoverUrl: String? = null,
    val categories: List<ForumCategory> = emptyList()
)

data class ForumCategory(
    val id: Int,
    val name: String
)

private fun formatRelativeTimeSeconds(timestampSeconds: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestampSeconds
    return when {
        diff < 60 -> "just now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        diff < 604800 -> "${diff / 86400}d ago"
        diff < 2592000 -> "${diff / 604800}w ago"
        diff < 31536000 -> "${diff / 2592000}mo ago"
        else -> "${diff / 31536000}y ago"
    }
}

private fun formatStatValue(value: Int): String {
    return when {
        value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format("%.1fk", value / 1_000.0)
        else -> value.toString()
    }
}

// ============================================================================
// --- Jetpack Compose Previews ---
// ============================================================================

@Preview(showBackground = true, backgroundColor = 0xFFF3F4F6)
@Composable
private fun PreviewForumThreadCard_Normal() {
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
                    createdAt = System.currentTimeMillis() / 1000 - 3600, // 1h ago
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
private fun PreviewForumThreadCard_StickyAndMedia() {
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
                    createdAt = System.currentTimeMillis() / 1000 - 86400 * 2, // 2d ago
                    updatedAt = 1680000000,
                    isSticky = true,
                    isLocked = true,
                    isSubscribed = true,
                    siteUrl = null,
                    replyUserName = "GojoFan99",
                    repliedAt = System.currentTimeMillis() / 1000 - 300, // 5m ago
                    replyCount = 8900,
                    likeCount = 3450,
                    isLiked = true,
                    viewCount = 150000,
                    mediaCoverUrl = "https://example.com/mock.jpg",
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
private fun PreviewForumThreadCard_Skeleton() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            ForumThreadCardSkeleton()
        }
    }
}