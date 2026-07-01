package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.ui.theme.emphasis
import com.anisync.android.R
import com.anisync.android.domain.SocialThreadComment
import com.anisync.android.domain.SocialUser
import com.anisync.android.presentation.components.SegmentedTabItem
import com.anisync.android.presentation.forum.components.ForumThreadCard
import com.anisync.android.presentation.forum.components.shared.AuthorRow
import com.anisync.android.presentation.forum.components.shared.StatBadge
import com.anisync.android.presentation.profile.ProfileSocialTab
import com.anisync.android.presentation.profile.ProfileUiState
import com.anisync.android.presentation.profile.components.PlaceholderTabContent
import com.anisync.android.presentation.util.bouncyClickable
import org.jsoup.Jsoup

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun LazyListScope.profileSocialTab(
    uiState: ProfileUiState,
    onTabSelected: (ProfileSocialTab) -> Unit,
    modifier: Modifier = Modifier,
    onUserClick: (String) -> Unit = {},
    onThreadClick: (threadId: Int, threadTitle: String) -> Unit = { _, _ -> },
    onCommentClick: (threadId: Int, commentId: Int, threadTitle: String) -> Unit = { _, _, _ -> },
    onLoadMore: () -> Unit = {},
    userColumns: Int = 3
) {
    val selectedTab = uiState.selectedSocialTab
    val tabs = ProfileSocialTab.entries
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    item(key = "social_tabs") {
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(tabs) { index, tab ->
                SegmentedTabItem(
                    index = index,
                    selectedIndex = selectedIndex,
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    icon = socialTabIcon(tab),
                    label = stringResource(tab.labelRes)
                )
            }
        }
    }

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.socialErrorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val isPaginating = uiState.isSocialPaginating
    val hasNextPage = when (selectedTab) {
        ProfileSocialTab.FOLLOWING -> uiState.followingHasNextPage
        ProfileSocialTab.FOLLOWERS -> uiState.followersHasNextPage
        ProfileSocialTab.FORUM_THREADS -> uiState.threadsHasNextPage
        ProfileSocialTab.FORUM_COMMENTS -> uiState.commentsHasNextPage
    }

    when (selectedTab) {
        ProfileSocialTab.FOLLOWING -> renderSocialUsers(uiState.socialFollowing, selectedTab, modifier, onUserClick, hasNextPage, isPaginating, onLoadMore, userColumns)
        ProfileSocialTab.FOLLOWERS -> renderSocialUsers(uiState.socialFollowers, selectedTab, modifier, onUserClick, hasNextPage, isPaginating, onLoadMore, userColumns)
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
    userColumns: Int
) {
    if (users.isEmpty()) {
        item(key = "social_users_empty") {
            Spacer(modifier = Modifier.height(16.dp))
            PlaceholderTabContent(
                message = stringResource(R.string.profile_social_placeholder, stringResource(selectedTab.labelRes)),
                modifier = modifier
            )
        }
    } else {
        val rowItems = users.chunked(userColumns)
        item(key = "social_top_spacer") { Spacer(modifier = Modifier.height(16.dp)) }

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
                    Box(modifier = Modifier.weight(1f)) {
                        SocialUserItem(
                            user = user,
                            onClick = { onUserClick(user.name) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                repeat(userColumns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (isPaginating) {
            item(key = "social_users_paginating") { PaginatingSpinner() }
        }
    }
}

private fun socialTabIcon(tab: ProfileSocialTab): ImageVector {
    return when (tab) {
        ProfileSocialTab.FOLLOWING -> Icons.Default.Person
        ProfileSocialTab.FOLLOWERS -> Icons.Default.Groups
        ProfileSocialTab.FORUM_THREADS -> Icons.Default.RateReview
        ProfileSocialTab.FORUM_COMMENTS -> Icons.Default.ChatBubbleOutline
    }
}

@Composable
private fun SocialUserItem(
    user: SocialUser,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageShape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .clip(imageShape)
            .bouncyClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = user.name
            )
            .padding(bottom = dimensionResource(R.dimen.spacing_small))
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(dimensionResource(R.dimen.character_image_height))
                .fillMaxWidth()
                .clip(imageShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
        Text(
            text = user.name,
            style = MaterialTheme.typography.labelMedium.emphasis(),
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.primary
        )
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
        Jsoup.parse(comment.commentHtml ?: "").text()
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
