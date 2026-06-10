package com.anisync.android.domain

import com.anisync.android.domain.ThreadSortOption.Companion.Default
import com.anisync.android.type.ThreadSort

/**
 * Thread ordering options exposed in the advanced forum search, each mapped to
 * the AniList [ThreadSort] values sent to `Page.threads(sort:)`.
 *
 * [RELEVANCE] (`SEARCH_MATCH`) only ranks meaningfully when a text query is
 * present; the data layer falls back to [Default] for a blank query.
 */
enum class ThreadSortOption(val apiValue: List<ThreadSort>) {
    RECENTLY_REPLIED(listOf(ThreadSort.REPLIED_AT_DESC)),
    NEWEST(listOf(ThreadSort.CREATED_AT_DESC)),
    OLDEST(listOf(ThreadSort.CREATED_AT)),
    MOST_REPLIES(listOf(ThreadSort.REPLY_COUNT_DESC)),
    MOST_VIEWED(listOf(ThreadSort.VIEW_COUNT_DESC)),
    TITLE(listOf(ThreadSort.TITLE)),
    RELEVANCE(listOf(ThreadSort.SEARCH_MATCH));

    companion object {
        val Default = RECENTLY_REPLIED
    }
}

/**
 * Filter state for the advanced forum (thread) search. Mirrors the role of
 * [SearchFilters] for media, but constrained to what AniList's `Page.threads`
 * query actually supports: a single forum [category], a single related [media],
 * a single [author], a [subscribedOnly] toggle, plus [sort]. There is
 * intentionally no genre/tag/format/year filtering — those apply to media, not
 * to threads.
 *
 * [media] reuses [LibraryEntry] (as the create-thread media picker does) so the
 * same media-search path feeds the chip; [author] reuses
 * [SearchResult.UserResult] from the shared user search.
 */
data class ForumSearchFilters(
    val query: String = "",
    val category: ForumCategory? = null,
    val media: LibraryEntry? = null,
    val author: SearchResult.UserResult? = null,
    val subscribedOnly: Boolean = false,
    val sort: ThreadSortOption = ThreadSortOption.Default
) {
    /**
     * Whether any structured filter is active. When true the search runs even
     * with a blank/short query (e.g. "show all threads for this media").
     */
    val hasActiveFilters: Boolean
        get() = category != null || media != null || author != null ||
                subscribedOnly || sort != ThreadSortOption.Default

    /** Count of active structured filters, for chip-bar "active" badges. */
    val activeCount: Int
        get() = listOf(
            category != null,
            media != null,
            author != null,
            subscribedOnly,
            sort != ThreadSortOption.Default
        ).count { it }
}
