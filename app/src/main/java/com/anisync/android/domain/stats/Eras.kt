package com.anisync.android.domain.stats

/** The Eras view: when the titles on a list came out, and when the user started them. */
data class Eras(
    val oldestYear: Int,
    val newestYear: Int,
    /** Every decade from the oldest to the newest, the empty ones included. */
    val decades: List<DecadeCount>,
    val peakDecade: DecadeCount,
    val total: Int,
    val peakReleaseYear: YearCount,
    val peakStartYear: YearCount?,
    val releaseYears: Map<Int, Int>,
    val startYears: Map<Int, Int>,
) {
    val peakDecadeShare: Double get() = peakDecade.count.toDouble() / total
}

data class DecadeCount(val decade: Int, val count: Int)

data class YearCount(val year: Int, val count: Int)

/** Null when no release year is known. A tie for the peak goes to the more recent one. */
fun eras(releaseYears: Map<Int, Int>, startYears: Map<Int, Int>): Eras? {
    val released = releaseYears.filterValues { it > 0 }
    if (released.isEmpty()) return null
    val oldest = released.keys.min()
    val newest = released.keys.max()
    val counts = released.entries.groupBy({ decadeOf(it.key) }, { it.value }).mapValues { it.value.sum() }
    val decades = (decadeOf(oldest)..decadeOf(newest) step 10).map { DecadeCount(it, counts[it] ?: 0) }
    return Eras(
        oldestYear = oldest,
        newestYear = newest,
        decades = decades,
        peakDecade = decades.maxWith(compareBy({ it.count }, { it.decade })),
        total = released.values.sum(),
        peakReleaseYear = released.peak()!!,
        peakStartYear = startYears.filterValues { it > 0 }.peak(),
        releaseYears = released,
        startYears = startYears.filterValues { it > 0 },
    )
}

private fun decadeOf(year: Int) = Math.floorDiv(year, 10) * 10

private fun Map<Int, Int>.peak(): YearCount? =
    entries.maxWithOrNull(compareBy({ it.value }, { it.key }))?.let { YearCount(it.key, it.value) }
