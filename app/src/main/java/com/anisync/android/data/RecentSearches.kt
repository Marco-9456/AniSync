package com.anisync.android.data

/** Each search keeps its own recent list, so a forum query never turns up in Discover. */
enum class SearchSurface { DISCOVER, LIBRARY, FORUM }

const val RECENT_SEARCH_LIMIT = 10

/** No search runs on fewer letters, so nothing shorter is worth keeping. */
private const val MIN_RECENT_SEARCH_LENGTH = 2

/** [query] goes to the front and any earlier spelling of it leaves, matched ignoring case. */
fun List<String>.withRecentSearch(query: String, limit: Int = RECENT_SEARCH_LIMIT): List<String> {
    val cleaned = query.trim().replace(WHITESPACE, " ")
    if (cleaned.length < MIN_RECENT_SEARCH_LENGTH) return this
    return (listOf(cleaned) + filterNot { it.equals(cleaned, ignoreCase = true) }).take(limit)
}

private val WHITESPACE = Regex("\\s+")
