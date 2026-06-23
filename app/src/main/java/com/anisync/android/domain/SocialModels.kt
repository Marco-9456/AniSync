package com.anisync.android.domain

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class SocialUser(
    val id: Int,
    val name: String,
    val avatarUrl: String?
)

@Immutable
@Serializable
data class SocialThreadComment(
    val id: Int,
    val threadId: Int,
    val threadTitle: String,
    val commentHtml: String?,
    val likeCount: Int,
    val isLiked: Boolean,
    val createdAt: Long,
    val authorId: Int,
    val authorName: String,
    val authorAvatarUrl: String?
)

@Immutable
data class UserSocialData(
    val following: List<SocialUser>,
    val followers: List<SocialUser>,
    val threads: List<ForumThread>,
    val comments: List<SocialThreadComment>
)

@Immutable
data class UserSocialPage(
    val data: UserSocialData,
    val followingHasNextPage: Boolean,
    val followersHasNextPage: Boolean,
    val threadsHasNextPage: Boolean,
    val commentsHasNextPage: Boolean
)

@Immutable
data class UserReviewsPage(
    val reviews: List<MediaReview>,
    val hasNextPage: Boolean
)

@Immutable
data class UserActivitiesPage(
    val activities: List<UserActivity>,
    val hasNextPage: Boolean,
    val currentPage: Int
)
