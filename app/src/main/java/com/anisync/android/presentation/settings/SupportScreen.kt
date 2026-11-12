package com.anisync.android.presentation.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.anisync.android.R
import com.anisync.android.data.WhatsNewItem
import com.anisync.android.domain.Supporter
import com.anisync.android.domain.formatExactCount
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import com.anisync.android.presentation.components.LocalWhatsNew
import com.anisync.android.presentation.share.cards.cardText
import com.anisync.android.presentation.share.cards.cardUpper
import com.anisync.android.presentation.share.cards.fitted
import com.anisync.android.presentation.util.adaptiveReadingWidth
import com.anisync.android.presentation.util.formatMonthYear
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.presentation.util.rememberAnimationsEnabled
import com.anisync.android.ui.theme.ExpressiveShapes
import com.anisync.android.ui.theme.LocalExpressiveTypography
import com.anisync.android.ui.theme.lineBox
import com.anisync.android.util.ShareUtils
import com.anisync.android.util.launchUrl
import java.time.YearMonth

private const val KOFI_URL = "https://ko-fi.com/marco_9456"
private const val GITHUB_SPONSORS_URL = "https://github.com/sponsors/Marco-9456"
private const val REPOSITORY_URL = "https://github.com/Marco-9456/AniSync"
private const val NEW_ISSUE_URL = "$REPOSITORY_URL/issues/new/choose"
private const val FDROID_URL = "https://f-droid.org/packages/com.anisync.android/"

private const val HERO_KEY = "hero"

/**
 * Support AniSync: a tip through Ko-fi or GitHub Sponsors, the people who have left one, and the
 * ways to help that cost nothing. The page prints its own title, so the bar only takes it over once
 * the headline has scrolled underneath.
 */
@Composable
fun SupportScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SupportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onAction = viewModel::onAction
    val context = LocalContext.current
    val whatsNewStore = LocalWhatsNew.current
    LaunchedEffect(Unit) { whatsNewStore.markSeen(WhatsNewItem.SETTINGS_SUPPORT) }

    val openTip: (String) -> Unit = { url ->
        onAction(SupportAction.TipOpened)
        context.launchUrl(url)
    }

    val listState = rememberLazyListState()
    val pinnedScroll = rememberScrollState()
    var twoColumns by remember { mutableStateOf(false) }
    var headlineBottom by remember { mutableIntStateOf(Int.MAX_VALUE) }
    val showBarTitle by remember {
        derivedStateOf {
            if (twoColumns) {
                pinnedScroll.value > headlineBottom
            } else {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > headlineBottom
            }
        }
    }

    CollapsingTopBarScaffold(
        title = if (showBarTitle) stringResource(R.string.sponsors_title) else "",
        heroTitle = false,
        onBackClick = onBackClick,
        modifier = modifier,
        scrollableState = listState,
        scrolledContainerColor = MaterialTheme.colorScheme.background,
        enableEnterAnimation = true,
    ) { topPadding ->
        val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp
        val supporters = uiState.supporters.orEmpty()
        val shareText = stringResource(R.string.support_share_text, FDROID_URL, REPOSITORY_URL)

        val hero: @Composable () -> Unit = { SupportHero(onHeadlineBottom = { headlineBottom = it }) }
        val actions: @Composable (Modifier) -> Unit = { actionsModifier ->
            TipActions(
                onKofi = { openTip(KOFI_URL) },
                onGitHub = { openTip(GITHUB_SPONSORS_URL) },
                modifier = actionsModifier,
            )
        }
        val thanks: @Composable (Modifier) -> Unit = { thanksModifier ->
            if (supporters.isNotEmpty()) {
                ThanksWall(
                    supporters = supporters,
                    onOpenProfile = { context.launchUrl(it) },
                    modifier = thanksModifier,
                )
            }
        }
        val rest: @Composable () -> Unit = {
            OtherWaysToHelp(
                onStar = { context.launchUrl(REPOSITORY_URL) },
                onTranslate = { context.launchUrl(WEBLATE_URL) },
                onReport = { context.launchUrl(NEW_ISSUE_URL) },
                onShare = { ShareUtils.shareText(context, shareText) },
            )
            Reminders(
                enabled = uiState.remindersEnabled,
                onEnabledChange = { onAction(SupportAction.SetRemindersEnabled(it)) },
            )
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Only a window wider than it is tall splits. On a tablet held upright the hero would
            // leave half the page empty beside a long second column, so one centred column reads
            // better there.
            val wide = maxWidth >= SettingsTwoColumnMinWidth && maxWidth > maxHeight
            SideEffect { twoColumns = wide }
            if (!wide) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = topPadding + 8.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = bottomPadding,
                    ),
                ) {
                    item(HERO_KEY) {
                        Column(Modifier.fillMaxWidth().adaptiveReadingWidth()) {
                            hero()
                            actions(Modifier.padding(top = 24.dp))
                        }
                    }
                    item("thanks") {
                        thanks(Modifier.fillMaxWidth().adaptiveReadingWidth().padding(top = 40.dp))
                    }
                    item("rest") {
                        Column(Modifier.fillMaxWidth().adaptiveReadingWidth()) { rest() }
                    }
                }
            } else {
                // The buttons open the second column: under the hero they fell below a 1280 by
                // 800dp tablet's pane, and the one thing the page asks for needed a scroll.
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 480.dp)
                            .fillMaxHeight()
                            .verticalScroll(pinnedScroll)
                            .padding(top = topPadding + 8.dp, bottom = bottomPadding),
                    ) { hero() }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 480.dp)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(top = topPadding + 8.dp, bottom = bottomPadding),
                    ) {
                        item("actions") { actions(Modifier) }
                        item("thanks") { thanks(Modifier.fillMaxWidth().padding(top = 40.dp)) }
                        item("rest") { Column { rest() } }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportHero(onHeadlineBottom: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val headline = LocalExpressiveTypography.current.heroWide.style(50.sp, 60.sp, (-2).sp)
    DeveloperMark()
    Eyebrow(stringResource(R.string.support_eyebrow), Modifier.padding(top = 20.dp))
    Text(
        text = stringResource(R.string.sponsors_hero_headline),
        style = headline,
        color = scheme.onSurface,
        modifier = Modifier
            .padding(top = 8.dp)
            .lineBox(headline)
            .semantics { heading() }
            .onPlaced { onHeadlineBottom((it.positionInParent().y + it.size.height).toInt()) },
    )
    Text(
        text = stringResource(R.string.sponsors_hero_body),
        style = MaterialTheme.typography.bodyLarge,
        color = scheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
}

/**
 * The developer's avatar from About in a slowly turning 9-sided cookie, drawn twice the way
 * About's easter egg does it, so the head breaks out over the top edge. A Ko-fi cup sits on a sunny
 * shape at its foot.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DeveloperMark() {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val avatar = remember(context) {
        ImageRequest.Builder(context).data(DEVELOPER_AVATAR_URL).crossfade(true).build()
    }
    val cookie = MaterialShapes.Cookie9Sided.fitted()
    val turn = rememberCookieTurn()
    Box(
        Modifier
            .size(width = MARK_SIZE + 32.dp, height = HEAD_ROOM + MARK_SIZE)
            .preferredFrameRate(COOKIE_FRAME_RATE)
            .clearAndSetSemantics { },
    ) {
        // The angle is read only inside the layers, so a frame of the turn redraws them and
        // recomposes or measures nothing.
        Box(
            Modifier
                .offset(y = HEAD_ROOM)
                .size(MARK_SIZE)
                .graphicsLayer {
                    rotationZ = turn.value
                    shape = cookie
                    clip = true
                }
                .background(scheme.tertiaryContainer),
        ) {
            AsyncImage(
                model = avatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    // Turned back by the same angle, so the body stays upright under the head.
                    .graphicsLayer {
                        rotationZ = -turn.value
                        scaleX = AVATAR_SCALE
                        scaleY = AVATAR_SCALE
                    }
                    .offset(y = AVATAR_LIFT),
            )
        }
        AsyncImage(
            model = avatar,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .offset(y = HEAD_ROOM)
                .size(MARK_SIZE)
                .scale(AVATAR_SCALE)
                .offset(y = AVATAR_LIFT)
                .clip(CircleShape)
                .drawWithContent {
                    clipRect(bottom = size.height * 0.45f) { this@drawWithContent.drawContent() }
                },
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(x = MARK_SIZE - 44.dp, y = HEAD_ROOM + MARK_SIZE - 66.dp)
                .size(76.dp)
                .clip(MaterialShapes.Sunny.fitted())
                .background(scheme.primary),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_kofi),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

/** The cookie's angle: one slow turn every [COOKIE_TURN_MILLIS], held still when animations are off. */
@Composable
private fun rememberCookieTurn(): State<Float> {
    if (!rememberAnimationsEnabled()) return remember { mutableFloatStateOf(0f) }
    return rememberInfiniteTransition(label = "CookieTurn").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(COOKIE_TURN_MILLIS, easing = LinearEasing)),
        label = "CookieTurn",
    )
}

@Composable
private fun TipActions(onKofi: () -> Unit, onGitHub: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onKofi,
            shape = ExpressiveShapes.pill,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_kofi),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.sponsors_become_button), style = cardText(16, 22, FontWeight.Bold))
        }
        FilledTonalButton(
            onClick = onGitHub,
            shape = ExpressiveShapes.pill,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_github),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.sponsors_github_button), style = cardText(14, 20, FontWeight.Bold))
        }
        Text(
            text = stringResource(R.string.support_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Everyone who has left a tip, newest first, each in the same cookie with an initial. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThanksWall(
    supporters: List<Supporter>,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val typography = LocalExpressiveTypography.current
    val locale = LocalConfiguration.current.locales[0]
    val count = typography.heroWide.style(120.sp, 120.sp, (-5).sp)
    val word = typography.titleWide.style(30.sp, 36.sp)
    val since = YearMonth.from(supporters.minOf { it.date })
    Column(modifier) {
        Eyebrow(stringResource(R.string.support_thanks_eyebrow))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) { },
        ) {
            Text(
                text = formatExactCount(supporters.size, locale),
                style = count,
                color = scheme.primary,
                modifier = Modifier.lineBox(count),
            )
            Column(Modifier.padding(start = 14.dp, top = 18.dp)) {
                Text(
                    text = pluralStringResourceOrDefault(R.plurals.support_thanks_people, supporters.size),
                    style = word,
                    color = scheme.onSurface,
                    modifier = Modifier.lineBox(word),
                )
                Text(
                    text = stringResource(R.string.support_thanks_since, formatMonthYear(since, locale)),
                    style = cardText(16, 22, FontWeight.Normal),
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        BoxWithConstraints(
            Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
        ) {
            val columns = ((maxWidth + CELL_GAP) / (MIN_CELL + CELL_GAP)).toInt().coerceAtLeast(1)
            // Whole dp: an exact third can overshoot the row by a fraction once rounded to pixels,
            // and the flow then wraps the last cell of every row.
            val cellWidth = ((maxWidth - CELL_GAP * (columns - 1)) / columns).value.toInt().dp
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(CELL_GAP),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                maxItemsInEachRow = columns,
            ) {
                supporters.forEach { supporter ->
                    SupporterCell(supporter, onOpenProfile, Modifier.width(cellWidth))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SupporterCell(supporter: Supporter, onOpenProfile: (String) -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val url = supporter.url
    val initial = supporter.initial(locale)
    val letter = LocalExpressiveTypography.current.heroWide.style(36.sp, 40.sp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(if (url != null) Modifier.clickable { onOpenProfile(url) } else Modifier)
            .semantics(mergeDescendants = true) { },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .clip(MaterialShapes.Cookie9Sided.fitted())
                .background(scheme.primaryContainer)
                .clearAndSetSemantics { },
        ) {
            if (initial != null) {
                Text(initial, style = letter, color = scheme.onPrimaryContainer, modifier = Modifier.lineBox(letter))
            } else {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = scheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Text(
            text = supporter.name ?: stringResource(R.string.support_anonymous),
            style = cardText(14, 20, FontWeight.SemiBold),
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun OtherWaysToHelp(
    onStar: () -> Unit,
    onTranslate: () -> Unit,
    onReport: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = 40.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsSectionLabel(stringResource(R.string.support_other_ways))
        SettingsGroup {
            SettingsItem(
                title = stringResource(R.string.support_star_title),
                subtitle = stringResource(R.string.support_star_desc),
                icon = Icons.Rounded.Star,
                onClick = onStar,
            )
            SettingsItem(
                title = stringResource(R.string.support_translate_title),
                subtitle = stringResource(R.string.support_translate_desc),
                icon = Icons.Outlined.Translate,
                onClick = onTranslate,
            )
            SettingsItem(
                title = stringResource(R.string.support_report_title),
                subtitle = stringResource(R.string.support_report_desc),
                icon = Icons.Outlined.BugReport,
                onClick = onReport,
            )
            SettingsItem(
                title = stringResource(R.string.support_share_title),
                subtitle = stringResource(R.string.support_share_desc),
                icon = Icons.Outlined.Share,
                onClick = onShare,
            )
        }
    }
}

@Composable
private fun Reminders(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsSectionLabel(stringResource(R.string.support_reminders_section))
        SettingsGroup {
            SwitchSettingsItem(
                title = stringResource(R.string.support_reminders_title),
                subtitle = stringResource(R.string.support_reminders_desc),
                checked = enabled,
                onCheckedChange = onEnabledChange,
                icon = Icons.Rounded.VolunteerActivism,
            )
        }
    }
}

/** The recap's small capitals label. */
@Composable
private fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    val style = cardText(12, 16, FontWeight.SemiBold, 1.6f)
    Text(
        text = text.cardUpper(),
        style = style,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        modifier = modifier.lineBox(style),
    )
}

private val MARK_SIZE = 184.dp

/** Room above the cookie for the head to break out into. */
private val HEAD_ROOM = 36.dp

/** About's easter egg scale and lift, at this size. */
private const val AVATAR_SCALE = 1.3f
private val AVATAR_LIFT = (-19).dp

/** Slow enough to read as calm. A nine-sided cookie looks the same every 40 degrees anyway. */
private const val COOKIE_TURN_MILLIS = 20_000

/** A turn this slow moves under a degree a frame at 30 fps, so a 120 Hz screen can idle lower. */
private const val COOKIE_FRAME_RATE = 30f

private val MIN_CELL = 112.dp
private val CELL_GAP = 13.dp
