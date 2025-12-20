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
 * @param sectionType The type of section: "trending", "popular", or "upcoming"
 */
@Serializable
data class SectionGrid(
    val sectionTitle: String,
    val sectionType: String
)