package com.anisync.android.domain.stats

/** How widely someone's scores spread, the words the Taste tile and card use instead of σ. */
enum class TasteSpread { FOCUSED, BALANCED, ECLECTIC }

/**
 * [standardDeviation] is AniList's σ, which is on the 100 point scale whatever score format the
 * user picked. Null when there is no spread to describe.
 */
fun tasteSpread(standardDeviation: Double): TasteSpread? = when {
    standardDeviation <= 0.0 -> null
    standardDeviation < FOCUSED_BELOW -> TasteSpread.FOCUSED
    standardDeviation < BALANCED_BELOW -> TasteSpread.BALANCED
    else -> TasteSpread.ECLECTIC
}

private const val FOCUSED_BELOW = 10.0
private const val BALANCED_BELOW = 16.0
