package com.anisync.android.widget.core

import android.content.Context
import android.util.Size
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Works out how large a widget may decode its images.
 *
 * `AppWidgetServiceImpl` rejects a `RemoteViews` whose bitmaps exceed roughly 1.5x the display at
 * four bytes per pixel, and the widget silently fails to render when that happens. The budget is
 * shared by every bitmap in one update, so the more images a layout shows the smaller each one has
 * to be. See the platform-samples `ImageUtils` helper this mirrors.
 *
 * On top of the platform cap there is a plain waste cap: decoding a poster larger than the dp box
 * it renders into costs memory and CPU for pixels nobody sees.
 */
object WidgetImageBudget {

    private const val BYTES_PER_PIXEL = 4

    /** 1.5x the display, at [BYTES_PER_PIXEL]. The multiplier the platform itself applies. */
    private const val DISPLAY_MULTIPLIER = 6

    /**
     * Fraction of the platform cap we are willing to spend. The launcher counts the whole
     * `RemoteViews` payload, not just our bitmaps, and a responsive widget ships one layout per
     * declared size in a single update, so filling the budget exactly is asking to be rejected.
     */
    private const val SAFETY_FRACTION = 0.4f

    /** Decoding beyond this on any edge buys nothing at widget sizes. */
    private const val MAX_EDGE_PX = 480

    /**
     * Pixel size to request for a poster.
     *
     * @param displayWidthDp the dp width the poster actually renders at
     * @param aspect height divided by width, 1.4 for AniList covers
     * @param imageCount how many images this one update will carry, which is what splits the budget
     */
    fun posterSize(
        context: Context,
        displayWidthDp: Int,
        aspect: Float = COVER_ASPECT,
        imageCount: Int = 1
    ): Size {
        val density = context.resources.displayMetrics.density
        // Two times the dp box keeps posters crisp on high-density screens without going silly.
        val wantedWidth = (displayWidthDp * density * 2f).toInt().coerceAtLeast(MIN_EDGE_PX)

        val budgetWidth = budgetedWidth(context, aspect, imageCount.coerceAtLeast(1))
        val width = min(min(wantedWidth, budgetWidth), MAX_EDGE_PX).coerceAtLeast(MIN_EDGE_PX)
        return Size(width, (width * aspect).toInt().coerceAtLeast(MIN_EDGE_PX))
    }

    /** Widest each image may be if [imageCount] of them must share the budget. */
    private fun budgetedWidth(context: Context, aspect: Float, imageCount: Int): Int {
        val metrics = context.resources.displayMetrics
        val totalBytes = DISPLAY_MULTIPLIER.toLong() * metrics.widthPixels * metrics.heightPixels
        val perImagePixels = (totalBytes * SAFETY_FRACTION / BYTES_PER_PIXEL / imageCount)
        if (perImagePixels <= 0) return MIN_EDGE_PX
        // pixels = w * (w * aspect), so w = sqrt(pixels / aspect)
        return sqrt(perImagePixels / aspect).toInt()
    }

    /** AniList covers are 460x650ish. */
    const val COVER_ASPECT = 1.4f

    private const val MIN_EDGE_PX = 48
}
