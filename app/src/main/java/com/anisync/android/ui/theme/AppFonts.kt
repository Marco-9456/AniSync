package com.anisync.android.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.anisync.android.R

@OptIn(ExperimentalTextApi::class)
private fun robotoFlex(weight: Int, opticalSize: Float) = Font(
    resId = R.font.roboto_flex,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", opticalSize)
    )
)

val DisplayFontFamily = FontFamily(
    robotoFlex(weight = 300, opticalSize = 36f),
    robotoFlex(weight = 400, opticalSize = 36f),
    robotoFlex(weight = 500, opticalSize = 36f),
    robotoFlex(weight = 700, opticalSize = 36f),
    robotoFlex(weight = 900, opticalSize = 36f)
)

val BodyFontFamily = FontFamily(
    robotoFlex(weight = 400, opticalSize = 14f),
    robotoFlex(weight = 500, opticalSize = 14f),
    robotoFlex(weight = 700, opticalSize = 14f)
)

val NumericFontFamily = FontFamily(
    robotoFlex(weight = 400, opticalSize = 36f),
    robotoFlex(weight = 700, opticalSize = 36f),
    robotoFlex(weight = 900, opticalSize = 36f)
)
