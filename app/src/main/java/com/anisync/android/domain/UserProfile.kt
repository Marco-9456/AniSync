package com.anisync.android.domain

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
data class UserProfile(
    val id: Int,
    val name: String,
    /** AniList profile highlight color: a named color (blue/purple/…) or a donator hex, or null
     *  for the default. Drives the optional per-visited-profile theme tint. */
    val profileColor: String? = null,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val about: String?,
    /** Raw markdown source for the bio. Used to prefill the edit-profile screen so the
     *  server-rendered HTML isn't sent back as the new about (which would corrupt formatting). */
    val aboutMarkdown: String? = null,
    val activeAt: Long?,
    val animeCount: Int,
    val daysWatched: Float, // Converted from minutes
    val mangaCount: Int,
    val chaptersRead: Int,
    val meanScore: Float,
    val animeStatusCounts: AnimeStatusCounts,
    val favoriteAnime: List<LibraryEntry>,
    val activities: List<UserActivity>,
    val topGenres: List<GenreStat> = emptyList(),
    val favoriteMangaOverview: List<LibraryEntry> = emptyList(),
    val favoriteCharactersOverview: List<CharacterInfo> = emptyList(),
    val favoriteStaffOverview: List<StaffDetails> = emptyList(),
    val favoriteStudiosOverview: List<StudioInfo> = emptyList(),
    val donatorTier: Int = 0,
    val donatorBadge: String? = null,
    val moderatorRoles: List<String> = emptyList(),
    val createdAt: Long? = null
)

@Immutable
@Serializable
data class StudioInfo(
    val id: Int,
    val name: String
)

/**
 * Holds counts of anime by status for the profile status bar.
 */
@Immutable
@Serializable
data class AnimeStatusCounts(
    val watching: Int = 0,
    val completed: Int = 0,
    val onHold: Int = 0,
    val dropped: Int = 0,
    val planning: Int = 0
)

enum class ActivityType {
    TEXT,
    MESSAGE,
    MEDIA_LIST,
    UNKNOWN
}

/** Media kind behind a MEDIA_LIST activity; drives the card's Anime/Manga type label. */
@Serializable
enum class ActivityMediaType { ANIME, MANGA }

@Immutable
@Serializable
data class UserActivity(
    val id: Int,
    val type: ActivityType = ActivityType.MEDIA_LIST,
    val status: String? = null, // e.g. "watched episode", "read chapter"
    val progress: String? = null, // "1", "12 - 13"
    val mediaId: Int? = null,
    val mediaTitle: String = "",
    val mediaCoverUrl: String? = null,
    val mediaCover: CoverImage? = null,
    /** AniList cover-image accent color (hex, e.g. "#e85d75"); tints the list-activity card body. */
    val mediaCoverColor: String? = null,
    /** AniList 18+ flag for the activity's media. Used to hide adult ListActivity from the feed
     *  when the viewer's account has displayAdultContent off (matches the website). */
    val mediaIsAdult: Boolean = false,
    /** ANIME or MANGA for MEDIA_LIST activities; null for text/message. Tags the card with a
     *  type label that mirrors AniList's rendered media links. */
    val mediaType: ActivityMediaType? = null,
    val timestamp: Long,
    val mediaScore: Int? = null,
    val text: String? = null,
    /** Raw markdown source (asHtml: false). Set for own TEXT/MESSAGE so cards can offer inline edit. */
    val bodyMarkdown: String? = null,
    val userId: Int? = null,
    val userName: String? = null,
    val userAvatarUrl: String? = null,
    val recipientId: Int? = null,
    val recipientName: String? = null,
    val recipientAvatarUrl: String? = null,
    val replyUserName: String? = null,
    val replyUserAvatarUrl: String? = null,
    val repliedAt: Long? = null,
    val lastReplyId: Int? = null,
    val likeCount: Int = 0,
    val replyCount: Int = 0,
    val isLiked: Boolean = false,
    val isLocked: Boolean = false,
    val isSubscribed: Boolean = false,
    val isPrivate: Boolean = false,
    val isPinned: Boolean = false,
    val isAuthorMod: Boolean = false
)
