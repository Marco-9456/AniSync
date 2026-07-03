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

    // Per-section Discover prefixes. The same media routinely appears in several Discover
    // sections at once (trending + popular, upcoming + popular, a search-grid result over the
    // feed), and all sections sit in one LazyColumn — a single shared prefix would register
    // duplicate cover keys and let a return morph land on the wrong section's card. Each
    // section owns a prefix, carried to details via MediaDetails.sourceScreen.
    /** Discover Trending Now hero pager prefix */
    const val DISCOVER_TRENDING = "discover_trending"

    /** Discover All Time Popular row prefix */
    const val DISCOVER_POPULAR = "discover_popular"

    /** Discover Upcoming row prefix */
    const val DISCOVER_UPCOMING = "discover_upcoming"

    /** Discover Newly Added row prefix */
    const val DISCOVER_NEWLY_ADDED = "discover_newly_added"

    /** Discover TBA row prefix */
    const val DISCOVER_TBA = "discover_tba"
    
    /** Profile tab/screen prefix */
    const val PROFILE = "profile"
    
    /** Profile favorites section prefix */
    const val PROFILE_FAV = "profile_fav"
    
    /** Section grid screen prefix */
    const val SECTION_GRID = "sectiongrid"
    
    /** Media details screen prefix */
    const val MEDIA_DETAILS = "media_details"

    /** Recent reviews list screen prefix */
    const val RECENT_REVIEWS = "recent_reviews"

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
     * A short, stable token identifying a *specific* cover image, derived from its URL
     * filename (resolution-independent on the AniList CDN, e.g. `bx21459-hash.jpg`).
     *
     * Append to [imageCacheKey] when building a Coil cache key for a cover. The id-based key
     * alone never changes, so when AniList swaps a media's cover the new URL can't evict the
     * old bitmap and the stale poster stays pinned in memory. Adding this token makes a
     * changed cover bust the cache, while an unchanged cover yields the same token on both the
     * source card and the detail screen — so the shared-element transition still reuses the
     * bitmap. Returns "" for a null/blank URL (key unchanged).
     */
    fun coverVersion(url: String?): String {
        if (url.isNullOrBlank()) return ""
        val file = url.substringAfterLast('/').substringBefore('?')
        return "-v" + Integer.toHexString(file.hashCode())
    }
    
    /**
     * Creates a shared element key for relation/related media covers.
     *
     * @param relationId The unique relation media identifier
     * @return Key in format: "relation_cover_{relationId}"
     */
    fun relationCover(relationId: Int): String = "relation_cover_$relationId"

    /**
     * Creates a shared element key for a review card's media banner.
     * Prefixed per source screen (like [cover]) so the same review appearing on
     * Discover and on the Recent Reviews list can't cross-match mid-transition.
     *
     * @param prefix The screen prefix (e.g. [DISCOVER], [RECENT_REVIEWS])
     * @param reviewId The unique review identifier
     * @return Key in format: "{prefix}_review_banner_{reviewId}"
     */
    fun reviewBanner(prefix: String, reviewId: Int): String = "${prefix}_review_banner_$reviewId"
}
