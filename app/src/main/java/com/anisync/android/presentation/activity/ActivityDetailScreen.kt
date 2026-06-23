package com.anisync.android.presentation.activity

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.AlertDialog
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.components.alert.rememberRateLimitedRefresh
import com.anisync.android.presentation.components.menu.Menu
import com.anisync.android.presentation.components.richtext.RichTextInputSheet
import com.anisync.android.presentation.forum.FlatComment
import com.anisync.android.presentation.forum.components.ContentStatsBar
import com.anisync.android.presentation.forum.components.FoldedAncestorStrip
import com.anisync.android.presentation.forum.components.ThreadBodyItem
import com.anisync.android.presentation.forum.components.ThreadCommentItem
import com.anisync.android.presentation.forum.components.shared.AuthorRow
import com.anisync.android.presentation.forum.flattenComments
import com.anisync.android.ui.theme.LocalAvatarShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val INDENT_PER_LEVEL_DP = 32
private const val MIN_CONTENT_WIDTH_DP = 200

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activityId: Int,
    onBackClick: () -> Unit,
    onUserClick: (String) -> Unit,
    targetReplyId: Int? = null,
    navigationIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    onEditActivity: (activityId: Int) -> Unit = {},
    viewModel: ActivityDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current

    var collapsedIds by rememberSaveable(
        saver = listSaver<MutableState<Set<Int>>, Int>(
            save = { it.value.toList() },
            restore = { mutableStateOf(it.toHashSet()) }
        )
    ) { mutableStateOf(emptySet<Int>()) }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val depthWindowSize = remember(screenWidthDp) {
        ((screenWidthDp - MIN_CONTENT_WIDTH_DP) / INDENT_PER_LEVEL_DP).coerceIn(3, 12)
    }

    var drillDownStack by rememberSaveable(
        saver = listSaver<MutableState<List<Int>>, Int>(
            save = { it.value },
            restore = { mutableStateOf(it.toList()) }
        )
    ) { mutableStateOf(emptyList<Int>()) }
    var prevDrillDownSize by rememberSaveable { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    var showDeleteActivityDialog by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }
    var likesTarget by remember { mutableStateOf<com.anisync.android.presentation.components.likes.LikesTarget?>(null) }

    LaunchedEffect(activityId) {
        viewModel.onAction(ActivityDetailAction.Load(activityId))
    }

    LaunchedEffect(Unit) {
        viewModel.finishedEvents.collect { onBackClick() }
    }

    LaunchedEffect(uiState.scrollToBottom) {
        if (uiState.scrollToBottom) {
            val total = listState.layoutInfo.totalItemsCount
            if (total > 0) listState.animateScrollToItem(total - 1)
            viewModel.onAction(ActivityDetailAction.ConsumeScrollToBottom)
        }
    }

    var flatComments by remember { mutableStateOf(emptyList<FlatComment>()) }
    LaunchedEffect(uiState.replyNodes) {
        withContext(Dispatchers.Default) {
            flatComments = flattenComments(uiState.replyNodes)
        }
    }

    val focusRootId = drillDownStack.lastOrNull()
    val focusFlat = remember(flatComments, focusRootId) {
        if (focusRootId == null) null
        else flatComments.firstOrNull { it.comment.id == focusRootId }
    }
    val depthOffset = focusFlat?.depth ?: 0

    val drillDownBreadcrumbs = remember(flatComments, drillDownStack) {
        drillDownStack.mapNotNull { id ->
            flatComments.firstOrNull { it.comment.id == id }?.comment
        }
    }

    val visibleComments by remember {
        derivedStateOf {
            val collapsed = collapsedIds
            val focusId = drillDownStack.lastOrNull()
            flatComments.filter { flat ->
                flat.ancestorIds.none { it in collapsed } &&
                    (focusId == null || flat.comment.id == focusId || focusId in flat.ancestorIds)
            }
        }
    }

    LaunchedEffect(drillDownStack) {
        val newSize = drillDownStack.size
        val oldSize = prevDrillDownSize
        prevDrillDownSize = newSize
        if (newSize == oldSize) return@LaunchedEffect
        kotlinx.coroutines.yield()
        val headerCount = buildList {
            add("activity_header_top")
            if (uiState.parsedBody != null) add("activity_body")
            add("activity_stats")
            add("replies_header")
            if (newSize > 0) add("drill_down_breadcrumb")
        }.size
        val targetScrollIndex = headerCount
        val totalItems = listState.layoutInfo.totalItemsCount
        if (targetScrollIndex < totalItems) listState.scrollToItem(targetScrollIndex)
    }

    val highlightAlpha = remember { Animatable(0f) }
    var hasScrolledToTarget by remember { mutableStateOf(false) }

    LaunchedEffect(visibleComments, targetReplyId) {
        if (targetReplyId == null || hasScrolledToTarget || visibleComments.isEmpty()) return@LaunchedEffect
        val targetFlat = flatComments.firstOrNull { it.comment.id == targetReplyId }
        if (targetFlat != null && targetFlat.depth >= depthWindowSize) {
            val chain = mutableListOf<Int>()
            for (ancestorId in targetFlat.ancestorIds) {
                val ancestor = flatComments.firstOrNull { it.comment.id == ancestorId } ?: continue
                if (ancestor.depth > 0 && ancestor.depth % depthWindowSize == 0) {
                    chain.add(ancestorId)
                }
            }
            if (chain.isNotEmpty()) drillDownStack = chain
        }
        val targetIndex = visibleComments.indexOfFirst { it.comment.id == targetReplyId }
        if (targetIndex >= 0) {
            hasScrolledToTarget = true
            val headerItems = if (drillDownStack.isEmpty()) 4 else 5
            listState.scrollToItem(targetIndex + headerItems)
            highlightAlpha.snapTo(1f)
            highlightAlpha.animateTo(0f, animationSpec = tween(1500))
        }
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.activity_detail_title),
        onBackClick = onBackClick,
        navigationIcon = navigationIcon,
        scrollableState = listState,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        enableEnterAnimation = true,
        actions = {
            uiState.activity?.let { act ->
                IconButton(onClick = { viewModel.onAction(ActivityDetailAction.ToggleSubscription) }) {
                    Icon(
                        imageVector = if (act.isSubscribed) Icons.Filled.Notifications
                        else Icons.Outlined.NotificationsNone,
                        contentDescription = if (act.isSubscribed) "Unsubscribe" else "Subscribe",
                        tint = if (act.isSubscribed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
                    val siteUrl = uiState.activity?.siteUrl
                    if (siteUrl != null) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, siteUrl)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share"))
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    val activity = uiState.activity
                    val viewerId = uiState.viewerId
                    // Edit allowed on own TextActivity OR own outgoing MessageActivity. Never on
                    // ListActivity (status updates / progress are derived, not authored).
                    val isOwnAuthored = activity != null && viewerId != null &&
                        activity.authorId == viewerId
                    val canEdit = isOwnAuthored && (
                        !activity!!.isMessage || activity.recipientId != viewerId
                    )
                    val canDelete = activity != null && viewerId != null && (
                        activity.authorId == viewerId ||
                            (activity.isMessage && activity.recipientId == viewerId && !activity.isAuthorMod)
                    )
                    if (canDelete || canEdit) {
                        Box {
                            IconButton(onClick = { showOverflow = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.more_options),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Menu(
                                expanded = showOverflow,
                                onDismissRequest = { showOverflow = false }
                            ) {
                                if (canEdit) {
                                    item(
                                        text = stringResource(R.string.edit),
                                        leadingIcon = Icons.Default.Edit,
                                        onClick = {
                                            showOverflow = false
                                            onEditActivity(activityId)
                                        }
                                    )
                                }
                                if (canDelete) {
                                    if (canEdit) gap()
                                    item(
                                        text = stringResource(R.string.delete),
                                        leadingIcon = Icons.Default.Delete,
                                        destructive = true,
                                        onClick = {
                                            showOverflow = false
                                            showDeleteActivityDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
        floatingActionButton = {
            if (uiState.activity != null) {
                Box(modifier = Modifier.navigationBarsPadding()) {
                    FloatingActionButton(
                        onClick = { viewModel.onAction(ActivityDetailAction.OpenReply(null, null)) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.activity_detail_reply)
                        )
                    }
                }
            }
        }
    ) { topContentPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            state = pullToRefreshState,
            onRefresh = rememberRateLimitedRefresh { viewModel.onAction(ActivityDetailAction.Refresh) },
            indicator = {
                CustomPullToRefreshIndicator(
                    isRefreshing = uiState.isRefreshing,
                    state = pullToRefreshState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = topContentPadding)
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isLoading && uiState.activity == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) { AppCircularProgressIndicator() }
                }
                uiState.errorMessage != null && uiState.activity == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding)
                    ) {
                        ErrorState(
                            message = uiState.errorMessage!!,
                            onRetry = { viewModel.onAction(ActivityDetailAction.Load(activityId)) }
                        )
                    }
                }
                uiState.activity != null -> {
                    val activity = uiState.activity!!
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = topContentPadding),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        item(key = "activity_header_top") {
                            ActivityHeaderBlock(
                                authorName = activity.authorName,
                                authorAvatarUrl = activity.authorAvatarUrl,
                                createdAt = activity.createdAt,
                                isMessage = activity.isMessage,
                                isPrivate = activity.isPrivate,
                                recipientName = activity.recipientName,
                                recipientAvatarUrl = activity.recipientAvatarUrl,
                                onUserClick = onUserClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        uiState.parsedBody?.let { body ->
                            item(key = "activity_body") {
                                ThreadBodyItem(
                                    body = body,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        item(key = "activity_stats") {
                            ContentStatsBar(
                                replyCount = activity.replyCount,
                                viewCount = null,
                                likeCount = activity.likeCount,
                                isLiked = activity.isLiked,
                                onLikeClick = {
                                    viewModel.onAction(ActivityDetailAction.ToggleActivityLike)
                                },
                                onLikeCountClick = if (activity.likeCount > 0) {
                                    { likesTarget = com.anisync.android.presentation.components.likes.LikesTarget.Activity(activity.id) }
                                } else null,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item(key = "replies_header") {
                            SectionHeader(
                                title = stringResource(
                                    R.string.activity_detail_replies,
                                    activity.replyCount
                                ),
                                level = HeaderLevel.Section,
                                padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        if (drillDownStack.isNotEmpty()) {
                            item(key = "drill_down_breadcrumb") {
                                FoldedAncestorStrip(
                                    breadcrumbs = drillDownBreadcrumbs,
                                    onNavigateBack = {
                                        drillDownStack = drillDownStack.dropLast(1)
                                    },
                                    onNavigateToLevel = { index ->
                                        drillDownStack = drillDownStack.take(index)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (visibleComments.isEmpty()) {
                            item(key = "empty_replies") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.activity_detail_no_replies),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = visibleComments,
                                key = { _, c -> "reply_${c.comment.id}" },
                                contentType = { _, _ -> "Reply" }
                            ) { _, flat ->
                                val isTarget = targetReplyId != null && flat.comment.id == targetReplyId
                                val highlightColor = if (isTarget) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = highlightAlpha.value * 0.5f)
                                } else Color.Transparent

                                val rebasedDepth = flat.depth - depthOffset
                                val isAtMaxDepth = rebasedDepth >= depthWindowSize - 1
                                val canDrillDown = isAtMaxDepth && flat.descendantCount > 0
                                val isOwnReply = uiState.viewerId != null &&
                                    flat.comment.authorId == uiState.viewerId

                                ThreadCommentItem(
                                    comment = flat.comment,
                                    isCollapsed = collapsedIds.contains(flat.comment.id),
                                    onToggleCollapse = {
                                        collapsedIds = if (collapsedIds.contains(flat.comment.id)) {
                                            collapsedIds - flat.comment.id
                                        } else {
                                            collapsedIds + flat.comment.id
                                        }
                                    },
                                    descendantCount = flat.descendantCount,
                                    onLikeClick = { commentId, _ ->
                                        viewModel.onAction(ActivityDetailAction.ToggleReplyLike(commentId))
                                    },
                                    onLikeCountClick = { commentId ->
                                        likesTarget = com.anisync.android.presentation.components.likes.LikesTarget.ActivityReply(commentId)
                                    },
                                    onReplyClick = { commentId, authorName ->
                                        viewModel.onAction(
                                            ActivityDetailAction.OpenReply(commentId, authorName)
                                        )
                                    },
                                    threadAuthorId = activity.authorId,
                                    depth = flat.depth,
                                    depthOffset = depthOffset,
                                    maxVisualDepth = depthWindowSize,
                                    onDrillDown = if (canDrillDown) {
                                        { drillDownStack = drillDownStack + flat.comment.id }
                                    } else null,
                                    onUserClick = onUserClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(highlightColor),
                                    actionSlot = {
                                        if (isOwnReply) {
                                            ReplyOverflowMenu(
                                                onEdit = {
                                                    viewModel.onAction(
                                                        ActivityDetailAction.EditReply(flat.comment.id)
                                                    )
                                                },
                                                onDelete = {
                                                    viewModel.onAction(
                                                        ActivityDetailAction.DeleteReply(flat.comment.id)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        item(key = "bottom_spacer") {
                            Spacer(modifier = Modifier.height(96.dp))
                        }
                    }
                }
            }
        }
    }

    // Replies (new or edit) and message-style activities use the bottom sheet —
    // matches replies' compact UX. Editing the root TextActivity opens a full-screen
    // editor instead because it's a longer-form post.
    if (uiState.isReplySheetVisible) {
        val sheetTitle = if (uiState.editingReplyId != null) {
            stringResource(R.string.activity_edit_reply_title)
        } else {
            stringResource(R.string.forum_write_reply)
        }
        val sheetSubmit = if (uiState.editingReplyId != null) {
            stringResource(R.string.activity_edit_save)
        } else {
            stringResource(R.string.forum_post_reply)
        }
        val replyBounds = com.anisync.android.domain.ContentLimits.Reply
        RichTextInputSheet(
            title = sheetTitle,
            placeholder = stringResource(R.string.forum_reply_hint),
            submitLabel = sheetSubmit,
            replyingToLabel = uiState.replyingToAuthor?.let {
                stringResource(R.string.forum_replying_to, it)
            },
            isSubmitting = uiState.isSubmittingReply,
            minLength = replyBounds.min,
            maxLength = replyBounds.max,
            onSubmit = { body -> viewModel.onAction(ActivityDetailAction.SubmitReply(body)) },
            onDismiss = { viewModel.onAction(ActivityDetailAction.CloseReply) },
            prefillBody = uiState.replyPrefillBody
        )
    }

    likesTarget?.let { target ->
        com.anisync.android.presentation.components.likes.LikesSheet(
            target = target,
            onDismiss = { likesTarget = null },
            onUserClick = { username ->
                likesTarget = null
                onUserClick(username)
            }
        )
    }

    if (showDeleteActivityDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteActivityDialog = false },
            title = { Text(stringResource(R.string.activity_delete_confirm_title)) },
            text = { Text(stringResource(R.string.activity_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteActivityDialog = false
                    viewModel.onAction(ActivityDetailAction.DeleteActivity)
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteActivityDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ActivityHeaderBlock(
    authorName: String,
    authorAvatarUrl: String?,
    createdAt: Long,
    isMessage: Boolean,
    isPrivate: Boolean,
    recipientName: String?,
    recipientAvatarUrl: String?,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 8.dp)
        ) {
            if (isMessage && recipientName != null) {
                MessageActivityHeader(
                    authorName = authorName,
                    authorAvatarUrl = authorAvatarUrl,
                    recipientName = recipientName,
                    recipientAvatarUrl = recipientAvatarUrl,
                    createdAt = createdAt,
                    isPrivate = isPrivate,
                    onUserClick = onUserClick
                )
            } else {
                AuthorRow(
                    name = authorName,
                    avatarUrl = authorAvatarUrl,
                    timestampSeconds = createdAt,
                    avatarSize = 44.dp,
                    onUserClick = onUserClick
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MessageActivityHeader(
    authorName: String,
    authorAvatarUrl: String?,
    recipientName: String,
    recipientAvatarUrl: String?,
    createdAt: Long,
    isPrivate: Boolean,
    onUserClick: (String) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeaderAvatar(url = authorAvatarUrl, name = authorName, onClick = { onUserClick(authorName) })
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HeaderAvatar(url = recipientAvatarUrl, name = recipientName, onClick = { onUserClick(recipientName) })
            if (isPrivate) {
                Spacer(Modifier.size(8.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.activity_detail_private),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.activity_detail_wrote_to, authorName, recipientName),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = com.anisync.android.presentation.profile.util.formatProfileRelativeTime(createdAt * 1000L),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun HeaderAvatar(url: String?, name: String, onClick: () -> Unit) {
    if (url != null) {
        UserAvatar(
            url = url,
            contentDescription = name,
            size = 44.dp,
            modifier = Modifier.clickable { onClick() }
        )
    } else {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(LocalAvatarShape.current)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReplyOverflowMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more_options),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Menu(expanded = expanded, onDismissRequest = { expanded = false }) {
            item(
                text = stringResource(R.string.edit),
                leadingIcon = Icons.Default.Edit,
                onClick = {
                    expanded = false
                    onEdit()
                }
            )
            gap()
            item(
                text = stringResource(R.string.delete),
                leadingIcon = Icons.Default.Delete,
                destructive = true,
                onClick = {
                    expanded = false
                    confirmDelete = true
                }
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.reply_delete_confirm_title)) },
            text = { Text(stringResource(R.string.reply_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
