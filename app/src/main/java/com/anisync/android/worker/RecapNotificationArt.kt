package com.anisync.android.worker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.anisync.android.R
import com.anisync.android.ui.theme.seededColorScheme
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What the notification's picture shows: the period's name, its covers and its colours. */
data class RecapArtSpec(
    /** "September", or the year on a yearly recap. */
    val headline: String,
    /** The year under a month's name, null on a yearly recap. */
    val subline: String?,
    val seed: Color?,
    val coverUrl: String?,
    val secondCoverUrl: String?,
)

/**
 * Draws WE2's big picture without a window: plain Canvas work on a background thread, so the
 * worker can post it while the app is closed. The frame is 392 by 150 and is drawn at three times
 * that, the cover in the nine-sided cookie and the second cover in a circle over it.
 */
object RecapNotificationArt {
    private const val SCALE = 3f
    private const val WIDTH = 392
    private const val HEIGHT = 150

    /** The weight, width and roundness of the heroWide role, the frames' month and year type. */
    private const val HERO_AXES = "'wght' 900, 'wdth' 125, 'ROND' 100, 'opsz' 27"

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    suspend fun render(context: Context, imageLoader: ImageLoader, spec: RecapArtSpec): Bitmap {
        val cover = spec.coverUrl?.let { load(context, imageLoader, it, 564) }
        val second = spec.secondCoverUrl?.let { load(context, imageLoader, it, 288) }
        return withContext(Dispatchers.Default) {
            val scheme = spec.seed?.let { seededColorScheme(it, darkTheme = true, amoled = false, paletteStyle = PaletteStyle.TonalSpot) }
            val container = scheme?.primaryContainer ?: Color(0xFF2C586A)
            val onContainer = scheme?.onPrimaryContainer ?: Color(0xFFBFEAFF)
            val primary = scheme?.primary ?: Color(0xFFA2CDE2)
            val bitmap = createBitmap(px(WIDTH), px(HEIGHT))
            val canvas = Canvas(bitmap)
            canvas.drawColor(container.toArgb())
            if (cover != null) {
                drawInPolygon(canvas, cover, MaterialShapes.Cookie9Sided, RectF(dp(232f), dp(-20f), dp(232f + 187.6f), dp(-20f + 185.3f)))
            }
            if (second != null) {
                drawInCircle(canvas, second, RectF(dp(184f), dp(66f), dp(280f), dp(162f)))
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = ResourcesCompat.getFont(context, R.font.google_sans_flex)
                fontVariationSettings = HERO_AXES
                textSize = dp(27f)
            }
            // The frames set 27/36 lines from their tops, so the baseline sits where the line box puts it.
            val lineTop = { top: Float -> top + (dp(36f) - (paint.descent() - paint.ascent())) / 2 - paint.ascent() }
            if (spec.subline != null) {
                paint.color = onContainer.toArgb()
                canvas.drawText(spec.headline, dp(18f), lineTop(dp(58f)), paint)
                paint.color = primary.toArgb()
                canvas.drawText(spec.subline, dp(18f), lineTop(dp(92f)), paint)
            } else {
                paint.color = onContainer.toArgb()
                canvas.drawText(spec.headline, dp(18f), lineTop(dp(75f)), paint)
            }
            bitmap
        }
    }

    private fun dp(value: Float): Float = value * SCALE

    private fun px(value: Int): Int = (value * SCALE).toInt()

    private suspend fun load(context: Context, imageLoader: ImageLoader, url: String, size: Int): Bitmap? {
        val request = ImageRequest.Builder(context).data(url).size(size, size).allowHardware(false).build()
        return (imageLoader.execute(request) as? SuccessResult)?.image?.toBitmap()
    }

    private fun drawInPolygon(canvas: Canvas, image: Bitmap, polygon: RoundedPolygon, into: RectF) {
        val bounds = polygon.calculateBounds()
        val path = polygon.toPath(Path())
        val fit = Matrix().apply {
            setRectToRect(RectF(bounds[0], bounds[1], bounds[2], bounds[3]), into, Matrix.ScaleToFit.CENTER)
        }
        path.transform(fit)
        canvas.drawPath(path, croppedPaint(image, into))
    }

    private fun drawInCircle(canvas: Canvas, image: Bitmap, into: RectF) {
        canvas.drawOval(into, croppedPaint(image, into))
    }

    /** A paint that fills [into] with [image] cropped to cover it, the way a cover is cut. */
    private fun croppedPaint(image: Bitmap, into: RectF): Paint {
        val scale = maxOf(into.width() / image.width, into.height() / image.height)
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(into.centerX() - image.width * scale / 2, into.centerY() - image.height * scale / 2)
        }
        val shader = BitmapShader(image, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(matrix) }
        return Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader }
    }
}
