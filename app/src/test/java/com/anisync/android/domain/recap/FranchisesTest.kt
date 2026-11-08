package com.anisync.android.domain.recap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FranchisesTest {

    @Test
    fun `generic words never name a franchise`() {
        val media = listOf(
            title(1, "Kawa no Hikari: The Movie", relations = listOf(2)),
            title(2, "Umi no Koe: The Movie", relations = listOf(1)),
        ).associateBy { it.id }
        val franchise = largestFranchise(listOf(1, 2), media)!!
        assertNull(franchise.name)
        assertEquals(listOf(1, 2), franchise.mediaIds)
    }

    @Test
    fun `the longest shared word names the group and gathers its other titles`() {
        val media = listOf(
            title(1, "Kimetsu no Yaiba: Yuukaku-hen", relations = listOf(2)),
            title(2, "Kimetsu no Yaiba Movie: Mugen Ressha-hen", relations = listOf(1)),
            title(3, "Kimetsu Gakuen!", relations = emptyList()),
            title(4, "Yuukaku no Hana", relations = emptyList()),
        ).associateBy { it.id }
        val franchise = largestFranchise(listOf(4, 2, 1, 3), media)!!
        assertEquals("Kimetsu", franchise.name)
        assertEquals(listOf(2, 1, 3), franchise.mediaIds)
    }

    @Test
    fun `numbers alone do not name a group`() {
        val media = listOf(
            title(1, "Alpha 2", relations = listOf(2)),
            title(2, "Beta 2", relations = listOf(1)),
        ).associateBy { it.id }
        assertNull(largestFranchise(listOf(1, 2), media)!!.name)
    }

    private fun title(id: Int, title: String, relations: List<Int>) = RecapMedia(
        id = id, type = RecapMediaType.ANIME, title = title, format = "TV", episodes = 12, chapters = null,
        duration = 24, genres = emptyList(), mainStudio = null, isAdult = false, startDate = null, endDate = null,
        relationIds = relations,
    )
}
