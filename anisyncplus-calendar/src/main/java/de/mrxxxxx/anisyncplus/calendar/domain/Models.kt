package de.mrxxxxx.anisyncplus.calendar.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class ReleaseKind { EPISODE, FILM, SPECIAL, UNKNOWN }

enum class AniWorldReleaseLanguage {
    DE_SUB,
    DE_DUB,
    DE_SUB_AND_DUB,
    EN_SUB,
    UNKNOWN;

    val isVisibleGerman: Boolean
        get() = this == DE_SUB || this == DE_DUB || this == DE_SUB_AND_DUB
}

enum class ReleaseDiagnosticStatus {
    VALID,
    MISSING_TIME,
    MISSING_INSTALLMENT,
    INVALID_TIME,
    DST_GAP,
    DST_OVERLAP_EARLIER_OFFSET,
    UNKNOWN_LANGUAGE
}

enum class MatchStatus { MATCHED, AMBIGUOUS, UNMATCHED, MANUALLY_CONFIRMED }

data class AniWorldRelease(
    val localId: String,
    val snapshotId: String,
    val sourceSeriesKey: String,
    val sourceSlug: String?,
    val sourceUrl: String?,
    val rawTitle: String,
    val normalizedTitle: String,
    val sourceDate: LocalDate,
    val sourceLocalTime: LocalTime?,
    val sourceZoneId: ZoneId,
    val resolvedInstant: Instant?,
    val isApproximate: Boolean,
    val releaseKind: ReleaseKind,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val installmentNumber: Int?,
    val rawInstallmentToken: String?,
    val language: AniWorldReleaseLanguage,
    val rawLanguageMarkers: Set<String>,
    val sourceOrder: Int,
    val diagnosticStatus: ReleaseDiagnosticStatus
)

data class AniWorldParsedDocument(
    val snapshotId: String,
    val fetchedAt: Instant,
    val rangeStart: LocalDate,
    val rangeEnd: LocalDate,
    val documentSha256: String,
    val parserVersion: String,
    val daySectionCount: Int,
    val releases: List<AniWorldRelease>
) {
    val visibleGermanCount: Int = releases.count { it.language.isVisibleGerman }
}

data class AniListMatchCandidate(
    val mediaId: Int,
    val titleUserPreferred: String,
    val titleEnglish: String? = null,
    val titleRomaji: String? = null,
    val titleNative: String? = null,
    val synonyms: List<String> = emptyList(),
    val coverImageUrl: String? = null,
    val averageScore: Int? = null,
    val seasonNumber: Int? = null
) {
    val titleVariants: List<String>
        get() = listOfNotNull(titleUserPreferred, titleEnglish, titleRomaji, titleNative) + synonyms
}

data class TitleMatchDecision(
    val status: MatchStatus,
    val candidate: AniListMatchCandidate?,
    val candidateCount: Int,
    val bestScore: Double?,
    val secondBestScore: Double?,
    val scoreMargin: Double?,
    val reason: String,
    val matcherVersion: String
)

data class AniListUserMediaState(
    val mediaId: Int,
    val status: String,
    val progress: Int
)

data class EffectiveRelease(
    val aniListMediaId: Int?,
    val sourceTitle: String,
    val aniListTitle: String?,
    val coverImageUrl: String?,
    val averageScore: Int?,
    val releaseKind: ReleaseKind,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val installmentNumber: Int?,
    val instant: Instant?,
    val sourceDate: LocalDate,
    val sourceLocalTime: LocalTime?,
    val isApproximate: Boolean,
    val language: AniWorldReleaseLanguage,
    val matchStatus: MatchStatus,
    val libraryStatus: String?
)

data class EffectiveCalendarSnapshot(
    val snapshotId: String,
    val fetchedAt: Instant,
    val rangeStart: LocalDate,
    val rangeEnd: LocalDate,
    val releases: List<EffectiveRelease>
)

data class AniWorldSyncState(
    val lastAttemptAt: Instant? = null,
    val lastSuccessAt: Instant? = null,
    val lastErrorType: String? = null,
    val lastErrorMessage: String? = null,
    val httpStatus: Int? = null,
    val parsedCount: Int = 0,
    val visibleGermanCount: Int = 0,
    val matchedCount: Int = 0,
    val ambiguousCount: Int = 0,
    val unmatchedCount: Int = 0,
    val activeSnapshotId: String? = null,
    val rangeStart: LocalDate? = null,
    val rangeEnd: LocalDate? = null,
    val parserVersion: String = PARSER_VERSION,
    val matcherVersion: String = MATCHER_VERSION
)

const val PARSER_VERSION = "1"
const val MATCHER_VERSION = "1"
const val SOURCE_ZONE_ID = "Europe/Berlin"
const val SOURCE_URL = "https://aniworld.to/animekalender"
