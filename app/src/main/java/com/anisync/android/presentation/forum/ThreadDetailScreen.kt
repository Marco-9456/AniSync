package com.anisync.android.presentation.forum

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.ForumComment
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.EmptyStateConfigs
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.LocalExoPlayerCache
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.components.rememberExoPlayerCache
import com.anisync.android.presentation.forum.components.FoldedAncestorStrip
import com.anisync.android.presentation.forum.components.ReplyBottomSheetContent
import com.anisync.android.presentation.forum.components.SkeletonLine
import com.anisync.android.presentation.forum.components.ThreadBodyItem
import com.anisync.android.presentation.forum.components.ThreadCommentItem
import com.anisync.android.presentation.forum.components.ThreadHeaderStats
import com.anisync.android.presentation.forum.components.ThreadHeaderTop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Base indent per depth level (must match ThreadCommentItem.indentSize + basePadding overhead)
private const val INDENT_PER_LEVEL_DP = 32
// Minimum content width (dp) to keep comments readable at max indent
private const val MIN_CONTENT_WIDTH_DP = 200

/** Internal data model representing a flattened comment tree node for optimal LazyColumn rendering. */
internal data class FlatComment(
    val comment: ForumComment,
    val depth: Int,
    val ancestorIds: List<Int>,
    val descendantCount: Int
)

private data class CommentSortOption(val sort: String, val label: String)

private val commentSortOptions = listOf(
    CommentSortOption("ID", "Oldest"),
    CommentSortOption("ID_DESC", "Newest")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadDetailScreen(
    threadId: Int,
    threadTitle: String,
    onBackClick: () -> Unit,
    onUserClick: (String) -> Unit,
    targetCommentId: Int? = null,
    viewModel: ThreadDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val replySheetState = rememberModalBottomSheetState()
    val pullToRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current

    var collapsedIds by rememberSaveable(
        saver = listSaver<MutableState<Set<Int>>, Int>(
            save = { it.value.toList() },
            restore = { mutableStateOf(it.toHashSet()) }
        )
    ) { mutableStateOf(emptySet<Int>()) }
    val playerCache = rememberExoPlayerCache()

    // Adaptive depth: compute max visible depth from screen width
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val depthWindowSize = remember(screenWidthDp) {
        ((screenWidthDp - MIN_CONTENT_WIDTH_DP) / INDENT_PER_LEVEL_DP).coerceIn(3, 12)
    }

    // Drill-down stack: each entry is a comment ID whose subtree we "drilled into"
    var drillDownStack by rememberSaveable(
        saver = listSaver<MutableState<List<Int>>, Int>(
            save = { it.value },
            restore = { mutableStateOf(it.toList()) }
        )
    ) { mutableStateOf(emptyList<Int>()) }
    // Track previous stack size to detect navigation direction for scroll positioning
    var prevDrillDownSize by rememberSaveable { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(threadId) {
        viewModel.onAction(ThreadDetailAction.Load(threadId))
    }

    LaunchedEffect(uiState.scrollToBottom) {
        if (uiState.scrollToBottom) {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems > 0) listState.animateScrollToItem(totalItems - 1)
        }
    }

    var flatComments by remember { mutableStateOf(emptyList<FlatComment>()) }

    LaunchedEffect(uiState.comments) {
        withContext(Dispatchers.Default) {
            flatComments = flattenComments(uiState.comments)
        }
    }

    // Build drill-down context: which comment is the focus root and its depth offset
    val focusRootId = drillDownStack.lastOrNull()
    val focusFlat = remember(flatComments, focusRootId) {
        if (focusRootId == null) null
        else flatComments.firstOrNull { it.comment.id == focusRootId }
    }
    val depthOffset = focusFlat?.depth ?: 0

    // Build breadcrumb info for the drill-down chain
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

    // Reset drill-down when comment sort changes
    LaunchedEffect(uiState.commentSortLabel) {
        drillDownStack = emptyList()
    }

    // Smooth scroll positioning after drill-down navigation
    LaunchedEffect(drillDownStack) {
        val newSize = drillDownStack.size
        val oldSize = prevDrillDownSize
        prevDrillDownSize = newSize

        if (newSize == oldSize) return@LaunchedEffect

        // Wait one frame for recomposition to settle with new items
        kotlinx.coroutines.yield()

        // Count header items before comments
        val headerCount = buildList {
            add("thread_header_top")
            if (uiState.parsedBody != null) add("thread_body")
            add("thread_header_stats")
            add("comments_header")
            if (newSize > 0) add("drill_down_breadcrumb")
        }.size

        // Scroll so the first comment in the new view is near the top
        val targetScrollIndex = headerCount
        val totalItems = listState.layoutInfo.totalItemsCount
        if (targetScrollIndex < totalItems) {
            listState.scrollToItem(targetScrollIndex)
        }
    }

    // Scroll-to-comment logic for deep links
    val highlightAlpha = remember { Animatable(0f) }
    var hasScrolledToTarget by remember { mutableStateOf(false) }

    LaunchedEffect(visibleComments, targetCommentId) {
        if (targetCommentId == null || hasScrolledToTarget || visibleComments.isEmpty()) return@LaunchedEffect
        // Auto drill-down if target is deeper than visible depth limit
        val targetFlat = flatComments.firstOrNull { it.comment.id == targetCommentId }
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
        val targetIndex = visibleComments.indexOfFirst { it.comment.id == targetCommentId }
        if (targetIndex >= 0) {
            hasScrolledToTarget = true
            val headerItems = if (drillDownStack.isEmpty()) 4 else 5
            listState.scrollToItem(targetIndex + headerItems)
            highlightAlpha.snapTo(1f)
            highlightAlpha.animateTo(0f, animationSpec = tween(1500))
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.forum_thread_appbar),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    val siteUrl = uiState.thread?.siteUrl
                    if (siteUrl != null) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, siteUrl)
                                    putExtra(Intent.EXTRA_SUBJECT, uiState.thread?.title ?: "")
                                }
                                context.startActivity(Intent.createChooser(intent, "Share thread"))
                            },
                            modifier = Modifier.semantics { contentDescription = "Share thread" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.onAction(ThreadDetailAction.ToggleSubscribe) },
                        modifier = Modifier.semantics {
                            contentDescription =
                                if (uiState.thread?.isSubscribed == true) "Unsubscribe from thread" else "Subscribe to thread"
                        }
                    ) {
                        Icon(
                            imageVector = if (uiState.thread?.isSubscribed == true) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone,
                            contentDescription = null,
                            tint = if (uiState.thread?.isSubscribed == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { viewModel.onAction(ThreadDetailAction.ToggleSave) },
                        modifier = Modifier.semantics {
                            contentDescription =
                                if (uiState.isSaved) "Unsave thread" else "Save thread"
                        }
                    ) {
                        Icon(
                            imageVector = if (uiState.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = if (uiState.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            val thread = uiState.thread
            if (thread != null && !thread.isLocked) {
                Box(modifier = Modifier.navigationBarsPadding()) {
                    FloatingActionButton(
                        onClick = { viewModel.onAction(ThreadDetailAction.OpenReply(null, null)) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.forum_reply)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.thread != null,
            state = pullToRefreshState,
            onRefresh = { viewModel.onAction(ThreadDetailAction.Load(threadId)) },
            indicator = {
                CustomPullToRefreshIndicator(
                    isRefreshing = uiState.isLoading && uiState.thread != null,
                    state = pullToRefreshState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CompositionLocalProvider(LocalExoPlayerCache provides playerCache) {
            when {
                uiState.isLoading && uiState.thread == null -> ThreadDetailSkeleton()

                uiState.errorMessage != null -> ErrorState(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.onAction(ThreadDetailAction.Load(threadId)) }
                )

                else -> {
                    val thread = uiState.thread ?: return@CompositionLocalProvider

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        item(key = "thread_header_top") {
                            ThreadHeaderTop(
                                thread = thread,
                                onUserClick = onUserClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        uiState.parsedBody?.let { body ->
                            item(key = "thread_body") {
                                ThreadBodyItem(
                                    body = body,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        item(key = "thread_header_stats") {
                            ThreadHeaderStats(
                                thread = thread,
                                onLikeClick = {
                                    viewModel.onAction(
                                        ThreadDetailAction.ToggleLike(
                                            isThread = true,
                                            id = thread.id,
                                            currentLiked = thread.isLiked
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item(key = "comments_header") {
                            Column {
                                SectionHeader(
                                    title = stringResource(R.string.forum_comments),
                                    level = HeaderLevel.Section,
                                    padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    commentSortOptions.forEach { option ->
                                        FilterChip(
                                            selected = uiState.commentSortLabel == option.label,
                                            onClick = {
                                                viewModel.onAction(
                                                    ThreadDetailAction.ChangeCommentSort(
                                                        option.sort,
                                                        option.label
                                                    )
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = option.label,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Drill-down breadcrumb when viewing a focused subtree
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

                        if (visibleComments.isEmpty() && !uiState.isLoadingMoreComments) {
                            item(key = "empty_comments") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyStateConfigs.ForumNoComments()
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = visibleComments,
                                key = { _, c -> "comment_${c.comment.id}" },
                                contentType = { _, _ -> "Comment" }
                            ) { _, flat ->
                                val isTarget = targetCommentId != null && flat.comment.id == targetCommentId
                                val highlightColor = if (isTarget) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = highlightAlpha.value * 0.5f)
                                } else {
                                    Color.Transparent
                                }
                                // Determine drill-down eligibility: at max visual depth with children
                                val rebasedDepth = flat.depth - depthOffset
                                val isAtMaxDepth = rebasedDepth >= depthWindowSize - 1
                                val canDrillDown = isAtMaxDepth && flat.descendantCount > 0

                                ThreadCommentItem(
                                    comment = flat.comment,
                                    isCollapsed = collapsedIds.contains(flat.comment.id),
                                    onUserClick = onUserClick,
                                    onToggleCollapse = {
                                        collapsedIds = if (collapsedIds.contains(flat.comment.id)) {
                                            collapsedIds - flat.comment.id
                                        } else {
                                            collapsedIds + flat.comment.id
                                        }
                                    },
                                    descendantCount = flat.descendantCount,
                                    onLikeClick = { commentId, currentLiked ->
                                        viewModel.onAction(
                                            ThreadDetailAction.ToggleLike(
                                                isThread = false,
                                                id = commentId,
                                                currentLiked = currentLiked
                                            )
                                        )
                                    },
                                    onReplyClick = if (!thread.isLocked) {
                                        { commentId, authorName ->
                                            viewModel.onAction(
                                                ThreadDetailAction.OpenReply(
                                                    commentId,
                                                    authorName
                                                )
                                            )
                                        }
                                    } else null,
                                    threadAuthorId = thread.authorId,
                                    depth = flat.depth,
                                    depthOffset = depthOffset,
                                    maxVisualDepth = depthWindowSize,
                                    onDrillDown = if (canDrillDown) {
                                        {
                                            drillDownStack = drillDownStack + flat.comment.id
                                        }
                                    } else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(highlightColor)
                                )
                            }
                        }

                        if (uiState.hasMoreComments) {
                            item(key = "load_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoadingMoreComments) {
                                        CircularProgressIndicator()
                                    } else {
                                        FilledTonalButton(
                                            onClick = { viewModel.onAction(ThreadDetailAction.LoadMoreComments) },
                                            shape = RoundedCornerShape(percent = 50)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.forum_load_more_comments),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
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
    }

    if (uiState.isReplySheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onAction(ThreadDetailAction.CloseReply) },
            sheetState = replySheetState
        ) {
            ReplyBottomSheetContent(
                replyingToAuthor = uiState.replyTargetAuthorName,
                isSubmitting = uiState.isSubmittingReply,
                onSubmit = { body ->
                    viewModel.onAction(ThreadDetailAction.SubmitReply(threadId, body))
                },
                onDismiss = { viewModel.onAction(ThreadDetailAction.CloseReply) }
            )
        }
    }
}

@Composable
private fun ThreadDetailSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonLine(
                fraction = 0f,
                height = 40.dp,
                modifier = Modifier
                    .width(40.dp)
                    .height(40.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                SkeletonLine(fraction = 0.4f, height = 14.dp)
                Spacer(Modifier.height(4.dp))
                SkeletonLine(fraction = 0.25f, height = 12.dp)
            }
        }
        Spacer(Modifier.height(20.dp))
        SkeletonLine(fraction = 0.85f, height = 24.dp)
        Spacer(Modifier.height(8.dp))
        SkeletonLine(fraction = 0.6f, height = 24.dp)
        Spacer(Modifier.height(20.dp))
        SkeletonLine(fraction = 1f, height = 16.dp)
        Spacer(Modifier.height(6.dp))
        SkeletonLine(fraction = 1f, height = 16.dp)
        Spacer(Modifier.height(6.dp))
        SkeletonLine(fraction = 0.8f, height = 16.dp)
        Spacer(Modifier.height(6.dp))
        SkeletonLine(fraction = 0.9f, height = 16.dp)
        Spacer(Modifier.height(6.dp))
        SkeletonLine(fraction = 0.5f, height = 16.dp)
        Spacer(Modifier.height(24.dp))
        SkeletonLine(fraction = 0.5f, height = 16.dp)
        Spacer(Modifier.height(32.dp))
        SkeletonLine(fraction = 0.3f, height = 20.dp)
        Spacer(Modifier.height(16.dp))

        repeat(3) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonLine(
                    fraction = 0f,
                    height = 32.dp,
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonLine(fraction = 0.3f, height = 12.dp)
                    Spacer(Modifier.height(8.dp))
                    SkeletonLine(fraction = 0.9f, height = 14.dp)
                    Spacer(Modifier.height(4.dp))
                    SkeletonLine(fraction = 0.6f, height = 14.dp)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun flattenComments(
    comments: List<ForumComment>,
    depth: Int = 0,
    ancestors: List<Int> = emptyList()
): List<FlatComment> {
    val flatList = mutableListOf<FlatComment>()
    for (comment in comments) {
        val descendants = countDescendants(comment)
        flatList.add(FlatComment(comment, depth, ancestors, descendants))
        if (comment.childComments.isNotEmpty()) {
            flatList.addAll(
                flattenComments(
                    comments = comment.childComments,
                    depth = depth + 1,
                    ancestors = ancestors + comment.id
                )
            )
        }
    }
    return flatList
}

private fun countDescendants(comment: ForumComment): Int {
    var count = comment.childComments.size
    for (child in comment.childComments) {
        count += countDescendants(child)
    }
    return count
}