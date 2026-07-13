package com.anisync.android.domain

import androidx.compose.runtime.Immutable
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaType
import kotlinx.serialization.Serializable

enum class LibraryStatus {
    CURRENT,
    PLANNING,
    COMPLETED,
    DROPPED,
    PAUSED,
    REPEATING,
    UNKNOWN
}

@Immutable
@Serializable
data class LibraryEntry(
    val id: Int,
    val mediaId: Int,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val titleNative: String?,
    val titleUserPreferred: String,
    val coverUrl: String?,
    val cover: CoverImage? = null,
    val progress: Int,
    val totalEpisodes: Int?,
    val totalChapters: Int?,
    val totalVolumes: Int?,
    val type: MediaType?,
    val format: MediaFormat? = null,
    val status: LibraryStatus,
    val nextAiringEpisode: Int? = null,
    val timeUntilAiring: Int? = null,
    val nextAiringEpisodeTime: Long? = null, // Absolute timestamp (seconds since epoch)
    /** AniWorld-only effective fields for Anime/CURRENT. Null means no safe source value. */
    val latestReleasedEpisode: Int? = null,
    val nextAiringIsApproximate: Boolean = false,
    val mediaStatus: String? = null,
    val averageScore: Int? = null,
    val score: Double? = 0.0,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val rewatches: Int = 0,
    val notes: String? = null,
    /**
     * The owning user's score display format. Carried in-memory for read-only views of another
     * user's list (profile media list, #78) so their score renders in their own format; null for
     * the local library, where score isn't shown on cards.
     */
    val scoreFormat: ScoreFormat? = null,
    val updatedAt: Long? = null,
    val createdAt: Long? = null,
    val mediaStartDate: Long? = null,
    val customLists: List<String> = emptyList(),
    val isPrivate: Boolean = false,
    val hiddenFromStatusLists: Boolean = false
) {
    /**
     * Computes the dynamic time until airing based on the absolute timestamp.
     * Returns null if no airing scheduled or already aired.
     */
    val dynamicTimeUntilAiring: Int?
        get() {
            val airingTime = nextAiringEpisodeTime ?: return timeUntilAiring
            val now = System.currentTimeMillis() / 1000
            val remaining = (airingTime - now).toInt()
            return if (remaining > 0) remaining else null
        }
}