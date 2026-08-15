package com.anisync.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.anisync.android.domain.MediaTheme

/**
 * Cached AnimeThemes lookup for one title.
 *
 * The themes are stored as one JSON blob rather than three tables. They are read whole,
 * written whole, and never queried across titles, so relational storage would buy nothing.
 *
 * An empty [themes] list is a real answer, not a missing one: it records that AnimeThemes
 * does not list the title, which stops every visit re-asking.
 */
@Entity(tableName = "media_themes")
data class MediaThemesEntity(
    @PrimaryKey val mediaId: Int,
    val animeSlug: String?,
    val themes: List<MediaTheme>,
    val fetchedAt: Long
)
