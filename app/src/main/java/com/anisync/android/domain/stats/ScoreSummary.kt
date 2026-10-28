package com.anisync.android.domain.stats

/**
 * What a score distribution says beyond AniList's own mean. Scores are on the 100 point scale,
 * as AniList's statistics return them, and a score of 0 means unscored.
 */
data class ScoreSummary(
    val scored: Int,
    /** The lower middle score when the count is even. */
    val median: Int?,
    /** The score given most often. A tie goes to the higher score. */
    val mostGiven: Int?,
) {
    /** Under five scored titles the Taste view shows a prompt instead of a spread. */
    val hasSpread: Boolean get() = scored >= MIN_SCORED_FOR_SPREAD
}

fun summarizeScores(countsByScore: Map<Int, Int>): ScoreSummary {
    val scored = countsByScore.filter { (score, count) -> score > 0 && count > 0 }.toSortedMap()
    val total = scored.values.sum()
    if (total == 0) return ScoreSummary(scored = 0, median = null, mostGiven = null)

    val middle = (total + 1) / 2
    var seen = 0
    var median: Int? = null
    for ((score, count) in scored) {
        seen += count
        if (seen >= middle) {
            median = score
            break
        }
    }
    val mostGiven = scored.entries.maxWith(compareBy({ it.value }, { it.key })).key
    return ScoreSummary(scored = total, median = median, mostGiven = mostGiven)
}

private const val MIN_SCORED_FOR_SPREAD = 5
