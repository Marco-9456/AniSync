package com.anisync.android.presentation.forum

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.anisync.android.presentation.components.menu.Menu
import com.anisync.android.presentation.components.richtext.RichTextInputScreen
import com.anisync.android.presentation.components.richtext.RichTextInputSheet
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.CommentNode
import com.anisync.android.domain.toCommentNode
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.EmptyStateConfigs
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.LocalExoPlayerCache
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.components.rememberExoPlayerCache
import com.anisync.android.presentation.forum.components.FoldedAncestorStrip
import com.anisync.android.presentation.forum.components.PageJumperBottomSheet
import com.anisync.android.presentation.forum.components.SkeletonLine
import com.anisync.android.presentation.forum.components.ThreadBodyItem
import com.anisync.android.presentation.forum.components.ThreadCommentItem
import com.anisync.android.presentation.forum.components.ThreadHeaderStats
import com.anisync.android.presentation.forum.components.ThreadHeaderTop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

// Base indent per depth level
private const val INDENT_PER_LEVEL_DP = 32
private const val MIN_CONTENT_WIDTH_DP = 200

internal data class FlatComment(
    val comment: CommentNode,
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
    val pullToRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current

    var collapsedIds by rememberSaveable(
        saver = listSaver<MutableState<Set<Int>>, Int>(
            save = { it.value.toList() },
            restore = { mutableStateOf(it.toHashSet()) }
        )
    ) { mutableStateOf(emptySet<Int>()) }

    val playerCache = rememberExoPlayerCache()
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

    LaunchedEffect(threadId, targetCommentId) {
        viewModel.onAction(ThreadDetailAction.Load(threadId, targetCommentId))
    }

    LaunchedEffect(viewModel) {
        viewModel.actions.collectLatest { event ->
            // Actions handled here (navigation, etc.)
        }
    }

    val anchorCommentId = uiState.anchorCommentId

    LaunchedEffect(uiState.scrollToBottom) {
        if (uiState.scrollToBottom) {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems > 0) listState.animateScrollToItem(totalItems - 1)
        }
    }

    var flatComments by remember { mutableStateOf(emptyList<FlatComment>()) }

    LaunchedEffect(uiState.comments) {
        withContext(Dispatchers.Default) {
            flatComments = flattenComments(uiState.comments.map { it.toCommentNode() })
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

    LaunchedEffect(uiState.commentSortLabel) {
        drillDownStack = emptyList()
    }

    LaunchedEffect(drillDownStack) {
        val newSize = drillDownStack.size
        val oldSize = prevDrillDownSize
        prevDrillDownSize = newSize
        if (newSize == oldSize) return@LaunchedEffect
        kotlinx.coroutines.yield()
        val headerCount = computeHeaderItemCount(
            hasParsedBody = uiState.parsedBody != null,
            hasDrillDown = newSize > 0,
            hasPagePill = uiState.lastPage > 1 || uiState.totalComments > 0,
            hasLoadEarlier = uiState.hasEarlierComments,
        )
        val totalItems = listState.layoutInfo.totalItemsCount
        if (headerCount < totalItems) {
            listState.scrollToItem(headerCount)
        }
    }

    val highlightAlpha = remember { Animatable(0f) }
    var lastScrolledAnchor by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(visibleComments, anchorCommentId) {
        val anchor = anchorCommentId ?: return@LaunchedEffect
        if (anchor == lastScrolledAnchor || visibleComments.isEmpty()) return@LaunchedEffect
        val targetFlat = flatComments.firstOrNull { it.comment.id == anchor }
        if (targetFlat != null && targetFlat.depth >= depthWindowSize) {
            val chain = mutableListOf<Int>()
            for (ancestorId in targetFlat.ancestorIds) {
                val ancestor = flatComments.firstOrNull { it.comment.id == ancestorId } ?: continue
                if (ancestor.depth > 0 && ancestor.depth % depthWindowSize == 0) chain.add(
                    ancestorId
                )
            }
            if (chain.isNotEmpty()) drillDownStack = chain
        }
        val targetIndex = visibleComments.indexOfFirst { it.comment.id == anchor }
        if (targetIndex >= 0) {
            lastScrolledAnchor = anchor
            val headerItems = computeHeaderItemCount(
                hasParsedBody = uiState.parsedBody != null,
                hasDrillDown = drillDownStack.isNotEmpty(),
                hasPagePill = uiState.lastPage > 1 || uiState.totalComments > 0,
                hasLoadEarlier = uiState.hasEarlierComments,
            )
            listState.scrollToItem(targetIndex + headerItems)
            highlightAlpha.snapTo(1f)
            highlightAlpha.animateTo(0f, animationSpec = tween(1500))
        }
    }

    LaunchedEffect(uiState.pendingScrollToTop) {
        if (!uiState.pendingScrollToTop) return@LaunchedEffect
        val headerItems = computeHeaderItemCount(
            hasParsedBody = uiState.parsedBody != null,
            hasDrillDown = drillDownStack.isNotEmpty(),
            hasPagePill = uiState.lastPage > 1 || uiState.totalComments > 0,
            hasLoadEarlier = uiState.hasEarlierComments,
        )
        kotlinx.coroutines.yield()
        val total = listState.layoutInfo.totalItemsCount
        if (headerItems < total) listState.scrollToItem(headerItems)
        viewModel.onAction(ThreadDetailAction.ScrollToTopConsumed)
    }

    var likesTarget by remember { mutableStateOf<com.anisync.android.presentation.components.likes.LikesTarget?>(null) }
    var threadOverflow by remember { mutableStateOf(false) }
    var showDeleteThreadDialog by remember { mutableStateOf(false) }

    // Pop the screen once after a successful thread deletion.
    LaunchedEffect(uiState.threadDeleted) {
        if (uiState.threadDeleted) onBackClick()
    }

    // Determine FAB expanded state based on scroll direction (MD3E reactive layout)
    val isFabExpanded by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 || !listState.canScrollBackward
        }
    }

    CollapsingTopBarScaffold(
        title = threadTitle.ifEmpty { stringResource(R.string.forum_thread_appbar) },
        onBackClick = onBackClick,
        scrollableState = listState,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        actions = {
                    val siteUrl = uiState.thread?.siteUrl
                    if (siteUrl != null) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, siteUrl)
                                putExtra(Intent.EXTRA_SUBJECT, uiState.thread?.title ?: "")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share thread"))
                        }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = stringResource(R.string.cd_share),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.onAction(ThreadDetailAction.ToggleSubscribe) }) {
                        Icon(
                            imageVector = if (uiState.thread?.isSubscribed == true) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone,
                            contentDescription = stringResource(R.string.cd_toggle_subscription),
                            tint = if (uiState.thread?.isSubscribed == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.onAction(ThreadDetailAction.ToggleSave) }) {
                        Icon(
                            imageVector = if (uiState.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = stringResource(R.string.cd_toggle_save),
                            tint = if (uiState.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val isOwnThread = uiState.thread != null && uiState.viewerId != null &&
                        uiState.thread!!.authorId == uiState.viewerId
                    if (isOwnThread) {
                        Box {
                            IconButton(onClick = { threadOverflow = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.more_options),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Menu(
                                expanded = threadOverflow,
                                onDismissRequest = { threadOverflow = false }
                            ) {
                                item(
                                    text = stringResource(R.string.edit),
                                    leadingIcon = Icons.Default.Edit,
                                    onClick = {
                                        threadOverflow = false
                                        viewModel.onAction(ThreadDetailAction.EditThread)
                                    }
                                )
                                gap()
                                item(
                                    text = stringResource(R.string.delete),
                                    leadingIcon = Icons.Default.Delete,
                                    destructive = true,
                                    onClick = {
                                        threadOverflow = false
                                        showDeleteThreadDialog = true
                                    }
                                )
                            }
                        }
                    }
                },
        floatingActionButton = {
            val thread = uiState.thread
            if (thread != null && !thread.isLocked) {
                Box(modifier = Modifier.navigationBarsPadding()) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.onAction(ThreadDetailAction.OpenReply(null, null)) },
                        expanded = isFabExpanded,
                        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        text = {
                            Text(
                                stringResource(R.string.forum_reply),
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        shape = RoundedCornerShape(20.dp) // MD3 Expressive pill shape
                    )
                }
            }
        }
    ) { topContentPadding ->
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
                        .padding(top = topContentPadding)
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            CompositionLocalProvider(LocalExoPlayerCache provides playerCache) {
                when {
                    uiState.isLoading && uiState.thread == null -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding)
                    ) { ThreadDetailSkeleton() }

                    uiState.errorMessage != null -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding)
                    ) {
                        ErrorState(
                            message = uiState.errorMessage!!,
                            onRetry = { viewModel.onAction(ThreadDetailAction.Load(threadId)) }
                        )
                    }

                    else -> {
                        val thread = uiState.thread ?: return@CompositionLocalProvider

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = topContentPadding),
                            verticalArrangement = Arrangement.spacedBy(8.dp) // Expressive vertical spacing
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
                                    ThreadBodyItem(body = body, modifier = Modifier.fillMaxWidth())
                                }
                            }

                            item(key = "thread_header_stats") {
                                ThreadHeaderStats(
                                    thread = thread,
                                    onLikeClick = {
                                        viewModel.onAction(
                                            ThreadDetailAction.ToggleLike(
                                                true,
                                                thread.id,
                                                thread.isLiked
                                            )
                                        )
                                    },
                                    onLikeCountClick = if (thread.likeCount > 0) {
                                        {
                                            likesTarget =
                                                com.anisync.android.presentation.components.likes.LikesTarget.Thread(thread.id)
                                        }
                                    } else null,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            item(key = "comments_header") {
                                Column(modifier = Modifier.padding(top = 16.dp)) {
                                    SectionHeader(
                                        title = stringResource(R.string.forum_comments),
                                        level = HeaderLevel.Section,
                                        padding = PaddingValues(
                                            horizontal = 24.dp,
                                            vertical = 8.dp
                                        ) // MD3 padding
                                    )
                                    Row(
                                        modifier = Modifier
                                            .horizontalScroll(rememberScrollState())
                                            .padding(horizontal = 20.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                ),
                                                shape = RoundedCornerShape(16.dp),
                                                border = null
                                            )
                                        }
                                    }
                                }
                            }

                            val hasPagePill = uiState.lastPage > 1 || uiState.totalComments > 0
                            if (hasPagePill) {
                                item(key = "page_pill") {
                                    PageProgressPill(
                                        totalComments = uiState.totalComments,
                                        currentPage = uiState.loadedPageRange?.last ?: 1,
                                        lastPage = uiState.lastPage,
                                        onClick = { viewModel.onAction(ThreadDetailAction.ShowPageJumper) },
                                        onJumpFirst = { viewModel.onAction(ThreadDetailAction.JumpToFirstPage) },
                                        onJumpLatest = { viewModel.onAction(ThreadDetailAction.JumpToLatestPage) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            if (drillDownStack.isNotEmpty()) {
                                item(key = "drill_down_breadcrumb") {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                                    ) {
                                        FoldedAncestorStrip(
                                            breadcrumbs = drillDownBreadcrumbs,
                                            onNavigateBack = {
                                                drillDownStack = drillDownStack.dropLast(1)
                                            },
                                            onNavigateToLevel = { index ->
                                                drillDownStack = drillDownStack.take(index)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp)
                                        )
                                    }
                                }
                            }

                            if (uiState.hasEarlierComments) {
                                item(key = "load_earlier") {
                                    val perPage = uiState.perPage.coerceAtLeast(1)
                                    val pagesAbove =
                                        ((uiState.loadedPageRange?.first ?: 1) - 1).coerceAtLeast(0)
                                    val countAbove =
                                        if (uiState.totalComments > 0) (pagesAbove * perPage).coerceAtMost(
                                            uiState.totalComments
                                        ) else pagesAbove * perPage

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp, horizontal = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (uiState.isLoadingEarlierComments) CircularProgressIndicator()
                                        else {
                                            FilledTonalButton(
                                                onClick = { viewModel.onAction(ThreadDetailAction.LoadEarlierComments) },
                                                shape = RoundedCornerShape(32.dp),
                                                contentPadding = PaddingValues(
                                                    horizontal = 32.dp,
                                                    vertical = 16.dp
                                                )
                                            ) {
                                                Text(
                                                    text = stringResource(
                                                        R.string.forum_load_earlier_count,
                                                        countAbove
                                                    ), fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
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
                                    val isTarget =
                                        anchorCommentId != null && flat.comment.id == anchorCommentId
                                    val highlightColor =
                                        if (isTarget) MaterialTheme.colorScheme.tertiaryContainer.copy(
                                            alpha = highlightAlpha.value * 0.4f
                                        ) else Color.Transparent

                                    val rebasedDepth = flat.depth - depthOffset
                                    val isAtMaxDepth = rebasedDepth >= depthWindowSize - 1
                                    val canDrillDown = isAtMaxDepth && flat.descendantCount > 0

                                    val isOwnComment = uiState.viewerId != null &&
                                        flat.comment.authorId == uiState.viewerId
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = highlightColor
                                    ) {
                                        ThreadCommentItem(
                                            comment = flat.comment,
                                            isCollapsed = collapsedIds.contains(flat.comment.id),
                                            onUserClick = onUserClick,
                                            actionSlot = {
                                                if (isOwnComment) {
                                                    ForumCommentOverflowMenu(
                                                        onEdit = {
                                                            viewModel.onAction(
                                                                ThreadDetailAction.EditComment(flat.comment.id)
                                                            )
                                                        },
                                                        onDelete = {
                                                            viewModel.onAction(
                                                                ThreadDetailAction.DeleteComment(flat.comment.id)
                                                            )
                                                        }
                                                    )
                                                }
                                            },
                                            onToggleCollapse = {
                                                collapsedIds =
                                                    if (collapsedIds.contains(flat.comment.id)) collapsedIds - flat.comment.id else collapsedIds + flat.comment.id
                                            },
                                            descendantCount = flat.descendantCount,
                                            onLikeClick = { id, liked ->
                                                viewModel.onAction(
                                                    ThreadDetailAction.ToggleLike(false, id, liked)
                                                )
                                            },
                                            onLikeCountClick = { commentId ->
                                                likesTarget =
                                                    com.anisync.android.presentation.components.likes.LikesTarget.ThreadComment(commentId)
                                            },
                                            onReplyClick = if (!thread.isLocked) {
                                                { id, author ->
                                                    viewModel.onAction(
                                                        ThreadDetailAction.OpenReply(id, author)
                                                    )
                                                }
                                            } else null,
                                            threadAuthorId = thread.authorId,
                                            depth = flat.depth,
                                            depthOffset = depthOffset,
                                            maxVisualDepth = depthWindowSize,
                                            onDrillDown = if (canDrillDown) {
                                                {
                                                    drillDownStack =
                                                        drillDownStack + flat.comment.id
                                                }
                                            } else null,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            if (uiState.hasMoreComments) {
                                item(key = "load_more") {
                                    val perPage = uiState.perPage.coerceAtLeast(1)
                                    val range = uiState.loadedPageRange
                                    val pagesBelow = (uiState.lastPage - (range?.last
                                        ?: uiState.lastPage)).coerceAtLeast(0)
                                    val approxBatch = perPage.coerceAtMost(
                                        if (uiState.totalComments > 0) (uiState.totalComments - uiState.comments.size).coerceAtLeast(
                                            perPage
                                        ) else perPage
                                    )
                                    val remaining =
                                        if (uiState.totalComments > 0) (uiState.totalComments - uiState.comments.size).coerceAtLeast(
                                            pagesBelow * perPage
                                        ) else pagesBelow * perPage

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp, horizontal = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (uiState.isLoadingMoreComments) CircularProgressIndicator()
                                        else {
                                            FilledTonalButton(
                                                onClick = { viewModel.onAction(ThreadDetailAction.LoadMoreComments) },
                                                shape = RoundedCornerShape(32.dp),
                                                contentPadding = PaddingValues(
                                                    horizontal = 32.dp,
                                                    vertical = 16.dp
                                                )
                                            ) {
                                                Text(
                                                    text = stringResource(
                                                        R.string.forum_load_more_count,
                                                        approxBatch,
                                                        remaining
                                                    ), fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(112.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isReplySheetVisible) {
        val sheetTitle = if (uiState.editingCommentId != null) {
            stringResource(R.string.activity_edit_reply_title)
        } else {
            stringResource(R.string.forum_write_reply)
        }
        val sheetSubmit = if (uiState.editingCommentId != null) {
            stringResource(R.string.activity_edit_save)
        } else {
            stringResource(R.string.forum_post_reply)
        }
        RichTextInputSheet(
            title = sheetTitle,
            placeholder = stringResource(R.string.forum_reply_hint),
            submitLabel = sheetSubmit,
            replyingToLabel = uiState.replyTargetAuthorName?.let {
                stringResource(
                    R.string.forum_replying_to,
                    it
                )
            },
            prefillBody = uiState.replyPrefillBody,
            isSubmitting = uiState.isSubmittingReply,
            minLength = com.anisync.android.domain.ContentLimits.ThreadComment.min,
            maxLength = com.anisync.android.domain.ContentLimits.ThreadComment.max,
            onSubmit = { body ->
                viewModel.onAction(
                    ThreadDetailAction.SubmitReply(
                        threadId,
                        body
                    )
                )
            },
            onDismiss = { viewModel.onAction(ThreadDetailAction.CloseReply) }
        )
    }

    if (uiState.pendingThreadEdit && uiState.thread != null) {
        val thread = uiState.thread!!
        RichTextInputScreen(
            title = stringResource(R.string.forum_edit_thread_title),
            placeholder = stringResource(R.string.forum_thread_body_hint),
            initialBody = thread.bodyMarkdown ?: thread.body.orEmpty(),
            isSubmitting = uiState.isSubmittingReply,
            minLength = com.anisync.android.domain.ContentLimits.ThreadBody.min,
            maxLength = com.anisync.android.domain.ContentLimits.ThreadBody.max,
            onSubmit = { body ->
                viewModel.onAction(ThreadDetailAction.SubmitThreadEdit(body))
            },
            onDismiss = { viewModel.onAction(ThreadDetailAction.ConsumeThreadEdit) }
        )
    }

    if (showDeleteThreadDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteThreadDialog = false },
            title = { Text(stringResource(R.string.thread_delete_confirm_title)) },
            text = { Text(stringResource(R.string.thread_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteThreadDialog = false
                    viewModel.onAction(ThreadDetailAction.DeleteThread)
                }) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteThreadDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (uiState.isPageJumperVisible) {
        val jumperState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        PageJumperBottomSheet(
            currentPage = uiState.loadedPageRange?.last ?: 1,
            lastPage = uiState.lastPage,
            sheetState = jumperState,
            onDismiss = { viewModel.onAction(ThreadDetailAction.HidePageJumper) },
            onJumpTo = { page -> viewModel.onAction(ThreadDetailAction.JumpToPage(page)) },
            onJumpFirst = { viewModel.onAction(ThreadDetailAction.JumpToFirstPage) },
            onJumpLatest = { viewModel.onAction(ThreadDetailAction.JumpToLatestPage) },
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
}

@Composable
private fun PageProgressPill(
    totalComments: Int,
    currentPage: Int,
    lastPage: Int,
    onClick: () -> Unit,
    onJumpFirst: () -> Unit,
    onJumpLatest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.forum_comments_count, totalComments),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (lastPage > 1) {
                    Text(
                        text = stringResource(
                            R.string.forum_page_of,
                            currentPage.coerceIn(1, lastPage),
                            lastPage
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (lastPage > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onClick,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.forum_jump), fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.width(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun computeHeaderItemCount(
    hasParsedBody: Boolean,
    hasDrillDown: Boolean,
    hasPagePill: Boolean,
    hasLoadEarlier: Boolean,
): Int {
    var n = 1
    if (hasParsedBody) n++
    n++
    n++
    if (hasPagePill) n++
    if (hasDrillDown) n++
    if (hasLoadEarlier) n++
    return n
}

@Composable
private fun ThreadDetailSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonLine(
                fraction = 0f,
                height = 48.dp,
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                SkeletonLine(fraction = 0.5f, height = 16.dp)
                Spacer(Modifier.height(8.dp))
                SkeletonLine(fraction = 0.3f, height = 14.dp)
            }
        }
        Spacer(Modifier.height(32.dp))
        SkeletonLine(fraction = 0.9f, height = 32.dp)
        Spacer(Modifier.height(12.dp))
        SkeletonLine(fraction = 0.65f, height = 32.dp)
        Spacer(Modifier.height(28.dp))
        SkeletonLine(fraction = 1f, height = 16.dp)
        Spacer(Modifier.height(8.dp))
        SkeletonLine(fraction = 0.95f, height = 16.dp)
        Spacer(Modifier.height(8.dp))
        SkeletonLine(fraction = 0.85f, height = 16.dp)
        Spacer(Modifier.height(48.dp))
        repeat(3) {
            Row(verticalAlignment = Alignment.Top) {
                SkeletonLine(
                    fraction = 0f,
                    height = 40.dp,
                    modifier = Modifier
                        .width(40.dp)
                        .height(40.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonLine(fraction = 0.4f, height = 14.dp)
                    Spacer(Modifier.height(12.dp))
                    SkeletonLine(fraction = 0.95f, height = 16.dp)
                    Spacer(Modifier.height(6.dp))
                    SkeletonLine(fraction = 0.7f, height = 16.dp)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

internal fun flattenComments(
    comments: List<CommentNode>,
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
                    comment.childComments,
                    depth + 1,
                    ancestors + comment.id
                )
            )
        }
    }
    return flatList
}

private fun countDescendants(comment: CommentNode): Int {
    var count = comment.childComments.size
    for (child in comment.childComments) count += countDescendants(child)
    return count
}

/**
 * Overflow menu rendered next to the viewer's own forum comments. Wraps the Menu
 * (Expressive) item DSL with an Edit + Delete pair plus a confirmation dialog —
 * mirrors the same pattern used by [com.anisync.android.presentation.activity.ActivityDetailScreen]'s
 * `ReplyOverflowMenu` so the two surfaces feel identical.
 */
@Composable
private fun ForumCommentOverflowMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Sized to match the like/reply pills in the same FlowRow (32dp anchor, 18dp icon)
    // so the trio shares one baseline. Default IconButton (48dp) made the dots sit
    // lower than the pills.
    Box(modifier = Modifier.size(32.dp)) {
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