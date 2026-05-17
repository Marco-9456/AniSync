package com.anisync.android.data

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

enum class FavouriteEntity { CHARACTER, STAFF, STUDIO }

/**
 * Bridges AniList's eventual-consistency window after a ToggleFavourite mutation.
 *
 * AniList's `Character`/`Staff`/`Studio` queries can return a stale `isFavourite`
 * for several seconds after the mutation lands. Without an override, re-entering
 * the detail screen shows the heart as un-toggled even though the favourite was
 * recorded server-side. Mirrors the role `MediaDetailsDao.updateFavouriteStatus`
 * plays for media (see DetailsRepositoryImpl.toggleFavourite).
 *
 * Entries auto-expire after TTL as a safety bound; they are also cleared as soon
 * as the server response matches the override (server has caught up).
 */
@Singleton
class FavouriteOverrideStore @Inject constructor() {

    private data class Entry(val value: Boolean, val expiresAt: Long)

    private val overrides = ConcurrentHashMap<Pair<FavouriteEntity, Int>, Entry>()

    fun set(entity: FavouriteEntity, id: Int, value: Boolean) {
        overrides[entity to id] = Entry(value, System.currentTimeMillis() + TTL_MS)
    }

    /**
     * Returns the override if present and not expired, else null.
     * Expired entries are evicted on read.
     */
    fun get(entity: FavouriteEntity, id: Int): Boolean? {
        val key = entity to id
        val entry = overrides[key] ?: return null
        if (System.currentTimeMillis() > entry.expiresAt) {
            overrides.remove(key)
            return null
        }
        return entry.value
    }

    /** Clear the override if the freshly-fetched server value matches it. */
    fun clearIfMatches(entity: FavouriteEntity, id: Int, serverValue: Boolean) {
        val key = entity to id
        val entry = overrides[key] ?: return
        if (entry.value == serverValue) overrides.remove(key)
    }

    companion object {
        private const val TTL_MS = 60L * 60L * 1000L // 1 hour safety bound
    }
}
