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
    LEAST_RECENTLY_REPLIED(listOf(ThreadSort.REPLIED_AT)),
    NEWEST(listOf(ThreadSort.CREATED_AT_DESC)),
    OLDEST(listOf(ThreadSort.CREATED_AT)),
    MOST_REPLIES(listOf(ThreadSort.REPLY_COUNT_DESC)),
    FEWEST_REPLIES(listOf(ThreadSort.REPLY_COUNT)),
    MOST_VIEWED(listOf(ThreadSort.VIEW_COUNT_DESC)),
    LEAST_VIEWED(listOf(ThreadSort.VIEW_COUNT)),
    TITLE(listOf(ThreadSort.TITLE)),
    TITLE_DESC(listOf(ThreadSort.TITLE_DESC)),
    RELEVANCE(listOf(ThreadSort.SEARCH_MATCH));

    // Not named `field`: inside a property accessor that identifier is the backing field.
    /** Which column this orders by, ignoring direction. */
    val sortField: ThreadSortField
        get() = ThreadSortField.entries.first { it.descending == this || it.ascending == this }

    /** True when this is the ascending half of its [sortField]. */
    val isAscending: Boolean get() = sortField.ascending == this

    companion object {
        val Default = RECENTLY_REPLIED
    }
}

/**
 * The column a thread list is ordered by. Direction is a separate control, the way Library's sort
 * sheet has it, so each column costs one pill instead of two and every column gets both directions
 * rather than only the one somebody thought to list.
 */
enum class ThreadSortField(
    val ascending: ThreadSortOption?,
    val descending: ThreadSortOption
) {
    LAST_REPLY(ThreadSortOption.LEAST_RECENTLY_REPLIED, ThreadSortOption.RECENTLY_REPLIED),
    CREATED(ThreadSortOption.OLDEST, ThreadSortOption.NEWEST),
    REPLIES(ThreadSortOption.FEWEST_REPLIES, ThreadSortOption.MOST_REPLIES),
    VIEWS(ThreadSortOption.LEAST_VIEWED, ThreadSortOption.MOST_VIEWED),
    TITLE(ThreadSortOption.TITLE, ThreadSortOption.TITLE_DESC),
    /** Match quality has no meaningful ascending half. */
    RELEVANCE(null, ThreadSortOption.RELEVANCE);

    fun toOption(isAscending: Boolean): ThreadSortOption =
        if (isAscending) (ascending ?: descending) else descending

    /** Relevance is one-way, so the direction control is inert on it. */
    val hasDirection: Boolean get() = ascending != null
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
