package com.anisync.android.domain

interface FeedRepository {
    suspend fun getFeed(
        page: Int,
        perPage: Int,
        filter: FeedFilter,
        scope: FeedScope,
        mediaType: FeedMediaType
    ): Result<FeedPage>
}
