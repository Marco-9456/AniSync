package com.anisync.android.presentation.share.cards

import androidx.compose.ui.graphics.Color
import com.anisync.android.ui.theme.GenreColor
import com.anisync.android.ui.theme.paletteRoles
import com.materialkolor.PaletteStyle
import java.util.concurrent.ConcurrentHashMap

private val tierColors = ConcurrentHashMap<Pair<Int, Boolean>, GenreColor>()

/**
 * A review score's tier as a container and its content, from the traffic light seeds every review
 * surface uses, so the card's pill reads in its own light or dark tone.
 */
internal fun reviewTierColor(score: Int, dark: Boolean): GenreColor {
    val tier = when {
        score >= 75 -> 0
        score >= 50 -> 1
        else -> 2
    }
    return tierColors.getOrPut(tier to dark) {
        val roles = paletteRoles(TierSeeds[tier], dark, PaletteStyle.TonalSpot)
        GenreColor(roles.primaryContainer(), roles.onPrimaryContainer())
    }
}

private val TierSeeds = listOf(Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFFFF5722))
