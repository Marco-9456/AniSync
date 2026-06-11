package com.anisync.android.domain

import com.anisync.android.type.ReviewSort

/**
 * Ordering options for review lists (the expanded "Recent Reviews" screen),
 * each mapped to the AniList [ReviewSort] value sent to `Page.reviews(sort:)`.
 *
 * Mirrors the role of [ThreadSortOption] for the forum search, constrained to
 * the orderings that read sensibly for reviews.
 */
enum class ReviewSortOption(val apiValue: ReviewSort) {
    NEWEST(ReviewSort.CREATED_AT_DESC),
    OLDEST(ReviewSort.CREATED_AT),
    HIGHEST_SCORE(ReviewSort.SCORE_DESC),
    LOWEST_SCORE(ReviewSort.SCORE),
    MOST_LIKED(ReviewSort.RATING_DESC),
    RECENTLY_UPDATED(ReviewSort.UPDATED_AT_DESC);

    companion object {
        val Default = NEWEST
    }
}
