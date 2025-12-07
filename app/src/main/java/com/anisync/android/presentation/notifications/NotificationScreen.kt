package com.anisync.android.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.anisync.android.domain.ActivityLikeNotification
import com.anisync.android.domain.ActivityReplyNotification
import com.anisync.android.domain.AiringNotification
import com.anisync.android.domain.FollowingNotification
import com.anisync.android.domain.Notification
import com.anisync.android.type.NotificationType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit = {},
    onUserClick: (Int) -> Unit = {},
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is NotificationUiState.Loading -> {
                   Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                       Text("Loading...")
                   }
                }
                is NotificationUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is NotificationUiState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = false,
                        onRefresh = { viewModel.refresh() }
                    ) {
                        LazyColumn {
                            items(state.notifications) { notification ->
                                NotificationItem(
                                    notification = notification,
                                    onMediaClick = onMediaClick,
                                    onUserClick = onUserClick
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
fun NotificationItem(
    notification: Notification,
    onMediaClick: (Int) -> Unit,
    onUserClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Determine default click action
                when (notification) {
                     is AiringNotification -> notification.media?.id?.let { onMediaClick(it) }
                     is FollowingNotification -> notification.user?.id?.let { onUserClick(it) }
                     is ActivityLikeNotification -> notification.user?.id?.let { onUserClick(it) } // Navigate to activity details ideally
                     is ActivityReplyNotification -> notification.user?.id?.let { onUserClick(it) }
                     else -> {}
                }
            }
            .padding(16.dp)
    ) {
        // Icon/Image
        Box(modifier = Modifier.size(48.dp)) {
            when (notification) {
                is AiringNotification -> {
                    AsyncImage(
                        model = notification.media?.coverUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                is FollowingNotification, is ActivityLikeNotification, is ActivityReplyNotification -> {
                     val aviUrl = when(notification) {
                         is FollowingNotification -> notification.user?.avatarUrl
                         is ActivityLikeNotification -> notification.user?.avatarUrl
                         is ActivityReplyNotification -> notification.user?.avatarUrl
                         else -> null
                     }
                     AsyncImage(
                        model = aviUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                     Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer, CircleShape))
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            val text = when (notification) {
                is AiringNotification -> {
                    val contexts = notification.contexts
                    val episode = notification.episode
                    val title = notification.media?.title ?: "Unknown Media"
                    // Basic construction of message "Ep 10 of One Piece aired"
                    "Episode $episode of $title aired." 
                }
                is FollowingNotification -> {
                    "${notification.user?.name} started following you."
                }
                is ActivityLikeNotification -> {
                   "${notification.user?.name} liked your activity."
                }
                is ActivityReplyNotification -> {
                    "${notification.user?.name} replied to your activity."
                }
                else -> "Unknown notification"
            }
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Time (Mock for now, would use relative time)
            Text(
                text = "Just now", // TODO: Format notification.createdAt
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
