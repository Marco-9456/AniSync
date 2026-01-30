package com.anisync.android.widget.data.model

import android.graphics.Bitmap

/**
 * Widget-specific UI model for up-next items.
 * Contains pre-computed display values for efficient widget rendering.
 */
data class UpNextUiModel(
    val mediaId: Int,
    val title: String,
    val coverUrl: String?,
    val coverBitmap: Bitmap? = null,
    val nextEpisode: Int,
    val progress: Int,
    val totalEpisodes: Int?,
    val airingTimeSeconds: Long,
    val streamingUrl: String?,
    val mediaStatus: String?
) {
    /**
     * Progress as a percentage (0f to 1f).
     */
    val progressPercent: Float
        get() = if (totalEpisodes != null && totalEpisodes > 0) {
            progress.toFloat() / totalEpisodes
        } else {
            0f
        }

    /**
     * Formatted progress string (e.g., "5/12 watched").
     */
    val progressText: String
        get() = if (totalEpisodes != null) {
            "$progress/$totalEpisodes watched"
        } else {
            "$progress watched"
        }

    /**
     * Time until next episode airs in seconds.
     */
    fun getTimeUntilAiring(currentTimeSeconds: Long): Long {
        return if (airingTimeSeconds > 0) airingTimeSeconds - currentTimeSeconds else 0L
    }

    /**
     * Whether the next episode is currently airing.
     */
    fun isAiringNow(currentTimeSeconds: Long): Boolean {
        val diff = getTimeUntilAiring(currentTimeSeconds)
        return airingTimeSeconds > 0 && diff <= 0 && diff > -1800
    }

    /**
     * Whether this show is currently releasing new episodes.
     */
    val isReleasing: Boolean
        get() = mediaStatus == null || mediaStatus == "RELEASING"
}
