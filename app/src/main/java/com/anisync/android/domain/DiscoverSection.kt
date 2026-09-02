package com.anisync.android.domain

import com.anisync.android.type.MediaType

/**
 * One rail on Discover, in the order the viewer put it.
 *
 * The set is not the same on both tabs. [AIRING_TODAY] reads AniList's airing schedule, which only
 * exists for anime, and [RELEASING_NOW] fills that slot on Manga instead. Because the sets differ,
 * the order is stored per media type rather than once for the screen.
 *
 * [id] is what gets persisted, so it must outlive renames of the enum constant.
 */
enum class DiscoverSection(val id: String) {
    TRENDING("trending"),
    AIRING_TODAY("airing_today"),
    RELEASING_NOW("releasing_now"),
    POPULAR("popular"),
    NOT_YET_RELEASED("not_yet_released"),
    NEWLY_ADDED("newly_added"),
    REVIEWS("reviews");

    /** Whether this rail has anything to show for [type]. */
    fun supports(type: MediaType): Boolean = when (this) {
        AIRING_TODAY -> type == MediaType.ANIME
        RELEASING_NOW -> type == MediaType.MANGA
        else -> true
    }

    companion object {
        fun fromId(id: String): DiscoverSection? = entries.firstOrNull { it.id == id }

        /** Declaration order, minus whatever the tab cannot show. */
        fun defaultOrder(type: MediaType): List<DiscoverSection> = entries.filter { it.supports(type) }

        /**
         * The stored order, repaired against the current build: ids that no longer exist are
         * dropped, sections that do not belong to this tab are dropped, and a section the stored
         * order predates is appended rather than lost. Appending keeps an order the viewer set
         * intact when a later release adds a rail.
         */
        fun resolveOrder(type: MediaType, stored: List<String>): List<DiscoverSection> {
            val supported = defaultOrder(type)
            val restored = stored.mapNotNull(::fromId).filter { it in supported }
            return restored + supported.filterNot { it in restored }
        }
    }
}
