package com.anisync.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle

/**
 * Represents a color palette option for the app theme.
 * 
 * @param id Unique identifier for persistence
 * @param name User-facing display name
 * @param seedColor The seed color used to generate the full M3 color scheme
 * @param isDynamic Whether this palette uses Android 12+ dynamic colors
 */
@Immutable
data class ThemePalette(
    val id: String,
    val name: String,
    val seedColor: Color,
    val isDynamic: Boolean = false
)

/**
 * Available preset color palettes for theme selection.
 */
object PresetPalettes {
    val Dynamic = ThemePalette(
        id = "dynamic",
        name = "Dynamic",
        seedColor = Color.Unspecified,
        isDynamic = true
    )
    
    val Pink = ThemePalette(
        id = "pink",
        name = "Pink",
        seedColor = Color(0xFFE91E63)
    )
    
    val Blue = ThemePalette(
        id = "blue",
        name = "Blue",
        seedColor = Color(0xFF2196F3)
    )
    
    val Green = ThemePalette(
        id = "green",
        name = "Green",
        seedColor = Color(0xFF4CAF50)
    )
    
    val Purple = ThemePalette(
        id = "purple",
        name = "Purple",
        seedColor = Color(0xFF9C27B0)
    )
    
    val Orange = ThemePalette(
        id = "orange",
        name = "Orange",
        seedColor = Color(0xFFFF9800)
    )
    
    val Teal = ThemePalette(
        id = "teal",
        name = "Teal",
        seedColor = Color(0xFF009688)
    )
    
    val Red = ThemePalette(
        id = "red",
        name = "Red",
        seedColor = Color(0xFFF44336)
    )
    
    val Indigo = ThemePalette(
        id = "indigo",
        name = "Indigo",
        seedColor = Color(0xFF3F51B5)
    )
    
    val Amber = ThemePalette(
        id = "amber",
        name = "Amber",
        seedColor = Color(0xFFFFC107)
    )
    
    val Cyan = ThemePalette(
        id = "cyan",
        name = "Cyan",
        seedColor = Color(0xFF00BCD4)
    )
    
    /**
     * All available preset palettes, with Dynamic first.
     */
    val all: List<ThemePalette> = listOf(
        Dynamic,
        Pink,
        Blue,
        Green,
        Purple,
        Orange,
        Teal,
        Red,
        Indigo,
        Amber,
        Cyan
    )
    
    /**
     * Find a palette by its ID.
     */
    fun findById(id: String): ThemePalette? = all.find { it.id == id }
}

/**
 * Extension to map PaletteStyle enum to user-friendly labels.
 */
fun PaletteStyle.toDisplayName(): String = when (this) {
    PaletteStyle.TonalSpot -> "Tonal Spot"
    PaletteStyle.Neutral -> "Neutral"
    PaletteStyle.Vibrant -> "Vibrant"
    PaletteStyle.Expressive -> "Expressive"
    PaletteStyle.Rainbow -> "Rainbow"
    PaletteStyle.FruitSalad -> "Fruit Salad"
    PaletteStyle.Monochrome -> "Monochrome"
    PaletteStyle.Fidelity -> "Fidelity"
    PaletteStyle.Content -> "Content"
}

/**
 * Extension to provide brief descriptions for each palette style.
 */
fun PaletteStyle.toDescription(): String = when (this) {
    PaletteStyle.TonalSpot -> "Calm, balanced colors"
    PaletteStyle.Neutral -> "Subtle, grayscale-leaning"
    PaletteStyle.Vibrant -> "Bold, maximum colorfulness"
    PaletteStyle.Expressive -> "Playful, diverse hues"
    PaletteStyle.Rainbow -> "Varied hue spectrum"
    PaletteStyle.FruitSalad -> "Colorful, lively mix"
    PaletteStyle.Monochrome -> "Pure black, white, gray"
    PaletteStyle.Fidelity -> "Preserves source color"
    PaletteStyle.Content -> "Analogous color harmony"
}
