package com.anisync.android.presentation.share.cards

import android.graphics.Bitmap
import coil3.size.Size
import coil3.transform.Transformation
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Blurs a card's artwork in software. Modifier.blur does nothing below Android 12, and a card has to
 * look the same on every device it is saved from, so the blur is baked into the bitmap instead.
 *
 * The image is shrunk first, which keeps the blur cheap and lets three box passes stand in for the
 * frames' Gaussian. Compose stretches the result back up with bilinear filtering.
 */
internal class ArtworkBlur : Transformation() {
    override val cacheKey: String = "share-artwork-blur-$SMALL_SIDE-$RADIUS"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val scale = SMALL_SIDE.toFloat() / max(input.width, input.height)
        val width = (input.width * scale).roundToInt().coerceAtLeast(1)
        val height = (input.height * scale).roundToInt().coerceAtLeast(1)
        val source = if (input.config == Bitmap.Config.HARDWARE) input.copy(Bitmap.Config.ARGB_8888, false) else input
        val small = Bitmap.createScaledBitmap(source, width, height, true)
        val pixels = IntArray(width * height)
        small.getPixels(pixels, 0, width, 0, 0, width, height)
        repeat(PASSES) { boxBlur(pixels, width, height, RADIUS) }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private companion object {
        /** About 1/7 of the 440dp the frames blow the artwork up to. */
        const val SMALL_SIDE = 64

        /** Three passes of this radius come close to the frames' 24dp blur at that scale. */
        const val RADIUS = 3
        const val PASSES = 3
    }
}

/** One horizontal and one vertical box pass over ARGB [pixels], edges clamped. */
internal fun boxBlur(pixels: IntArray, width: Int, height: Int, radius: Int) {
    if (radius <= 0 || width <= 0 || height <= 0) return
    val scratch = IntArray(pixels.size)
    blurLine(pixels, scratch, lines = height, length = width, step = 1, lineStep = width, radius = radius)
    blurLine(scratch, pixels, lines = width, length = height, step = width, lineStep = 1, radius = radius)
}

private fun blurLine(src: IntArray, dst: IntArray, lines: Int, length: Int, step: Int, lineStep: Int, radius: Int) {
    val window = radius * 2 + 1
    for (line in 0 until lines) {
        val start = line * lineStep
        var a = 0
        var r = 0
        var g = 0
        var b = 0
        for (i in -radius..radius) {
            val p = src[start + i.coerceIn(0, length - 1) * step]
            a += p ushr 24
            r += (p shr 16) and 0xFF
            g += (p shr 8) and 0xFF
            b += p and 0xFF
        }
        for (i in 0 until length) {
            dst[start + i * step] = ((a / window) shl 24) or ((r / window) shl 16) or ((g / window) shl 8) or (b / window)
            val out = src[start + (i - radius).coerceIn(0, length - 1) * step]
            val enter = src[start + (i + radius + 1).coerceIn(0, length - 1) * step]
            a += (enter ushr 24) - (out ushr 24)
            r += ((enter shr 16) and 0xFF) - ((out shr 16) and 0xFF)
            g += ((enter shr 8) and 0xFF) - ((out shr 8) and 0xFF)
            b += (enter and 0xFF) - (out and 0xFF)
        }
    }
}
