package com.anisync.android.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreBucketsTest {

    @Test
    fun `twenty steps of five whatever the scores`() {
        val buckets = scoreBuckets(emptyMap())
        assertEquals(20, buckets.size)
        assertEquals((1..20).map { it * 5 }, buckets.map { it.score })
        assertEquals(0, buckets.sumOf { it.count })
    }

    @Test
    fun `a score between two steps goes to the nearer one`() {
        val buckets = scoreBuckets(mapOf(72 to 1, 73 to 2, 7 to 3, 8 to 4, 98 to 5))
        assertEquals(1, buckets.single { it.score == 70 }.count)
        assertEquals(2, buckets.single { it.score == 75 }.count)
        assertEquals(3, buckets.single { it.score == 5 }.count)
        assertEquals(4, buckets.single { it.score == 10 }.count)
        assertEquals(5, buckets.single { it.score == 100 }.count)
    }

    @Test
    fun `unscored titles are left out`() {
        val buckets = scoreBuckets(mapOf(0 to 40, 85 to 6))
        assertEquals(6, buckets.sumOf { it.count })
    }

    @Test
    fun `a star list keeps the points it was scored in`() {
        // Marco9456 scores in stars now, but AniList still reports 75s, 85s and 95s.
        val buckets = scoreBuckets(mapOf(20 to 1, 60 to 1, 70 to 4, 75 to 1, 80 to 8, 85 to 6, 90 to 16, 95 to 5, 100 to 6))
        assertEquals(listOf(20 to 1, 60 to 1, 70 to 4, 75 to 1, 80 to 8, 85 to 6, 90 to 16, 95 to 5, 100 to 6),
            buckets.filter { it.count > 0 }.map { it.score to it.count })
    }

    @Test
    fun `the teaser shows the top half`() {
        val buckets = scoreBuckets((1..20).associate { it * 5 to it })
        assertEquals((11..20).map { it * 5 }, teaserBuckets(buckets, mostGiven = 85).map { it.score })
        assertEquals((11..20).map { it * 5 }, teaserBuckets(buckets, mostGiven = null).map { it.score })
    }

    @Test
    fun `the teaser slides down to a low most given score`() {
        val buckets = scoreBuckets((1..20).associate { it * 5 to 1 })
        assertEquals((5..14).map { it * 5 }, teaserBuckets(buckets, mostGiven = 50).map { it.score })
        assertEquals((1..10).map { it * 5 }, teaserBuckets(buckets, mostGiven = 15).map { it.score })
    }
}
