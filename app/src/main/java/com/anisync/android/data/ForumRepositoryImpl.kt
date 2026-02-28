package com.anisync.android.data

import com.anisync.android.CreateForumCommentMutation
import com.anisync.android.CreateForumCommentReplyMutation
import com.anisync.android.CreateForumThreadMutation
import com.anisync.android.GetForumOverviewQuery
import com.anisync.android.GetForumThreadQuery
import com.anisync.android.GetForumThreadsQuery
import com.anisync.android.GetThreadCommentsQuery
import com.anisync.android.ToggleLikeThreadCommentMutation
import com.anisync.android.ToggleLikeThreadMutation
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
import com.apollographql.apollo.exception.ApolloException
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class ForumRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient
) : ForumRepository {

    // =========================================================================
    // READ: Recent threads (Forum hub / overview)
    // =========================================================================

    override suspend fun getRecentThreads(page: Int, sort: String?): Result<PaginatedResult<ForumThread>> {
        return try {
            // Parse sort string like "IS_STICKY,REPLIED_AT_DESC" into ThreadSort enums
            val sortList = sort?.split(",")
                ?.mapNotNull { s -> ThreadSort.entries.find { it.name == s.trim() } }
                ?: listOf(ThreadSort.IS_STICKY, ThreadSort.REPLIED_AT_DESC)

            val response = apolloClient
                .query(GetForumOverviewQuery(
                    page = Optional.present(page),
                    sort = Optional.present(sortList)
                ))
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            val pageData = response.data?.Page
            val threads = pageData?.threads
                ?.filterNotNull()
                ?.map { it.toForumThread() }
                ?: emptyList()

            val pageInfo = pageData?.pageInfo
            Result.Success(
                PaginatedResult(
                    items = threads,
                    hasNextPage = pageInfo?.hasNextPage ?: false,
                    currentPage = pageInfo?.currentPage ?: page,
                    totalPages = 0
                )
            )
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
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
        return try {
            val response = apolloClient
                .query(
                    GetForumThreadsQuery(
                        page = Optional.present(page),
                        categoryId = categoryId?.let { Optional.present(it) } ?: Optional.absent(),
                        search = search?.takeIf { it.isNotBlank() }
                            ?.let { Optional.present(it) } ?: Optional.absent()
                    )
                )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            val pageData = response.data?.Page
            val threads = pageData?.threads
                ?.filterNotNull()
                ?.map { it.toForumThread() }
                ?: emptyList()

            val pageInfo = pageData?.pageInfo
            Result.Success(
                PaginatedResult(
                    items = threads,
                    hasNextPage = pageInfo?.hasNextPage ?: false,
                    currentPage = pageInfo?.currentPage ?: page,
                    totalPages = 0
                )
            )
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    // =========================================================================
    // READ: Single thread detail
    // =========================================================================

    override suspend fun getThread(threadId: Int): Result<ForumThread> {
        return try {
            val response = apolloClient
                .query(GetForumThreadQuery(id = threadId))
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            val thread = response.data?.Thread ?: return Result.Error("Thread not found")
            Result.Success(thread.toForumThread())
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    // =========================================================================
    // READ: Thread comments (paginated)
    // =========================================================================

    override suspend fun getComments(threadId: Int, page: Int): Result<PaginatedResult<ForumComment>> {
        return try {
            val response = apolloClient
                .query(GetThreadCommentsQuery(threadId = threadId, page = Optional.present(page)))
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            val pageData = response.data?.Page
            val allComments = pageData?.threadComments
                ?.filterNotNull()
                ?.map { it.toForumComment() }
                ?: emptyList()

            // AniList returns ALL comments flat (including replies). Each root comment
            // also embeds its children in the `childComments` JSON field. We must
            // deduplicate by collecting all child IDs and showing only roots at top level.
            val childIds = mutableSetOf<Int>()
            fun collectChildIds(comments: List<ForumComment>) {
                for (c in comments) {
                    for (child in c.childComments) {
                        childIds.add(child.id)
                        collectChildIds(child.childComments)
                    }
                }
            }
            collectChildIds(allComments)
            val rootComments = allComments.filter { it.id !in childIds }

            val pageInfo = pageData?.pageInfo
            Result.Success(
                PaginatedResult(
                    items = rootComments,
                    hasNextPage = pageInfo?.hasNextPage ?: false,
                    currentPage = pageInfo?.currentPage ?: page,
                    totalPages = 0
                )
            )
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    // =========================================================================
    // WRITE: Create thread
    // =========================================================================

    override suspend fun createThread(
        title: String,
        body: String,
        categoryIds: List<Int>
    ): Result<ForumThread> {
        return try {
            val response = apolloClient
                .mutation(
                    CreateForumThreadMutation(
                        title = title,
                        body = body,
                        categories = categoryIds
                    )
                )
                .execute()

            val thread = response.data?.SaveThread ?: return Result.Error("Failed to create thread")
            Result.Success(thread.toForumThread())
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    // =========================================================================
    // WRITE: Create comment
    // =========================================================================

    override suspend fun createComment(threadId: Int, comment: String): Result<ForumComment> {
        return try {
            val response = apolloClient
                .mutation(CreateForumCommentMutation(threadId = threadId, comment = comment))
                .execute()

            val saved = response.data?.SaveThreadComment
                ?: return Result.Error("Failed to post comment")
            Result.Success(saved.toForumComment())
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    // =========================================================================
    // WRITE: Reply to comment
    // =========================================================================

    override suspend fun replyToComment(
        threadId: Int,
        comment: String,
        parentCommentId: Int
    ): Result<ForumComment> {
        return try {
            val response = apolloClient
                .mutation(
                    CreateForumCommentReplyMutation(
                        threadId = threadId,
                        comment = comment,
                        parentCommentId = parentCommentId
                    )
                )
                .execute()

            val saved = response.data?.SaveThreadComment
                ?: return Result.Error("Failed to post reply")
            Result.Success(saved.toForumComment())
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    // =========================================================================
    // WRITE: Toggle like (thread)
    // =========================================================================

    override suspend fun toggleLikeThread(threadId: Int): Result<Unit> {
        return try {
            apolloClient
                .mutation(ToggleLikeThreadMutation(id = threadId))
                .execute()
            Result.Success(Unit)
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    // =========================================================================
    // WRITE: Toggle like (comment)
    // =========================================================================

    override suspend fun toggleLikeComment(commentId: Int): Result<Unit> {
        return try {
            apolloClient
                .mutation(ToggleLikeThreadCommentMutation(id = commentId))
                .execute()
            Result.Success(Unit)
        } catch (e: ApolloException) {
            Result.Error("Network error: ${e.message}", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }

    // =========================================================================
    // PRIVATE MAPPERS
    // =========================================================================

    private fun GetForumOverviewQuery.Thread.toForumThread() = ForumThread(
        id = id ?: 0,
        title = title ?: "(Untitled)",
        body = null,
        replyCount = replyCount ?: 0,
        viewCount = viewCount ?: 0,
        likeCount = likeCount ?: 0,
        isLiked = isLiked ?: false,
        isSubscribed = false,
        isLocked = isLocked ?: false,
        authorId = user?.id ?: 0,
        authorName = user?.name ?: "Unknown",
        authorAvatarUrl = user?.avatar?.large,
        categories = categories?.filterNotNull()?.map { ForumCategory(it.id ?: 0, it.name ?: "") } ?: emptyList(),
        createdAt = (createdAt ?: 0).toLong(),
        updatedAt = (repliedAt ?: createdAt ?: 0).toLong(),
        siteUrl = null
    )

    private fun GetForumThreadsQuery.Thread.toForumThread() = ForumThread(
        id = id ?: 0,
        title = title ?: "(Untitled)",
        body = null,
        replyCount = replyCount ?: 0,
        viewCount = viewCount ?: 0,
        likeCount = likeCount ?: 0,
        isLiked = isLiked ?: false,
        isSubscribed = false,
        isLocked = isLocked ?: false,
        authorId = user?.id ?: 0,
        authorName = user?.name ?: "Unknown",
        authorAvatarUrl = user?.avatar?.large,
        categories = categories?.filterNotNull()?.map { ForumCategory(it.id ?: 0, it.name ?: "") } ?: emptyList(),
        createdAt = (createdAt ?: 0).toLong(),
        updatedAt = (repliedAt ?: createdAt ?: 0).toLong(),
        siteUrl = null
    )

    private fun GetForumThreadQuery.Thread.toForumThread() = ForumThread(
        id = id ?: 0,
        title = title ?: "(Untitled)",
        body = body,
        replyCount = replyCount ?: 0,
        viewCount = viewCount ?: 0,
        likeCount = likeCount ?: 0,
        isLiked = isLiked ?: false,
        isSubscribed = isSubscribed ?: false,
        isLocked = isLocked ?: false,
        authorId = user?.id ?: 0,
        authorName = user?.name ?: "Unknown",
        authorAvatarUrl = user?.avatar?.large,
        categories = categories?.filterNotNull()?.map { ForumCategory(it.id ?: 0, it.name ?: "") } ?: emptyList(),
        createdAt = (createdAt ?: 0).toLong(),
        updatedAt = (updatedAt ?: createdAt ?: 0).toLong(),
        siteUrl = siteUrl
    )

    private fun CreateForumThreadMutation.SaveThread.toForumThread() = ForumThread(
        id = id ?: 0,
        title = title ?: "(Untitled)",
        body = body,
        replyCount = replyCount ?: 0,
        viewCount = viewCount ?: 0,
        likeCount = likeCount ?: 0,
        isLiked = isLiked ?: false,
        isSubscribed = false,
        isLocked = isLocked ?: false,
        authorId = user?.id ?: 0,
        authorName = user?.name ?: "Unknown",
        authorAvatarUrl = user?.avatar?.large,
        categories = categories?.filterNotNull()?.map { ForumCategory(it.id ?: 0, it.name ?: "") } ?: emptyList(),
        createdAt = (createdAt ?: 0).toLong(),
        updatedAt = (createdAt ?: 0).toLong(),
        siteUrl = siteUrl
    )

    private fun GetThreadCommentsQuery.ThreadComment.toForumComment(): ForumComment {
        val children = parseChildComments(childComments)
        return ForumComment(
            id = id ?: 0,
            body = comment ?: "",
            likeCount = likeCount ?: 0,
            isLiked = isLiked ?: false,
            authorId = user?.id ?: 0,
            authorName = user?.name ?: "Unknown",
            authorAvatarUrl = user?.avatar?.large,
            createdAt = (createdAt ?: 0).toLong(),
            siteUrl = siteUrl,
            childComments = children
        )
    }

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
     * AniList returns nested child comments as a raw JSON string.
     * We parse it recursively up to a max depth of 3.
     */
    private fun parseChildComments(rawJson: String?, depth: Int = 0): List<ForumComment> {
        if (depth >= 3) return emptyList()
        if (rawJson.isNullOrBlank() || rawJson == "null" || rawJson == "[]") return emptyList()
        return try {
            val array = JSONArray(rawJson)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                parseChildCommentObject(obj, depth)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseChildCommentObject(obj: JSONObject, depth: Int = 0): ForumComment {
        val userObj = obj.optJSONObject("user")
        val avatarObj = userObj?.optJSONObject("avatar")

        // Recursively parse nested children
        val nestedChildrenRaw = obj.opt("childComments")
        val nestedChildren = when (nestedChildrenRaw) {
            is JSONArray -> parseChildComments(nestedChildrenRaw.toString(), depth + 1)
            is String -> parseChildComments(nestedChildrenRaw, depth + 1)
            else -> emptyList()
        }

        return ForumComment(
            id = obj.optInt("id"),
            body = obj.optString("comment", ""),
            likeCount = obj.optInt("likeCount"),
            isLiked = obj.optBoolean("isLiked"),
            authorId = userObj?.optInt("id") ?: 0,
            authorName = userObj?.optString("name", "Unknown") ?: "Unknown",
            authorAvatarUrl = avatarObj?.optString("large"),
            createdAt = obj.optLong("createdAt"),
            siteUrl = obj.optString("siteUrl").takeIf { it.isNotBlank() },
            childComments = nestedChildren
        )
    }
}
