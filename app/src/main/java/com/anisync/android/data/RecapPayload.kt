package com.anisync.android.data

import com.anisync.android.GetRecapMediaQuery
import com.anisync.android.GetUserListActivitiesQuery
import com.anisync.android.domain.recap.FetchedActivity
import com.anisync.android.domain.recap.ListActivityRecord
import com.anisync.android.domain.recap.RecapMedia
import com.anisync.android.domain.recap.RecapMediaType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Bumped whenever the stored shape changes, so older rows are fetched again rather than misread. */
internal const val RECAP_PAYLOAD_VERSION = 1

/**
 * What a stored recap keeps: the activities that count in its period, the last earlier one for each
 * title, and the facts about those titles. Short field names, because a year of a busy list is a few
 * thousand activities.
 */
@Serializable
internal data class RecapPayload(
    /** Required rather than defaulted, so it is always written out and an old row never reads as new. */
    @SerialName("v") val version: Int,
    /** The time zone the period was cut in, which every later count has to use too. */
    @SerialName("z") val zone: String,
    @SerialName("a") val activities: List<StoredActivity>,
    @SerialName("m") val media: List<StoredMedia>,
    /** True when [airing] was fetched, so an empty list means none of the titles aired. */
    @SerialName("k") val hasAiring: Boolean = false,
    @SerialName("s") val airing: List<StoredAiring> = emptyList(),
)

/** One episode's airing time, epoch seconds. */
@Serializable
internal data class StoredAiring(
    @SerialName("m") val mediaId: Int,
    @SerialName("e") val episode: Int,
    @SerialName("t") val airingAt: Long,
)

@Serializable
internal data class StoredActivity(
    @SerialName("i") val id: Int,
    @SerialName("m") val mediaId: Int,
    @SerialName("s") val status: String? = null,
    @SerialName("p") val progress: String? = null,
    @SerialName("t") val createdAt: Long,
)

@Serializable
internal data class StoredMedia(
    @SerialName("i") val id: Int,
    @SerialName("k") val type: RecapMediaType,
    @SerialName("t") val title: String,
    @SerialName("tr") val titleRomaji: String? = null,
    @SerialName("te") val titleEnglish: String? = null,
    @SerialName("tn") val titleNative: String? = null,
    @SerialName("f") val format: String? = null,
    @SerialName("e") val episodes: Int? = null,
    @SerialName("c") val chapters: Int? = null,
    @SerialName("d") val duration: Int? = null,
    @SerialName("g") val genres: List<String> = emptyList(),
    @SerialName("st") val mainStudio: String? = null,
    @SerialName("x") val isAdult: Boolean = false,
    /** ISO dates. */
    @SerialName("sd") val startDate: String? = null,
    @SerialName("ed") val endDate: String? = null,
    @SerialName("r") val relationIds: List<Int> = emptyList(),
    @SerialName("cv") val coverUrl: String? = null,
    @SerialName("cl") val color: String? = null,
    @SerialName("se") val season: String? = null,
    @SerialName("sy") val seasonYear: Int? = null,
)

private val payloadJson = Json { ignoreUnknownKeys = true }

internal fun RecapPayload.encode(): String = payloadJson.encodeToString(RecapPayload.serializer(), this)

/** The stored payload, or null when it is unreadable or from another version. */
internal fun decodeRecapPayload(json: String): RecapPayload? =
    runCatching { payloadJson.decodeFromString(RecapPayload.serializer(), json) }.getOrNull()
        ?.takeIf { it.version == RECAP_PAYLOAD_VERSION }

/** Packs [records], the facts about their titles and, for a year, their airing times for storage. */
internal fun recapPayload(
    zone: ZoneId,
    records: List<ListActivityRecord>,
    media: Map<Int, RecapMedia>,
    airing: Map<Int, Map<Int, Long>>? = null,
): RecapPayload {
    val titles = records.mapTo(LinkedHashSet()) { it.mediaId }
    return RecapPayload(
        version = RECAP_PAYLOAD_VERSION,
        zone = zone.id,
        activities = records.map { StoredActivity(it.id, it.mediaId, it.status, it.progress, it.createdAt) },
        media = titles.mapNotNull { media[it]?.toStored() },
        hasAiring = airing != null,
        airing = airing.orEmpty().filterKeys { it in titles }.flatMap { (mediaId, episodes) ->
            episodes.map { (episode, airedAt) -> StoredAiring(mediaId, episode, airedAt) }
        },
    )
}

/** The stored airing times by title and episode, or null when none were fetched for this period. */
internal fun RecapPayload.airingByTitle(): Map<Int, Map<Int, Long>>? =
    if (!hasAiring) null else airing.groupBy { it.mediaId }.mapValues { (_, rows) -> rows.associate { it.episode to it.airingAt } }

internal val RecapPayload.zoneId: ZoneId
    get() = runCatching { ZoneId.of(zone) }.getOrDefault(ZoneId.systemDefault())

internal fun RecapPayload.mediaById(): Map<Int, RecapMedia> = media.associate { it.id to it.toDomain() }

/** The stored activities with their titles' types, leaving out any whose title is missing. */
internal fun RecapPayload.records(media: Map<Int, RecapMedia> = mediaById()): List<ListActivityRecord> =
    activities.mapNotNull { activity ->
        media[activity.mediaId]?.let {
            ListActivityRecord(activity.id, activity.mediaId, it.type, activity.status, activity.progress, activity.createdAt)
        }
    }

private fun RecapMedia.toStored() = StoredMedia(
    id = id,
    type = type,
    title = title,
    titleRomaji = titleRomaji,
    titleEnglish = titleEnglish,
    titleNative = titleNative,
    format = format,
    episodes = episodes,
    chapters = chapters,
    duration = duration,
    genres = genres,
    mainStudio = mainStudio,
    isAdult = isAdult,
    startDate = startDate?.toString(),
    endDate = endDate?.toString(),
    relationIds = relationIds,
    coverUrl = coverUrl,
    color = color,
    season = season,
    seasonYear = seasonYear,
)

private fun StoredMedia.toDomain() = RecapMedia(
    id = id,
    type = type,
    title = title,
    format = format,
    episodes = episodes,
    chapters = chapters,
    duration = duration,
    genres = genres,
    mainStudio = mainStudio,
    isAdult = isAdult,
    startDate = startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    endDate = endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    relationIds = relationIds,
    titleRomaji = titleRomaji,
    titleEnglish = titleEnglish,
    titleNative = titleNative,
    coverUrl = coverUrl,
    color = color,
    season = season,
    seasonYear = seasonYear,
)

internal fun GetUserListActivitiesQuery.OnListActivity.toFetched(): FetchedActivity? {
    val mediaId = media?.id ?: return null
    return FetchedActivity(id, mediaId, status, progress, createdAt.toLong())
}

/** Null for a title without a type, which a recap cannot place. */
internal fun GetRecapMediaQuery.Medium.toRecapMedia(): RecapMedia? {
    val mediaType = when (type?.rawValue) {
        "ANIME" -> RecapMediaType.ANIME
        "MANGA" -> RecapMediaType.MANGA
        else -> return null
    }
    val names = title
    return RecapMedia(
        id = id,
        type = mediaType,
        title = names?.userPreferred ?: names?.romaji ?: names?.english ?: names?.native ?: "",
        format = format?.rawValue,
        episodes = episodes,
        chapters = chapters,
        duration = duration,
        genres = genres?.filterNotNull().orEmpty(),
        mainStudio = studios?.nodes?.firstNotNullOfOrNull { it?.name },
        isAdult = isAdult == true,
        startDate = startDate?.let { fuzzyStart(it.year, it.month, it.day) },
        endDate = endDate?.let { fuzzyEnd(it.year, it.month, it.day) },
        relationIds = relations?.nodes?.mapNotNull { it?.id }.orEmpty(),
        titleRomaji = names?.romaji,
        titleEnglish = names?.english,
        titleNative = names?.native,
        coverUrl = coverImage?.extraLarge,
        color = coverImage?.color,
        season = season?.rawValue,
        seasonYear = seasonYear,
    )
}

/** A fuzzy date read as early as it allows: a year alone is its first day. */
internal fun fuzzyStart(year: Int?, month: Int?, day: Int?): LocalDate? {
    year ?: return null
    val yearMonth = YearMonth.of(year, (month ?: 1).coerceIn(1, 12))
    return yearMonth.atDay((day ?: 1).coerceIn(1, yearMonth.lengthOfMonth()))
}

/** A fuzzy date read as late as it allows: a year alone is its last day. */
internal fun fuzzyEnd(year: Int?, month: Int?, day: Int?): LocalDate? {
    year ?: return null
    val yearMonth = YearMonth.of(year, (month ?: 12).coerceIn(1, 12))
    return yearMonth.atDay((day ?: yearMonth.lengthOfMonth()).coerceIn(1, yearMonth.lengthOfMonth()))
}
