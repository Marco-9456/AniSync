package com.anisync.android.widget.core

import android.content.Context
import android.util.Size
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Works out how big a widget is allowed to decode its images.
 *
 * AppWidgetServiceImpl throws out a RemoteViews whose bitmaps go past about 1.5x the display at four
 * bytes per pixel, and the widget then silently fails to render. One budget covers every bitmap in
 * the update, so the more images a layout shows the smaller each has to be. Same idea as the
 * ImageUtils helper in platform-samples.
 *
 * There is also a plain waste cap on top: decoding a poster bigger than the box it draws into just
 * burns memory on pixels nobody sees.
 */
object WidgetImageBudget {

    private const val BYTES_PER_PIXEL = 4

    /** 1.5x the display at [BYTES_PER_PIXEL], the same multiplier the platform uses. */
    private const val DISPLAY_MULTIPLIER = 6

    /**
     * How much of the platform cap we actually spend. The launcher counts the whole RemoteViews
     * payload, not only bitmaps, and we ship one layout per declared size in the same update, so
     * filling the budget exactly gets the update rejected.
     */
    private const val SAFETY_FRACTION = 0.4f

    /** Past this on any edge you gain nothing at widget sizes. */
    private const val MAX_EDGE_PX = 480

    /**
     * Pixel size to request for a poster.
     *
     * @param displayWidthDp the dp width the poster actually renders at
     * @param aspect height divided by width, 1.4 for AniList covers
     * @param imageCount how many images this one update will carry, which is what splits the budget
     * @param variantCount declared sizes in the update. Each carries its own copy of every row and
     *   bitmap, so the real cost is [imageCount] times this. Get it wrong and the platform drops the
     *   update with no exception, leaving the widget on its initial layout.
     */
    fun posterSize(
        context: Context,
        displayWidthDp: Int,
        aspect: Float = COVER_ASPECT,
        imageCount: Int = 1,
        variantCount: Int = 1
    ): Size {
        val density = context.resources.displayMetrics.density
        // Twice the dp box keeps posters sharp on dense screens without going overboard.
        val wantedWidth = (displayWidthDp * density * 2f).toInt().coerceAtLeast(MIN_EDGE_PX)

        val copies = imageCount.coerceAtLeast(1) * variantCount.coerceAtLeast(1)
        val budgetWidth = budgetedWidth(context, aspect, copies)
        val width = min(min(wantedWidth, budgetWidth), MAX_EDGE_PX).coerceAtLeast(MIN_EDGE_PX)
        return Size(width, (width * aspect).toInt().coerceAtLeast(MIN_EDGE_PX))
    }

    /** Widest each image can be when [imageCount] of them share the budget. */
    private fun budgetedWidth(context: Context, aspect: Float, imageCount: Int): Int {
        val metrics = context.resources.displayMetrics
        val totalBytes = DISPLAY_MULTIPLIER.toLong() * metrics.widthPixels * metrics.heightPixels
        val perImagePixels = (totalBytes * SAFETY_FRACTION / BYTES_PER_PIXEL / imageCount)
        if (perImagePixels <= 0) return MIN_EDGE_PX
        // pixels = w * (w * aspect), so w = sqrt(pixels / aspect)
        return sqrt(perImagePixels / aspect).toInt()
    }

    /** AniList covers are about 460x650. */
    const val COVER_ASPECT = 1.4f

    private const val MIN_EDGE_PX = 48
}
