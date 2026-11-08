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
    /** The best scored release year among those with enough titles to mean something. */
    val bestScoredYear: YearMean? = null,
) {
    val peakDecadeShare: Double get() = peakDecade.count.toDouble() / total
}

data class DecadeCount(val decade: Int, val count: Int)

data class YearCount(val year: Int, val count: Int)

data class YearMean(val year: Int, val mean: Double)

/** Null when no release year is known. A tie for the peak goes to the more recent one. */
fun eras(
    releaseYears: Map<Int, Int>,
    startYears: Map<Int, Int>,
    releaseMeans: Map<Int, Double> = emptyMap(),
): Eras? {
    val released = releaseYears.filterValues { it > 0 }
    if (released.isEmpty()) return null
    val oldest = released.keys.min()
    val newest = released.keys.max()
    val counts = released.entries.groupBy({ decadeOf(it.key) }, { it.value }).mapValues { it.value.sum() }
    val decades = (decadeOf(oldest)..decadeOf(newest) step 10).map { DecadeCount(it, counts[it] ?: 0) }
    val peak = released.peak()!!
    val floor = maxOf(BEST_SCORED_MIN_TITLES, peak.count / BEST_SCORED_PEAK_SHARE)
    val bestScored = releaseMeans
        .filter { (year, mean) -> mean > 0.0 && (released[year] ?: 0) >= floor }
        .entries
        .maxWithOrNull(compareBy({ it.value }, { it.key }))
        ?.let { YearMean(it.key, it.value) }
    return Eras(
        oldestYear = oldest,
        newestYear = newest,
        decades = decades,
        peakDecade = decades.maxWith(compareBy({ it.count }, { it.decade })),
        total = released.values.sum(),
        peakReleaseYear = peak,
        peakStartYear = startYears.filterValues { it > 0 }.peak(),
        releaseYears = released,
        startYears = startYears.filterValues { it > 0 },
        bestScoredYear = bestScored,
    )
}

/** A year needs this many titles, and a tenth of the peak year's, before its mean counts. */
private const val BEST_SCORED_MIN_TITLES = 5
private const val BEST_SCORED_PEAK_SHARE = 10

private fun decadeOf(year: Int) = Math.floorDiv(year, 10) * 10

private fun Map<Int, Int>.peak(): YearCount? =
    entries.maxWithOrNull(compareBy({ it.value }, { it.key }))?.let { YearCount(it.key, it.value) }
