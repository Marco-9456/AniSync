package com.anisync.android.domain

import com.anisync.android.type.MediaType
import kotlinx.coroutines.flow.Flow

interface DetailsRepository {
    /**
     * Observe media details from local cache (reactive).
     */
    fun observeMediaDetails(id: Int): Flow<MediaDetails?>

    /**
     * Fetch fresh media details from network and update cache.
     */
    suspend fun refreshMediaDetails(id: Int): Result<Unit>

    /**
     * Update media list entry (status, progress).
     */
    suspend fun updateMediaListEntry(
        mediaId: Int,
        status: LibraryStatus,
        progress: Int
    ): Result<Unit>

    /**
     * Delete media list entry.
     * @param entryId The list entry ID to delete from the API
     * @param mediaId The media ID to remove from local library cache
     */
    suspend fun deleteMediaListEntry(entryId: Int, mediaId: Int): Result<Unit>

    /**
     * Toggle favorite status for a media.
     */
    suspend fun toggleFavourite(mediaId: Int, mediaType: MediaType): Result<Boolean>

    suspend fun toggleCharacterFavourite(characterId: Int): Result<Boolean>

    suspend fun toggleStaffFavourite(staffId: Int): Result<Boolean>

    suspend fun getCharacterDetails(id: Int, page: Int = 1): Result<CharacterDetails>

    suspend fun getStaffDetails(id: Int, page: Int = 1): Result<StaffDetails>

    /**
     * Rate a media review.
     */
    suspend fun rateReview(
        reviewId: Int,
        rating: com.anisync.android.type.ReviewRating
    ): Result<MediaReview>

    /**
     * Get paginated media reviews.
     * Returns a pair of: List of reviews, and a boolean indicating if there is a next page.
     */
    suspend fun getMediaReviews(mediaId: Int, page: Int): Result<Pair<List<MediaReview>, Boolean>>

    /**
     * Get paginated list of media list entries belonging to users the viewer follows.
     * Returns a pair of: list of entries, and a boolean indicating if there is a next page.
     */
    suspend fun getMediaFollowing(
        mediaId: Int,
        page: Int,
        perPage: Int
    ): Result<Pair<List<MediaFollowingEntry>, Boolean>>

    /**
     * Rate a recommendation (like/dislike/clear).
     * @param mediaId The source media ID
     * @param recommendationId The recommended media ID
     * @param rating The rating to apply
     * @return Updated rating and userRating
     */
    suspend fun rateRecommendation(
        mediaId: Int,
        recommendationId: Int,
        rating: com.anisync.android.type.RecommendationRating
    ): Result<Pair<Int, String?>>
}