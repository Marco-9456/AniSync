package com.anisync.android.data

import com.anisync.android.CreateForumCommentMutation
import com.anisync.android.CreateForumCommentReplyMutation
import com.anisync.android.CreateForumThreadMutation
import com.anisync.android.GetForumCommentsQuery
import com.anisync.android.GetForumOverviewQuery
import com.anisync.android.GetForumThreadQuery
import com.anisync.android.GetForumThreadsQuery
import com.anisync.android.ToggleLikeThreadCommentMutation
import com.anisync.android.ToggleLikeThreadMutation
import com.anisync.android.ToggleThreadSubscriptionMutation
import com.anisync.android.data.local.dao.SavedForumThreadDao
import com.anisync.android.data.local.entity.SavedForumThreadEntity
import com.anisync.android.domain.ForumCategory
import com.anisync.android.domain.ForumComment
import com.anisync.android.domain.ForumRepository
import com.anisync.android.domain.ForumThread
import com.anisync.android.domain.PaginatedResult
import com.anisync.android.domain.Result
import com.anisync.android.type.ThreadSort
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ForumRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient,
    private val savedForumThreadDao: SavedForumThreadDao
) : ForumRepository {

    // =========================================================================
    // READ: Recent threads (Forum hub / overview)
    // =========================================================================

    override suspend fun getRecentThreads(
        page: Int,
        sort: String?
    ): Result<PaginatedResult<ForumThread>> {
        return runCatchingApi("load forum threads") {
            val sortList = sort?.split(",")
                ?.mapNotNull { s -> ThreadSort.entries.find { it.name == s.trim() } }
                ?: listOf(ThreadSort.IS_STICKY, ThreadSort.REPLIED_AT_DESC)

            val response = apolloClient
                .query(
                    GetForumOverviewQuery(
                        page = Optional.present(page),
                        sort = Optional.present(sortList)
                    )
                )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            val pageData = response.data?.Page
            val threads =
                pageData?.threads?.filterNotNull()?.map { it.toForumThread() } ?: emptyList()

            PaginatedResult(
                items = threads,
                hasNextPage = pageData?.pageInfo?.hasNextPage ?: false,
                currentPage = pageData?.pageInfo?.currentPage ?: page,
                totalPages = 0
            )
        }
    }

    // =========================================================================
    // READ: Threads by category / search
    // =========================================================================

    override suspend fun getThreadsByCategory(
        categoryId: Int?,
        search: String?,
        page: Int
    ): Result<PaginatedResult<ForumThread>> {
        return runCatchingApi("load forum threads") {
            val response = apolloClient
                .query(
                    GetForumThreadsQuery(
                        page = Optional.present(page),
                        categoryId = categoryId?.let { Optional.present(it) } ?: Optional.absent(),
                        search = search?.takeIf { it.isNotBlank() }?.let { Optional.present(it) }
                            ?: Optional.absent()
                    )
                )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            val pageData = response.data?.Page
            val threads =
                pageData?.threads?.filterNotNull()?.map { it.toForumThread() } ?: emptyList()

            PaginatedResult(
                items = threads,
                hasNextPage = pageData?.pageInfo?.hasNextPage ?: false,
                currentPage = pageData?.pageInfo?.currentPage ?: page,
                totalPages = 0
            )
        }
    }

    // =========================================================================
    // READ: Single thread detail
    // =========================================================================

    override suspend fun getThread(threadId: Int): Result<ForumThread> {
        return runCatchingApi("load thread details") {
            val response = apolloClient
                .query(GetForumThreadQuery(id = threadId))
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            response.data?.Thread?.toForumThread()
                ?: throw IllegalStateException("Thread not found")
        }
    }

    // =========================================================================
    // READ: Thread comments (Recursive tree build)
    // =========================================================================

    override suspend fun getComments(
        threadId: Int,
        page: Int,
        sort: String?
    ): Result<PaginatedResult<ForumComment>> {
        return runCatchingApi("load comments") {
            val response = apolloClient
                .query(GetForumCommentsQuery(threadId = threadId, page = Optional.present(page)))
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            val pageData = response.data?.Page
            val rootNodes = pageData?.threadComments?.filterNotNull() ?: emptyList()

            val rootComments = rootNodes.mapNotNull { mapToForumComment(it) }

            PaginatedResult(
                items = rootComments,
                hasNextPage = pageData?.pageInfo?.hasNextPage ?: false,
                currentPage = pageData?.pageInfo?.currentPage ?: page,
                totalPages = 0
            )
        }
    }

    /**
     * Handles recursive conversion of both Apollo types and nested Map types
     * resulting from the Json scalar Any-mapping.
     */
    private fun mapToForumComment(node: Any?): ForumComment? {
        if (node == null) return null

        return when (node) {
            is GetForumCommentsQuery.ThreadComment -> {
                val children =
                    (node.childComments as? List<*>)?.mapNotNull { mapToForumComment(it) }
                        ?: emptyList()
                ForumComment(
                    id = node.id ?: 0,
                    body = node.comment ?: "",
                    likeCount = node.likeCount ?: 0,
                    isLiked = node.isLiked ?: false,
                    authorId = node.user?.id ?: 0,
                    authorName = node.user?.name ?: "Unknown",
                    authorAvatarUrl = node.user?.avatar?.large,
                    createdAt = (node.createdAt ?: 0).toLong(),
                    siteUrl = node.siteUrl,
                    childComments = children
                )
            }

            is Map<*, *> -> {
                val userMap = node["user"] as? Map<*, *>
                val avatarMap = userMap?.get("avatar") as? Map<*, *>
                val children =
                    (node["childComments"] as? List<*>)?.mapNotNull { mapToForumComment(it) }
                        ?: emptyList()

                ForumComment(
                    id = (node["id"] as? Number)?.toInt() ?: 0,
                    body = node["comment"] as? String ?: "",
                    likeCount = (node["likeCount"] as? Number)?.toInt() ?: 0,
                    isLiked = node["isLiked"] as? Boolean ?: false,
                    authorId = (userMap?.get("id") as? Number)?.toInt() ?: 0,
                    authorName = userMap?.get("name") as? String ?: "Unknown",
                    authorAvatarUrl = avatarMap?.get("large") as? String,
                    createdAt = (node["createdAt"] as? Number)?.toLong() ?: 0L,
                    siteUrl = node["siteUrl"] as? String,
                    childComments = children
                )
            }

            else -> null
        }
    }

    // =========================================================================
    // WRITE: Mutations
    // =========================================================================

    override suspend fun createThread(
        title: String,
        body: String,
        categoryIds: List<Int>
    ): Result<ForumThread> {
        return runCatchingApi("create thread") {
            val response = apolloClient.mutation(
                CreateForumThreadMutation(
                    title = title,
                    body = body,
                    categories = categoryIds
                )
            ).execute()
            response.data?.SaveThread?.toForumThread()
                ?: throw IllegalStateException("Failed to create thread")
        }
    }

    override suspend fun createComment(threadId: Int, comment: String): Result<ForumComment> {
        return runCatchingApi("post comment") {
            val response = apolloClient.mutation(
                CreateForumCommentMutation(
                    threadId = threadId,
                    comment = comment
                )
            ).execute()
            response.data?.SaveThreadComment?.toForumComment()
                ?: throw IllegalStateException("Failed to post comment")
        }
    }

    override suspend fun replyToComment(
        threadId: Int,
        comment: String,
        parentCommentId: Int
    ): Result<ForumComment> {
        return runCatchingApi("post reply") {
            val response = apolloClient.mutation(
                CreateForumCommentReplyMutation(
                    threadId = threadId,
                    comment = comment,
                    parentCommentId = parentCommentId
                )
            ).execute()
            response.data?.SaveThreadComment?.toForumComment()
                ?: throw IllegalStateException("Failed to post reply")
        }
    }

    override suspend fun toggleLikeThread(threadId: Int): Result<Unit> {
        return runCatchingApi("update like") {
            apolloClient.mutation(ToggleLikeThreadMutation(id = threadId)).execute()
        }
    }

    override suspend fun toggleLikeComment(commentId: Int): Result<Unit> {
        return runCatchingApi("update like") {
            apolloClient.mutation(
                ToggleLikeThreadCommentMutation(
                    id = commentId
                )
            ).execute()
        }
    }

    override suspend fun getSubscribedThreads(page: Int): Result<PaginatedResult<ForumThread>> {
        return runCatchingApi("load subscribed threads") {
            val response = apolloClient.query(
                GetForumOverviewQuery(
                    page = Optional.present(page),
                    sort = Optional.present(listOf(ThreadSort.REPLIED_AT_DESC)),
                    subscribed = Optional.present(true)
                )
            ).fetchPolicy(FetchPolicy.NetworkOnly).execute()
            val threads = response.data?.Page?.threads?.filterNotNull()?.map { it.toForumThread() }
                ?: emptyList()
            PaginatedResult(
                items = threads,
                hasNextPage = response.data?.Page?.pageInfo?.hasNextPage ?: false,
                currentPage = response.data?.Page?.pageInfo?.currentPage ?: page,
                totalPages = 0
            )
        }
    }

    override suspend fun toggleThreadSubscription(threadId: Int, subscribe: Boolean): Result<Unit> {
        return runCatchingApi("update subscription") {
            apolloClient.mutation(
                ToggleThreadSubscriptionMutation(threadId = threadId, subscribe = subscribe)
            ).execute()
        }
    }

    // =========================================================================
    // LOCAL: Saved Threads (Persistence)
    // =========================================================================

    override suspend fun getSavedThreads(): List<ForumThread> {
        return savedForumThreadDao.getAll().first().map { entity ->
            ForumThread(
                id = entity.threadId,
                title = entity.title,
                body = null,
                replyCount = entity.replyCount,
                viewCount = entity.viewCount,
                likeCount = entity.likeCount,
                isLiked = entity.isLiked,
                isSubscribed = false,
                isLocked = entity.isLocked,
                authorId = 0,
                authorName = entity.authorName,
                authorAvatarUrl = entity.authorAvatarUrl,
                repliedAt = entity.repliedAt,
                replyUserName = entity.replyUserName,
                replyUserAvatarUrl = entity.replyUserAvatarUrl,
                categories = entity.categories,
                mediaTitle = entity.mediaTitle,
                mediaCoverUrl = entity.mediaCoverUrl,
                createdAt = entity.savedAt,
                updatedAt = entity.savedAt,
                siteUrl = null
            )
        }
    }

    override suspend fun saveThread(thread: ForumThread) {
        savedForumThreadDao.insert(
            SavedForumThreadEntity(
                threadId = thread.id,
                title = thread.title,
                authorName = thread.authorName,
                authorAvatarUrl = thread.authorAvatarUrl,
                replyCount = thread.replyCount,
                viewCount = thread.viewCount,
                likeCount = thread.likeCount,
                isLiked = thread.isLiked,
                isLocked = thread.isLocked,
                repliedAt = thread.repliedAt,
                replyUserName = thread.replyUserName,
                replyUserAvatarUrl = thread.replyUserAvatarUrl,
                categories = thread.categories,
                mediaTitle = thread.mediaTitle,
                mediaCoverUrl = thread.mediaCoverUrl
            )
        )
    }

    override suspend fun unsaveThread(threadId: Int) = savedForumThreadDao.delete(threadId)
    override suspend fun isThreadSaved(threadId: Int) = savedForumThreadDao.exists(threadId)

    // =========================================================================
    // PRIVATE MAPPERS
    // =========================================================================

    private fun buildForumThread(
        id: Int?,
        title: String?,
        body: String? = null,
        replyCount: Int?,
        viewCount: Int?,
        likeCount: Int?,
        isLiked: Boolean?,
        isSubscribed: Boolean?,
        isLocked: Boolean?,
        isSticky: Boolean? = null,
        userId: Int?,
        userName: String?,
        userAvatarLarge: String?,
        repliedAt: Int? = null,
        replyUserName: String? = null,
        replyUserAvatarLarge: String? = null,
        replyCommentId: Int? = null,
        createdAt: Int?,
        updatedAt: Int? = null,
        siteUrl: String? = null,
        categories: List<Pair<Int?, String?>>? = null,
        mediaTitle: String? = null,
        mediaCoverUrl: String? = null
    ) = ForumThread(
        id = id ?: 0,
        title = title ?: "(Untitled)",
        body = body,
        replyCount = replyCount ?: 0,
        viewCount = viewCount ?: 0,
        likeCount = likeCount ?: 0,
        isLiked = isLiked ?: false,
        isSubscribed = isSubscribed ?: false,
        isLocked = isLocked ?: false,
        isSticky = isSticky ?: false,
        authorId = userId ?: 0,
        authorName = userName ?: "Unknown",
        authorAvatarUrl = userAvatarLarge,
        repliedAt = repliedAt?.toLong(),
        replyUserName = replyUserName,
        replyUserAvatarUrl = replyUserAvatarLarge,
        replyCommentId = replyCommentId,
        categories = categories?.map { (cId, cName) -> ForumCategory(cId ?: 0, cName ?: "") }
            ?: emptyList(),
        createdAt = (createdAt ?: 0).toLong(),
        updatedAt = (updatedAt ?: repliedAt ?: createdAt ?: 0).toLong(),
        siteUrl = siteUrl,
        mediaTitle = mediaTitle,
        mediaCoverUrl = mediaCoverUrl)

    private fun GetForumOverviewQuery.Thread.toForumThread() = buildForumThread(
        id = id,
        title = title,
        replyCount = replyCount,
        viewCount = viewCount,
        likeCount = likeCount,
        isLiked = isLiked,
        isSubscribed = isSubscribed,
        isLocked = isLocked,
        isSticky = isSticky,
        userId = user?.id,
        userName = user?.name,
        userAvatarLarge = user?.avatar?.large,
        repliedAt = repliedAt,
        replyUserName = replyUser?.name,
        replyUserAvatarLarge = replyUser?.avatar?.large,
        replyCommentId = replyCommentId,
        createdAt = createdAt,
        categories = categories?.filterNotNull()?.map { it.id to it.name },
        mediaTitle = mediaCategories?.firstOrNull()?.title?.romaji,
        mediaCoverUrl = mediaCategories?.firstOrNull()?.coverImage?.medium
    )

    private fun GetForumThreadsQuery.Thread.toForumThread() = buildForumThread(
        id = id,
        title = title,
        body = body,
        replyCount = replyCount,
        viewCount = viewCount,
        likeCount = likeCount,
        isLiked = isLiked,
        isSubscribed = isSubscribed,
        isLocked = isLocked,
        isSticky = isSticky,
        userId = user?.id,
        userName = user?.name,
        userAvatarLarge = user?.avatar?.large,
        repliedAt = repliedAt,
        replyUserName = replyUser?.name,
        replyUserAvatarLarge = replyUser?.avatar?.large,
        replyCommentId = replyCommentId,
        createdAt = createdAt,
        categories = categories?.filterNotNull()?.map { it.id to it.name },
        mediaTitle = mediaCategories?.firstOrNull()?.title?.romaji,
        mediaCoverUrl = mediaCategories?.firstOrNull()?.coverImage?.medium
    )

    private fun GetForumThreadQuery.Thread.toForumThread() = buildForumThread(
        id = id,
        title = title,
        body = body,
        replyCount = replyCount,
        viewCount = viewCount,
        likeCount = likeCount,
        isLiked = isLiked,
        isSubscribed = isSubscribed,
        isLocked = isLocked,
        isSticky = isSticky,
        userId = user?.id,
        userName = user?.name,
        userAvatarLarge = user?.avatar?.large,
        repliedAt = repliedAt,
        replyUserName = replyUser?.name,
        replyUserAvatarLarge = replyUser?.avatar?.large,
        createdAt = createdAt,
        updatedAt = updatedAt,
        siteUrl = siteUrl,
        categories = categories?.filterNotNull()?.map { it.id to it.name },
        mediaTitle = mediaCategories?.firstOrNull()?.title?.romaji,
        mediaCoverUrl = mediaCategories?.firstOrNull()?.coverImage?.medium
    )

    private fun CreateForumThreadMutation.SaveThread.toForumThread() = buildForumThread(
        id = id,
        title = title,
        body = body,
        replyCount = replyCount,
        viewCount = viewCount,
        likeCount = likeCount,
        isLiked = isLiked,
        isSubscribed = null,
        isLocked = isLocked,
        userId = user?.id,
        userName = user?.name,
        userAvatarLarge = user?.avatar?.large,
        createdAt = createdAt,
        siteUrl = siteUrl,
        categories = categories?.filterNotNull()?.map { it.id to it.name })

    private fun CreateForumCommentMutation.SaveThreadComment.toForumComment() = ForumComment(
        id = id ?: 0,
        body = comment ?: "",
        likeCount = likeCount ?: 0,
        isLiked = isLiked ?: false,
        authorId = user?.id ?: 0,
        authorName = user?.name ?: "Unknown",
        authorAvatarUrl = user?.avatar?.large,
        createdAt = (createdAt ?: 0).toLong(),
        siteUrl = siteUrl
    )

    private fun CreateForumCommentReplyMutation.SaveThreadComment.toForumComment() = ForumComment(
        id = id ?: 0,
        body = comment ?: "",
        likeCount = likeCount ?: 0,
        isLiked = isLiked ?: false,
        authorId = user?.id ?: 0,
        authorName = user?.name ?: "Unknown",
        authorAvatarUrl = user?.avatar?.large,
        createdAt = (createdAt ?: 0).toLong(),
        siteUrl = siteUrl
    )

    /**
     * Delegates to the centralized [safeApiCall] for consistent error handling.
     * Kept as a wrapper to preserve the `action` parameter for context in logs.
     */
    private suspend inline fun <T> runCatchingApi(action: String, crossinline block: suspend () -> T): Result<T> {
        return com.anisync.android.data.util.safeApiCall { block() }
    }
}