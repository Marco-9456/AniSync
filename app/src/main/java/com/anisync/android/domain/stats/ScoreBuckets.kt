package com.anisync.android.domain.stats

import com.anisync.android.domain.ScoreStat

/** How many titles were given one score, on AniList's 100 point scale. */
data class ScoreBucket(val score: Int, val count: Int)

/**
 * AniList counts a list's scores in the format the list had when its statistics were last counted:
 * steps of five on the 100 point scale for 100 point and decimal lists, and 1 to 10, 1 to 5 or
 * 1 to 3 for ten point, star and smiley lists. The bars keep the steps the list was counted in,
 * placed on the 100 point scale, a 100 point score between two steps going to the nearer one.
 * Unscored titles are left out.
 */
fun scoreBuckets(scores: List<ScoreStat>): List<ScoreBucket> {
    val scored = scores.filter { it.score > 0 && it.count > 0 }
    val steps = countedSteps(scored)
    val counts = IntArray(steps.points.size)
    scored.forEach { counts[steps.index(it.score)] += it.count }
    return steps.points.mapIndexed { index, points -> ScoreBucket(score = points, count = counts[index]) }
}

/**
 * The ten steps the Overview teaser has room for. Scores gather in the top half for nearly
 * everyone, so it shows 55 to 100, and slides down to keep the most given score in view when that
 * sits lower. A list counted in ten steps or fewer shows them all.
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

/** The steps a list can be counted in, each at its place on the 100 point scale. */
private enum class ScoreSteps(val points: List<Int>) {
    FIVES((1..20).map { it * 5 }),
    TENS((1..10).map { it * 10 }),
    STARS((1..5).map { it * 20 }),
    SMILEYS(listOf(35, 60, 85));

    /** Where AniList's counted [score] sits on the 100 point scale. */
    fun pointsOf(score: Int): Int = if (this == FIVES) score else points[score - 1]

    fun index(score: Int): Int = if (this == FIVES) ((score + 2) / 5).coerceIn(1, points.size) - 1 else score - 1

    fun fits(score: Int): Boolean = this == FIVES || score <= points.size
}

/**
 * Scores past 10 can only be steps of five. Below that, each score's mean, which AniList keeps on
 * the 100 point scale, tells the formats apart: the list was counted in the one whose steps sit
 * closest to the means. A 100 point list that never scored past 10 has means equal to its scores,
 * so it stays in steps of five.
 */
private fun countedSteps(scored: List<ScoreStat>): ScoreSteps {
    if (scored.isEmpty() || scored.any { it.score > 10 }) return ScoreSteps.FIVES
    return ScoreSteps.entries
        .filter { steps -> scored.all { steps.fits(it.score) } }
        .minBy { steps ->
            scored.sumOf { stat ->
                val miss = stat.meanScore - steps.pointsOf(stat.score)
                stat.count * (miss * miss).toDouble()
            }
        }
}

private const val TEASER_BUCKETS = 10

/** Steps kept below the most given score when the window has to slide down. */
private const val TEASER_LEAD = 5
