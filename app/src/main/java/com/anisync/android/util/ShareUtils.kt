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
}

/**
 * Utility object for sharing content via Android's share sheet.
 */
object ShareUtils {

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
        val shareText = "$title\n$url"

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        context.startActivity(Intent.createChooser(shareIntent, null))
    }

    /**
     * Shares character content using Android's share sheet.
     */
    fun shareCharacter(context: Context, name: String, characterId: Int) {
        val url = AniListUrls.characterUrl(characterId)
        val shareText = "$name\n$url"

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        context.startActivity(Intent.createChooser(shareIntent, null))
    }

    /**
     * Shares staff content using Android's share sheet.
     */
    fun shareStaff(context: Context, name: String, staffId: Int) {
        val url = AniListUrls.staffUrl(staffId)
        val shareText = "$name\n$url"

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        context.startActivity(Intent.createChooser(shareIntent, null))
    }
}
