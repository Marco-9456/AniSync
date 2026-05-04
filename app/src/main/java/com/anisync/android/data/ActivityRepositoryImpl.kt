package com.anisync.android.data

import com.anisync.android.DeleteActivityMutation
import com.anisync.android.DeleteActivityReplyMutation
import com.anisync.android.GetActivityLikesQuery
import com.anisync.android.GetActivityQuery
import com.anisync.android.GetActivityReplyLikesQuery
import com.anisync.android.GetViewerQuery
import com.anisync.android.SaveActivityReplyMutation
import com.anisync.android.SaveTextActivityMutation
import com.anisync.android.ToggleActivityLikeMutation
import com.anisync.android.ToggleActivityReplyLikeMutation
import com.anisync.android.ToggleActivitySubscriptionMutation
import com.anisync.android.data.util.ApiError
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.ActivityDetail
import com.anisync.android.domain.ActivityReply
import com.anisync.android.domain.ActivityRepository
import com.anisync.android.domain.LikeState
import com.anisync.android.domain.Result
import com.anisync.android.domain.UserSummary
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import javax.inject.Inject

class ActivityRepositoryImpl @Inject constructor(
    private val apolloClient: ApolloClient
) : ActivityRepository {

    @Volatile private var cachedViewerId: Int? = null

    override suspend fun getActivity(id: Int): Result<ActivityDetail> = safeApiCall {
        val response = apolloClient
            .query(GetActivityQuery(id))
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()

        if (response.hasErrors()) {
            throw Exception(response.errors?.first()?.message ?: "Failed to load activity")
        }

        val activity = response.data?.Activity
            ?: throw ApiError.GraphQLError(listOf("Activity not found"), 404)

        activity.toDomain() ?: throw Exception("Unsupported activity type")
    }

    override suspend fun sendReply(activityId: Int, text: String): Result<ActivityReply> = safeApiCall {
        val response = apolloClient
            .mutation(SaveActivityReplyMutation(activityId = activityId, text = text))
            .execute()

        if (response.hasErrors()) {
            throw Exception(response.errors?.first()?.message ?: "Failed to send reply")
        }

        val reply = response.data?.SaveActivityReply
            ?: throw Exception("Empty reply response")

        ActivityReply(
            id = reply.id,
            body = reply.text.orEmpty(),
            likeCount = reply.likeCount,
            isLiked = reply.isLiked == true,
            authorId = reply.user?.id ?: 0,
            authorName = reply.user?.name.orEmpty(),
            authorAvatarUrl = reply.user?.avatar?.large,
            createdAt = reply.createdAt.toLong()
        )
    }

    override suspend fun toggleActivityLike(id: Int): Result<LikeState> = safeApiCall {
        val response = apolloClient
            .mutation(ToggleActivityLikeMutation(id))
            .execute()

        if (response.hasErrors()) {
            throw Exception(response.errors?.first()?.message ?: "Failed to toggle like")
        }

        val result = response.data?.ToggleLikeV2
            ?: throw Exception("Empty like response")

        val text = result.onTextActivity
        val message = result.onMessageActivity
        val list = result.onListActivity
        when {
            text != null -> LikeState(text.id, text.likeCount, text.isLiked == true)
            message != null -> LikeState(message.id, message.likeCount, message.isLiked == true)
            list != null -> LikeState(list.id, list.likeCount, list.isLiked == true)
            else -> throw Exception("Unsupported like target")
        }
    }

    override suspend fun toggleReplyLike(id: Int): Result<LikeState> = safeApiCall {
        val response = apolloClient
            .mutation(ToggleActivityReplyLikeMutation(id))
            .execute()

        if (response.hasErrors()) {
            throw Exception(response.errors?.first()?.message ?: "Failed to toggle like")
        }

        val reply = response.data?.ToggleLikeV2?.onActivityReply
            ?: throw Exception("Unsupported like target")

        LikeState(reply.id, reply.likeCount, reply.isLiked == true)
    }

    override suspend fun deleteActivity(id: Int): Result<Unit> = safeApiCall {
        val response = apolloClient
            .mutation(DeleteActivityMutation(id))
            .execute()
        if (response.hasErrors()) {
            throw Exception(response.errors?.first()?.message ?: "Failed to delete activity")
        }
        if (response.data?.DeleteActivity?.deleted != true) {
            throw Exception("Activity was not deleted")
        }
        Unit
    }

    override suspend fun deleteReply(id: Int): Result<Unit> = safeApiCall {
        val response = apolloClient
            .mutation(DeleteActivityReplyMutation(id))
            .execute()
        if (response.hasErrors()) {
            throw Exception(response.errors?.first()?.message ?: "Failed to delete reply")
        }
        if (response.data?.DeleteActivityReply?.deleted != true) {
            throw Exception("Reply was not deleted")
        }
        Unit
    }

    override suspend fun saveTextActivity(text: String): Result<Unit> = safeApiCall {
        val response = apolloClient
            .mutation(SaveTextActivityMutation(text = text))
            .execute()
        if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Failed to post status")
        }
        if (response.data?.SaveTextActivity?.id == null) {
            throw Exception("Status was not posted")
        }
        Unit
    }

    override suspend fun toggleSubscription(activityId: Int, subscribe: Boolean): Result<Unit> = safeApiCall {
        val response = apolloClient
            .mutation(ToggleActivitySubscriptionMutation(activityId = activityId, subscribe = subscribe))
            .execute()
        if (response.hasErrors()) {
            throw Exception(response.errors?.first()?.message ?: "Failed to toggle subscription")
        }
        Unit
    }

    override suspend fun getActivityLikes(activityId: Int): Result<List<UserSummary>> = safeApiCall {
        val response = apolloClient
            .query(GetActivityLikesQuery(activityId))
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()

        if (response.hasErrors()) {
            throw Exception(response.errors?.first()?.message ?: "Failed to load likes")
        }

        val activity = response.data?.Activity
            ?: throw ApiError.GraphQLError(listOf("Activity not found"), 404)

        // Each fragment generates its own Like type, so map per-variant rather
        // than relying on a single `?:` chain returning a uniform type.
        activity.onTextActivity?.likes?.filterNotNull()?.map {
            UserSummary(it.id, it.name, it.avatar?.large ?: it.avatar?.medium)
        } ?: activity.onListActivity?.likes?.filterNotNull()?.map {
            UserSummary(it.id, it.name, it.avatar?.large ?: it.avatar?.medium)
        } ?: activity.onMessageActivity?.likes?.filterNotNull()?.map {
            UserSummary(it.id, it.name, it.avatar?.large ?: it.avatar?.medium)
        } ?: emptyList()
    }

    override suspend fun getActivityReplyLikes(replyId: Int): Result<List<UserSummary>> = safeApiCall {
        val response = apolloClient
            .query(GetActivityReplyLikesQuery(replyId))
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()

        if (response.hasErrors()) {
            throw Exception(response.errors?.first()?.message ?: "Failed to load likes")
        }

        val reply = response.data?.ActivityReply
            ?: throw Exception("Reply not found")

        reply.likes?.filterNotNull()?.map { user ->
            UserSummary(
                id = user.id,
                name = user.name,
                avatarUrl = user.avatar?.large ?: user.avatar?.medium
            )
        } ?: emptyList()
    }

    override suspend fun getViewerId(): Int? {
        cachedViewerId?.let { return it }
        return try {
            val response = apolloClient
                .query(GetViewerQuery())
                .fetchPolicy(FetchPolicy.CacheFirst)
                .execute()
            val id = response.data?.Viewer?.id
            if (id != null) cachedViewerId = id
            id
        } catch (_: Exception) {
            null
        }
    }

    private fun GetActivityQuery.Activity.toDomain(): ActivityDetail? {
        onListActivity?.let { l ->
            val mediaTitle = l.media?.title?.userPreferred ?: "media"
            val mediaUrl = l.media?.siteUrl
            val statusText = (l.status ?: "Updated").trim()
            val progressText = l.progress?.trim().orEmpty()
            val mediaLink = if (mediaUrl != null) "[$mediaTitle]($mediaUrl)" else mediaTitle
            val body = buildString {
                append(statusText.replaceFirstChar { it.titlecase() })
                if (progressText.isNotEmpty()) append(" ").append(progressText).append(" of")
                append(" ").append(mediaLink)
            }
            return ActivityDetail(
                id = l.id,
                body = body,
                createdAt = l.createdAt.toLong(),
                likeCount = l.likeCount,
                isLiked = l.isLiked == true,
                replyCount = l.replyCount,
                siteUrl = l.siteUrl,
                isMessage = false,
                isPrivate = false,
                authorId = l.user?.id ?: 0,
                authorName = l.user?.name.orEmpty(),
                authorAvatarUrl = l.user?.avatar?.large,
                recipientId = null,
                recipientName = null,
                recipientAvatarUrl = null,
                replies = l.replies?.filterNotNull()?.map { r ->
                    ActivityReply(
                        id = r.id,
                        body = r.text.orEmpty(),
                        likeCount = r.likeCount,
                        isLiked = r.isLiked == true,
                        authorId = r.user?.id ?: 0,
                        authorName = r.user?.name.orEmpty(),
                        authorAvatarUrl = r.user?.avatar?.large,
                        createdAt = r.createdAt.toLong()
                    )
                } ?: emptyList()
            )
        }
        onTextActivity?.let { t ->
            return ActivityDetail(
                id = t.id,
                body = t.text.orEmpty(),
                createdAt = t.createdAt.toLong(),
                likeCount = t.likeCount,
                isLiked = t.isLiked == true,
                replyCount = t.replyCount,
                siteUrl = t.siteUrl,
                isMessage = false,
                isPrivate = false,
                authorId = t.user?.id ?: 0,
                authorName = t.user?.name.orEmpty(),
                authorAvatarUrl = t.user?.avatar?.large,
                recipientId = null,
                recipientName = null,
                recipientAvatarUrl = null,
                replies = t.replies?.filterNotNull()?.map { r ->
                    ActivityReply(
                        id = r.id,
                        body = r.text.orEmpty(),
                        likeCount = r.likeCount,
                        isLiked = r.isLiked == true,
                        authorId = r.user?.id ?: 0,
                        authorName = r.user?.name.orEmpty(),
                        authorAvatarUrl = r.user?.avatar?.large,
                        createdAt = r.createdAt.toLong()
                    )
                } ?: emptyList()
            )
        }
        onMessageActivity?.let { m ->
            return ActivityDetail(
                id = m.id,
                body = m.message.orEmpty(),
                createdAt = m.createdAt.toLong(),
                likeCount = m.likeCount,
                isLiked = m.isLiked == true,
                replyCount = m.replyCount,
                siteUrl = m.siteUrl,
                isMessage = true,
                isPrivate = m.isPrivate == true,
                authorId = m.messenger?.id ?: 0,
                authorName = m.messenger?.name.orEmpty(),
                authorAvatarUrl = m.messenger?.avatar?.large,
                recipientId = m.recipient?.id,
                recipientName = m.recipient?.name,
                recipientAvatarUrl = m.recipient?.avatar?.large,
                isAuthorMod = !m.messenger?.moderatorRoles.isNullOrEmpty(),
                replies = m.replies?.filterNotNull()?.map { r ->
                    ActivityReply(
                        id = r.id,
                        body = r.text.orEmpty(),
                        likeCount = r.likeCount,
                        isLiked = r.isLiked == true,
                        authorId = r.user?.id ?: 0,
                        authorName = r.user?.name.orEmpty(),
                        authorAvatarUrl = r.user?.avatar?.large,
                        createdAt = r.createdAt.toLong()
                    )
                } ?: emptyList()
            )
        }
        return null
    }
}
