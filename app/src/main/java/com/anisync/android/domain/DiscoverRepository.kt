package com.anisync.android.domain

import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaType

/**
 * Repository interface for discover/browse operations.
 * Provides methods to fetch trending, popular, and upcoming media.
 */
interface DiscoverRepository {
    /**
     * Get trending media by type.
     * @param type Media type (ANIME or MANGA)
     * @return List of trending entries or error
     */
    suspend fun getTrending(type: MediaType): Result<List<LibraryEntry>>

    /**
     * Get popular (all-time) media by type.
     * @param type Media type (ANIME or MANGA)
     * @return List of popular entries or error
     */
    suspend fun getPopular(type: MediaType): Result<List<LibraryEntry>>

    /**
     * Get upcoming/not-yet-released media by type.
     * @param type Media type (ANIME or MANGA)
     * @return List of upcoming entries or error
     */
    suspend fun getUpcoming(type: MediaType): Result<List<LibraryEntry>>

    /**
     * Get TBA (To Be Announced) media by type.
     * @param type Media type (ANIME or MANGA)
     * @return List of TBA entries or error
     */
    suspend fun getTBA(type: MediaType): Result<List<LibraryEntry>>

    /**
     * Get newly added media by type — the entries most recently added to AniList
     * (sorted by descending database id).
     * @param type Media type (ANIME or MANGA)
     * @return List of newly added entries or error
     */
    suspend fun getNewlyAdded(type: MediaType): Result<List<LibraryEntry>>

    /**
     * Get the most recently published reviews across AniList, newest first.
     * @param mediaType Optional filter (ANIME or MANGA); null returns reviews for both
     * @param page Page number (1-indexed)
     * @return Paginated reviews or error
     */
    suspend fun getRecentReviews(mediaType: MediaType?, page: Int): Result<UserReviewsPage>
    
    /**
     * Fetches paginated media for grid screens with optional format filtering.
     * @param sectionType One of: "trending", "popular", "upcoming", "tba"
     * @param type MediaType (ANIME or MANGA)
     * @param page Page number (1-indexed)
     * @param format Optional format filter (TV, MOVIE, OVA, etc.)
     * @return Paginated result with entries or error
     */
    suspend fun getPaginatedSection(
        sectionType: String,
        type: MediaType,
        page: Int,
        format: MediaFormat? = null
    ): Result<PaginatedResult<LibraryEntry>>
}
