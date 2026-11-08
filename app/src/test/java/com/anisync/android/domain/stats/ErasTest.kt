package com.anisync.android.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ErasTest {

    private val releaseYears = mapOf(
        1925 to 3, 1929 to 12, 1938 to 43, 1945 to 7, 1952 to 4, 1977 to 5, 1988 to 33,
        1995 to 64, 2005 to 162, 2014 to 400, 2018 to 431, 2020 to 138, 2026 to 333,
    )

    @Test
    fun `nothing released has no eras`() {
        assertNull(eras(emptyMap(), emptyMap()))
        assertNull(eras(mapOf(2001 to 0), emptyMap()))
    }

    @Test
    fun `the range runs from the oldest to the newest year`() {
        val eras = eras(releaseYears, emptyMap())!!
        assertEquals(1925, eras.oldestYear)
        assertEquals(2026, eras.newestYear)
    }

    @Test
    fun `every decade is listed, the empty one included`() {
        val decades = eras(releaseYears, emptyMap())!!.decades
        assertEquals((1920..2020 step 10).toList(), decades.map { it.decade })
        assertEquals(15, decades.first().count)
        assertEquals(0, decades.single { it.decade == 1960 }.count)
        assertEquals(831, decades.single { it.decade == 2010 }.count)
    }

    @Test
    fun `the peak decade and its share`() {
        val eras = eras(releaseYears, emptyMap())!!
        assertEquals(2010, eras.peakDecade.decade)
        assertEquals(831.0 / 1635, eras.peakDecadeShare, 0.0001)
    }

    @Test
    fun `peak years come from releases and starts, a tie to the later year`() {
        val eras = eras(mapOf(2019 to 50, 2020 to 50), mapOf(2021 to 70, 2022 to 9))!!
        assertEquals(YearCount(2020, 50), eras.peakReleaseYear)
        assertEquals(YearCount(2021, 70), eras.peakStartYear)
    }

    @Test
    fun `no start years leaves the start peak out`() {
        assertNull(eras(releaseYears, emptyMap())!!.peakStartYear)
    }

    @Test
    fun `the best scored year needs a fair share of titles`() {
        val eras = eras(
            releaseYears = mapOf(2020 to 138, 2025 to 4, 2026 to 18, 2019 to 100),
            startYears = emptyMap(),
            releaseMeans = mapOf(2020 to 70.0, 2025 to 95.0, 2026 to 84.7, 2019 to 72.0),
        )!!
        assertEquals(YearMean(2026, 84.7), eras.bestScoredYear)
    }

    @Test
    fun `no means leave no best scored year`() {
        assertNull(eras(mapOf(2020 to 10), emptyMap())!!.bestScoredYear)
    }
}
