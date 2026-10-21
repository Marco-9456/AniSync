package com.anisync.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.anisync.android.domain.Tag

/**
 * The tags AniList gives one title, cached for the library's tag filter.
 *
 * Kept out of library_entries on purpose. Tags belong to the title rather than to anyone's list
 * entry, they change rarely, and a title can carry twenty or more, so as a column they would be
 * parsed again with every entry on each progress tick and widget render.
 *
 * An empty [tags] list is a real answer: AniList has no tags for the title.
 */
@Entity(tableName = "media_tags")
data class MediaTagsEntity(
    @PrimaryKey val mediaId: Int,
    val tags: List<Tag>,
    val fetchedAt: Long
)

/** When each title's tags were last fetched, without reading the tags themselves. */
data class MediaTagsStamp(
    val mediaId: Int,
    val fetchedAt: Long
)
