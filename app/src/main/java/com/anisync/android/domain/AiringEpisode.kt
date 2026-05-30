package com.anisync.android.domain

/**
 * A single anime episode airing entry, used by the in-app airing calendar.
 *
 * Richer than [AiringSchedule] (which backs the home-screen widgets): it carries the
 * fields the calendar UI needs — score, format, the four title variants, and the
 * viewer's list status for the "following only" filter and the "Watching" chip.
 */
data class AiringEpisode(
    val id: Int,
    val episode: Int,
    /** Unix time (seconds) the episode airs, in UTC. */
    val airingAt: Long,
    val mediaId: Int,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val titleNative: String?,
    val titleUserPreferred: String,
    val coverImageUrl: String?,
    val format: String?,
    val averageScore: Int?,
    /** True when the media is on the viewer's list (any status). */
    val isOnList: Boolean,
    /** The viewer's list status for this media, if on their list. */
    val listStatus: LibraryStatus?,
    val isAdult: Boolean
)
