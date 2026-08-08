package com.anisync.android.widget.core

import androidx.core.util.SizeFCompat
import kotlin.math.floor

/**
 * The size ladder every AniSync widget declares.
 *
 * Density matters more than it looks. From API 31 the launcher picks the largest declared size that
 * fits and never calls back, so the gap between rungs is dead space: a widget dragged to 280dp tall
 * renders the 250dp layout and wastes the difference. Below 31 the same ladder is narrowed to a
 * landscape and a portrait entry. Sixteen entries is the platform maximum.
 */
object WidgetSizes {

    /**
     * Three widths and five heights, fifteen of the sixteen entries the platform allows.
     *
     * Weighted towards width on purpose. Height granularity stopped mattering much once the lists
     * scrolled, since a taller widget shows more of the same list rather than needing a different
     * layout, whereas width decides whether a title fits beside its filter chips.
     */
    fun ladder(minHeightDp: Int): List<SizeFCompat> {
        val heights = HEIGHTS.filter { it >= minHeightDp }.ifEmpty { listOf(minHeightDp) }
        return WIDTHS.flatMap { width -> heights.map { SizeFCompat(width.toFloat(), it.toFloat()) } }
            .take(MAX_SIZES)
    }

    /**
     * True on the narrow rung, where a header title, an icon and two chips do not fit.
     *
     * Deliberately a property of the ladder rather than a hand-picked breakpoint: there are exactly
     * two widths, so the answer is which rung the launcher chose, not a guess about pixels. Letting
     * the title ellipsize instead leaves a bare "…" sitting between the icon and the chips.
     */
    fun isNarrow(size: SizeFCompat): Boolean = size.width < WIDE_WIDTH

    /**
     * True where the chrome would leave no room for a whole row.
     *
     * A header and a filter row cost about 96dp together, and a card is around 98dp. Below this a
     * widget showing both is showing a header and a sliver, so the short layouts drop the chrome
     * and show one card instead.
     */
    fun isShort(size: SizeFCompat): Boolean = size.height < SHORT_HEIGHT

    private const val WIDE_WIDTH = 300f
    private const val SHORT_HEIGHT = 200f
    private val WIDTHS = listOf(180, WIDE_WIDTH.toInt(), 380)
    private val HEIGHTS = listOf(100, 150, 200, 300, 400)
    private const val MAX_SIZES = 16
}

/**
 * How many rows a widget draws at a given size.
 *
 * RemoteViews has no scrolling container short of an adapter, and an adapter means either a
 * `RemoteViewsService` round trip or an API 31 floor. Neither is worth it here: every one of these
 * widgets shows a short, ranked list where the rows past the fold were never the point. So the
 * widget renders exactly the rows that fit and no more, which also keeps the bitmap budget honest,
 * because [WidgetImageBudget] is told the real on-screen image count.
 */
object WidgetRows {

    /**
     * Rows that fit in [size].
     *
     * @param rowHeightDp the row's full height including its bottom margin
     * @param chromeDp everything above and below the list: padding, header, filter bar
     * @param max never return more than this, whatever the size
     */
    fun fit(size: SizeFCompat, rowHeightDp: Int, chromeDp: Int, max: Int): Int {
        val usable = size.height - chromeDp
        if (usable <= 0) return 1
        val fits = floor(usable / rowHeightDp.toFloat()).toInt()
        return fits.coerceIn(1, max)
    }
}
