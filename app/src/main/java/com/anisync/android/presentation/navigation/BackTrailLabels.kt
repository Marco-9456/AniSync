package com.anisync.android.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph
import androidx.navigation.toRoute
import com.anisync.android.R
import com.anisync.android.presentation.forum.forumCategoryTabs
import com.anisync.android.presentation.util.formatAndYear
import com.anisync.android.type.MediaFormat

/**
 * What a page that loads its own title tells the trail: the title as the page shows it and what kind
 * of page it is. Media also carry the format and year their row prints under the title.
 */
data class TrailLabel(
    val title: String,
    val kind: TrailKind,
    val format: String? = null,
    val year: Int? = null,
)

enum class TrailKind { ANIME, MANGA, CHARACTER, STAFF, STUDIO, THREAD }

/**
 * Keeps [label] in this entry's own saved state, so the trail of a later page can name it and so it
 * survives process death. A ViewModel's SavedStateHandle is a different handle and would not do.
 */
fun NavBackStackEntry.recordTrailLabel(label: TrailLabel) {
    savedStateHandle[KEY_TITLE] = label.title
    savedStateHandle[KEY_KIND] = label.kind.name
    savedStateHandle[KEY_FORMAT] = label.format
    savedStateHandle[KEY_YEAR] = label.year
}

private fun NavBackStackEntry.trailLabel(): TrailLabel? {
    val handle = savedStateHandle
    val title = handle.get<String>(KEY_TITLE) ?: return null
    val kind = handle.get<String>(KEY_KIND)?.let { name -> TrailKind.entries.find { it.name == name } }
        ?: return null
    return TrailLabel(title, kind, handle.get<String>(KEY_FORMAT), handle.get<Int>(KEY_YEAR))
}

private const val KEY_TITLE = "back_trail_title"
private const val KEY_KIND = "back_trail_kind"
private const val KEY_FORMAT = "back_trail_format"
private const val KEY_YEAR = "back_trail_year"

internal fun NavDestination.isMainTab(): Boolean =
    hasRoute<Library>() || hasRoute<Discover>() || hasRoute<Feed>() || hasRoute<Forum>() ||
        hasRoute<Profile>()

/** Editors leave the trail out: their back means discard, and nothing navigates on from them. */
private fun NavDestination.isEditor(): Boolean =
    hasRoute<WriteReview>() || hasRoute<CreateThread>() || hasRoute<EditThreadBody>() ||
        hasRoute<EditActivity>() || hasRoute<CreateStatus>()

internal fun NavBackStackEntry.toTrailNode(): TrailNode {
    val destination = destination
    val key = when {
        destination is NavGraph || destination.hasRoute<Login>() || destination.isEditor() -> null
        destination.hasRoute<MediaDetails>() -> "media:${toRoute<MediaDetails>().mediaId}"
        destination.hasRoute<CharacterDetails>() -> "character:${toRoute<CharacterDetails>().characterId}"
        destination.hasRoute<StaffDetails>() -> "staff:${toRoute<StaffDetails>().staffId}"
        destination.hasRoute<StudioDetails>() -> "studio:${toRoute<StudioDetails>().studioId}"
        destination.hasRoute<StaffMediaGrid>() -> "staff_media:${toRoute<StaffMediaGrid>().staffId}"
        destination.hasRoute<StaffProductionMediaGrid>() ->
            "staff_production:${toRoute<StaffProductionMediaGrid>().staffId}"
        destination.hasRoute<StudioMediaGrid>() -> "studio_media:${toRoute<StudioMediaGrid>().studioId}"
        destination.hasRoute<CharacterMediaGrid>() ->
            "character_media:${toRoute<CharacterMediaGrid>().characterId}"
        destination.hasRoute<MediaRelationsGrid>() -> "relations:${toRoute<MediaRelationsGrid>().mediaId}"
        destination.hasRoute<MediaRecommendationsGrid>() ->
            "recommendations:${toRoute<MediaRecommendationsGrid>().mediaId}"
        destination.hasRoute<MediaThemes>() -> "themes:${toRoute<MediaThemes>().mediaId}"
        destination.hasRoute<SectionGrid>() -> toRoute<SectionGrid>().let {
            "section:${it.sectionType}:${it.mediaType}"
        }
        destination.hasRoute<ForumThreadDetail>() -> "thread:${toRoute<ForumThreadDetail>().threadId}"
        destination.hasRoute<ForumCategoryBrowse>() ->
            "forum_category:${toRoute<ForumCategoryBrowse>().categoryId}"
        destination.hasRoute<ForumMediaThreads>() -> "media_threads:${toRoute<ForumMediaThreads>().mediaId}"
        destination.hasRoute<ReviewDetail>() -> "review:${toRoute<ReviewDetail>().reviewId}"
        destination.hasRoute<ActivityDetail>() -> "activity:${toRoute<ActivityDetail>().activityId}"
        destination.hasRoute<UserProfile>() -> "user:${toRoute<UserProfile>().username}"
        // Every other page is a single screen, so its route is identity enough.
        else -> destination.route
    }
    return TrailNode(entryId = id, key = key, isTab = destination.isMainTab())
}

/** One row of the trail: the page's icon, its title as the page shows it, and an optional second line. */
@Immutable
internal data class TrailRow(val icon: ImageVector, val title: String, val supporting: String? = null)

/** The tab a trail ends on, with its navigation bar icon in the outlined style. */
@Composable
internal fun NavBackStackEntry.tabRow(): TrailRow {
    val destination = destination
    return when {
        destination.hasRoute<Discover>() -> TrailRow(Icons.Outlined.Explore, stringResource(R.string.nav_discover))
        destination.hasRoute<Feed>() -> TrailRow(Icons.Outlined.DynamicFeed, stringResource(R.string.nav_feed))
        destination.hasRoute<Forum>() -> TrailRow(Icons.Outlined.Forum, stringResource(R.string.nav_forum))
        destination.hasRoute<Profile>() -> TrailRow(Icons.Outlined.Person, stringResource(R.string.nav_profile))
        else -> TrailRow(Icons.Outlined.VideoLibrary, stringResource(R.string.nav_library))
    }
}

@Composable
internal fun NavBackStackEntry.trailRow(): TrailRow {
    val destination = destination
    val label = trailLabel()
    return when {
        destination.hasRoute<MediaDetails>() -> {
            val manga = label?.kind == TrailKind.MANGA
            val format = label?.format
                ?.let { MediaFormat.safeValueOf(it) }
                ?.takeIf { it != MediaFormat.UNKNOWN__ }
            TrailRow(
                icon = if (manga) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.Tv,
                title = label?.title
                    ?: stringResource(if (manga) R.string.media_type_manga else R.string.media_type_anime),
                supporting = formatAndYear(format, label?.year),
            )
        }
        destination.hasRoute<CharacterDetails>() ->
            kindRow(Icons.Default.Face, label?.title, stringResource(R.string.share_person_character))
        destination.hasRoute<StaffDetails>() ->
            kindRow(Icons.Default.Person, label?.title, stringResource(R.string.share_person_staff))
        destination.hasRoute<StudioDetails>() ->
            kindRow(Icons.Default.Domain, label?.title, stringResource(R.string.studio_label_other_studio))
        destination.hasRoute<ForumThreadDetail>() -> TrailRow(
            Icons.Outlined.ChatBubbleOutline,
            label?.title ?: toRoute<ForumThreadDetail>().threadTitle.ifBlank { stringResource(R.string.nav_forum) }
        )
        destination.hasRoute<ForumCategoryBrowse>() -> toRoute<ForumCategoryBrowse>().let { route ->
            TrailRow(
                forumCategoryTabs.find { it.id == route.categoryId }?.icon ?: Icons.Outlined.Forum,
                route.categoryName
            )
        }
        destination.hasRoute<ForumMediaThreads>() ->
            TrailRow(Icons.Outlined.ChatBubbleOutline, stringResource(R.string.forum_discussions_title))
        destination.hasRoute<UserProfile>() ->
            TrailRow(Icons.Outlined.AccountCircle, toRoute<UserProfile>().username)
        destination.hasRoute<SectionGrid>() ->
            TrailRow(Icons.Outlined.GridView, toRoute<SectionGrid>().sectionTitle)
        else -> screenRow(destination)
    }
}

private fun kindRow(icon: ImageVector, title: String?, kind: String): TrailRow =
    if (title == null) TrailRow(icon, kind) else TrailRow(icon, title, kind)

/** Pages with a fixed name: their own screen title, one line. */
@Composable
private fun screenRow(destination: NavDestination): TrailRow {
    val (icon, title) = when {
        destination.hasRoute<MediaRelationsGrid>() -> Icons.Outlined.GridView to R.string.section_related
        destination.hasRoute<MediaRecommendationsGrid>() ->
            Icons.Outlined.GridView to R.string.section_recommendations
        destination.hasRoute<CharacterMediaGrid>() -> Icons.Outlined.GridView to R.string.featured_media
        destination.hasRoute<StaffMediaGrid>() -> Icons.Outlined.GridView to R.string.voiced_characters
        destination.hasRoute<StaffProductionMediaGrid>() -> Icons.Outlined.GridView to R.string.production_roles
        destination.hasRoute<StudioMediaGrid>() -> Icons.Outlined.GridView to R.string.studio_label_works
        destination.hasRoute<MediaThemes>() -> Icons.Outlined.MusicNote to R.string.section_themes
        destination.hasRoute<ReviewDetail>() -> Icons.Outlined.RateReview to R.string.label_review
        destination.hasRoute<RecentReviews>() -> Icons.Outlined.RateReview to R.string.recent_reviews_title
        destination.hasRoute<ActivityDetail>() -> Icons.Outlined.DynamicFeed to R.string.activity_detail_title
        destination.hasRoute<Calendar>() -> Icons.Default.CalendarMonth to R.string.calendar_title
        destination.hasRoute<Notes>() -> Icons.AutoMirrored.Filled.EventNote to R.string.notes_journal_title
        destination.hasRoute<Notifications>() -> Icons.Outlined.Notifications to R.string.notifications_title
        destination.hasRoute<Settings>() -> Icons.Outlined.Settings to R.string.settings
        destination.hasRoute<SettingsGeneral>() -> Icons.Outlined.Settings to R.string.settings_general
        destination.hasRoute<SettingsLookAndFeel>() -> Icons.Outlined.Palette to R.string.settings_look_and_feel
        destination.hasRoute<SettingsLanguage>() -> Icons.Outlined.Translate to R.string.settings_app_language
        destination.hasRoute<SettingsAniList>() -> Icons.Outlined.Tune to R.string.settings_anilist_account
        destination.hasRoute<SettingsNotifications>() ->
            Icons.Outlined.Notifications to R.string.settings_notifications
        destination.hasRoute<SettingsStorage>() -> Icons.Outlined.Storage to R.string.settings_storage
        destination.hasRoute<SettingsMediaUpload>() -> Icons.Outlined.CloudUpload to R.string.settings_media_upload
        destination.hasRoute<SettingsUpdates>() -> Icons.Outlined.Update to R.string.settings_updates
        destination.hasRoute<SettingsSponsors>() -> Icons.Rounded.VolunteerActivism to R.string.sponsors_title
        destination.hasRoute<SettingsAbout>() -> Icons.Outlined.Info to R.string.settings_about
        destination.hasRoute<SettingsOpenSourceLicenses>() ->
            Icons.Outlined.Description to R.string.settings_open_source_licenses
        destination.hasRoute<SettingsAcknowledgments>() -> Icons.Outlined.Info to R.string.settings_acknowledgments
        destination.hasRoute<SettingsLinks>() -> Icons.Rounded.Link to R.string.settings_links
        destination.hasRoute<SettingsDeveloperTools>() -> Icons.Outlined.Build to R.string.settings_developer_tools
        destination.hasRoute<SettingsFontPlayground>() -> Icons.Outlined.TextFields to R.string.font_playground_title
        else -> Icons.AutoMirrored.Outlined.ArrowBack to R.string.back
    }
    return TrailRow(icon, stringResource(title))
}
