package com.anisync.android.domain

import kotlinx.serialization.Serializable

/**
 * Opening and ending themes for a title, sourced from AnimeThemes.
 *
 * The shape mirrors the API's three levels rather than flattening them, because the
 * middle one carries real information the UI needs:
 *
 * - [MediaTheme] is the slot a show credits, "OP1" or "ED2".
 * - [ThemeVersion] is one cut of it, with its own episode range. A theme that changes
 *   part way through a run is two versions, not two themes.
 * - [ThemeVideo] is a file, with several usually available per version (creditless or
 *   not, disc or broadcast, different resolutions).
 */
@Serializable
data class MediaTheme(
    val id: Int,
    val type: ThemeType,
    val sequence: Int?,
    val slug: String,
    val songTitle: String?,
    val artists: List<String>,
    val versions: List<ThemeVersion>,
    /** AnimeThemes' own name for a variant slot, "English Version" or "TV Version". */
    val groupName: String? = null,
    val performers: List<ThemePerformer> = emptyList()
) {
    /** Every episode this theme plays over, across all of its versions. */
    val episodeSpans: List<EpisodeSpan>
        get() = mergeEpisodeSpans(versions.flatMap { it.episodeSpans })

    /** True when no version carries usable episode information. */
    val hasEpisodeData: Boolean
        get() = episodeSpans.isNotEmpty()

    /** True when any version is flagged as spoiling its episode placement. */
    val isSpoiler: Boolean
        get() = versions.any { it.spoiler }

    val isAdult: Boolean
        get() = versions.any { it.nsfw }

    /** False for a song AnimeThemes lists without any file, which is most insert songs. */
    val isPlayable: Boolean
        get() = versions.any { it.videos.isNotEmpty() }

    /** The video shown first: creditless and highest resolution when one exists. */
    val preferredVideo: ThemeVideo?
        get() = versions.firstOrNull()?.preferredVideo

    /**
     * What the slug carries beyond the plain "OP1" form, "EN" for a dub or "EN4Kids" for an
     * edit. One Piece lists OP1 and OP1-EN as separate themes with the same number, so dropping
     * this would leave two rows both calling themselves Opening 1.
     */
    val qualifier: String?
        get() = slug.removePrefix(type.name)
            .dropWhile { it.isDigit() }
            .trim('-', ' ')
            .takeIf { it.isNotEmpty() }

    /** The variant label worth a pill: the group's readable name, or the bare slug suffix. */
    val variantLabel: String?
        get() = groupName ?: qualifier

    /**
     * True when the credits say more than the artist line already does: a band's members, or a
     * singer credited as the character they voice.
     */
    val hasPerformerDetail: Boolean
        get() = performers.any { it.character != null || it.band != null }
}

/**
 * One credited voice on a song. A band is credited once per member, so [band] names the act and
 * [name] the member. [character] is the role sung as, for a character song.
 */
@Serializable
data class ThemePerformer(
    val name: String,
    val character: String? = null,
    val band: String? = null
)

/**
 * A whole lookup: the themes plus the AnimeThemes slug they came from, which is what the
 * "open on AnimeThemes" links are built out of.
 */
@Serializable
data class MediaThemes(
    val animeSlug: String? = null,
    val themes: List<MediaTheme> = emptyList()
)

@Serializable
enum class ThemeType { OP, ED, IN }

@Serializable
data class ThemeVersion(
    val id: Int,
    val version: Int,
    val rawEpisodes: String?,
    val notes: String?,
    val spoiler: Boolean,
    val nsfw: Boolean,
    val videos: List<ThemeVideo>,
    /** The song without the picture, a few megabytes against tens for the video. */
    val audio: ThemeAudio? = null
) {
    val episodeSpans: List<EpisodeSpan>
        get() = parseEpisodeSpans(rawEpisodes)

    val preferredVideo: ThemeVideo?
        get() = videos.minWithOrNull(
            compareByDescending<ThemeVideo> { it.creditless }
                .thenBy { it.overlap != VideoOverlap.None }
                .thenByDescending { it.resolution ?: 0 }
                .thenBy { it.lyrics }
                .thenBy { it.subbed }
        )
}

@Serializable
data class ThemeVideo(
    val id: Int,
    val url: String,
    val resolution: Int?,
    val creditless: Boolean,
    val subbed: Boolean,
    val lyrics: Boolean,
    val source: String?,
    val overlap: VideoOverlap = VideoOverlap.None
)

/**
 * Whether a video runs into episode footage. A transition cuts into or out of the episode, an
 * overlap plays over it, so neither is the clean cut of the song.
 */
@Serializable
enum class VideoOverlap { None, Transition, Over }

@Serializable
data class ThemeAudio(
    val url: String,
    val sizeBytes: Long?
)

/**
 * A run of episodes. [end] is null for a range the API left open, written "13-",
 * which means the theme was still playing when the entry was recorded.
 */
@Serializable
data class EpisodeSpan(val start: Int, val end: Int?) {
    val isOpen: Boolean get() = end == null

    fun contains(episode: Int): Boolean = episode >= start && (end == null || episode <= end)

    /** Length in episodes, needing [totalEpisodes] to close an open range. */
    fun length(totalEpisodes: Int?): Int {
        val last = end ?: totalEpisodes ?: return 0
        return (last - start + 1).coerceAtLeast(0)
    }
}

/**
 * Reads AnimeThemes' `episodes` field, which is free text rather than numbers:
 * "1-13", "14-22, 24", "5", "13-" for an open range, and null or "None" when the
 * range was never recorded. Anything unparseable is dropped rather than guessed at.
 */
fun parseEpisodeSpans(raw: String?): List<EpisodeSpan> {
    if (raw.isNullOrBlank() || raw.equals("None", ignoreCase = true)) return emptyList()

    val spans = raw.split(',').mapNotNull { part ->
        val piece = part.trim()
        if (piece.isEmpty()) return@mapNotNull null
        val dash = piece.indexOf('-')
        when {
            dash < 0 -> piece.toIntOrNull()?.let { EpisodeSpan(it, it) }
            dash == piece.lastIndex -> piece.dropLast(1).trim().toIntOrNull()?.let { EpisodeSpan(it, null) }
            else -> {
                val start = piece.substring(0, dash).trim().toIntOrNull()
                val end = piece.substring(dash + 1).trim().toIntOrNull()
                if (start != null && end != null && end >= start) EpisodeSpan(start, end) else null
            }
        }
    }
    return mergeEpisodeSpans(spans)
}

/** Sorts spans and joins the ones that touch, so a bar never draws the same episode twice. */
fun mergeEpisodeSpans(spans: List<EpisodeSpan>): List<EpisodeSpan> {
    if (spans.size < 2) return spans
    val sorted = spans.sortedWith(compareBy({ it.start }, { it.end ?: Int.MAX_VALUE }))
    val merged = mutableListOf(sorted.first())
    for (span in sorted.drop(1)) {
        val last = merged.last()
        if (last.end == null) break
        if (span.start <= last.end + 1) {
            merged[merged.lastIndex] = EpisodeSpan(
                start = last.start,
                end = if (span.end == null) null else maxOf(last.end, span.end)
            )
        } else {
            merged.add(span)
        }
    }
    return merged
}

/** Renders spans the way the API writes them, "14–22, 24", with an en dash. */
fun formatEpisodeSpans(spans: List<EpisodeSpan>): String = spans.joinToString(", ") { span ->
    when {
        span.end == null -> "${span.start}+"
        span.end == span.start -> "${span.start}"
        else -> "${span.start}–${span.end}"
    }
}

/** How many episodes the spans cover, capped at [totalEpisodes] when it is known. */
fun countCoveredEpisodes(spans: List<EpisodeSpan>, totalEpisodes: Int?): Int {
    val covered = spans.sumOf { it.length(totalEpisodes) }
    return if (totalEpisodes != null) covered.coerceAtMost(totalEpisodes) else covered
}
