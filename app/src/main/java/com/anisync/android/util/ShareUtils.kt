package com.anisync.android.util

import android.content.Context
import android.content.Intent
import com.anisync.android.type.MediaType

/**
 * Utility object for building AniList URLs and sharing media content.
 */
object AniListUrls {
    private const val BASE_URL = "https://anilist.co"

    /**
     * Builds the AniList URL for a media item.
     *
     * @param mediaId The AniList media ID
     * @param mediaType The type of media (ANIME or MANGA)
     * @return The full AniList URL (e.g., "https://anilist.co/anime/16498")
     */
    fun mediaUrl(mediaId: Int, mediaType: MediaType?): String {
        val typePath = when (mediaType) {
            MediaType.MANGA -> "manga"
            else -> "anime" // Default to anime for ANIME type or unknown
        }
        return "$BASE_URL/$typePath/$mediaId"
    }

    /**
     * Builds the AniList URL for a character.
     */
    fun characterUrl(characterId: Int): String {
        return "$BASE_URL/character/$characterId"
    }

    /**
     * Builds the AniList URL for a staff member.
     */
    fun staffUrl(staffId: Int): String {
        return "$BASE_URL/staff/$staffId"
    }

    /**
     * Builds the AniList URL for a studio.
     */
    fun studioUrl(studioId: Int): String {
        return "$BASE_URL/studio/$studioId"
    }
}

/**
 * Utility object for sharing content via Android's share sheet.
 */
object ShareUtils {

    fun shareText(context: Context, text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        context.startActivity(Intent.createChooser(shareIntent, null))
    }

    /**
     * Shares media content using Android's share sheet.
     *
     * @param context The context to start the share activity
     * @param title The media title to display in the share text
     * @param mediaId The AniList media ID
     * @param mediaType The type of media (ANIME or MANGA)
     */
    fun shareMedia(
        context: Context,
        title: String,
        mediaId: Int,
        mediaType: MediaType?
    ) {
        val url = AniListUrls.mediaUrl(mediaId, mediaType)
        shareText(context, "$title\n$url")
    }

    /**
     * Shares character content using Android's share sheet.
     */
    fun shareCharacter(context: Context, name: String, characterId: Int) {
        val url = AniListUrls.characterUrl(characterId)
        shareText(context, "$name\n$url")
    }

    /**
     * Shares staff content using Android's share sheet.
     */
    fun shareStaff(context: Context, name: String, staffId: Int) {
        val url = AniListUrls.staffUrl(staffId)
        shareText(context, "$name\n$url")
    }

    /**
     * Shares studio content using Android's share sheet.
     */
    fun shareStudio(context: Context, name: String, studioId: Int) {
        val url = AniListUrls.studioUrl(studioId)
        shareText(context, "$name\n$url")
    }
}
