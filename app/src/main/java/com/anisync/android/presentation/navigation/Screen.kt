package com.anisync.android.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for navigation.
 * - Objects: Routes without arguments
 * - Data classes: Routes with arguments
 */

@Serializable
object Login

@Serializable
object Library

@Serializable
object Discover

@Serializable
object Profile

/**
 * Details screen route with media ID and source screen for shared element matching.
 * @param mediaId The ID of the media to display
 * @param sourceScreen The screen that initiated navigation (e.g., "library", "discover", "profile")
 *                     Used to match shared element keys and prevent cross-tab transitions.
 */
@Serializable
data class MediaDetails(
    val mediaId: Int,
    val sourceScreen: String = "unknown"
)

@Serializable
data class CharacterDetails(
    val characterId: Int
)

/**
 * Section grid screen route for displaying all items from a Discover section.
 * @param sectionTitle The title of the section to display
 * @param sectionType The type of section: "trending", "popular", "upcoming", or "tba"
 * @param mediaType The media type: "ANIME" or "MANGA"
 */
@Serializable
data class SectionGrid(
    val sectionTitle: String,
    val sectionType: String,
    val mediaType: String = "ANIME"
)

/**
 * Grid screen for displaying all characters from a media's cast.
 * @param mediaId The ID of the media
 * @param mediaTitle The title of the media (for display in app bar)
 */
@Serializable
data class MediaCharactersGrid(
    val mediaId: Int,
    val mediaTitle: String
)

/**
 * Grid screen for displaying all related media.
 * @param mediaId The ID of the media
 * @param mediaTitle The title of the media (for display in app bar)
 */
@Serializable
data class MediaRelationsGrid(
    val mediaId: Int,
    val mediaTitle: String
)

/**
 * Grid screen for displaying all media a character appears in.
 * @param characterId The ID of the character
 * @param characterName The name of the character (for display in app bar)
 */
@Serializable
data class CharacterMediaGrid(
    val characterId: Int,
    val characterName: String
)