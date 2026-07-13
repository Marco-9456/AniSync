package de.mrxxxxx.anisyncplus.calendar.parser

import de.mrxxxxx.anisyncplus.calendar.api.AniWorldCalendarParser
import de.mrxxxxx.anisyncplus.calendar.domain.AniWorldParsedDocument
import de.mrxxxxx.anisyncplus.calendar.domain.AniWorldRelease
import de.mrxxxxx.anisyncplus.calendar.domain.AniWorldReleaseLanguage
import de.mrxxxxx.anisyncplus.calendar.domain.PARSER_VERSION
import de.mrxxxxx.anisyncplus.calendar.domain.ReleaseDiagnosticStatus
import de.mrxxxxx.anisyncplus.calendar.domain.ReleaseKind
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URI
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class JsoupAniWorldCalendarParser @Inject constructor() : AniWorldCalendarParser {
    override fun parse(html: String, fetchedAt: Instant): AniWorldParsedDocument {
        val document = Jsoup.parse(html)
        val challengeText = (document.title() + " " + document.body()?.text().orEmpty()).lowercase()
        if (BLOCK_MARKERS.any(challengeText::contains)) throw AniWorldParseException.BlockPage()

        val container = document.selectFirst("#seriesContainer") ?: throw AniWorldParseException.MissingContainer()
        val sections = container.select("section.calendarList")
        if (sections.isEmpty()) throw AniWorldParseException.MissingDaySections()

        val documentHash = sha256(html)
        val snapshotId = "aw-${documentHash.take(24)}"
        var sourceOrder = 0
        val rawReleases = mutableListOf<AniWorldRelease>()
        val dates = sections.map { section ->
            val heading = section.children().firstOrNull { it.tagName() == "h3" }?.text()
                ?: section.selectFirst("h3")?.text().orEmpty()
            val dateToken = DATE_REGEX.find(heading)?.value ?: throw AniWorldParseException.InvalidDate(heading)
            val date = runCatching { LocalDate.parse(dateToken, DATE_FORMATTER) }
                .getOrElse { throw AniWorldParseException.InvalidDate(dateToken) }

            section.select("h3.seriesTitle").forEach { titleElement ->
                rawReleases += parseCard(titleElement, date, snapshotId, sourceOrder++)
            }
            date
        }

        val consolidated = consolidate(rawReleases)
        return AniWorldParsedDocument(
            snapshotId = snapshotId,
            fetchedAt = fetchedAt,
            rangeStart = dates.minOrNull()!!,
            rangeEnd = dates.maxOrNull()!!,
            documentSha256 = documentHash,
            parserVersion = PARSER_VERSION,
            daySectionCount = sections.size,
            releases = consolidated
        )
    }

    private fun parseCard(
        titleElement: Element,
        sourceDate: LocalDate,
        snapshotId: String,
        sourceOrder: Int
    ): AniWorldRelease {
        val card = titleElement.closest("[data-calendar-entry]")
            ?: titleElement.closest(".col-md-15, .col-sm-3, .col-xs-6")
            ?: titleElement.parent()
            ?: titleElement
        val anchor = card.selectFirst("a[href]")
        val sourceUrl = anchor?.attr("href")?.takeIf(String::isNotBlank)
        val sourceSlug = sourceUrl?.let(::extractSlug)
        val rawTitle = titleElement.ownText().ifBlank { titleElement.text() }.trim()
        val normalizedTitle = TitleNormalizer.normalize(rawTitle)
        val sourceSeriesKey = sourceSlug?.let { "slug:$it" } ?: "title:$normalizedTitle"
        val smalls = card.select("small")
        val installmentToken = smalls.firstOrNull()?.ownText()?.trim()?.takeIf(String::isNotBlank)
        val installment = parseInstallment(installmentToken)
        val timeText = smalls.drop(1).firstOrNull()?.text()?.trim().orEmpty()
        val parsedTime = parseTime(timeText)
        val resolved = BerlinReleaseTimeResolver.resolve(sourceDate, parsedTime.time)
        val markers = card.select("img.flag, small img").flatMap { image ->
            listOf(image.attr("data-src"), image.attr("src"), image.attr("alt"), image.attr("title"))
        }.filter(String::isNotBlank).toSet()
        val language = languageFor(markers)
        val diagnostic = when {
            parsedTime.invalid -> ReleaseDiagnosticStatus.INVALID_TIME
            resolved.diagnosticStatus != ReleaseDiagnosticStatus.VALID -> resolved.diagnosticStatus
            installment.kind == ReleaseKind.UNKNOWN && installmentToken == null -> ReleaseDiagnosticStatus.MISSING_INSTALLMENT
            language == AniWorldReleaseLanguage.UNKNOWN -> ReleaseDiagnosticStatus.UNKNOWN_LANGUAGE
            else -> ReleaseDiagnosticStatus.VALID
        }
        val identity = listOf(
            sourceSeriesKey,
            sourceDate,
            parsedTime.time,
            installment.kind,
            installment.season,
            installment.episode,
            installment.installment,
            language
        ).joinToString("|")

        return AniWorldRelease(
            localId = "release-${sha256(identity).take(32)}",
            snapshotId = snapshotId,
            sourceSeriesKey = sourceSeriesKey,
            sourceSlug = sourceSlug,
            sourceUrl = sourceUrl,
            rawTitle = rawTitle,
            normalizedTitle = normalizedTitle,
            sourceDate = sourceDate,
            sourceLocalTime = parsedTime.time,
            sourceZoneId = BerlinReleaseTimeResolver.zoneId,
            resolvedInstant = resolved.instant,
            isApproximate = parsedTime.approximate,
            releaseKind = installment.kind,
            seasonNumber = installment.season,
            episodeNumber = installment.episode,
            installmentNumber = installment.installment,
            rawInstallmentToken = installmentToken,
            language = language,
            rawLanguageMarkers = markers,
            sourceOrder = sourceOrder,
            diagnosticStatus = diagnostic
        )
    }

    private fun consolidate(releases: List<AniWorldRelease>): List<AniWorldRelease> {
        val exactDeduplicated = releases.distinctBy { release ->
            listOf(
                release.sourceSeriesKey,
                release.sourceDate,
                release.sourceLocalTime,
                release.releaseKind,
                release.seasonNumber,
                release.episodeNumber,
                release.installmentNumber,
                release.language
            )
        }
        val groups = exactDeduplicated.groupBy { release ->
            listOf(
                release.sourceSeriesKey,
                release.sourceDate,
                release.sourceLocalTime,
                release.releaseKind,
                release.seasonNumber,
                release.episodeNumber,
                release.installmentNumber
            )
        }
        return groups.values.flatMap { group ->
            val sub = group.firstOrNull { it.language == AniWorldReleaseLanguage.DE_SUB }
            val dub = group.firstOrNull { it.language == AniWorldReleaseLanguage.DE_DUB }
            if (sub != null && dub != null) {
                val mergedIdentity = sub.localId + "|" + dub.localId
                listOf(
                    sub.copy(
                        localId = "release-${sha256(mergedIdentity).take(32)}",
                        language = AniWorldReleaseLanguage.DE_SUB_AND_DUB,
                        rawLanguageMarkers = sub.rawLanguageMarkers + dub.rawLanguageMarkers,
                        sourceOrder = minOf(sub.sourceOrder, dub.sourceOrder)
                    )
                ) + group.filterNot { it === sub || it === dub }
            } else {
                group
            }
        }.sortedBy(AniWorldRelease::sourceOrder)
    }

    private fun parseTime(value: String): ParsedTime {
        if (value.isBlank()) return ParsedTime(null, approximate = false, invalid = false)
        val match = TIME_REGEX.find(value)
        if (match == null) {
            if (value.contains("Uhr", ignoreCase = true)) throw AniWorldParseException.InvalidTime(value)
            return ParsedTime(null, approximate = false, invalid = false)
        }
        val time = runCatching {
            LocalTime.of(match.groupValues[2].toInt(), match.groupValues[3].toInt())
        }.getOrElse { throw AniWorldParseException.InvalidTime(value) }
        return ParsedTime(time, match.groupValues[1].isNotBlank(), invalid = false)
    }

    private fun parseInstallment(token: String?): Installment {
        if (token == null) return Installment(ReleaseKind.UNKNOWN, null, null, null)
        EPISODE_REGEX.find(token)?.let {
            return Installment(ReleaseKind.EPISODE, it.groupValues[1].toInt(), it.groupValues[2].toInt(), null)
        }
        FILM_REGEX.find(token)?.let {
            return Installment(ReleaseKind.FILM, null, null, it.groupValues[1].toIntOrNull())
        }
        SPECIAL_REGEX.find(token)?.let {
            return Installment(ReleaseKind.SPECIAL, null, null, it.groupValues[1].toIntOrNull())
        }
        return Installment(ReleaseKind.UNKNOWN, null, null, null)
    }

    private fun languageFor(markers: Set<String>): AniWorldReleaseLanguage {
        val joined = markers.joinToString(" ").lowercase()
        return when {
            "japanese-german" in joined || "deutscher untertitel" in joined || "german subtitle" in joined -> AniWorldReleaseLanguage.DE_SUB
            "/german.svg" in joined || "deutsche flagge" in joined || "auf deutsch" in joined -> AniWorldReleaseLanguage.DE_DUB
            "japanese-english" in joined || "englischer untertitel" in joined || "english subtitle" in joined -> AniWorldReleaseLanguage.EN_SUB
            else -> AniWorldReleaseLanguage.UNKNOWN
        }
    }

    private fun extractSlug(url: String): String? = runCatching {
        val path = URI(url).path ?: url
        val marker = "/anime/stream/"
        path.substringAfter(marker, "").substringBefore('/').takeIf(String::isNotBlank)
    }.getOrNull()

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }

    private data class ParsedTime(val time: LocalTime?, val approximate: Boolean, val invalid: Boolean)
    private data class Installment(
        val kind: ReleaseKind,
        val season: Int?,
        val episode: Int?,
        val installment: Int?
    )

    private companion object {
        val DATE_REGEX = Regex("\\b\\d{2}\\.\\d{2}\\.\\d{4}\\b")
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu")
        val TIME_REGEX = Regex("(~)?\\s*(\\d{1,2}):(\\d{2})\\s*Uhr", RegexOption.IGNORE_CASE)
        val EPISODE_REGEX = Regex("S(\\d{1,2})E(\\d{1,4})", RegexOption.IGNORE_CASE)
        val FILM_REGEX = Regex("Film(?:\\s*(\\d+))?", RegexOption.IGNORE_CASE)
        val SPECIAL_REGEX = Regex("Special(?:\\s*(\\d+))?", RegexOption.IGNORE_CASE)
        val BLOCK_MARKERS = listOf("just a moment", "cf-chl-", "cloudflare", "access denied", "captcha")
    }
}
