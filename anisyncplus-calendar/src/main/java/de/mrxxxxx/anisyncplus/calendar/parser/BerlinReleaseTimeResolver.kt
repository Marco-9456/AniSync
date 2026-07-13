package de.mrxxxxx.anisyncplus.calendar.parser

import de.mrxxxxx.anisyncplus.calendar.domain.ReleaseDiagnosticStatus
import de.mrxxxxx.anisyncplus.calendar.domain.SOURCE_ZONE_ID
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

internal data class ResolvedReleaseTime(
    val instant: Instant?,
    val diagnosticStatus: ReleaseDiagnosticStatus
)

internal object BerlinReleaseTimeResolver {
    val zoneId: ZoneId = ZoneId.of(SOURCE_ZONE_ID)

    fun resolve(date: LocalDate, time: LocalTime?): ResolvedReleaseTime {
        if (time == null) return ResolvedReleaseTime(null, ReleaseDiagnosticStatus.MISSING_TIME)
        val localDateTime = LocalDateTime.of(date, time)
        val offsets = zoneId.rules.getValidOffsets(localDateTime)
        return when (offsets.size) {
            0 -> ResolvedReleaseTime(null, ReleaseDiagnosticStatus.DST_GAP)
            1 -> ResolvedReleaseTime(
                ZonedDateTime.ofLocal(localDateTime, zoneId, offsets.single()).toInstant(),
                ReleaseDiagnosticStatus.VALID
            )
            else -> ResolvedReleaseTime(
                ZonedDateTime.ofLocal(localDateTime, zoneId, offsets.first()).toInstant(),
                ReleaseDiagnosticStatus.DST_OVERLAP_EARLIER_OFFSET
            )
        }
    }
}
