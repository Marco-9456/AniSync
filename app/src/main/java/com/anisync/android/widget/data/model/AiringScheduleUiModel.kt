package com.anisync.android.widget.data.model

import android.graphics.Bitmap

/**
 * Widget-specific UI model for airing schedule items.
 * Contains pre-computed display values for efficient widget rendering.
 */
data class AiringScheduleUiModel(
    val mediaId: Int,
    val title: String,
    val coverUrl: String?,
    val coverBitmap: Bitmap? = null,
    val episode: Int,
    val airingAtSeconds: Long,
    val formattedTime: String,
    val isWatching: Boolean,
    val format: String?
) {
    /**
     * Time until airing in seconds.
     * Negative values indicate the episode has already aired.
     */
    fun getTimeUntilAiring(currentTimeSeconds: Long): Long {
        return airingAtSeconds - currentTimeSeconds
    }

    /**
     * Whether this episode is airing now (within 30 minutes of start time).
     */
    fun isAiringNow(currentTimeSeconds: Long): Boolean {
        val diff = getTimeUntilAiring(currentTimeSeconds)
        return diff <= 0 && diff > -1800 // Within 30 minutes past start
    }
}
