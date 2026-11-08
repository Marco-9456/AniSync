package com.anisync.android.presentation.share.studio

import android.graphics.Bitmap
import androidx.annotation.StringRes
import com.anisync.android.R
import com.materialkolor.hct.Hct
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score

/** Up to four colours the cover is made of, most telling first, with greys left out. */
internal fun coverSwatches(bitmap: Bitmap): List<Int> {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val opaque = pixels.filter { (it ushr 24) == 0xFF }.toIntArray()
    if (opaque.isEmpty()) return emptyList()
    val populations = QuantizerCelebi.quantize(opaque, MaxQuantized)
    return Score.score(populations, desired = MaxSwatches, fallbackColorArgb = null)
}

/**
 * A swatch's name from where its colour sits in HCT: one of eight hue families, muted or vivid by
 * chroma, so a cover's colours read as Wine, Slate or Sage rather than as hex codes.
 */
@StringRes
internal fun swatchName(argb: Int): Int {
    val hct = Hct.fromInt(argb)
    val muted = hct.chroma < MutedChroma
    val family = HueFamilies.first { hct.hue < it.upTo }
    return if (muted) family.muted else family.vivid
}

private class HueFamily(val upTo: Double, @StringRes val muted: Int, @StringRes val vivid: Int)

private val HueFamilies = listOf(
    HueFamily(50.0, R.string.share_swatch_wine, R.string.share_swatch_crimson),
    HueFamily(75.0, R.string.share_swatch_clay, R.string.share_swatch_amber),
    HueFamily(110.0, R.string.share_swatch_cream, R.string.share_swatch_gold),
    HueFamily(165.0, R.string.share_swatch_sage, R.string.share_swatch_leaf),
    HueFamily(210.0, R.string.share_swatch_seafoam, R.string.share_swatch_teal),
    HueFamily(265.0, R.string.share_swatch_slate, R.string.share_swatch_cobalt),
    HueFamily(325.0, R.string.share_swatch_lilac, R.string.share_swatch_violet),
    HueFamily(355.0, R.string.share_swatch_mauve, R.string.share_swatch_fuchsia),
    HueFamily(361.0, R.string.share_swatch_wine, R.string.share_swatch_crimson),
)

private const val MutedChroma = 36.0
private const val MaxQuantized = 128
private const val MaxSwatches = 4
