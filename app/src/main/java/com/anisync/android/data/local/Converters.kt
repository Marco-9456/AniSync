package com.anisync.android.data.local

import androidx.room.TypeConverter
import com.anisync.android.domain.UserActivity
import com.anisync.android.domain.AnimeStatusCounts
import com.anisync.android.domain.CharacterInfo
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.RelatedMedia
import com.anisync.android.type.MediaType
import kotlinx.serialization.json.Json

/**
 * Room TypeConverters for complex types and enums.
 */
class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    // --- Enum Converters ---
    
    @TypeConverter
    fun fromMediaType(value: String?): MediaType? = value?.let { MediaType.valueOf(it) }

    @TypeConverter
    fun toMediaType(type: MediaType?): String? = type?.name

    @TypeConverter
    fun fromLibraryStatus(value: String): LibraryStatus = LibraryStatus.valueOf(value)

    @TypeConverter
    fun toLibraryStatus(status: LibraryStatus): String = status.name

    @TypeConverter
    fun fromNullableLibraryStatus(value: String?): LibraryStatus? = value?.let { LibraryStatus.valueOf(it) }

    @TypeConverter
    fun toNullableLibraryStatus(status: LibraryStatus?): String? = status?.name

    // --- List Converters using kotlinx.serialization ---

    @TypeConverter
    fun fromStringList(value: String): List<String> = json.decodeFromString(value)

    @TypeConverter
    fun toStringList(list: List<String>): String = json.encodeToString(list)

    @TypeConverter
    fun fromCharacterList(value: String): List<CharacterInfo> = json.decodeFromString(value)

    @TypeConverter
    fun toCharacterList(list: List<CharacterInfo>): String = json.encodeToString(list)

    @TypeConverter
    fun fromRelationList(value: String): List<RelatedMedia> = json.decodeFromString(value)

    @TypeConverter
    fun toRelationList(list: List<RelatedMedia>): String = json.encodeToString(list)

    @TypeConverter
    fun fromLibraryEntryList(value: String): List<LibraryEntry> = json.decodeFromString(value)

    @TypeConverter
    fun toLibraryEntryList(list: List<LibraryEntry>): String = json.encodeToString(list)

    @TypeConverter
    fun fromUserActivityList(value: String): List<UserActivity> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }

    @TypeConverter
    fun toUserActivityList(list: List<UserActivity>): String = json.encodeToString(list)

    @TypeConverter
    fun fromAnimeStatusCounts(value: String): AnimeStatusCounts = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        AnimeStatusCounts()
    }

    @TypeConverter
    fun toAnimeStatusCounts(counts: AnimeStatusCounts): String = json.encodeToString(counts)
}
