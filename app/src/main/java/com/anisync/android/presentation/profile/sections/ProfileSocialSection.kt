package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.platform.LocalContext
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.SocialThreadComment
import com.anisync.android.domain.parser.RichTextPlainText
import com.anisync.android.domain.SocialUser
import com.anisync.android.presentation.components.EmptyState
import com.anisync.android.presentation.forum.components.ForumThreadCard
import com.anisync.android.presentation.forum.components.shared.AuthorRow
import com.anisync.android.presentation.forum.components.shared.StatBadge
import com.anisync.android.presentation.profile.ProfileSocialTab
import com.anisync.android.presentation.profile.ProfileUiState
import com.anisync.android.presentation.profile.components.PlaceholderTabContent
import com.anisync.android.util.AniListUrls
import com.anisync.android.util.ShareUtils

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun LazyListScope.profileSocialTab(
    uiState: ProfileUiState,
    modifier: Modifier = Modifier,
    onUserClick: (String) -> Unit = {},
    onThreadClick: (threadId: Int, threadTitle: String) -> Unit = { _, _ -> },
    onCommentClick: (threadId: Int, commentId: Int, threadTitle: String) -> Unit = { _, _, _ -> },
    onLoadMore: () -> Unit = {},
    userColumns: Int = 3,
    /** Set on the viewer's own profile, where each follower carries a remove button. */
    onRemoveFollower: ((SocialUser) -> Unit)? = null,
    selectedTab: ProfileSocialTab = uiState.selectedSocialTab,
    onToggleFollow: (SocialUser) -> Unit = {},
    onRetry: () -> Unit = {}
) {

    if (uiState.isSocialLoading) {
        item(key = "social_loading") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                AppCircularProgressIndicator()
            }
        }
        return
    }

    if (uiState.socialErrorMessage != null) {
        item(key = "social_error") {
            EmptyState(
                icon = Icons.Default.CloudOff,
                title = stringResource(R.string.forum_empty_failed_title),
                description = uiState.socialErrorMessage,
                actionLabel = stringResource(R.string.retry),
                actionIcon = Icons.Default.Refresh,
                onAction = onRetry,
                modifier = Modifier.padding(top = 32.dp)
            )
        }
        return
    }

    val isPaginating = uiState.isSocialPaginating
    val hasNextPage = when (selectedTab) {
        ProfileSocialTab.FOLLOWING -> uiState.followingHasNextPage
        ProfileSocialTab.FOLLOWERS -> uiState.followersHasNextPage
        ProfileSocialTab.MUTUALS -> uiState.mutualsHasNextPage
        ProfileSocialTab.FORUM_THREADS -> uiState.threadsHasNextPage
        ProfileSocialTab.FORUM_COMMENTS -> uiState.commentsHasNextPage
    }

    val isOwn = uiState.viewerId != null && uiState.viewerId == uiState.profile?.id
    when (selectedTab) {
        ProfileSocialTab.FOLLOWING -> renderSocialUsers(uiState.socialFollowing, selectedTab, modifier, onUserClick, hasNextPage, isPaginating, onLoadMore, userColumns, uiState.viewerId, onToggleFollow, ownerName = uiState.profile?.name.orEmpty(), isOwn = isOwn)
        ProfileSocialTab.FOLLOWERS -> renderSocialUsers(uiState.socialFollowers, selectedTab, modifier, onUserClick, hasNextPage, isPaginating, onLoadMore, userColumns, uiState.viewerId, onToggleFollow, onRemoveFollower, ownerName = uiState.profile?.name.orEmpty(), isOwn = isOwn)
        ProfileSocialTab.MUTUALS -> renderSocialUsers(uiState.socialMutuals, selectedTab, modifier, onUserClick, hasNextPage, isPaginating, onLoadMore, userColumns, uiState.viewerId, onToggleFollow, ownerName = uiState.profile?.name.orEmpty(), isOwn = isOwn)
        ProfileSocialTab.FORUM_THREADS -> {
            if (uiState.socialThreads.isEmpty()) {
                item(key = "social_threads_empty") {
                    Spacer(modifier = Modifier.height(16.dp))
                    PlaceholderTabContent(
                        message = stringResource(R.string.profile_social_placeholder, stringResource(selectedTab.labelRes)),
                        modifier = modifier
                    )
                }
            } else {
                itemsIndexed(
                    items = uiState.socialThreads,
                    key = { _, thread -> "social_thread_${thread.id}" }
                ) { index, thread ->
                    if (index >= uiState.socialThreads.size - 4 && hasNextPage && !isPaginating) {
                        LaunchedEffect(index) { onLoadMore() }
                    }
                    Spacer(modifier = Modifier.height(if (index == 0) 16.dp else 8.dp))
                    ForumThreadCard(
                        thread = thread,
                        onClick = { onThreadClick(thread.id, thread.title) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onUserClick = onUserClick,
                        onLastReplyClick = { threadId, commentId ->
                            onCommentClick(threadId, commentId, thread.title)
                        }
                    )
                    if (index == uiState.socialThreads.lastIndex) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                if (isPaginating) {
                    item(key = "threads_paginating") { PaginatingSpinner() }
                }
            }
        }
        ProfileSocialTab.FORUM_COMMENTS -> {
            if (uiState.socialComments.isEmpty()) {
                item(key = "social_comments_empty") {
                    Spacer(modifier = Modifier.height(16.dp))
                    PlaceholderTabContent(
                        message = stringResource(R.string.profile_social_placeholder, stringResource(selectedTab.labelRes)),
                        modifier = modifier
                    )
                }
            } else {
                itemsIndexed(
                    items = uiState.socialComments,
                    key = { _, comment -> "social_comment_${comment.id}" }
                ) { index, comment ->
                    if (index >= uiState.socialComments.size - 4 && hasNextPage && !isPaginating) {
                        LaunchedEffect(index) { onLoadMore() }
                    }
                    Spacer(modifier = Modifier.height(if (index == 0) 16.dp else 8.dp))
                    SocialThreadCommentCard(
                        comment = comment,
                        onClick = { onCommentClick(comment.threadId, comment.id, comment.threadTitle) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onUserClick = onUserClick
                    )
                    if (index == uiState.socialComments.lastIndex) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                if (isPaginating) {
                    item(key = "comments_paginating") { PaginatingSpinner() }
                }
            }
        }
    }
}

@Composable
private fun PaginatingSpinner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        AppCircularProgressIndicator()
    }
}

private fun LazyListScope.renderSocialUsers(
    users: List<SocialUser>,
    selectedTab: ProfileSocialTab,
    modifier: Modifier,
    onUserClick: (String) -> Unit,
    hasNextPage: Boolean,
    isPaginating: Boolean,
    onLoadMore: () -> Unit,
    userColumns: Int,
    viewerId: Int?,
    onToggleFollow: (SocialUser) -> Unit,
    onRemoveUser: ((SocialUser) -> Unit)? = null,
    ownerName: String = "",
    isOwn: Boolean = false
) {
    if (users.isEmpty()) {
        item(key = "social_users_empty") {
            val context = LocalContext.current
            val (title, body) = when (selectedTab) {
                ProfileSocialTab.FOLLOWERS -> if (isOwn) {
                    stringResource(R.string.profile_people_empty_followers_own) to stringResource(R.string.profile_people_empty_followers_own_body)
                } else {
                    stringResource(R.string.profile_people_empty_followers, ownerName) to stringResource(R.string.profile_people_empty_body)
                }
                ProfileSocialTab.MUTUALS -> if (isOwn) {
                    stringResource(R.string.profile_people_empty_mutual_own) to stringResource(R.string.profile_people_empty_mutual_own_body)
                } else {
                    stringResource(R.string.profile_people_empty_mutual, ownerName) to stringResource(R.string.profile_people_empty_body)
                }
                else -> if (isOwn) {
                    stringResource(R.string.profile_people_empty_following_own) to stringResource(R.string.profile_people_empty_following_own_body)
                } else {
                    stringResource(R.string.profile_people_empty_following, ownerName) to stringResource(R.string.profile_people_empty_body)
                }
            }
            val shareable = isOwn && selectedTab == ProfileSocialTab.FOLLOWERS && ownerName.isNotBlank()
            EmptyState(
                icon = Icons.Default.Groups,
                title = title,
                description = body,
                actionLabel = if (shareable) stringResource(R.string.profile_people_share_own) else null,
                actionIcon = if (shareable) Icons.Default.Share else null,
                onAction = if (shareable) {
                    { ShareUtils.shareText(context, "$ownerName\n${AniListUrls.userUrl(ownerName)}") }
                } else {
                    null
                },
                modifier = modifier.height(360.dp)
            )
        }
    } else {
        // Rows are wide, so a wide pane fits two side by side at most.
        val rowItems = users.chunked((userColumns / 3).coerceIn(1, 2))
        item(key = "social_top_spacer") { Spacer(modifier = Modifier.height(4.dp)) }

        itemsIndexed(
            items = rowItems,
            key = { index, _ -> "social_row_$index" },
            contentType = { _, _ -> "social_row" }
        ) { rowIndex, row ->
            if (rowIndex >= rowItems.size - 2 && hasNextPage && !isPaginating) {
                LaunchedEffect(rowIndex) { onLoadMore() }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { user ->
                    PersonRow(
                        user = user,
                        showFollow = viewerId != null && user.id != viewerId,
                        onClick = { onUserClick(user.name) },
                        onToggleFollow = { onToggleFollow(user) },
                        onRemove = onRemoveUser?.let { { it(user) } },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat((userColumns / 3).coerceIn(1, 2) - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        if (isPaginating) {
            item(key = "social_users_paginating") { PaginatingSpinner() }
        }
    }
}

@Composable
fun SocialThreadCommentCard(
    comment: SocialThreadComment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onUserClick: (String) -> Unit = {}
) {
    val cleanText = remember(comment.commentHtml) {
        RichTextPlainText.excerpt(comment.commentHtml.orEmpty())
    }
    
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AuthorRow(
                name = comment.authorName,
                avatarUrl = comment.authorAvatarUrl,
                timestampSeconds = comment.createdAt,
                onUserClick = onUserClick
            )
            
            if (comment.threadTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.replied_to, comment.threadTitle),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = cleanText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatBadge(
                    icon = Icons.Outlined.FavoriteBorder,
                    value = comment.likeCount,
                    contentDescription = "${comment.likeCount} likes",
                    tint = if (comment.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
