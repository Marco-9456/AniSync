package com.anisync.android.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PeopleStatisticsTest {

    private fun person(id: Int, count: Int, mean: Float, amount: Float) =
        PersonStat(id = id, name = "P$id", imageUrl = null, count = count, meanScore = mean, amount = amount)

    private val people = listOf(
        person(1, count = 51, mean = 69.8f, amount = 176f),
        person(2, count = 2, mean = 100f, amount = 3f),
        person(3, count = 39, mean = 76.5f, amount = 254f),
        person(4, count = 6, mean = 0f, amount = 9f),
    )

    @Test
    fun `count order puts the most roles first`() {
        assertEquals(listOf(1, 3, 4, 2), people.inPeopleOrder(PeopleSort.COUNT).map { it.id })
    }

    @Test
    fun `progress order puts the most time first`() {
        assertEquals(listOf(3, 1, 4, 2), people.inPeopleOrder(PeopleSort.PROGRESS).map { it.id })
    }

    @Test
    fun `score order skips small and unscored entries`() {
        assertEquals(listOf(3, 1), people.inPeopleOrder(PeopleSort.SCORE).map { it.id })
    }
}
