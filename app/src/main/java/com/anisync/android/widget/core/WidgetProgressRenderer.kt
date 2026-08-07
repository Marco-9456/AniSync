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
     * A progress bar, optionally carrying the aired marker.
     *
     * @param airedFraction position of the latest aired episode, or null when there is nothing to
     *   mark. The caller omits it when the viewer is caught up and for manga, which has no
     *   broadcast schedule.
     */
    fun bar(
        context: Context,
        widthDp: Int,
        heightDp: Int,
        progress: Float,
        trackColor: Int,
        fillColor: Int,
        airedFraction: Float? = null,
        dotColor: Int = fillColor
    ): Bitmap {
        val w = context.widgetPx(widthDp)
        val barPx = context.widgetPx(heightDp)
        val dot = (barPx * DOT_SCALE).toInt().coerceAtLeast(2)
        // The dot overhangs the bar, so the bitmap is as tall as the taller of the two.
        val h = maxOf(barPx, dot)
        val bitmap = createBitmap(w, h)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        val barH = barPx.toFloat()
        val top = (h - barH) / 2f
        val radius = barH / 2f

        paint.color = trackColor
        canvas.drawRoundRect(RectF(0f, top, w.toFloat(), top + barH), radius, radius, paint)

        val fillW = (w * progress.coerceIn(0f, 1f)).coerceAtLeast(if (progress > 0f) barH else 0f)
        if (fillW > 0f) {
            paint.color = fillColor
            canvas.drawRoundRect(RectF(0f, top, fillW, top + barH), radius, radius, paint)
        }

        if (airedFraction != null) {
            val r = dot / 2f
            val cx = (w * airedFraction.coerceIn(0f, 1f)).coerceIn(r, w - r)
            paint.color = dotColor
            canvas.drawCircle(cx, h / 2f, r, paint)
        }
        return bitmap
    }

    /** Twelve o'clock. Android's sweep origin is three o'clock. */
    private const val START_ANGLE = -90f
    private const val DOT_SCALE = 1.25f
}
