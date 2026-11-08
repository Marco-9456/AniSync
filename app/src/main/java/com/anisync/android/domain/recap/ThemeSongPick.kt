package com.anisync.android.domain.recap

import com.anisync.android.domain.MediaTheme
import com.anisync.android.domain.StoredRecap
import com.anisync.android.domain.ThemeAudio
import com.anisync.android.domain.ThemeType
import com.anisync.android.domain.ThemeVersion

/** One of the period's top titles, as the theme song pick sees it. */
data class ThemeSongCandidate(
    val mediaId: Int,
    val isFilm: Boolean,
    /** The episodes watched in the period, from the first to the last. */
    val watched: IntRange?,
)

data class ThemeSongChoice(
    val mediaId: Int,
    val theme: MediaTheme,
    val version: ThemeVersion,
    val audio: ThemeAudio,
)

/** How many of the period's top titles the song is picked from. */
private const val SONG_CANDIDATES = 5

/** The titles a recap's song is picked from: its top five, in ranked order. */
fun StoredRecap.songCandidates(): List<ThemeSongCandidate> =
    recap.ranked.take(SONG_CANDIDATES).map { ranked ->
        ThemeSongCandidate(ranked.mediaId, media[ranked.mediaId]?.isFilm == true, ranked.watched)
    }

/**
 * The song a recap plays: the top title's opening that covers the episodes watched (BLEACH plays
 * OP1 for episodes 1 to 25 and OP2 from 26). A film or a title without an opening plays its first
 * ending. A title AnimeThemes does not know, or one without audio, passes to the next candidate, and
 * with no match the story stays silent.
 *
 * [themesFor] returns null or an empty list for a title AnimeThemes does not list.
 */
fun pickThemeSong(
    candidates: List<ThemeSongCandidate>,
    themesFor: (mediaId: Int) -> List<MediaTheme>?,
): ThemeSongChoice? {
    for (candidate in candidates) {
        val themes = themesFor(candidate.mediaId).orEmpty().filter { it.playableVersions().isNotEmpty() }
        val openings = themes.filter { it.type == ThemeType.OP }
        val endings = themes.filter { it.type == ThemeType.ED }
        val theme = if (candidate.isFilm || openings.isEmpty()) {
            firstOf(endings)
        } else {
            openingFor(openings, candidate.watched) ?: firstOf(endings)
        } ?: continue
        val version = theme.versionFor(candidate.watched)
        return ThemeSongChoice(candidate.mediaId, theme, version, version.audio!!)
    }
    return null
}

/**
 * Cuts with audio. AnimeThemes' spoiler and NSFW flags describe the video, and a recap plays only
 * the song of a title the user has watched, so neither flag rules a cut out.
 */
private fun MediaTheme.playableVersions(): List<ThemeVersion> = versions.filter { it.audio != null }

/** The plain cut before a dub or an edit of the same slot, then by number. */
private val themeOrder = compareBy<MediaTheme>(
    { if (it.qualifier == null && it.groupName == null) 0 else 1 },
    { it.sequence ?: 1 },
)

private fun firstOf(themes: List<MediaTheme>): MediaTheme? = themes.minWithOrNull(themeOrder)

private fun openingFor(openings: List<MediaTheme>, watched: IntRange?): MediaTheme? {
    val ordered = openings.sortedWith(themeOrder)
    if (watched == null || ordered.none { it.hasEpisodeData }) return ordered.first()
    val covering = ordered.maxWithOrNull(compareBy<MediaTheme> { it.overlap(watched) }.thenByDescending { ordered.indexOf(it) })
    if (covering != null && covering.overlap(watched) > 0) return covering
    // Nothing covers the episodes, as when AnimeThemes lacks later openings: take the latest that started before them.
    return ordered.filter { theme -> theme.episodeSpans.any { it.start <= watched.first } }
        .maxByOrNull { theme -> theme.episodeSpans.filter { it.start <= watched.first }.maxOf { it.start } }
        ?: ordered.first()
}

private fun MediaTheme.overlap(watched: IntRange): Int =
    episodeSpans.sumOf { span ->
        val end = span.end ?: watched.last
        (minOf(end, watched.last) - maxOf(span.start, watched.first) + 1).coerceAtLeast(0)
    }

/** The cut covering [watched] when one does, otherwise the first cut with audio. */
private fun MediaTheme.versionFor(watched: IntRange?): ThemeVersion {
    val versions = playableVersions()
    return watched?.let { range ->
        versions.firstOrNull { version ->
            version.episodeSpans.any { it.start <= range.last && (it.end ?: range.last) >= range.first }
        }
    } ?: versions.first()
}
