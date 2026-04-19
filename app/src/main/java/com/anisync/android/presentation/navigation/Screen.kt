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

@Serializable
data class UserProfile(val username: String)

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

@Serializable
data class StaffDetails(
    val staffId: Int
)

/**
 * Grid screen for displaying all media a staff member voiced characters in.
 * @param staffId The ID of the staff member
 * @param staffName The name of the staff member (for display in app bar)
 */
@Serializable
data class StaffMediaGrid(
    val staffId: Int,
    val staffName: String
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

/**
 * Statistics screen route for displaying user anime/manga statistics.
 * @param userId The AniList user ID
 */
@Serializable
data class Statistics(
    val userId: Int
)

// =============================================================================
// FORUM ROUTES
// =============================================================================

/**
 * Main Forum tab — shows recent threads and category chips.
 */
@Serializable
object Forum

/**
 * Category browse screen — shows threads filtered by a specific category.
 * @param categoryId The AniList ThreadCategory ID
 * @param categoryName Display name for the app bar
 */
@Serializable
data class ForumCategoryBrowse(
    val categoryId: Int,
    val categoryName: String
)

/**
 * Thread detail screen — shows the full thread body and comments.
 * @param threadId The AniList Thread ID
 * @param threadTitle The thread title (for immediate display before data loads)
 */
@Serializable
data class ForumThreadDetail(
    val threadId: Int,
    val threadTitle: String = "",
    val commentId: Int = 0
)

/**
 * Create new thread screen.
 */
@Serializable
object CreateThread

// =============================================================================
// REVIEW ROUTES
// =============================================================================

/**
 * Standalone review screen — used by deep-links.
 */
@Serializable
data class ReviewDetail(val reviewId: Int)



/**
 * Main settings hub screen.
 */
@Serializable
object Settings

/**
 * Look and Feel settings (theme, colors, title language, streaming service, haptic).
 */
@Serializable
object SettingsLookAndFeel

/**
 * Notification settings with master toggle and granular controls.
 */
@Serializable
object SettingsNotifications

/**
 * Storage management (cache size, clear cache).
 */
@Serializable
object SettingsStorage

/**
 * Account settings (user info, logout).
 */
@Serializable
object SettingsAccount

/**
 * About app (version, licenses, acknowledgments).
 */
@Serializable
object SettingsAbout

/**
 * Open source licenses screen.
 */
@Serializable
object SettingsOpenSourceLicenses

/**
 * Acknowledgments screen (credits to contributors and libraries).
 */
@Serializable
object SettingsAcknowledgments

/**
 * App updates screen.
 */
@Serializable
object SettingsUpdates

/**
 * Developer and source links screen.
 */
@Serializable
object SettingsLinks

/**
 * Developer tools screen (debug builds only).
 */
@Serializable
object SettingsDeveloperTools