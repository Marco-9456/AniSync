package com.anisync.android.domain

import kotlinx.coroutines.flow.Flow

/**
 * Openings and endings for a title, looked up on AnimeThemes by AniList id.
 *
 * Room is the source of truth so a cached page paints its themes offline, and
 * [refreshThemes] is the only thing that touches the network.
 */
interface MediaThemesRepository {

    /** Cached themes for [mediaId]. Emits null until a lookup has been stored. */
    fun observeThemes(mediaId: Int): Flow<MediaThemes?>

    /**
     * Fetches from AnimeThemes and stores the answer, including an empty one, so a
     * title with no themes is not looked up again on every visit.
     */
    suspend fun refreshThemes(mediaId: Int): Result<MediaThemes>

    /** True when the stored answer is missing or old enough to be worth refetching. */
    suspend fun isStale(mediaId: Int): Boolean
}
