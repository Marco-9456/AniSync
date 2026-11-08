package com.anisync.android.domain.stats

import com.anisync.android.domain.ScoreStat
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreBucketsTest {

    /** Scores counted on the 100 point scale, each mean equal to its score. */
    private fun points(vararg counts: Pair<Int, Int>) = counts.map { (score, count) -> stat(score, count, score.toFloat()) }

    private fun stat(score: Int, count: Int, mean: Float) = ScoreStat(score = score, count = count, meanScore = mean, hoursWatched = 0f)

    @Test
    fun `an unscored list gets twenty empty steps of five`() {
        val buckets = scoreBuckets(emptyList())
        assertEquals((1..20).map { it * 5 }, buckets.map { it.score })
        assertEquals(0, buckets.sumOf { it.count })
    }

    @Test
    fun `a score between two steps goes to the nearer one`() {
        val buckets = scoreBuckets(points(72 to 1, 73 to 2, 7 to 3, 8 to 4, 98 to 5))
        assertEquals(1, buckets.single { it.score == 70 }.count)
        assertEquals(2, buckets.single { it.score == 75 }.count)
        assertEquals(3, buckets.single { it.score == 5 }.count)
        assertEquals(4, buckets.single { it.score == 10 }.count)
        assertEquals(5, buckets.single { it.score == 100 }.count)
    }

    @Test
    fun `unscored titles are left out`() {
        val buckets = scoreBuckets(points(0 to 40, 85 to 6))
        assertEquals(6, buckets.sumOf { it.count })
    }

    @Test
    fun `a list counted before a format change keeps the format it was counted in`() {
        // Marco9456 scores in stars now, but AniList last counted the list in points.
        val counted = listOf(20 to 1, 60 to 1, 70 to 4, 75 to 1, 80 to 8, 85 to 6, 90 to 16, 95 to 5, 100 to 6)
        val buckets = scoreBuckets(points(*counted.toTypedArray()))
        assertEquals(20, buckets.size)
        assertEquals(counted, buckets.filter { it.count > 0 }.map { it.score to it.count })
    }

    @Test
    fun `a ten point list keeps its ten steps`() {
        // Hameru's anime, counted 1 to 10. The 1s were scored in points before, so their mean is 10.19.
        val counts = listOf(16, 12, 19, 31, 63, 125, 343, 351, 90, 6)
        val buckets = scoreBuckets(counts.mapIndexed { index, count ->
            val score = index + 1
            stat(score, count, if (score == 1) 10.19f else score * 10f)
        })
        assertEquals((1..10).map { it * 10 }, buckets.map { it.score })
        assertEquals(counts, buckets.map { it.count })

        val summary = summarizeScores(buckets.associate { it.score to it.count })
        assertEquals(80, summary.mostGiven)
        assertEquals(70, summary.median)
    }

    @Test
    fun `a star list keeps its five steps`() {
        val buckets = scoreBuckets(listOf(stat(1, 2, 20f), stat(2, 4, 40f), stat(3, 28, 60f), stat(4, 39, 80f), stat(5, 27, 100f)))
        assertEquals(listOf(20 to 2, 40 to 4, 60 to 28, 80 to 39, 100 to 27), buckets.map { it.score to it.count })
    }

    @Test
    fun `a smiley list keeps its three steps`() {
        // A list scored in points before it moved to smileys, so the means drift from 35, 60 and 85.
        val buckets = scoreBuckets(listOf(stat(1, 24, 29.17f), stat(2, 107, 55.79f), stat(3, 129, 86.24f)))
        assertEquals(listOf(35 to 24, 60 to 107, 85 to 129), buckets.map { it.score to it.count })
    }

    @Test
    fun `a ten point list that never went past five is not read as stars`() {
        val buckets = scoreBuckets(listOf(stat(3, 4, 30f), stat(4, 9, 40f), stat(5, 2, 50f)))
        assertEquals(10, buckets.size)
        assertEquals(listOf(30 to 4, 40 to 9, 50 to 2), buckets.filter { it.count > 0 }.map { it.score to it.count })
    }

    @Test
    fun `a point list that never scored past ten stays in steps of five`() {
        val buckets = scoreBuckets(points(3 to 1, 10 to 2))
        assertEquals(20, buckets.size)
        assertEquals(listOf(5 to 1, 10 to 2), buckets.filter { it.count > 0 }.map { it.score to it.count })
    }

    @Test
    fun `the teaser shows the top half`() {
        val buckets = scoreBuckets(points(*(1..20).map { it * 5 to it }.toTypedArray()))
        assertEquals((11..20).map { it * 5 }, teaserBuckets(buckets, mostGiven = 85).map { it.score })
        assertEquals((11..20).map { it * 5 }, teaserBuckets(buckets, mostGiven = null).map { it.score })
    }

    @Test
    fun `the teaser slides down to a low most given score`() {
        val buckets = scoreBuckets(points(*(1..20).map { it * 5 to 1 }.toTypedArray()))
        assertEquals((5..14).map { it * 5 }, teaserBuckets(buckets, mostGiven = 50).map { it.score })
        assertEquals((1..10).map { it * 5 }, teaserBuckets(buckets, mostGiven = 15).map { it.score })
    }

    @Test
    fun `the teaser shows every step of a ten point list`() {
        val buckets = scoreBuckets((1..10).map { stat(it, it, it * 10f) })
        assertEquals((1..10).map { it * 10 }, teaserBuckets(buckets, mostGiven = 30).map { it.score })
    }
}
