package de.mrxxxxx.anisyncplus.calendar.matching

import de.mrxxxxx.anisyncplus.calendar.api.AniWorldTitleMatcher
import de.mrxxxxx.anisyncplus.calendar.domain.AniListMatchCandidate
import de.mrxxxxx.anisyncplus.calendar.domain.AniWorldRelease
import de.mrxxxxx.anisyncplus.calendar.domain.MATCHER_VERSION
import de.mrxxxxx.anisyncplus.calendar.domain.MatchStatus
import de.mrxxxxx.anisyncplus.calendar.domain.TitleMatchDecision
import de.mrxxxxx.anisyncplus.calendar.parser.TitleNormalizer
import javax.inject.Inject
import kotlin.math.max

class DefaultAniWorldTitleMatcher @Inject constructor() : AniWorldTitleMatcher {
    override fun match(
        release: AniWorldRelease,
        candidates: List<AniListMatchCandidate>
    ): TitleMatchDecision {
        val distinctCandidates = candidates.distinctBy(AniListMatchCandidate::mediaId)
        if (distinctCandidates.isEmpty()) return decision(MatchStatus.UNMATCHED, null, 0, null, null, "no_candidates")

        val source = release.normalizedTitle
        val exact = distinctCandidates.filter { candidate ->
            candidate.titleVariants.any { TitleNormalizer.normalize(it) == source }
        }
        val seasonExact = release.seasonNumber?.let { season -> exact.filter { it.seasonNumber == season } }.orEmpty()
        val resolvedExact = when {
            exact.size == 1 -> exact.single()
            seasonExact.size == 1 -> seasonExact.single()
            else -> null
        }
        if (resolvedExact != null) {
            return decision(
                MatchStatus.MATCHED,
                resolvedExact,
                distinctCandidates.size,
                1.0,
                if (exact.size > 1) 1.0 else secondScore(source, distinctCandidates, resolvedExact, release.seasonNumber),
                if (exact.size > 1) "exact_title_and_season" else "exact_normalized_title"
            )
        }
        if (exact.size > 1) {
            return decision(MatchStatus.AMBIGUOUS, null, distinctCandidates.size, 1.0, 1.0, "multiple_exact_titles")
        }

        val ranked = distinctCandidates.map { candidate ->
            candidate to candidateScore(source, candidate, release.seasonNumber)
        }.sortedWith(compareByDescending<Pair<AniListMatchCandidate, Double>> { it.second }.thenBy { it.first.mediaId })
        val best = ranked.first()
        val second = ranked.getOrNull(1)?.second
        val margin = second?.let { best.second - it } ?: best.second
        return when {
            best.second >= AUTO_MATCH_THRESHOLD && margin >= MINIMUM_MARGIN -> decision(
                MatchStatus.MATCHED,
                best.first,
                distinctCandidates.size,
                best.second,
                second,
                "unique_similarity"
            )
            best.second >= AMBIGUOUS_THRESHOLD -> decision(
                MatchStatus.AMBIGUOUS,
                null,
                distinctCandidates.size,
                best.second,
                second,
                "similarity_not_decisive"
            )
            else -> decision(
                MatchStatus.UNMATCHED,
                null,
                distinctCandidates.size,
                best.second,
                second,
                "below_similarity_threshold"
            )
        }
    }

    private fun secondScore(
        source: String,
        candidates: List<AniListMatchCandidate>,
        selected: AniListMatchCandidate,
        seasonNumber: Int?
    ): Double? = candidates.filterNot { it.mediaId == selected.mediaId }
        .maxOfOrNull { candidateScore(source, it, seasonNumber) }

    private fun candidateScore(
        source: String,
        candidate: AniListMatchCandidate,
        seasonNumber: Int?
    ): Double {
        val titleScore = candidate.titleVariants.maxOfOrNull { similarity(source, TitleNormalizer.normalize(it)) } ?: 0.0
        val seasonAdjusted = when {
            seasonNumber == null || candidate.seasonNumber == null -> titleScore
            seasonNumber == candidate.seasonNumber -> (titleScore + SEASON_BONUS).coerceAtMost(1.0)
            else -> (titleScore - SEASON_PENALTY).coerceAtLeast(0.0)
        }
        return seasonAdjusted
    }

    internal fun similarity(left: String, right: String): Double {
        if (left == right) return 1.0
        if (left.isEmpty() || right.isEmpty()) return 0.0
        val previous = IntArray(right.length + 1) { it }
        val current = IntArray(right.length + 1)
        left.forEachIndexed { leftIndex, leftCharacter ->
            current[0] = leftIndex + 1
            right.forEachIndexed { rightIndex, rightCharacter ->
                current[rightIndex + 1] = minOf(
                    current[rightIndex] + 1,
                    previous[rightIndex + 1] + 1,
                    previous[rightIndex] + if (leftCharacter == rightCharacter) 0 else 1
                )
            }
            current.copyInto(previous)
        }
        return 1.0 - previous[right.length].toDouble() / max(left.length, right.length).toDouble()
    }

    private fun decision(
        status: MatchStatus,
        candidate: AniListMatchCandidate?,
        candidateCount: Int,
        bestScore: Double?,
        secondBestScore: Double?,
        reason: String
    ) = TitleMatchDecision(
        status = status,
        candidate = candidate,
        candidateCount = candidateCount,
        bestScore = bestScore,
        secondBestScore = secondBestScore,
        scoreMargin = if (bestScore != null) bestScore - (secondBestScore ?: 0.0) else null,
        reason = reason,
        matcherVersion = MATCHER_VERSION
    )

    private companion object {
        const val AUTO_MATCH_THRESHOLD = 0.92
        const val AMBIGUOUS_THRESHOLD = 0.85
        const val MINIMUM_MARGIN = 0.08
        const val SEASON_BONUS = 0.02
        const val SEASON_PENALTY = 0.12
    }
}
