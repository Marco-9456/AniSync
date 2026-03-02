package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.domain.ForumThread
import com.anisync.android.presentation.components.AniListHtmlRenderer
import com.anisync.android.presentation.components.AnimatedFavoriteButton
import com.anisync.android.presentation.forum.components.shared.AuthorRow
import com.anisync.android.presentation.forum.components.shared.StatBadge

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThreadHeaderItem(
    thread: ForumThread,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // We use a bottom background color that matches the comment tree area (surfaceContainerLowest)
    // to allow the header to look like an elevated, rounded sheet resting on top.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            // MD3 Expressive: Massive bottom rounded corners for a "sheet" effect
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    top = 24.dp,
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 24.dp
                )
            ) {
                // Social Media Style header row
                AuthorRow(
                    name = thread.authorName,
                    avatarUrl = thread.authorAvatarUrl,
                    timestampSeconds = thread.createdAt,
                    avatarSize = 44.dp // Slightly larger for expressive avatar presence
                )

                Spacer(Modifier.height(20.dp))

                // Expressive Typography: Black weight, larger display feel
                Text(
                    text = thread.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = MaterialTheme.typography.headlineMedium.lineHeight * 1.1f
                )

                if (thread.categories.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        thread.categories.forEach { cat ->
                            Surface(
                                // Pill shapes are a staple of MD3 expressive
                                shape = RoundedCornerShape(100),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ) {
                                Text(
                                    text = cat.name.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                if (thread.isLocked) {
                    Surface(
                        shape = RoundedCornerShape(24.dp), // Friendlier, large radius
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(100),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = "This thread is locked and cannot receive new replies.",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                // High legibility body segment
                thread.body?.let { body ->
                    AniListHtmlRenderer(
                        html = body,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4f
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(32.dp))

                // Expressive Action Row: A floating tonal pill holding all stats
                Surface(
                    shape = RoundedCornerShape(100),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            StatBadge(
                                icon = Icons.Default.ChatBubbleOutline,
                                value = thread.replyCount,
                                contentDescription = "${thread.replyCount} replies"
                            )
                            StatBadge(
                                icon = Icons.Default.RemoveRedEye,
                                value = thread.viewCount,
                                contentDescription = "${thread.viewCount} views"
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(100))
                                .background(
                                    if (thread.isLiked) MaterialTheme.colorScheme.errorContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            AnimatedFavoriteButton(
                                isFavorite = thread.isLiked,
                                onClick = onLikeClick,
                                iconSize = 22.dp
                            )
                            Text(
                                text = thread.likeCount.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (thread.isLiked) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}