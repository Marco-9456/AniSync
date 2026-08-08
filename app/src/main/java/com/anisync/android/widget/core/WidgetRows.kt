package com.anisync.android.widget.core

import androidx.core.util.SizeFCompat
import kotlin.math.floor

/**
 * The size ladder every widget declares.
 *
 * One entry per layout the widget can really produce, no more. Each declared size carries its own
 * full RemoteViews, rows and cover bitmaps included, and AppWidgetServiceImpl silently drops an
 * update whose bitmaps go past roughly six bytes per display pixel. No exception, no log, the
 * widget just keeps its initial layout. A fifteen rung ladder copied a whole day of Airing Today
 * covers fifteen times and hit exactly that.
 *
 * Only [isNarrow] and [isShort] change the layout, so four entries cover everything. The host
 * stretches whichever one it picks anyway.
 */
object WidgetSizes {

    fun ladder(minHeightDp: Int): List<SizeFCompat> {
        val heights = HEIGHTS.filter { it >= minHeightDp }.ifEmpty { listOf(minHeightDp) }
        return WIDTHS.flatMap { width -> heights.map { SizeFCompat(width.toFloat(), it.toFloat()) } }
            .take(MAX_SIZES)
    }

    /**
     * True on the narrow rung, where a title, an icon and two chips will not fit together.
     *
     * Tied to the ladder rather than a hand-picked breakpoint. There are only two widths, so this is
     * just which rung the launcher took. Ellipsizing the title instead leaves a lone "…" between the
     * icon and the chips.
     */
    fun isNarrow(size: SizeFCompat): Boolean = size.width < WIDE_WIDTH

    /**
     * True where the chrome would leave no room for a full row.
     *
     * Header plus filter row is about 96dp and a card about 98dp, so below this you get a header and
     * a sliver. The short layouts drop the chrome and show one card instead.
     */
    fun isShort(size: SizeFCompat): Boolean = size.height < SHORT_HEIGHT

    private const val WIDE_WIDTH = 300f
    private const val SHORT_HEIGHT = 200f

    /** The two sides of [isNarrow] and [isShort]. Anything bigger stretches the wide/tall entry. */
    private val WIDTHS = listOf(180, WIDE_WIDTH.toInt())
    private val HEIGHTS = listOf(100, SHORT_HEIGHT.toInt())
    private const val MAX_SIZES = 16
}

/**
 * How many rows fit at a given size.
 *
 * Left over from before the lists scrolled, when each widget drew only the rows that fit. The
 * scrolling widgets pass their real row count to [WidgetImageBudget] instead, so nothing in the app
 * calls this now.
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
