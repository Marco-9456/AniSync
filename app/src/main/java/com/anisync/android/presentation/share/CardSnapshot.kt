package com.anisync.android.presentation.share

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Looper
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.os.HandlerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import kotlin.coroutines.resume

/**
 * Snapshots the recorded card for export. Below API 28 Compose reads the layer back through an
 * ImageReader but ignores the row stride, so a GPU that pads its rows (Mali pads 1080 px to 1088)
 * hands back a sheared image. That read is redone here with the padding skipped.
 */
suspend fun GraphicsLayer.toShareBitmap(): ImageBitmap =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        toImageBitmap()
    } else {
        readBackThroughImageReader().asImageBitmap()
    }

internal suspend fun GraphicsLayer.readBackThroughImageReader(): Bitmap {
    val width = size.width
    val height = size.height
    return ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1).use { reader ->
        val image = suspendCancellableCoroutine { continuation ->
            reader.setOnImageAvailableListener(
                { continuation.resume(it.acquireLatestImage()) },
                HandlerCompat.createAsync(Looper.myLooper() ?: Looper.getMainLooper()),
            )
            val surface = reader.surface
            val canvas = surface.lockHardwareCanvas()
            try {
                canvas.drawColor(android.graphics.Color.BLACK, PorterDuff.Mode.CLEAR)
                CanvasDrawScope().draw(
                    Density(1f),
                    LayoutDirection.Ltr,
                    Canvas(canvas),
                    Size(width.toFloat(), height.toFloat()),
                ) {
                    drawLayer(this@readBackThroughImageReader)
                }
            } finally {
                surface.unlockCanvasAndPost(canvas)
            }
        }
        image.use { it.toBitmap() }
    }
}

private fun Image.toBitmap(): Bitmap {
    val plane = planes[0]
    val pixels = plane.buffer.packRows(width * plane.pixelStride, plane.rowStride, height)
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        copyPixelsFromBuffer(pixels)
    }
}

/** Copies [rows] rows of [rowBytes] each out of a buffer whose rows start [rowStride] bytes apart. */
internal fun ByteBuffer.packRows(rowBytes: Int, rowStride: Int, rows: Int): ByteBuffer {
    if (rowStride == rowBytes) return this
    val source = duplicate()
    val packed = ByteArray(rowBytes * rows)
    val padding = ByteArray(rowStride - rowBytes)
    for (row in 0 until rows) {
        source.get(packed, row * rowBytes, rowBytes)
        if (row < rows - 1) source.get(padding)
    }
    return ByteBuffer.wrap(packed)
}
