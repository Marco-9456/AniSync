package com.anisync.android.presentation.recap

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.anisync.android.domain.ThemeType
import com.anisync.android.domain.recap.RankNote
import com.anisync.android.domain.recap.Recap
import com.anisync.android.domain.recap.RecapPage
import com.anisync.android.domain.recap.RecapPeriod
import com.anisync.android.presentation.share.cards.ShareSubject

sealed interface RecapStoryAction {
    data object Next : RecapStoryAction
    data object Previous : RecapStoryAction
    data class GoTo(val page: Int) : RecapStoryAction
    data object TogglePause : RecapStoryAction

    /** A finger held on the page pauses the story until it lifts. */
    data class Hold(val held: Boolean) : RecapStoryAction
    data object ToggleMute : RecapStoryAction
    data object Replay : RecapStoryAction
    data object Retry : RecapStoryAction

    /** The card page's link to the profile's Statistics. */
    data object SeeStatistics : RecapStoryAction

    /** The app went to the background or came back, the song follows it. */
    data class Visible(val visible: Boolean) : RecapStoryAction
}

/** One title as the story prints it. */
@Immutable
data class StoryTitle(
    val id: Int,
    val title: String,
    val coverUrl: String?,
    val color: Color?,
    val format: String?,
    val year: Int?,
    val studio: String?,
    val isFilm: Boolean,
    val isShort: Boolean,
)

/** Everything the pages print, worked out once when the story opens. */
@Immutable
data class StoryModel(
    val period: RecapPeriod,
    val recap: Recap,
    val userName: String,
    val avatarUrl: String?,
    val titles: Map<Int, StoryTitle>,
    val pages: List<RecapPage>,
    /** Each page's seed, null where the page wears the app's own palette. */
    val seeds: List<Color?>,
    /** Last month's episodes for the comparison, null when there is nothing to compare with. */
    val previousEpisodes: Int?,
    /** What the top five's footnote says. */
    val rankNote: RankNote,
    val card: ShareSubject.Recap,
    /** The top title's AniList colour, for the studio's chrome. */
    val coverColor: String?,
    val isPreview: Boolean,
) {
    val isYear: Boolean get() = period is RecapPeriod.Year

    fun title(id: Int?): StoryTitle? = id?.let(titles::get)

    /** The covers in ranked order, for the collages. */
    val rankedCovers: List<String?> get() = recap.ranked.map { titles[it.mediaId]?.coverUrl }
}

/** The song the story plays. [sourceRank] is where its title sits in the ranking, 0 for the top title. */
@Immutable
data class StorySong(
    val title: String,
    val artist: String?,
    val type: ThemeType,
    val sourceRank: Int,
    val sourceTitle: String,
    val muted: Boolean,
)

@Immutable
data class RecapStoryUiState(
    val loading: Boolean = true,
    /** Nothing could be loaded and a retry may help. */
    val failed: Boolean = false,
    /** The failure was AniList's rate limit outlasting every wait, not the connection. */
    val rateLimited: Boolean = false,
    /** Nothing counts in the period, so there is no story to tell. */
    val empty: Boolean = false,
    val story: StoryModel? = null,
    val page: Int = 0,
    /** Paused from the header, until the viewer plays again. */
    val paused: Boolean = false,
    /** Paused while a finger rests on the page. */
    val held: Boolean = false,
    val song: StorySong? = null,
    /** A song is on its way and has not played yet, so the opener waits for it. */
    val songPending: Boolean = false,
    /** Bumped by Replay, so the page timer starts over even on the same page. */
    val replays: Int = 0,
) {
    val isStopped: Boolean get() = paused || held
}
