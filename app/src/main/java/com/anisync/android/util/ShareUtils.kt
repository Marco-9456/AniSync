package com.anisync.android.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.anisync.android.type.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Opens the given URL in the system browser. Swallows ActivityNotFoundException
 * on devices without a browser to avoid crashing.
 */
fun Context.launchUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

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
     * Writes a captured composable to app cache and launches the system share sheet
     * with it as a PNG. The optional [text] rides along as EXTRA_TEXT for targets that
     * accept a caption (Twitter, messengers). Backed by the FileProvider authority
     * "${packageName}.fileprovider" and the "shared/" cache-path in res/xml/filepaths.xml.
     *
     * File I/O runs off the main thread; the chooser launch hops back to it.
     */
    suspend fun shareBitmap(context: Context, bitmap: ImageBitmap, text: String? = null) {
        val uri = withContext(Dispatchers.IO) { writeShareablePng(context, bitmap) }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            text?.let { putExtra(Intent.EXTRA_TEXT, it) }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    /** Copies a plain-text [text] link to the clipboard (paste into the Feed composer, chats, …). */
    fun copyText(context: Context, text: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("AniSync", text))
    }

    /**
     * Saves the captured card to the shared Pictures/AniSync gallery folder. Uses the scoped
     * MediaStore pipeline on API 29+ (no permission needed) and the legacy insert below it.
     * Returns true on success.
     */
    suspend fun saveCardToGallery(context: Context, bitmap: ImageBitmap): Boolean =
        withContext(Dispatchers.IO) {
            val bmp = bitmap.asAndroidBitmap()
            val name = "AniSync_${System.currentTimeMillis()}.png"
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, name)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AniSync")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: return@withContext false
                    resolver.openOutputStream(uri)?.use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    } ?: return@withContext false
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    true
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.insertImage(
                        context.contentResolver, bmp, name, "AniSync card"
                    ) != null
                }
            } catch (_: Exception) {
                false
            }
        }

    /** One reusable cache PNG (overwritten each call) exposed as a FileProvider URI. */
    private fun writeShareablePng(context: Context, bitmap: ImageBitmap): Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "anisync_share.png")
        file.outputStream().use { out ->
            bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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
