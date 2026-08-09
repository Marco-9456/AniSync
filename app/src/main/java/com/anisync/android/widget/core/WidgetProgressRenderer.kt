package com.anisync.android.widget.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap

/**
 * Draws the Watch Progress widget's progress indicators to bitmaps.
 *
 * RemoteViews offers no arc primitive, and no way to say "this child is 82% of the parent" without
 * a weight per state. Both indicators are therefore rasterised here and set with
 * `setImageViewBitmap`.
 *
 * Colours arrive already resolved to ARGB ints, from [WidgetColors].
 */
object WidgetProgressRenderer {

    /**
     * A completion ring. Used at the 2x1 size, where there is no width for a bar.
     *
     * @param discColor filled behind the ring, or null to leave the cover showing through. The
     *   compact layout passes the primary colour here for the finishing-soon state.
     */
    fun ring(
        context: Context,
        sizeDp: Int,
        progress: Float,
        trackColor: Int,
        fillColor: Int,
        thicknessDp: Int,
        discColor: Int? = null
    ): Bitmap {
        val px = context.widgetPx(sizeDp)
        val stroke = context.widgetPx(thicknessDp).toFloat()
        val bitmap = createBitmap(px, px)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val centre = px / 2f
        if (discColor != null) {
            paint.style = Paint.Style.FILL
            paint.color = discColor
            canvas.drawCircle(centre, centre, centre, paint)
        }

        val inset = stroke / 2f
        val bounds = RectF(inset, inset, px - inset, px - inset)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke

        paint.strokeCap = Paint.Cap.BUTT
        paint.color = trackColor
        canvas.drawArc(bounds, 0f, 360f, false, paint)

        val sweep = progress.coerceIn(0f, 1f) * 360f
        if (sweep > 0f) {
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = fillColor
            canvas.drawArc(bounds, START_ANGLE, sweep, false, paint)
        }
        return bitmap
    }

    /**
     * A progress bar in up to three zones: watched, aired but unwatched, and not yet out.
     *
     * The middle zone replaces what used to be a dot sitting on top of the fill. A small circle in
     * a third colour, drawn over a filled bar, was both hard to see and easy to mistake for a
     * rendering fault. A band reads immediately, needs no legend, and answers the question the
     * widget exists for, which is how much there is to catch up on.
     *
     * @param airedFraction where the latest released episode falls, or null when there is nothing
     *   to distinguish: manga, a finished series, or a viewer who is already caught up.
     */
    fun bar(
        context: Context,
        widthDp: Int,
        heightDp: Int,
        progress: Float,
        trackColor: Int,
        fillColor: Int,
        airedFraction: Float? = null,
        airedColor: Int = fillColor
    ): Bitmap {
        val w = context.widgetPx(widthDp)
        val h = context.widgetPx(heightDp)
        val bitmap = createBitmap(w, h)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        val radius = h / 2f
        val full = RectF(0f, 0f, w.toFloat(), h.toFloat())

        paint.color = trackColor
        canvas.drawRoundRect(full, radius, radius, paint)

        // Aired first, so the watched zone paints over its left edge and the two meet cleanly.
        airedFraction?.let { aired ->
            val airedW = w * aired.coerceIn(0f, 1f)
            if (airedW > 0f) {
                paint.color = airedColor
                canvas.drawRoundRect(RectF(0f, 0f, airedW, h.toFloat()), radius, radius, paint)
            }
        }

        val fillW = (w * progress.coerceIn(0f, 1f)).coerceAtLeast(if (progress > 0f) h.toFloat() else 0f)
        if (fillW > 0f) {
            paint.color = fillColor
            canvas.drawRoundRect(RectF(0f, 0f, fillW, h.toFloat()), radius, radius, paint)
        }
        return bitmap
    }

    /** [color] at [alpha], for the aired-but-unwatched band. */
    fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)

    /** Twelve o'clock. Android's sweep origin is three o'clock. */
    private const val START_ANGLE = -90f
}
