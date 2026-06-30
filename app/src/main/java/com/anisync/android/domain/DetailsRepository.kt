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
     * Refresh from network only when the cached copy is missing or older than its
     * status-based TTL (stale-while-revalidate). Cheap no-op when the cache is still
     * fresh. Called on screen entry so a revisited page self-updates (e.g. a newly
     * published airing schedule or changed cover) without a manual pull-to-refresh,
     * while finished media rarely re-hits the API.
     */
    suspend fun refreshMediaDetailsIfStale(id: Int): Result<Unit>

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

    suspend fun toggleCharacterFavourite(characterId: Int, newState: Boolean): Result<Unit>

    suspend fun toggleStaffFavourite(staffId: Int, newState: Boolean): Result<Unit>

    suspend fun toggleStudioFavourite(studioId: Int, newState: Boolean): Result<Unit>

    suspend fun getCharacterDetails(id: Int, page: Int = 1): Result<CharacterDetails>

    suspend fun getStaffDetails(
        id: Int,
        page: Int = 1,
        staffMediaPage: Int = 1
    ): Result<StaffDetails>

    suspend fun getStudioDetails(id: Int, page: Int = 1): Result<StudioDetails>

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

    /**
     * Fetch the authenticated viewer's own review for a media, if one exists.
     * Returns null when the viewer hasn't reviewed this media (used to decide
     * between create vs. edit in the review editor).
     */
    suspend fun getViewerReview(mediaId: Int): Result<ViewerReview?>

    /**
     * Create or update a review. Pass [reviewId] to update an existing review,
     * or null to create a new one.
     */
    suspend fun saveReview(
        reviewId: Int?,
        mediaId: Int,
        body: String,
        summary: String,
        score: Int,
        private: Boolean
    ): Result<Int>

    /**
     * Delete the viewer's review. [mediaId] is used to refresh the cached media
     * details so the removed review drops out of the reviews section.
     */
    suspend fun deleteReview(reviewId: Int, mediaId: Int): Result<Unit>
}

/**
 * The authenticated viewer's own review for a media, with the raw markdown body
 * so it can be loaded back into the editor for editing.
 */
data class ViewerReview(
    val id: Int,
    val summary: String,
    val body: String,
    val score: Int,
    val isPrivate: Boolean
)