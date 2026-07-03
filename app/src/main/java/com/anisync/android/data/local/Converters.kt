package com.anisync.android.data.local

import androidx.room.TypeConverter
import com.anisync.android.domain.AnimeStatusCounts
import com.anisync.android.domain.CharacterInfo
import com.anisync.android.domain.ExternalLink
import com.anisync.android.domain.ForumCategory
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaReview
import com.anisync.android.domain.RecommendedMedia
import com.anisync.android.domain.RelatedMedia
import com.anisync.android.domain.StudioInfo
import com.anisync.android.domain.StudioRef
import com.anisync.android.domain.Tag
import com.anisync.android.domain.Trailer
import com.anisync.android.domain.UserActivity
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
    fun fromStaffInfoList(value: String): List<com.anisync.android.domain.StaffInfo> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }

    @TypeConverter
    fun toStaffInfoList(list: List<com.anisync.android.domain.StaffInfo>): String = json.encodeToString(list)

    @TypeConverter
    fun fromRelationList(value: String): List<RelatedMedia> = json.decodeFromString(value)

    @TypeConverter
    fun toRelationList(list: List<RelatedMedia>): String = json.encodeToString(list)

    @TypeConverter
    fun fromExternalLinkList(value: String): List<ExternalLink> = json.decodeFromString(value)

    @TypeConverter
    fun toExternalLinkList(list: List<ExternalLink>): String = json.encodeToString(list)

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

    // --- Tag and Trailer Converters ---

    @TypeConverter
    fun fromTagList(value: String): List<Tag> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }

    @TypeConverter
    fun toTagList(list: List<Tag>): String = json.encodeToString(list)

    @TypeConverter
    fun fromMediaRankingList(value: String): List<com.anisync.android.domain.MediaRanking> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }

    @TypeConverter
    fun toMediaRankingList(list: List<com.anisync.android.domain.MediaRanking>): String =
        json.encodeToString(list)

    @TypeConverter
    fun fromTrailer(value: String?): Trailer? = value?.let {
        try {
            json.decodeFromString<Trailer>(it)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun toTrailer(trailer: Trailer?): String? = trailer?.let { json.encodeToString(it) }

    @TypeConverter
    fun fromForumCategoryList(value: String): List<ForumCategory> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }

    @TypeConverter
    fun toForumCategoryList(list: List<ForumCategory>): String = json.encodeToString(list)

    // --- RecommendedMedia Converters ---

    @TypeConverter
    fun fromRecommendedMediaList(value: String): List<RecommendedMedia> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }

    @TypeConverter
    fun toRecommendedMediaList(list: List<RecommendedMedia>): String = json.encodeToString(list)

    // --- MediaReview Converters ---

    @TypeConverter
    fun fromMediaReviewList(value: String): List<MediaReview> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }

    @TypeConverter
    fun toMediaReviewList(list: List<MediaReview>): String = json.encodeToString(list)

    @TypeConverter
    fun fromStaffDetailsList(value: String): List<com.anisync.android.domain.StaffDetails> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }

    @TypeConverter
    fun toStaffDetailsList(list: List<com.anisync.android.domain.StaffDetails>): String = json.encodeToString(list)

    @TypeConverter
    fun fromGenreStatList(value: String): List<com.anisync.android.domain.GenreStat> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }

    @TypeConverter
    fun toGenreStatList(list: List<com.anisync.android.domain.GenreStat>): String = json.encodeToString(list)

    @TypeConverter
    fun fromStudioInfoList(value: String): List<StudioInfo> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }

    @TypeConverter
    fun toStudioInfoList(list: List<StudioInfo>): String = json.encodeToString(list)

    @TypeConverter
    fun fromStudioRefList(value: String): List<StudioRef> = try {
        json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }

    @TypeConverter
    fun toStudioRefList(list: List<StudioRef>): String = json.encodeToString(list)
}
