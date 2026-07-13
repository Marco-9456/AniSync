package de.mrxxxxx.anisyncplus.calendar.local

import de.mrxxxxx.anisyncplus.calendar.domain.AniWorldParsedDocument
import de.mrxxxxx.anisyncplus.calendar.domain.AniWorldRelease
import de.mrxxxxx.anisyncplus.calendar.domain.AniWorldReleaseLanguage
import de.mrxxxxx.anisyncplus.calendar.domain.AniWorldSyncState
import de.mrxxxxx.anisyncplus.calendar.domain.MATCHER_VERSION
import de.mrxxxxx.anisyncplus.calendar.domain.MatchStatus
import de.mrxxxxx.anisyncplus.calendar.domain.PARSER_VERSION
import de.mrxxxxx.anisyncplus.calendar.domain.ReleaseDiagnosticStatus
import de.mrxxxxx.anisyncplus.calendar.domain.ReleaseKind
import de.mrxxxxx.anisyncplus.calendar.domain.SOURCE_URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

internal fun AniWorldParsedDocument.toEntity() = AniWorldSnapshotEntity(
    snapshotId = snapshotId,
    fetchedAtEpochMillis = fetchedAt.toEpochMilli(),
    rangeStartEpochDay = rangeStart.toEpochDay(),
    rangeEndEpochDay = rangeEnd.toEpochDay(),
    documentSha256 = documentSha256,
    parserVersion = parserVersion,
    entryCount = releases.size,
    germanVisibleEntryCount = visibleGermanCount,
    sourceUrl = SOURCE_URL
)

internal fun AniWorldRelease.toEntity(parserVersion: String) = AniWorldReleaseEntity(
    localId = localId,
    snapshotId = snapshotId,
    sourceSeriesKey = sourceSeriesKey,
    sourceSlug = sourceSlug,
    sourceUrl = sourceUrl,
    rawTitle = rawTitle,
    normalizedTitle = normalizedTitle,
    sourceDateEpochDay = sourceDate.toEpochDay(),
    sourceLocalTimeMinutes = sourceLocalTime?.let { it.hour * 60 + it.minute },
    sourceZoneId = sourceZoneId.id,
    resolvedInstantEpochSeconds = resolvedInstant?.epochSecond,
    isApproximate = isApproximate,
    releaseKind = releaseKind.name,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    installmentNumber = installmentNumber,
    rawInstallmentToken = rawInstallmentToken,
    language = language.name,
    rawLanguageMarkers = rawLanguageMarkers.sorted().joinToString(MARKER_SEPARATOR),
    parserVersion = parserVersion,
    sourceOrder = sourceOrder,
    diagnosticStatus = diagnosticStatus.name
)

internal fun AniWorldReleaseEntity.toDomain() = AniWorldRelease(
    localId = localId,
    snapshotId = snapshotId,
    sourceSeriesKey = sourceSeriesKey,
    sourceSlug = sourceSlug,
    sourceUrl = sourceUrl,
    rawTitle = rawTitle,
    normalizedTitle = normalizedTitle,
    sourceDate = LocalDate.ofEpochDay(sourceDateEpochDay),
    sourceLocalTime = sourceLocalTimeMinutes?.let { LocalTime.of(it / 60, it % 60) },
    sourceZoneId = ZoneId.of(sourceZoneId),
    resolvedInstant = resolvedInstantEpochSeconds?.let(Instant::ofEpochSecond),
    isApproximate = isApproximate,
    releaseKind = ReleaseKind.valueOf(releaseKind),
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    installmentNumber = installmentNumber,
    rawInstallmentToken = rawInstallmentToken,
    language = AniWorldReleaseLanguage.valueOf(language),
    rawLanguageMarkers = rawLanguageMarkers.split(MARKER_SEPARATOR).filter(String::isNotEmpty).toSet(),
    sourceOrder = sourceOrder,
    diagnosticStatus = ReleaseDiagnosticStatus.valueOf(diagnosticStatus)
)

internal fun AniWorldSyncStateEntity.toDomain() = AniWorldSyncState(
    lastAttemptAt = lastAttemptAtEpochMillis?.let(Instant::ofEpochMilli),
    lastSuccessAt = lastSuccessAtEpochMillis?.let(Instant::ofEpochMilli),
    lastErrorType = lastErrorType,
    lastErrorMessage = lastErrorMessage,
    httpStatus = httpStatus,
    parsedCount = parsedCount,
    visibleGermanCount = visibleGermanCount,
    matchedCount = matchedCount,
    ambiguousCount = ambiguousCount,
    unmatchedCount = unmatchedCount,
    activeSnapshotId = activeSnapshotId,
    rangeStart = rangeStartEpochDay?.let(LocalDate::ofEpochDay),
    rangeEnd = rangeEndEpochDay?.let(LocalDate::ofEpochDay),
    parserVersion = parserVersion,
    matcherVersion = matcherVersion
)

internal fun emptySyncStateEntity() = AniWorldSyncStateEntity(
    parserVersion = PARSER_VERSION,
    matcherVersion = MATCHER_VERSION
)

internal fun AniWorldMediaMappingEntity.matchStatus(): MatchStatus = MatchStatus.valueOf(status)

private const val MARKER_SEPARATOR = "\u001f"
