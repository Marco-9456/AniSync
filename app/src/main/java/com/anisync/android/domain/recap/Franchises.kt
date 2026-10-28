package com.anisync.android.domain.recap

private const val MIN_NAME_LENGTH = 4

/** Words two related titles often share without naming a franchise. */
private val GENERIC_WORDS = setOf(
    "movie", "film", "season", "part", "special", "specials", "gekijouban", "series", "final",
    "edition", "version", "chapter", "episode",
)

/**
 * The biggest group of related titles among [rankedIds], or null when no two are related.
 *
 * AniList's relations only link close neighbours: Petit Eva points at the Evangelion TV series,
 * not at the films. So a group starts from titles that are directly related, takes the longest word
 * their titles share as its name, and then gathers the other titles carrying that word. A tie for
 * size goes to the group holding the higher ranked title.
 */
internal fun largestFranchise(rankedIds: List<Int>, media: Map<Int, RecapMedia>): Franchise? {
    val ids = rankedIds.filter { it in media }
    val inPeriod = ids.toSet()
    val parent = HashMap<Int, Int>().apply { ids.forEach { put(it, it) } }
    fun root(id: Int): Int {
        var current = id
        while (parent.getValue(current) != current) current = parent.getValue(current)
        return current
    }
    for (id in ids) {
        for (related in media.getValue(id).relationIds) {
            if (related in inPeriod) parent[root(id)] = root(related)
        }
    }
    val rank = ids.withIndex().associate { it.value to it.index }
    return ids.groupBy(::root).values
        .filter { it.size >= 2 }
        .map { group ->
            val name = sharedWord(group.map { media.getValue(it).title })
            val members = if (name == null) {
                group
            } else {
                val key = name.lowercase()
                group + ids.filter { it !in group && key in words(media.getValue(it).title).map(String::lowercase) }
            }
            Franchise(name, members.sortedBy { rank.getValue(it) })
        }
        .maxWithOrNull(compareBy<Franchise> { it.mediaIds.size }.thenByDescending { rank.getValue(it.mediaIds.first()) })
}

private fun words(title: String): List<String> =
    title.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length >= MIN_NAME_LENGTH }

/** The longest word every title shares, as the first title spells it. */
private fun sharedWord(titles: List<String>): String? {
    val shared = titles.map { title -> words(title).map(String::lowercase).toSet() }
        .reduce { common, next -> common intersect next }
        .minus(GENERIC_WORDS)
    return words(titles.first())
        .filter { it.lowercase() in shared }
        .maxByOrNull { it.length }
}
