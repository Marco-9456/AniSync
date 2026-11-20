package com.anisync.android.presentation.details

import com.anisync.android.domain.MediaRanking
import com.anisync.android.domain.MediaRankingType
import com.anisync.android.presentation.navigation.PresetSearch
import com.anisync.android.type.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pins the search a ranking card on a title's page opens. */
class RankingPresetSearchTest {

    private fun ranking(
        type: MediaRankingType,
        allTime: Boolean,
        year: Int? = null,
        season: String? = null
    ) = MediaRanking(rank = 3, type = type, year = year, season = season, allTime = allTime, context = "")

    @Test
    fun `a seasonal ranking keeps its season and year`() {
        assertEquals(
            PresetSearch(mediaType = "ANIME", sort = "SCORE", season = "SPRING", year = 2024),
            ranking(MediaRankingType.RATED, allTime = false, year = 2024, season = "SPRING")
                .presetSearch(MediaType.ANIME)
        )
    }

    @Test
    fun `a yearly ranking keeps only its year`() {
        assertEquals(
            PresetSearch(mediaType = "MANGA", sort = "POPULARITY", year = 2019),
            ranking(MediaRankingType.POPULAR, allTime = false, year = 2019).presetSearch(MediaType.MANGA)
        )
    }

    @Test
    fun `a ranking of all time ignores any year or season it carries`() {
        assertEquals(
            PresetSearch(mediaType = "ANIME", sort = "SCORE"),
            ranking(MediaRankingType.RATED, allTime = true, year = 2024, season = "SPRING")
                .presetSearch(MediaType.ANIME)
        )
    }

    @Test
    fun `a title of no known type searches anime`() {
        assertEquals("ANIME", ranking(MediaRankingType.RATED, allTime = true).presetSearch(null).mediaType)
    }
}
