package com.anisync.android.presentation.util

/**
 * Centralized shared element transition key management.
 * 
 * Using consistent keys across the app ensures:
 * - Proper matching between source and destination screens
 * - No key collisions between different navigation paths
 * - Easy debugging and maintenance
 * 
 * ## Key Format
 * Keys follow the pattern: `{screen}_{element}_{id}`
 * 
 * ## Example
 * ```kotlin
 * val coverKey = TransitionKeys.cover(TransitionKeys.LIBRARY, item.mediaId)
 * // Result: "library_media_cover_123"
 * ```
 */
object TransitionKeys {
    
    // ==================== SCREEN PREFIXES ====================
    
    /** Library tab/screen prefix */
    const val LIBRARY = "library"
    
    /** Discover tab/screen prefix */
    const val DISCOVER = "discover"
    
    /** Profile tab/screen prefix */
    const val PROFILE = "profile"
    
    /** Profile favorites section prefix */
    const val PROFILE_FAV = "profile_fav"
    
    /** Section grid screen prefix */
    const val SECTION_GRID = "sectiongrid"
    
    /** Media details screen prefix */
    const val MEDIA_DETAILS = "media_details"

    /** Character details screen prefix */
    const val CHARACTER = "character"

    /** Character media grid screen prefix */
    const val CHARACTER_GRID = "character_grid"

    /** Staff details screen prefix */
    const val STAFF = "staff"

    /** Staff media grid screen prefix */
    const val STAFF_GRID = "staff_grid"

    /** Staff production-media grid screen prefix */
    const val STAFF_PRODUCTION_GRID = "staff_production_grid"

    /** Studio details screen prefix */
    const val STUDIO = "studio"

    /** Studio media grid screen prefix */
    const val STUDIO_GRID = "studio_grid"
    
    /** Hero carousel prefix */
    const val HERO = "hero"
    
    /** Relations grid screen prefix */
    const val RELATIONS_GRID = "relations_grid"
    
    /** Cast section prefix */
    const val CAST = "cast"
    
    /** Relations section prefix */
    const val RELATIONS = "relations"
    
    /** Poster card generic prefix */
    const val POSTER = "poster"
    
    // ==================== KEY BUILDERS ====================
    
    /**
     * Creates a shared element key for media cover images.
     * Use with `Modifier.sharedElement()` on AsyncImage composables.
     * 
     * @param prefix The screen prefix (use constants above)
     * @param mediaId The unique media identifier
     * @return Key in format: "{prefix}_media_cover_{mediaId}"
     */
    fun cover(prefix: String, mediaId: Int): String = "${prefix}_media_cover_$mediaId"
    
    /**
     * Creates a shared element key for media titles.
     * Use with `Modifier.sharedBounds()` on Text composables.
     * 
     * @param prefix The screen prefix
     * @param mediaId The unique media identifier  
     * @return Key in format: "{prefix}_media_title_{mediaId}"
     */
    fun title(prefix: String, mediaId: Int): String = "${prefix}_media_title_$mediaId"
    
    /**
     * Creates a shared element key for gradient overlays.
     * Use with `Modifier.sharedBounds()` on gradient Box composables.
     * 
     * @param prefix The screen prefix
     * @param mediaId The unique media identifier
     * @return Key in format: "{prefix}_gradient_{mediaId}"
     */
    fun gradient(prefix: String, mediaId: Int): String = "${prefix}_gradient_$mediaId"
    
    /**
     * Creates a shared element key for container bounds.
     * Use for parent containers that need to animate their bounds.
     * 
     * @param prefix The screen prefix
     * @param id The unique identifier
     * @return Key in format: "{prefix}_container_{id}"
     */
    fun container(prefix: String, id: Int): String = "${prefix}_container_$id"
    
    /**
     * Creates a shared element key for character images.
     * 
     * @param characterId The unique character identifier
     * @return Key in format: "character_image_{characterId}"
     */
    fun characterImage(characterId: Int): String = "character_image_$characterId"

    fun staffImage(staffId: Int): String = "staff_image_$staffId"
    
    /**
     * Creates a shared element key for character names.
     * 
     * @param characterId The unique character identifier
     * @return Key in format: "character_name_{characterId}"
     */
    fun characterName(characterId: Int): String = "character_name_$characterId"
    
    /**
     * Creates a cache key for image loading.
     * Use with Coil's memoryCacheKey and placeholderMemoryCacheKey.
     * 
     * @param prefix The screen prefix
     * @param mediaId The unique media identifier
     * @return Key in format: "{prefix}_cover_{mediaId}"
     */
    fun imageCacheKey(prefix: String, mediaId: Int): String = "${prefix}_cover_$mediaId"
    
    /**
     * Creates a shared element key for relation/related media covers.
     * 
     * @param relationId The unique relation media identifier
     * @return Key in format: "relation_cover_{relationId}"
     */
    fun relationCover(relationId: Int): String = "relation_cover_$relationId"
}
