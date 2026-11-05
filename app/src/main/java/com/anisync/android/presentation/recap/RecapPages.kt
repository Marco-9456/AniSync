package com.anisync.android.presentation.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.transformations
import com.anisync.android.R
import com.anisync.android.domain.ThemeType
import com.anisync.android.domain.recap.PeriodChange
import com.anisync.android.domain.recap.RankNote
import com.anisync.android.domain.recap.RecapPage
import com.anisync.android.domain.recap.RecapPeriod
import com.anisync.android.domain.recap.busiestMonth
import com.anisync.android.domain.recap.comparePeriods
import com.anisync.android.domain.recap.moreFinished
import com.anisync.android.presentation.share.cards.ArtworkBlur
import com.anisync.android.presentation.share.cards.CardPicture
import com.anisync.android.presentation.share.cards.cardText
import com.anisync.android.presentation.share.cards.cardUpper
import com.anisync.android.presentation.share.cards.fitted
import com.anisync.android.presentation.share.cards.formatLabel
import com.anisync.android.presentation.statistics.fittedStyle
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.ui.theme.LocalExpressiveTypography
import com.anisync.android.ui.theme.genreColor
import com.anisync.android.ui.theme.lineBox
import java.time.DayOfWeek
import java.time.Month
import java.time.temporal.WeekFields

/** The frames' covers cast their shadow 14dp down with a 30dp blur at 45% black. */
private val CoverShadow = Shadow(radius = 30.dp, color = Color.Black.copy(alpha = 0.45f), offset = DpOffset(0.dp, 14.dp))

/** What the page buttons do, the card page's own buttons included. */
internal class PageActions(
    val onStart: () -> Unit,
    val onCustomize: () -> Unit,
    val onSaveCard: () -> Unit,
    val onShareCard: () -> Unit,
    val onSeeStatistics: () -> Unit,
    val onReplay: () -> Unit,
)

/** The page's own content, laid out on the 448 by 997 frame the story is drawn at. */
@Composable
internal fun RecapPageContent(
    page: RecapPage,
    story: StoryModel,
    song: StorySong?,
    actions: PageActions,
    card: @Composable () -> Unit,
) {
    when (page) {
        RecapPage.OPENER -> MonthOpener(story, song, actions)
        RecapPage.TIME -> MonthTime(story)
        RecapPage.MOST_WATCHED -> MostWatched(story)
        RecapPage.TOP_FIVE -> MonthTopFive(story)
        RecapPage.MOOD -> Mood(story)
        RecapPage.FINISHED -> Finished(story)
        RecapPage.RHYTHM -> Rhythm(story)
        RecapPage.MONTH_CARD, RecapPage.YEAR_CARD -> CardPage(story, actions, card)
        RecapPage.YEAR_OPENER -> YearOpener(story, song, actions)
        RecapPage.YEAR_NUMBERS -> YearNumbers(story)
        RecapPage.YEAR_TOP_FIVE -> YearTopFive(story)
        RecapPage.YEAR_TASTE -> YearTaste(story)
        RecapPage.YEAR_HABITS -> YearHabits(story)
    }
}

/** The page's background: its scheme's background, or its primary container on the two number pages. */
@Composable
internal fun RecapPageBackground(page: RecapPage, story: StoryModel) {
    val scheme = MaterialTheme.colorScheme
    when (page) {
        RecapPage.TIME, RecapPage.YEAR_NUMBERS -> Box(Modifier.fillMaxSize().background(scheme.primaryContainer))
        RecapPage.MOST_WATCHED -> ArtworkBackdrop(story.rankedCovers.firstOrNull(), Modifier.fillMaxSize(), Backdrop.PAGE)
        RecapPage.YEAR_TOP_FIVE -> ArtworkBackdrop(story.rankedCovers.firstOrNull(), Modifier.fillMaxSize(), Backdrop.TOP)
        else -> Box(Modifier.fillMaxSize().background(scheme.background))
    }
}

/** Where the blurred artwork sits: behind all of WM3, behind WY3's top, or behind WT1's whole stage. */
internal enum class Backdrop { PAGE, TOP, STAGE }

/** The top cover blurred behind a page, bleeding past its edges and fading into the background. */
@Composable
internal fun ArtworkBackdrop(url: String?, modifier: Modifier, kind: Backdrop) {
    val background = MaterialTheme.colorScheme.background
    val context = LocalPlatformContext.current
    BoxWithConstraints(modifier.background(background)) {
        if (url != null) {
            val request = remember(url, context) { ImageRequest.Builder(context).data(url).transformations(ArtworkBlur()).build() }
            val bleed = if (kind == Backdrop.TOP) 80.dp else 60.dp
            val width = when (kind) {
                Backdrop.PAGE -> 568.dp
                Backdrop.TOP -> 608.dp
                Backdrop.STAGE -> maxWidth + bleed * 2
            }
            val height = when (kind) {
                Backdrop.PAGE -> 1117.dp
                Backdrop.TOP -> 620.dp
                Backdrop.STAGE -> maxHeight + bleed * 2
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                alpha = if (kind == Backdrop.PAGE) 0.75f else 0.55f,
                modifier = Modifier
                    .offset(-bleed, -bleed)
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .size(width, height),
            )
        }
        when (kind) {
            Backdrop.PAGE -> Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to background.copy(alpha = 0.45f), 0.5f to background.copy(alpha = 0.55f), 1f to background.copy(alpha = 0.95f))
                )
            )
            Backdrop.TOP -> Box(Modifier.fillMaxWidth().height(600.dp).background(Brush.verticalGradient(0f to background.copy(alpha = 0.3f), 1f to background)))
            Backdrop.STAGE -> Unit
        }
    }
}

// --- Shared pieces ---

@Composable
private fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, align: TextAlign? = null) {
    FrameText(
        text = text.cardUpper(),
        style = cardText(12, 16, FontWeight.SemiBold, 1.6f),
        color = color,
        textAlign = align,
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
private fun PageTitle(text: String, modifier: Modifier = Modifier, size: Int = 34, line: Int = 40, color: Color = MaterialTheme.colorScheme.onSurface) {
    val style = LocalExpressiveTypography.current.titleWide.style(size.sp, line.sp, (-0.5).sp)
    FrameText(text = text, style = style, color = color, modifier = modifier)
}

private enum class StoryShape { COOKIE9, COOKIE12, SUNNY, FLOWER, SOFT_BURST, CLOVER, CIRCLE, PILL }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StoryShape.shape(): Shape = when (this) {
    StoryShape.COOKIE9 -> MaterialShapes.Cookie9Sided.fitted()
    StoryShape.COOKIE12 -> MaterialShapes.Cookie12Sided.fitted()
    StoryShape.SUNNY -> MaterialShapes.Sunny.fitted()
    StoryShape.FLOWER -> MaterialShapes.Flower.fitted()
    StoryShape.SOFT_BURST -> MaterialShapes.SoftBurst.fitted()
    StoryShape.CLOVER -> MaterialShapes.Clover8Leaf.fitted()
    StoryShape.CIRCLE -> CircleShape
    StoryShape.PILL -> RoundedCornerShape(percent = 50)
}

@Composable
private fun ShapedCover(url: String?, shape: Shape, modifier: Modifier, shadow: Boolean = true) {
    CardPicture(url = url, shape = shape, modifier = if (shadow) modifier.dropShadow(shape, CoverShadow) else modifier)
}

@Composable
private fun arch(width: Dp, bottom: Dp): Shape = RoundedCornerShape(topStart = width / 2, topEnd = width / 2, bottomStart = bottom, bottomEnd = bottom)

/** The play button an opener starts the story with, and the song beside it. */
@Composable
private fun StartRow(song: StorySong?, onStart: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(scheme.primary)
                .clickable(onClick = onStart)
                .padding(start = 18.dp, end = 22.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(22.dp))
            FrameText(stringResource(R.string.recap_start), style = cardText(16, 22, FontWeight.Bold), color = scheme.onPrimary)
        }
        if (song != null) {
            Spacer(Modifier.width(16.dp))
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                FrameText(song.title, style = cardText(14, 20, FontWeight.Bold), color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                FrameText(
                    text = songRole(song),
                    style = cardText(12, 16, FontWeight.Medium),
                    color = scheme.primary.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** "Hikaru Utada, ending of your top title". */
@Composable
internal fun songRole(song: StorySong): String {
    val role = if (song.sourceRank == 0) {
        stringResource(
            when (song.type) {
                ThemeType.OP -> R.string.recap_song_opening_top
                ThemeType.ED -> R.string.recap_song_ending_top
                ThemeType.IN -> R.string.recap_song_insert_top
            }
        )
    } else {
        stringResource(
            when (song.type) {
                ThemeType.OP -> R.string.recap_song_opening_of
                ThemeType.ED -> R.string.recap_song_ending_of
                ThemeType.IN -> R.string.recap_song_insert_of
            },
            song.sourceTitle,
        )
    }
    return song.artist?.let { stringResource(R.string.recap_song_by, it, role) } ?: role.capitalized(recapLocale())
}

/** A rounded row with an icon disc, the WM7 and WY5 insight rows. */
@Composable
private fun InsightRow(icon: ImageVector, title: String, subtitle: String) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.surfaceContainerLow)
            .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(scheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            FrameText(title, style = cardText(15, 20, FontWeight.Bold), color = scheme.onSurface)
            FrameText(subtitle, style = cardText(13, 18, FontWeight.Normal), color = scheme.onSurfaceVariant)
        }
    }
}

/** Covers for the openers' collages, the top title in the largest slot. */
private data class CollageSpot(val rank: Int, val shape: StoryShape?, val x: Dp, val y: Dp, val width: Dp, val height: Dp, val corner: Dp? = null)

private val MonthCollage = listOf(
    CollageSpot(0, StoryShape.COOKIE9, 108.dp, 140.dp, 246.dp, 243.dp),
    CollageSpot(1, null, 300.dp, 340.dp, 110.dp, 150.dp, corner = 24.dp),
    CollageSpot(2, StoryShape.PILL, 332.dp, 150.dp, 92.dp, 170.dp),
    CollageSpot(3, StoryShape.CIRCLE, 40.dp, 330.dp, 124.dp, 124.dp),
    CollageSpot(4, StoryShape.SUNNY, 36.dp, 150.dp, 94.5.dp, 94.5.dp),
)

private val YearCollage = listOf(
    CollageSpot(0, StoryShape.COOKIE12, 150.dp, 150.dp, 268.dp, 268.dp),
    CollageSpot(1, null, 24.dp, 150.dp, 128.dp, 184.dp, corner = 24.dp),
    CollageSpot(2, StoryShape.CIRCLE, 40.dp, 350.dp, 120.dp, 120.dp),
    CollageSpot(3, StoryShape.PILL, 330.dp, 380.dp, 96.dp, 150.dp),
    CollageSpot(4, StoryShape.FLOWER, 176.dp, 400.dp, 122.dp, 122.dp),
)

@Composable
private fun Collage(story: StoryModel, spots: List<CollageSpot>) {
    val covers = story.rankedCovers
    // Smaller covers sit under the large one, as the frames stack them.
    spots.sortedByDescending { it.rank }.forEach { spot ->
        if (spot.rank >= covers.size) return@forEach
        val shape = spot.shape?.shape() ?: arch(spot.width, spot.corner ?: 24.dp)
        ShapedCover(covers[spot.rank], shape, Modifier.offset(spot.x, spot.y).size(spot.width, spot.height))
    }
}

// --- WM1 ---

@Composable
private fun MonthOpener(story: StoryModel, song: StorySong?, actions: PageActions) {
    val scheme = MaterialTheme.colorScheme
    val heroWide = LocalExpressiveTypography.current.heroWide
    val titleWide = LocalExpressiveTypography.current.titleWide
    val month = (story.period as RecapPeriod.Month).month
    Box(Modifier.fillMaxSize()) {
        Collage(story, MonthCollage)
        Column(Modifier.offset(y = 540.dp).padding(horizontal = 24.dp)) {
            Eyebrow(stringResource(R.string.recap_opener_month_eyebrow, story.userName))
            val title = heroWide.style(50.sp, 68.sp, (-2).sp)
            FrameText(
                text = monthName(month.month),
                style = fittedStyle(monthName(month.month), title, 404.dp),
                color = scheme.onSurface,
                maxLines = 1,
                modifier = Modifier.offset(x = (-4).dp).padding(top = 4.dp),
            )
            FrameText(
                text = stringResource(R.string.recap_opener_in_anime, month.year.toString()),
                style = titleWide.style(30.sp, 36.sp),
                color = scheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
            FrameText(
                text = monthOpenerLine(story),
                style = cardText(18, 26, FontWeight.Normal),
                color = scheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 18.dp).width(360.dp),
            )
        }
        StartRow(song, actions.onStart, Modifier.offset(24.dp, 780.dp).padding(end = 48.dp))
    }
}

/** "62 episodes, 17 titles and three Evangelion entries." */
@Composable
private fun monthOpenerLine(story: StoryModel): String {
    val recap = story.recap
    val franchise = recap.franchise
    return if (franchise?.name != null && franchise.mediaIds.size >= 2) {
        pluralStringResourceOrDefault(
            R.plurals.recap_opener_franchise,
            franchise.mediaIds.size,
            episodesText(recap.episodes),
            titlesText(recap.titles),
            spelled(franchise.mediaIds.size),
            franchise.name,
        )
    } else {
        stringResource(R.string.recap_opener_across, episodesText(recap.episodes), titlesText(recap.titles))
    }
}

// --- WM2 ---

@Composable
private fun MonthTime(story: StoryModel) {
    val scheme = MaterialTheme.colorScheme
    val content = scheme.onPrimaryContainer
    val heroWide = LocalExpressiveTypography.current.heroWide
    val titleWide = LocalExpressiveTypography.current.titleWide
    val recap = story.recap
    val month = (story.period as RecapPeriod.Month).month
    Column(Modifier.fillMaxSize().padding(top = 150.dp)) {
        Eyebrow(stringResource(R.string.recap_time_eyebrow, monthName(month.month)), Modifier.padding(start = 24.dp), color = content)
        val big = count(recap.episodes)
        val style = fittedStyle(big, heroWide.style(200.sp, 196.sp, (-8).sp), 416.dp)
        FrameText(big, style = style, color = content, maxLines = 1, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        FrameText(
            text = pluralStringResourceOrDefault(R.plurals.share_recap_episodes_word, recap.episodes),
            style = titleWide.style(40.sp, 46.sp),
            color = content,
            modifier = Modifier.padding(start = 24.dp, top = 2.dp),
        )
        FrameText(
            text = longDuration(recap.minutes, withAnd = true),
            style = cardText(22, 30, FontWeight.Medium),
            color = content.copy(alpha = 0.85f),
            modifier = Modifier.padding(start = 24.dp, top = 6.dp),
        )
        val previous = story.previousEpisodes
        if (previous != null) {
            CompareBox(month.month.minus(1), previous, month.month, recap.episodes, Modifier.padding(start = 16.dp, end = 16.dp, top = 40.dp))
        }
        FrameText(
            text = stringResource(
                R.string.recap_time_across,
                titlesText(recap.titles),
                pluralStringResourceOrDefault(R.plurals.recap_day_count, recap.activeDays, count(recap.activeDays)),
            ),
            style = cardText(18, 26, FontWeight.Normal),
            color = content.copy(alpha = 0.85f),
            modifier = Modifier.padding(start = 24.dp, top = if (previous != null) 54.dp else 40.dp).width(380.dp),
        )
    }
}

@Composable
private fun CompareBox(previousMonth: Month, previous: Int, month: Month, current: Int, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val content = scheme.onPrimaryContainer
    val max = maxOf(previous, current).coerceAtLeast(1)
    val comparison = comparePeriods(current, previous) ?: return
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(scheme.background.copy(alpha = 0.28f))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        listOf(previousMonth to previous, month to current).forEachIndexed { index, (name, episodes) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FrameText(monthName(name), style = cardText(15, 20, FontWeight.Bold), color = content)
                    FrameText(count(episodes), style = cardText(15, 20, FontWeight.ExtraBold), color = content)
                }
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val width = (maxWidth * 0.787f) * episodes / max
                    Box(
                        Modifier
                            .width(width.coerceAtLeast(22.dp))
                            .height(22.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (index == 1) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.5f))
                    )
                }
            }
        }
        val (icon, words) = comparisonWords(comparison.change, comparison.multiple, monthName(previousMonth))
        Row(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.primary)
                .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(18.dp))
            FrameText(words, style = cardText(14, 20, FontWeight.Bold), color = scheme.onPrimary)
        }
    }
}

@Composable
private fun comparisonWords(change: PeriodChange, multiple: Int, month: String): Pair<ImageVector, String> {
    val locale = recapLocale()
    val text = when (change) {
        PeriodChange.ALMOST_TIMES -> if (multiple == 2) {
            stringResource(R.string.recap_compare_almost_twice, month)
        } else {
            stringResource(R.string.recap_compare_almost_times, spelled(multiple), month)
        }
        PeriodChange.TIMES -> if (multiple == 2) {
            stringResource(R.string.recap_compare_twice, month)
        } else {
            stringResource(R.string.recap_compare_times, spelled(multiple), month)
        }
        PeriodChange.MORE_THAN_TIMES -> if (multiple == 2) {
            stringResource(R.string.recap_compare_more_than_twice, month)
        } else {
            stringResource(R.string.recap_compare_more_than_times, spelled(multiple), month)
        }
        PeriodChange.MORE -> stringResource(R.string.recap_compare_more, month)
        PeriodChange.ABOUT_THE_SAME -> stringResource(R.string.recap_compare_same, month)
        PeriodChange.FEWER -> stringResource(R.string.recap_compare_fewer, month)
        PeriodChange.LESS_THAN_HALF -> stringResource(R.string.recap_compare_under_half, month)
    }.capitalized(locale)
    val icon = when (change) {
        PeriodChange.ABOUT_THE_SAME -> Icons.AutoMirrored.Filled.TrendingFlat
        PeriodChange.FEWER, PeriodChange.LESS_THAN_HALF -> Icons.AutoMirrored.Filled.TrendingDown
        else -> Icons.AutoMirrored.Filled.TrendingUp
    }
    return icon to text
}

// --- WM3 ---

@Composable
private fun MostWatched(story: StoryModel) {
    val scheme = MaterialTheme.colorScheme
    val titleWide = LocalExpressiveTypography.current.titleWide
    val top = story.recap.ranked.first()
    val title = story.title(top.mediaId)
    Column(Modifier.fillMaxSize().padding(top = 146.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow(stringResource(R.string.recap_most_watched_eyebrow), align = TextAlign.Center)
        ShapedCover(title?.coverUrl, arch(240.dp, 28.dp), Modifier.padding(top = 14.dp).size(240.dp, 340.dp))
        FrameText(
            text = title?.title.orEmpty(),
            style = titleWide.style(32.sp, 38.sp, (-0.5).sp),
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 24.dp).width(400.dp),
        )
        FrameText(
            text = longDuration(top.minutes, withAnd = false),
            style = titleWide.style(24.sp, 30.sp),
            color = scheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp).width(400.dp),
        )
        val chips = listOfNotNull(title?.format?.let { formatLabel(it) }, title?.year?.toString(), title?.studio)
        if (chips.isNotEmpty()) {
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chips.forEach { chip ->
                    FrameText(
                        text = chip,
                        style = cardText(13, 18, FontWeight.SemiBold),
                        color = scheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(scheme.surfaceContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
        franchiseLine(story)?.let { line ->
            FrameText(
                text = line,
                style = cardText(16, 24, FontWeight.Normal),
                color = scheme.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 26.dp).width(368.dp),
            )
        }
    }
}

/** "With Evangelion 3.33 and Petit Eva, the franchise took three of your 17 titles." */
@Composable
private fun franchiseLine(story: StoryModel): String? {
    val recap = story.recap
    val franchise = recap.franchise ?: return null
    if (franchise.mediaIds.size < 2) return null
    val topId = recap.ranked.first().mediaId
    val size = franchise.mediaIds.size
    return if (topId in franchise.mediaIds) {
        val others = franchise.mediaIds.filter { it != topId }.mapNotNull { story.title(it)?.title }
        pluralStringResourceOrDefault(R.plurals.recap_franchise_took, recap.titles, joinedNames(others), spelled(size), count(recap.titles))
    } else {
        val name = franchise.name ?: return null
        pluralStringResourceOrDefault(R.plurals.recap_franchise_named_took, recap.titles, name, spelled(size), count(recap.titles))
    }
}

// --- WM4 and WY3 ---

@Composable
private fun MonthTopFive(story: StoryModel) {
    val scheme = MaterialTheme.colorScheme
    val heroWide = LocalExpressiveTypography.current.heroWide
    Column(Modifier.fillMaxSize().padding(top = 146.dp)) {
        Eyebrow(stringResource(R.string.recap_top_five_eyebrow), Modifier.padding(start = 24.dp))
        PageTitle(stringResource(R.string.recap_by_time), Modifier.padding(start = 24.dp, top = 4.dp), size = 30, line = 36)
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            story.recap.ranked.take(5).forEachIndexed { index, ranked ->
                val title = story.title(ranked.mediaId)
                val first = index == 0
                val content = if (first) scheme.onPrimaryContainer else scheme.onSurface
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (first) scheme.primaryContainer else scheme.surfaceContainer)
                        .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    FrameText(
                        text = count(index + 1),
                        style = heroWide.style(if (first) 40.sp else 30.sp, 44.sp),
                        color = if (first) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                        modifier = Modifier.width(30.dp),
                    )
                    if (first) {
                        ShapedCover(title?.coverUrl, StoryShape.COOKIE9.shape(), Modifier.size(83.dp, 82.dp), shadow = false)
                    } else {
                        CardPicture(title?.coverUrl, RoundedCornerShape(14.dp), Modifier.size(60.dp, 84.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        FrameText(
                            text = title?.title.orEmpty(),
                            style = cardText(if (first) 17 else 15, 22, FontWeight.Bold),
                            color = content,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        FrameText(
                            text = rankMeta(ranked.minutes, ranked.episodes, title?.isFilm == true),
                            style = cardText(13, 18, FontWeight.Normal),
                            color = (if (first) content else scheme.onSurfaceVariant).copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }
        FrameText(
            text = rankNoteText(story),
            style = cardText(14, 20, FontWeight.Normal),
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp).width(400.dp),
        )
    }
}

/** "2 h 35 min · movie", "2 h 16 min · 4 episodes". */
@Composable
private fun rankMeta(minutes: Int, episodes: Int, isFilm: Boolean): String = stringResource(
    R.string.filter_chip_value,
    shortDuration(minutes),
    if (isFilm) stringResource(R.string.recap_meta_movie) else episodesText(episodes),
)

@Composable
private fun rankNoteText(story: StoryModel): String {
    val media = story.titles
    return when (val note = story.rankNote) {
        is RankNote.Tie -> {
            val first = media[note.first]?.title.orEmpty()
            val second = media[note.second]?.title.orEmpty()
            when {
                note.firstFinished && note.secondFinished -> stringResource(R.string.recap_note_tie_both, first, second)
                note.firstFinished -> stringResource(R.string.recap_note_tie_finished, first, second)
                else -> stringResource(R.string.recap_note_tie, first, second)
            }
        }
        is RankNote.Shorts -> pluralStringResourceOrDefault(
            if (note.topIsFilm) R.plurals.recap_note_shorts_film else R.plurals.recap_note_shorts,
            note.episodes,
            count(note.episodes),
            spelled(note.minutesEach),
        )
        RankNote.ByTime -> stringResource(R.string.recap_note_by_time)
    }
}

@Composable
private fun YearTopFive(story: StoryModel) {
    val scheme = MaterialTheme.colorScheme
    val heroWide = LocalExpressiveTypography.current.heroWide
    val titleWide = LocalExpressiveTypography.current.titleWide
    val year = (story.period as RecapPeriod.Year).year
    val ranked = story.recap.ranked
    Column(Modifier.fillMaxSize().padding(top = 146.dp)) {
        Eyebrow(stringResource(R.string.recap_top_five_year_eyebrow, year.toString()), Modifier.padding(start = 24.dp))
        val top = ranked.first()
        val topTitle = story.title(top.mediaId)
        Row(Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            ShapedCover(topTitle?.coverUrl, StoryShape.COOKIE9.shape(), Modifier.size(148.dp, 146.dp))
            Column(Modifier.padding(start = 20.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FrameText(count(1), style = heroWide.style(48.sp, 48.sp), color = scheme.primary)
                FrameText(
                    text = topTitle?.title.orEmpty(),
                    style = titleWide.style(26.sp, 30.sp),
                    color = scheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                FrameText(rankMeta(top.minutes, top.episodes, topTitle?.isFilm == true), style = cardText(14, 20, FontWeight.Normal), color = scheme.onSurfaceVariant)
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ranked.drop(1).take(4).forEachIndexed { index, item ->
                val title = story.title(item.mediaId)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(scheme.surfaceContainer)
                        .padding(start = 10.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FrameText(count(index + 2), style = heroWide.style(28.sp, 34.sp), color = scheme.onSurfaceVariant, modifier = Modifier.width(24.dp))
                    CardPicture(title?.coverUrl, RoundedCornerShape(12.dp), Modifier.size(54.dp, 76.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        FrameText(title?.title.orEmpty(), style = cardText(15, 20, FontWeight.Bold), color = scheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        FrameText(rankMeta(item.minutes, item.episodes, title?.isFilm == true), style = cardText(13, 18, FontWeight.Normal), color = scheme.onSurfaceVariant)
                    }
                }
            }
        }
        FrameText(
            text = rankNoteText(story),
            style = cardText(14, 20, FontWeight.Normal),
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, top = 14.dp).width(400.dp),
        )
    }
}

// --- WM5 ---

/** WM5's bubbles by rank. Each is as tall as the frame draws it, its text from the top inset down. */
private data class BubbleSize(val horizontal: Dp, val top: Dp, val height: Dp, val name: Int, val nameLine: Float, val hours: Int, val hoursLine: Float)

private val BubbleSizes = listOf(
    BubbleSize(33.dp, 21.dp, 99.dp, 29, 36f, 15, 18.6f),
    BubbleSize(28.6.dp, 18.2.dp, 89.dp, 25, 31.2f, 13, 16.12f),
    BubbleSize(25.3.dp, 16.1.dp, 84.dp, 23, 27.6f, 12, 16f),
    BubbleSize(20.9.dp, 13.3.dp, 70.dp, 18, 22.8f, 12, 16f),
    BubbleSize(18.7.dp, 11.9.dp, 63.dp, 17, 20.4f, 12, 16f),
    BubbleSize(18.7.dp, 11.9.dp, 63.dp, 16, 20.4f, 12, 16f),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Mood(story: StoryModel) {
    val scheme = MaterialTheme.colorScheme
    val titleWide = LocalExpressiveTypography.current.titleWide
    val genres = story.recap.genres.take(BubbleSizes.size)
    val locale = recapLocale()
    Column(Modifier.fillMaxSize().padding(top = 146.dp)) {
        Eyebrow(stringResource(R.string.recap_mood_eyebrow), Modifier.padding(start = 24.dp))
        val names = genres.take(3).mapIndexed { index, genre -> if (index == 0) genre.name else genre.name.lowercase(locale) }
        PageTitle(joinedNames(names), Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp))
        FlowRow(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            genres.forEachIndexed { index, genre ->
                val size = BubbleSizes[index]
                val colors = genreColor(genre.name)
                val container = colors?.container ?: scheme.secondaryContainer
                val content = colors?.content ?: scheme.onSecondaryContainer
                Column(
                    Modifier
                        .align(Alignment.CenterVertically)
                        .height(size.height)
                        .clip(RoundedCornerShape(200.dp))
                        .background(container)
                        .padding(start = size.horizontal, end = size.horizontal, top = size.top),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    FrameText(genre.name, style = titleWide.style(size.name.sp, size.nameLine.sp), color = content, maxLines = 1)
                    FrameText(decimalHours(genre.minutes), style = cardText(size.hours, size.hoursLine.toInt(), FontWeight.SemiBold), color = content.copy(alpha = 0.8f))
                }
            }
        }
        FrameText(
            text = stringResource(R.string.recap_mood_note),
            style = cardText(14, 20, FontWeight.Normal),
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, top = 18.dp).width(400.dp),
        )
    }
}

// --- WM6 ---

@Composable
private fun Finished(story: StoryModel) {
    val scheme = MaterialTheme.colorScheme
    val heroWide = LocalExpressiveTypography.current.heroWide
    val titleWide = LocalExpressiveTypography.current.titleWide
    val recap = story.recap
    val shown = recap.finishedTitles.take(6)
    Column(Modifier.fillMaxSize().padding(top = 146.dp)) {
        Eyebrow(stringResource(R.string.recap_finished_eyebrow), Modifier.padding(start = 24.dp))
        Row(Modifier.padding(start = 20.dp, top = 2.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val style = heroWide.style(150.sp, 150.sp, (-6).sp)
            FrameText(count(recap.finished), style = style, color = scheme.primary, maxLines = 1)
            Column(Modifier.padding(bottom = 22.dp)) {
                FrameText(pluralStringResourceOrDefault(R.plurals.share_recap_titles_word, recap.finished), style = titleWide.style(30.sp, 36.sp), color = scheme.onSurface)
                FrameText(
                    text = pluralStringResourceOrDefault(R.plurals.recap_started_count, recap.started, count(recap.started)),
                    style = cardText(16, 22, FontWeight.Normal),
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        if (shown.isNotEmpty()) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 26.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                shown.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { id ->
                            Box(Modifier.size(132.dp, 186.dp)) {
                                CardPicture(story.title(id)?.coverUrl, RoundedCornerShape(20.dp), Modifier.fillMaxSize())
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 8.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(scheme.primary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                moreFinished(recap, shown.size)?.let { more ->
                    val first = story.title(more.first)?.title.orEmpty()
                    val last = story.title(more.last)?.title.orEmpty()
                    FrameText(
                        text = pluralStringResourceOrDefault(R.plurals.recap_more_finished, more.count, count(more.count), first, last),
                        style = cardText(14, 20, FontWeight.SemiBold),
                        color = scheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(scheme.surfaceContainer)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    )
                }
            }
        }
    }
}

// --- WM7 ---

@Composable
private fun Rhythm(story: StoryModel) {
    val scheme = MaterialTheme.colorScheme
    val recap = story.recap
    val month = (story.period as RecapPeriod.Month).month
    val locale = recapLocale()
    val firstDay = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    Column(Modifier.fillMaxSize().padding(top = 146.dp)) {
        Eyebrow(stringResource(R.string.recap_rhythm_eyebrow), Modifier.padding(start = 24.dp))
        PageTitle(
            pluralStringResourceOrDefault(R.plurals.recap_days_with_anime, recap.activeDays, count(recap.activeDays)),
            Modifier.padding(start = 24.dp, top = 4.dp),
        )
        Column(
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(scheme.surfaceContainerLow)
                .padding(start = 18.dp, end = 14.dp, top = 16.dp, bottom = 22.dp),
        ) {
            val days = (0 until 7).map { firstDay.plus(it.toLong()) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEach { day ->
                    FrameText(
                        text = day.getDisplayName(java.time.format.TextStyle.NARROW_STANDALONE, locale),
                        style = cardText(12, 16, FontWeight.SemiBold),
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(48.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            val lead = ((month.atDay(1).dayOfWeek.value - firstDay.value) + 7) % 7
            val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                cells.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        week.forEach { date ->
                            if (date == null) {
                                Spacer(Modifier.size(48.dp))
                            } else {
                                val watched = (recap.episodesByDay[date] ?: 0) > 0
                                val biggest = date == recap.biggestDay.date
                                val (fill, ink) = when {
                                    biggest -> scheme.primary to scheme.onPrimary
                                    watched -> scheme.primaryContainer to scheme.onPrimaryContainer
                                    else -> scheme.surfaceContainer to scheme.onSurfaceVariant
                                }
                                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(fill), contentAlignment = Alignment.Center) {
                                    FrameText(
                                        text = count(date.dayOfMonth),
                                        style = cardText(if (biggest) 16 else 14, 20, if (biggest) FontWeight.ExtraBold else FontWeight.SemiBold),
                                        color = ink,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (recap.streak.days >= 2) {
                InsightRow(
                    Icons.Default.Event,
                    pluralStringResourceOrDefault(R.plurals.recap_days_in_a_row, recap.streak.days, count(recap.streak.days)),
                    dateRange(recap.streak.start, recap.streak.end),
                )
            }
            WeekdayRow(story)
            BiggestDayRow(story, year = false)
        }
    }
}

@Composable
private fun WeekdayRow(story: StoryModel) {
    val recap = story.recap
    val day = recap.busiestWeekday
    InsightRow(
        Icons.Default.Today,
        stringResource(R.string.recap_weekday_your_day, weekdayName(day)),
        pluralStringResourceOrDefault(R.plurals.recap_of_your_episodes, recap.episodes, count(recap.episodesByWeekday[day] ?: 0), count(recap.episodes)),
    )
}

/** "Biggest day: 20 September" over "25 episodes, 24 of them Petit Eva shorts", or the year's form. */
@Composable
private fun BiggestDayRow(story: StoryModel, year: Boolean) {
    val biggest = story.recap.biggestDay
    val total = episodesText(biggest.episodes)
    val leaders = biggest.leaders.mapNotNull { story.title(it) }
    val lead = when {
        leaders.size >= 2 -> stringResource(R.string.recap_each, joinedNames(leaders.map { it.title }), spelled(biggest.leadingEpisodes))
        leaders.size == 1 && biggest.leadingEpisodes < biggest.episodes -> {
            val name = leaders.first().let { if (it.isShort) stringResource(R.string.recap_title_shorts, it.title) else it.title }
            stringResource(R.string.recap_of_them, count(biggest.leadingEpisodes), name)
        }
        else -> null
    }
    if (year) {
        InsightRow(
            Icons.Default.PlayArrow,
            stringResource(R.string.recap_biggest_day_lead, dayAndMonth(biggest.date), total),
            lead ?: leaders.firstOrNull()?.title.orEmpty(),
        )
    } else {
        InsightRow(
            Icons.Default.PlayArrow,
            stringResource(R.string.recap_biggest_day, dayAndMonth(biggest.date)),
            lead?.let { stringResource(R.string.recap_biggest_day_lead, total, it) } ?: total,
        )
    }
}

// --- WM8 and WY6 ---

@Composable
private fun CardPage(story: StoryModel, actions: PageActions, card: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val label = (story.period as? RecapPeriod.Month)?.let { monthName(it.month.month) } ?: (story.period as RecapPeriod.Year).year.toString()
    Column(Modifier.fillMaxSize().padding(top = 142.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow(stringResource(R.string.recap_card_eyebrow, label), align = TextAlign.Center)
        Box(Modifier.padding(top = 10.dp).size(315.dp, 560.dp)) { card() }
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardButton(Icons.Default.Tune, stringResource(R.string.recap_customize), false, actions.onCustomize, Modifier.weight(1f))
            CardButton(Icons.Default.Download, stringResource(R.string.share_action_save), false, actions.onSaveCard, Modifier.weight(1f))
            CardButton(Icons.Default.Share, stringResource(R.string.share_action_share), true, actions.onShareCard, Modifier.weight(1f))
        }
        val (text, icon, onClick) = if (story.isYear) {
            Triple(stringResource(R.string.recap_replay), Icons.Default.Refresh, actions.onReplay)
        } else {
            Triple(stringResource(R.string.recap_see_statistics), Icons.Default.ChevronRight, actions.onSeeStatistics)
        }
        if (!story.isPreview || story.isYear) {
            Row(
                Modifier
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FrameText(text, style = cardText(14, 20, FontWeight.Bold), color = scheme.primary)
                Icon(icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun CardButton(icon: ImageVector, label: String, primary: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val ink = if (primary) scheme.onPrimary else scheme.onSurface
    Row(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (primary) scheme.primary else scheme.surfaceContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
        // A third of the row is short of "Customize" at 14sp, as the studio's own actions are.
        Text(
            text = label,
            style = cardText(14, 20, FontWeight.Bold),
            color = ink,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp),
        )
    }
}

// --- WY1 ---

@Composable
private fun YearOpener(story: StoryModel, song: StorySong?, actions: PageActions) {
    val scheme = MaterialTheme.colorScheme
    val heroWide = LocalExpressiveTypography.current.heroWide
    val recap = story.recap
    val year = (story.period as RecapPeriod.Year).year
    Box(Modifier.fillMaxSize()) {
        Collage(story, YearCollage)
        Column(Modifier.offset(y = 556.dp).padding(horizontal = 24.dp)) {
            Eyebrow(stringResource(R.string.recap_opener_year_eyebrow, story.userName))
            val style = heroWide.style(112.sp, 116.sp, (-5).sp)
            FrameText(year.toString(), style = style, color = scheme.onSurface, maxLines = 1, modifier = Modifier.offset(x = (-10).dp).padding(top = 2.dp))
            val busiest = busiestMonth(recap)
            FrameText(
                text = if (busiest?.ranAway == true) {
                    stringResource(R.string.recap_opener_year_busy, episodesText(recap.episodes), titlesText(recap.titles), monthName(busiest.month))
                } else {
                    stringResource(R.string.recap_opener_across, episodesText(recap.episodes), titlesText(recap.titles))
                },
                style = cardText(18, 26, FontWeight.Normal),
                color = scheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 14.dp).width(380.dp),
            )
        }
        StartRow(song, actions.onStart, Modifier.offset(24.dp, 790.dp).padding(end = 48.dp))
    }
}

// --- WY2 ---

@Composable
private fun YearNumbers(story: StoryModel) {
    val scheme = MaterialTheme.colorScheme
    val content = scheme.onPrimaryContainer
    val heroWide = LocalExpressiveTypography.current.heroWide
    val titleWide = LocalExpressiveTypography.current.titleWide
    val recap = story.recap
    val year = (story.period as RecapPeriod.Year).year
    val panel = scheme.background.copy(alpha = 0.3f)
    Column(Modifier.fillMaxSize().padding(top = 146.dp)) {
        Eyebrow(stringResource(R.string.recap_time_eyebrow, year.toString()), Modifier.padding(start = 24.dp), color = content)
        Row(Modifier.padding(start = 16.dp, top = 2.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val big = count(recap.episodes)
            val style = fittedStyle(big, heroWide.style(108.sp, 110.sp, (-5).sp), 300.dp)
            FrameText(big, style = style, color = content, maxLines = 1)
            FrameText(
                pluralStringResourceOrDefault(R.plurals.share_recap_episodes_word, recap.episodes),
                style = titleWide.style(22.sp, 32.sp),
                color = content,
                modifier = Modifier.padding(bottom = 18.dp),
            )
        }
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 32.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                wholeHours(recap.minutes) to stringResource(R.string.recap_watching_label),
                count(recap.titles) to pluralStringResourceOrDefault(R.plurals.share_recap_titles_word, recap.titles),
                count(recap.activeDays) to pluralStringResourceOrDefault(R.plurals.stats_active_days, recap.activeDays),
            ).forEach { (value, label) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(panel).padding(horizontal = 14.dp, vertical = 12.dp)) {
                    FrameText(value, style = titleWide.style(24.sp, 30.sp), color = content, maxLines = 1)
                    FrameText(label, style = cardText(12, 16, FontWeight.Medium), color = content.copy(alpha = 0.8f), maxLines = 1)
                }
            }
        }
        MonthsChart(story, panel, content, Modifier.padding(start = 16.dp, end = 16.dp, top = 28.dp))
        busiestMonth(recap)?.let { busiest ->
            val quiet = recap.quietestMonth?.takeIf { it != busiest.month }
            val first = if (busiest.ranAway) {
                stringResource(R.string.recap_busiest_ran_away, monthName(busiest.month))
            } else {
                stringResource(R.string.recap_busiest_month, monthName(busiest.month))
            }
            val second = quiet?.let {
                val episodes = recap.episodesByMonth?.get(it) ?: 0
                pluralStringResourceOrDefault(R.plurals.recap_quietest_month, episodes, monthName(it), spelled(episodes))
            }
            FrameText(
                text = listOfNotNull(first, second).joinToString(" "),
                style = cardText(17, 24, FontWeight.Normal),
                color = content.copy(alpha = 0.9f),
                modifier = Modifier.padding(start = 24.dp, top = 20.dp).width(390.dp),
            )
        }
    }
}

@Composable
private fun MonthsChart(story: StoryModel, panel: Color, content: Color, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val months = story.recap.episodesByMonth ?: return
    val busiest = story.recap.busiestMonth
    val max = months.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val locale = recapLocale()
    Column(modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(28.dp)).background(panel).padding(start = 20.dp, end = 14.5.dp, top = 18.dp)) {
        Eyebrow(stringResource(R.string.recap_episodes_by_month), color = content.copy(alpha = 0.8f))
        Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(190.dp)) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Month.entries.forEach { month ->
                    val episodes = months[month] ?: 0
                    val lit = month == busiest
                    Box(
                        Modifier
                            .width(24.dp)
                            .height((190f * episodes / max).coerceAtLeast(8f).dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (lit) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.55f))
                    )
                }
            }
            if (busiest != null) {
                val index = busiest.ordinal
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val pitch = (maxWidth - 24.dp) / 11
                    FrameText(
                        text = stringResource(R.string.filter_chip_value, monthName(busiest), count(months[busiest] ?: 0)),
                        style = cardText(13, 18, FontWeight.Bold),
                        color = scheme.onPrimary,
                        maxLines = 1,
                        modifier = Modifier
                            .offset(x = (pitch * index).coerceAtMost(maxWidth - 120.dp), y = (190f - 190f * (months[busiest] ?: 0) / max - 2f).dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.primary)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Row(Modifier.padding(top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Month.entries.forEach { month ->
                FrameText(
                    text = month.getDisplayName(java.time.format.TextStyle.NARROW_STANDALONE, locale),
                    style = cardText(12, 16, FontWeight.Bold),
                    color = content,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(24.dp),
                )
            }
        }
    }
}

// --- WY4 ---

@Composable
private fun YearTaste(story: StoryModel) {
    val scheme = MaterialTheme.colorScheme
    val titleWide = LocalExpressiveTypography.current.titleWide
    val recap = story.recap
    val locale = recapLocale()
    val genres = recap.genres.take(5)
    val max = genres.firstOrNull()?.minutes?.coerceAtLeast(1) ?: 1
    Column(Modifier.fillMaxSize().padding(top = 146.dp)) {
        Eyebrow(stringResource(R.string.recap_taste_eyebrow), Modifier.padding(start = 24.dp))
        val rest = genres.drop(1).take(2).map { it.name.lowercase(locale) }
        PageTitle(
            text = if (rest.isEmpty()) {
                stringResource(R.string.recap_taste_title_one, genres.first().name)
            } else {
                stringResource(R.string.recap_taste_title, genres.first().name, joinedNames(rest))
            },
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp),
            size = 30,
            line = 36,
        )
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            genres.forEach { genre ->
                val colors = genreColor(genre.name)
                val content = colors?.content ?: scheme.onSecondaryContainer
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .width((maxWidth * genre.minutes / max).coerceAtLeast(160.dp))
                            .height(52.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors?.container ?: scheme.secondaryContainer)
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        FrameText(genre.name, style = titleWide.style(18.sp, 24.sp), color = content, maxLines = 1)
                        FrameText(decimalHours(genre.minutes), style = cardText(15, 20, FontWeight.Bold), color = content.copy(alpha = 0.85f), maxLines = 1)
                    }
                }
            }
        }
        val studios = recap.studios.take(2)
        if (studios.isNotEmpty()) {
            Eyebrow(stringResource(R.string.recap_studios_eyebrow), Modifier.padding(start = 24.dp, top = 26.dp))
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                studios.forEach { studio ->
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(scheme.surfaceContainerLow)
                            .padding(start = 14.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(48.dp).clip(StoryShape.COOKIE12.shape()).background(scheme.primaryContainer), contentAlignment = Alignment.Center) {
                            FrameText(monogram(studio.name), style = cardText(15, 20, FontWeight.ExtraBold), color = scheme.onPrimaryContainer, maxLines = 1)
                        }
                        Column(Modifier.weight(1f)) {
                            FrameText(studio.name, style = cardText(16, 22, FontWeight.Bold), color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            FrameText(decimalHours(studio.minutes), style = cardText(13, 18, FontWeight.Normal), color = scheme.onSurfaceVariant)
                        }
                    }
                }
                if (studios.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        FrameText(
            text = stringResource(R.string.recap_taste_note),
            style = cardText(14, 20, FontWeight.Normal),
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp).width(400.dp),
        )
    }
}

/** "GoHands" reads GH, "MAPPA" MA, "Kyoto Animation" KA. */
internal fun monogram(name: String): String {
    val words = name.split(' ', '-').filter { it.isNotBlank() }
    val capitals = name.filter { it.isUpperCase() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
        capitals.length >= 2 && capitals.length < name.length -> capitals.take(2)
        else -> name.take(2).uppercase()
    }
}

// --- WY5 ---

@Composable
private fun YearHabits(story: StoryModel) {
    val scheme = MaterialTheme.colorScheme
    val recap = story.recap
    val locale = recapLocale()
    val firstDay = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val days: List<DayOfWeek> = (0 until 7).map { firstDay.plus(it.toLong()) }
    val busiest = recap.busiestWeekday
    val max = recap.episodesByWeekday.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    Column(Modifier.fillMaxSize().padding(top = 146.dp)) {
        Eyebrow(stringResource(R.string.recap_habits_eyebrow), Modifier.padding(start = 24.dp))
        PageTitle(mostlyOn(busiest), Modifier.padding(start = 24.dp, top = 4.dp))
        Row(
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 18.dp)
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(scheme.surfaceContainerLow)
                .padding(start = 16.dp, end = 8.dp, top = 30.dp, bottom = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            days.forEach { day ->
                val lit = day == busiest
                Column(Modifier.fillMaxSize().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    val episodes = recap.episodesByWeekday[day] ?: 0
                    Box(
                        Modifier
                            .width(40.dp)
                            .height((120f * episodes / max).coerceAtLeast(14f).dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (lit) scheme.primary else scheme.primaryContainer)
                    )
                    FrameText(
                        text = day.getDisplayName(java.time.format.TextStyle.NARROW_STANDALONE, locale),
                        style = cardText(13, 18, FontWeight.Bold),
                        color = if (lit) scheme.primary else scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WeekdayRow(story)
            if (recap.streak.days >= 2) {
                InsightRow(
                    Icons.Default.Event,
                    pluralStringResourceOrDefault(R.plurals.recap_days_in_a_row, recap.streak.days, count(recap.streak.days)),
                    stringResource(R.string.recap_longest_run_year),
                )
            }
            BiggestDayRow(story, year = true)
            recap.keptUpWhileAiring?.takeIf { it > 0 }?.let { kept ->
                InsightRow(
                    Icons.Default.LiveTv,
                    pluralStringResourceOrDefault(R.plurals.recap_kept_up, kept, count(kept)),
                    stringResource(R.string.recap_kept_up_note),
                )
            }
            recap.manga?.let { manga ->
                val top = story.title(manga.topMediaId)
                Row(
                    Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(scheme.primaryContainer)
                        .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (top != null) CardPicture(top.coverUrl, RoundedCornerShape(10.dp), Modifier.size(44.dp, 62.dp))
                    Column(Modifier.weight(1f)) {
                        FrameText(
                            pluralStringResourceOrDefault(R.plurals.recap_manga_chapter_count, manga.chapters, count(manga.chapters)),
                            style = LocalExpressiveTypography.current.titleWide.style(16.sp, 22.sp),
                            color = scheme.onPrimaryContainer,
                        )
                        if (top != null) {
                            FrameText(
                                stringResource(R.string.recap_of_them, count(manga.topChapters), top.title),
                                style = cardText(13, 18, FontWeight.Normal),
                                color = scheme.onPrimaryContainer.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
