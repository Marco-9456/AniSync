package com.anisync.android.domain.stats

/** How many titles were given one score, on AniList's 100 point scale. */
data class ScoreBucket(val score: Int, val count: Int)

/**
 * AniList reports a list's scores on its 100 point scale whatever format the list uses, so a list
 * switched from stars to points still carries its 75s and 95s. The Taste view draws them in twenty
 * steps of five, a score between two steps going to the nearer one. Unscored titles are left out.
 */
fun scoreBuckets(countsByScore: Map<Int, Int>): List<ScoreBucket> {
    val counts = IntArray(BUCKETS)
    countsByScore.forEach { (score, count) ->
        if (score <= 0 || count <= 0) return@forEach
        val index = ((score + STEP / 2) / STEP).coerceIn(1, BUCKETS) - 1
        counts[index] += count
    }
    return counts.mapIndexed { index, count -> ScoreBucket(score = (index + 1) * STEP, count = count) }
}

/**
 * The ten steps the Overview teaser has room for. Scores gather in the top half for nearly
 * everyone, so it shows 55 to 100, and slides down to keep the most given score in view when that
 * sits lower.
 */
fun teaserBuckets(buckets: List<ScoreBucket>, mostGiven: Int?): List<ScoreBucket> {
    if (buckets.size <= TEASER_BUCKETS) return buckets
    val topStart = buckets.size - TEASER_BUCKETS
    val mostGivenIndex = buckets.indexOfFirst { it.score == mostGiven }
    val start = if (mostGivenIndex in 0 until topStart) {
        (mostGivenIndex - TEASER_LEAD).coerceIn(0, topStart)
    } else {
        topStart
    }
    return buckets.subList(start, start + TEASER_BUCKETS)
}

private const val STEP = 5
private const val BUCKETS = 100 / STEP
private const val TEASER_BUCKETS = 10

/** Steps kept below the most given score when the window has to slide down. */
private const val TEASER_LEAD = 5
