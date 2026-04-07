package com.anisync.android.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.UserActivity
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.profile.util.formatProfileRelativeTime

/**
 * Redesigned Expressive Updates Section.
 * Ditches the traditional dot-timeline for bold, chunky, asymmetrical cards.
 */
@Composable
fun RecentUpdatesSection(
    activities: List<UserActivity>,
    modifier: Modifier = Modifier,
    onUserClick: (String) -> Unit = {}
) {
    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.section_recent_updates),
            level = HeaderLevel.Section,
            padding = PaddingValues(bottom = 20.dp)
        )

        val displayedActivities = remember(activities) { activities.take(5) }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            displayedActivities.forEach { activity ->
                key(activity.id) {
                    RecentUpdateCard(activity = activity, onUserClick = onUserClick)
                }
            }
        }
    }
}

/**
 * An expressive, standalone activity card.
 */
@Composable
fun RecentUpdateCard(
    activity: UserActivity,
    modifier: Modifier = Modifier,
    onUserClick: (String) -> Unit = {}
) {
    val cardShape = remember {
        RoundedCornerShape(topStart = 24.dp, topEnd = 8.dp, bottomEnd = 24.dp, bottomStart = 8.dp)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isMedia = activity.type == com.anisync.android.domain.ActivityType.MEDIA_LIST
            val imageUrl = if (isMedia) activity.mediaCoverUrl else activity.userAvatarUrl
            val fallbackText = if (isMedia) {
                if (activity.mediaTitle.isNotEmpty()) activity.mediaTitle.take(2).uppercase() else "??"
            } else {
                activity.userName?.take(2)?.uppercase() ?: "??"
            }

            val onUserClicked = {
                if (!isMedia) {
                    val username = activity.userName?.trim().orEmpty()
                    if (username.isNotEmpty()) {
                        onUserClick(username)
                    }
                }
            }

            Box(
                modifier = if (isMedia) {
                    Modifier
                        .width(48.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                } else {
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onUserClicked() }
                },
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = fallbackText,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                val timeAgo = remember(activity.timestamp) {
                    formatProfileRelativeTime(activity.timestamp)
                }

                Text(
                    text = timeAgo.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                val primaryColor = MaterialTheme.colorScheme.primary
                
                if (isMedia) {
                    val styledActivityText = remember(
                        activity.status,
                        activity.progress,
                        activity.mediaTitle,
                        primaryColor
                    ) {
                        val statusText = activity.status ?: "Updated"
                        val progressText = activity.progress ?: ""
                        val capStatus =
                            statusText.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

                        buildAnnotatedString {
                            append("$capStatus ")
                            if (progressText.isNotEmpty()) {
                                withStyle(
                                    SpanStyle(
                                        color = primaryColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(progressText)
                                }
                                append(" of ")
                            }
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Unspecified
                                )
                            ) {
                                append(activity.mediaTitle)
                            }
                        }
                    }

                    Text(
                        text = styledActivityText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (activity.mediaScore != null && activity.mediaScore > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "Score: ${activity.mediaScore}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    val titleText = if (activity.type == com.anisync.android.domain.ActivityType.MESSAGE) {
                        "${activity.userName} \u2192 ${activity.recipientName}"
                    } else {
                        activity.userName ?: "Unknown"
                    }
                    
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable { onUserClicked() }
                    )
                    
                    if (activity.text != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activity.text.replace(Regex("<.*?>"), ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
