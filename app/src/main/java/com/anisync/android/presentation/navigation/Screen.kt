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

/**
 * Airing calendar — weekly anime airing schedule. Reached from the Library top bar.
 */
@Serializable
object Calendar

/**
 * Notes journal — every library entry the viewer has written a note on, searchable. Reached from the
 * Library top bar. Surfaces notes that were otherwise only visible inside the edit sheet.
 */
@Serializable
object Notes

@Serializable
object Feed

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

@Serializable
data class StudioDetails(
    val studioId: Int
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
 * Grid screen for displaying all works (media productions) of a studio.
 * @param studioId The ID of the studio
 * @param studioName The name of the studio (for display in app bar)
 */
@Serializable
data class StudioMediaGrid(
    val studioId: Int,
    val studioName: String
)

/**
 * Grid screen for displaying all media where a staff member has a production role
 * (director, writer, composer, etc.) — distinct from the voiced-characters grid.
 * @param staffId The ID of the staff member
 * @param staffName The name of the staff member (for display in app bar)
 */
@Serializable
data class StaffProductionMediaGrid(
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
 * Grid screen for displaying all recommendations for a media.
 * @param mediaId The ID of the media
 * @param mediaTitle The title of the media (for display in app bar)
 */
@Serializable
data class MediaRecommendationsGrid(
    val mediaId: Int,
    val mediaTitle: String
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
 * Thread-body editor — edits the body of an existing thread the viewer owns. A real full-screen
 * destination (not an inline overlay) so authoring is never confined to a list-detail pane on large
 * screens. Title and categories are preserved; only the body is editable here.
 * @param threadId The AniList thread id being edited
 */
@Serializable
data class EditThreadBody(val threadId: Int)

/**
 * Create new thread screen.
 *
 * Optionally pre-attaches a media to the thread's `mediaCategories` when launched
 * from a media's discussions (forum search media filter / detail "Discussions").
 * Defaults of [mediaId] = 0 / empty strings mean "no pre-attached media" so the
 * plain FAB entry point still works. Nullable primitives are avoided because
 * type-safe nav can't represent a nullable `Int` argument without a custom
 * NavType.
 * @param mediaId AniList media id to pre-attach, or 0 for none
 * @param mediaTitle Display title for the pre-attached media chip
 * @param mediaCoverUrl Cover image URL for the pre-attached media chip
 */
@Serializable
data class CreateThread(
    val mediaId: Int = 0,
    val mediaTitle: String = "",
    val mediaCoverUrl: String = ""
)

/**
 * Per-media discussions list — every thread with [mediaId] as a `mediaCategory`.
 * Reached from the media detail "Discussions" section ("View all").
 * @param mediaId The AniList media id
 * @param mediaTitle Display title for the app bar
 */
@Serializable
data class ForumMediaThreads(
    val mediaId: Int,
    val mediaTitle: String = ""
)

// =============================================================================
// REVIEW ROUTES
// =============================================================================

/**
 * Standalone review screen — used by deep-links.
 *
 * @param sourceScreen The screen that initiated navigation ("discover", "recent_reviews", …).
 *                     Used to match shared element keys; deep links keep the default.
 */
@Serializable
data class ReviewDetail(val reviewId: Int, val sourceScreen: String = "link")

/**
 * Recent reviews list — AniList's newest reviews, filterable by media type.
 * Reached from the "Recent Reviews" section on Discover.
 * @param mediaType Initial filter: "ANIME", "MANGA", or "ALL".
 */
@Serializable
data class RecentReviews(val mediaType: String = "ANIME")

/**
 * Review editor — write a new review or edit the viewer's existing one for a media.
 * @param mediaId The media being reviewed
 * @param mediaTitle The media title (for display in app bar)
 */
@Serializable
data class WriteReview(
    val mediaId: Int,
    val mediaTitle: String = ""
)

// =============================================================================
// ACTIVITY ROUTES
// =============================================================================

@Serializable
data class ActivityDetail(
    val activityId: Int,
    val targetReplyId: Int = 0
)

/**
 * Status editor — edits the body of the viewer's own text or message activity. A real full-screen
 * destination (not an inline overlay) so authoring is never confined to a list-detail pane on large
 * screens.
 * @param activityId The AniList activity id being edited
 */
@Serializable
data class EditActivity(val activityId: Int)

/**
 * New-status composer — posts a new text activity to the viewer's feed. A real full-screen
 * destination (not an inline overlay) so authoring is never confined to the Feed list pane on large
 * screens (where a detail pane may shrink the list to a sliver).
 */
@Serializable
object CreateStatus

/**
 * Notifications inbox screen for the current viewer.
 */
@Serializable
object Notifications



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
 * Theme picker subscreen — light/dark/system mode + AMOLED pure-black toggle.
 */
@Serializable
object SettingsTheme

/**
 * App language picker subscreen.
 */
@Serializable
object SettingsLanguage

/**
 * AniList settings: account management (add / switch / remove / logout) merged with the AniList
 * account options (adult content, languages, score format, activity, profile color).
 */
@Serializable
object SettingsAniList

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
 * About app (version, licenses, acknowledgments).
 */
@Serializable
object SettingsAbout

/**
 * Sponsors screen — lists GitHub Sponsors backers.
 */
@Serializable
object SettingsSponsors

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

/**
 * Font playground — live variable-font axis sliders (debug builds only).
 */
@Serializable
object SettingsFontPlayground

/**
 * Media upload host configuration (Catbox / Litterbox / custom).
 */
@Serializable
object SettingsMediaUpload