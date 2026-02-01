package com.anisync.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material Design 3 Expressive Shape tokens for the app.
 * 
 * MD3 Expressive emphasizes bolder, friendlier shapes with larger corner radii.
 * This shape scale provides consistent, reusable shape tokens across the app.
 * 
 * Shape Scale:
 * - extraSmall: 4dp - Tiny elements, progress indicators
 * - small: 8dp - Small badges, chips
 * - medium: 12dp - Default elements, list items
 * - large: 16dp - Medium cards, containers
 * - extraLarge: 28dp - Hero cards, large containers (MD3 Expressive)
 * 
 * Additional Expressive tokens:
 * - pill: 50% - Full stadium shape for chips, badges, buttons
 */
val AppShapes = Shapes(
    // Tiny elements, progress indicators
    extraSmall = RoundedCornerShape(4.dp),
    
    // Small badges, small chips, icon buttons
    small = RoundedCornerShape(8.dp),
    
    // Default elements, list items, thumbnails
    medium = RoundedCornerShape(12.dp),
    
    // Medium cards, standard containers
    large = RoundedCornerShape(16.dp),
    
    // Hero cards, large containers - MD3 Expressive recommends 28dp
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Extended shape tokens for MD3 Expressive design.
 * Use these for specific UI patterns that need shapes beyond the standard scale.
 */
object ExpressiveShapes {
    /**
     * Medium-Large shape (20dp) - For genre cards, studio cards
     * Sits between large (16dp) and extraLarge (28dp)
     */
    val mediumLarge = RoundedCornerShape(20.dp)
    
    /**
     * Large shape (24dp) - For standard content cards
     * Slightly smaller than extraLarge for visual hierarchy
     */
    val large = RoundedCornerShape(24.dp)
    
    /**
     * Full stadium/pill shape - For chips, badges, filter pills
     * Creates fully rounded ends regardless of height
     */
    val pill = RoundedCornerShape(percent = 50)
    
    /**
     * Extra-large shape (28dp) - For hero/stat cards
     * Maximum expressiveness for prominent UI elements
     */
    val extraLarge = RoundedCornerShape(28.dp)
}
