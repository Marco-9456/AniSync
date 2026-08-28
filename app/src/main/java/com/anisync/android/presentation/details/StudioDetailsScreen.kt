package com.anisync.android.presentation.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.StudioDetails
import com.anisync.android.domain.StudioMediaEntry
import com.anisync.android.domain.url
import com.anisync.android.presentation.components.AnimatedFavoriteButton
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.details.components.AppearanceRow
import com.anisync.android.presentation.details.components.CharacterSkeletonContent
import com.anisync.android.presentation.details.components.PersonDropdownChip
import com.anisync.android.presentation.details.components.PersonEmptyState
import com.anisync.android.presentation.details.components.PersonFact
import com.anisync.android.presentation.details.components.PersonFactsCard
import com.anisync.android.presentation.details.components.PersonHero
import com.anisync.android.presentation.details.components.PersonListFooter
import com.anisync.android.presentation.details.components.PersonToggleChip
import com.anisync.android.presentation.details.components.StudioCoverMark
import com.anisync.android.presentation.details.components.StudioLinkRow
import com.anisync.android.presentation.details.components.personGridItems
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.LocalLibraryStatuses
import com.anisync.android.presentation.util.LocalPaneIsRoot
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.formatAsTitle
import com.anisync.android.ui.theme.StarGold
import com.anisync.android.util.getTitle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun StudioDetailsScreen(
    studioId: Int,
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit = {},
    viewModel: StudioDetailsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isScrolled by remember {
        derivedStateOf { scrollBehavior.state.contentOffset < -50f }
    }
    val details = (uiState as? StudioDetailsUiState.Success)?.details
    // The wide layout carries the name on the banner as the header collapses, so the app bar
    // would only say it twice.
    val adaptive = LocalAdaptiveInfo.current
    val wideLayout = adaptive.supportsTwoPane

    val chromeActions: @Composable RowScope.() -> Unit = {
        details?.let {
            BannerIconButton(
                icon = if (it.isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = stringResource(R.string.a11y_person_favourite),
                onClick = viewModel::toggleFavourite,
                tint = if (it.isFavourite) MaterialTheme.colorScheme.error else Color.White
            )
        }
        BannerIconButton(
            icon = Icons.Default.Share,
            contentDescription = stringResource(R.string.cd_share),
            onClick = { viewModel.shareStudio(context) }
        )
        if (LocalPaneIsRoot.current) {
            BannerIconButton(
                icon = Icons.Default.Close,
                contentDescription = stringResource(R.string.pane_close),
                onClick = onBackClick
            )
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (wideLayout) return@Scaffold
            val title = details?.name ?: ""
            val iconTint by animateColorAsState(
                if (isScrolled) MaterialTheme.colorScheme.onSurface else Color.White,
                label = "studioIconTint"
            )

            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = isScrolled,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    if (!LocalPaneIsRoot.current) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = iconTint
                            )
                        }
                    }
                },
                actions = {
                    // Favouriting lives here rather than in a pill beside the count: the old pill
                    // only appeared once a studio already had favourites, so nobody could give the
                    // first one.
                    if (details != null) {
                        AnimatedFavoriteButton(
                            isFavorite = details.isFavourite,
                            onClick = viewModel::toggleFavourite,
                            inactiveColor = iconTint
                        )
                    }
                    IconButton(
                        onClick = { viewModel.shareStudio(context) },
                        enabled = details != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.cd_share),
                            tint = iconTint
                        )
                    }
                    // At a two-pane detail root the close (✕) is the trailing-most action
                    // (right-thumb reach) in place of a leading back arrow.
                    if (LocalPaneIsRoot.current) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.pane_close),
                                tint = iconTint
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            when (val state = uiState) {
                is StudioDetailsUiState.Loading -> {
                    CharacterSkeletonContent(onBackClick = onBackClick)
                }

                is StudioDetailsUiState.Success -> {
                    StudioDetailsContent(
                        studio = state.details,
                        titleLanguage = titleLanguage,
                        onMediaClick = onMediaClick,
                        onLoadMore = viewModel::loadMoreMedia,
                        onBackClick = onBackClick,
                        chromeActions = chromeActions,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }

                is StudioDetailsUiState.Error -> {
                    StudioErrorState(
                        message = state.message,
                        onRetry = viewModel::loadStudioDetails,
                        onBackClick = onBackClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun StudioDetailsContent(
    studio: StudioDetails,
    titleLanguage: TitleLanguage,
    onMediaClick: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onBackClick: () -> Unit,
    chromeActions: @Composable RowScope.() -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    var sortIndex by rememberSaveable { mutableIntStateOf(0) }
    var formatIndex by rememberSaveable { mutableIntStateOf(0) }
    var mainStudioOnly by rememberSaveable { mutableStateOf(false) }
    var onListOnly by rememberSaveable { mutableStateOf(false) }

    val adaptive = LocalAdaptiveInfo.current
    val wide = adaptive.supportsTwoPane
    val columns = if (wide) 2 else 1
    val gutter = Modifier.padding(horizontal = 24.dp)
    val uriHandler = LocalUriHandler.current
    val listStatuses = LocalLibraryStatuses.current

    // A studio has no banner of its own, so the hero borrows the one from the work it is best
    // known for and says so, exactly as the person screens do.
    val backdrop = remember(studio.media, titleLanguage) {
        studio.media.firstOrNull { it.bannerUrl != null }
            ?.let { it.bannerUrl to it.getTitle(titleLanguage) }
    }
    // Work the studio led comes first: a co-production is a poor face for the studio, and the
    // API already hands the list over in popularity order.
    val markSources = remember(studio.media) { studio.media.sortedByDescending { it.isMainStudio } }
    val markCovers = markSources.take(4).mapNotNull { it.cover.url() ?: it.coverUrl }

    // Exact from the API for a short catalogue, exact from what is loaded once the last page is
    // in, and absent while AniList is only willing to say "500".
    val worksTotal = if (studio.hasNextPage) studio.mediaTotal else studio.media.size

    val typeLabel = stringResource(
        if (studio.isAnimationStudio) R.string.studio_label_animation_studio
        else R.string.studio_label_other_studio
    )
    val metaLine = if (worksTotal != null) {
        "$typeLabel · " + pluralStringResource(R.plurals.studio_titles_count, worksTotal, worksTotal)
    } else {
        typeLabel
    }

    val onListCount = studio.media.count { listStatuses.containsKey(it.mediaId) }
    val facts = studioFacts(studio = studio, onListCount = onListCount)

    val anyFormatLabel = stringResource(R.string.studio_filter_any_format)
    val formats = remember(studio.media) {
        studio.media.mapNotNull { it.format }.distinct().sorted()
    }
    val formatOptions = remember(formats, anyFormatLabel) {
        listOf(anyFormatLabel) + formats.map { format -> format.formatAsTitle() ?: format }
    }
    val sortOptions = listOf(
        stringResource(R.string.person_sort_popularity),
        stringResource(R.string.person_sort_newest),
        stringResource(R.string.person_sort_score),
        stringResource(R.string.studio_sort_title)
    )

    val works = remember(
        studio.media,
        sortIndex,
        formatIndex,
        mainStudioOnly,
        onListOnly,
        listStatuses,
        formats,
        titleLanguage
    ) {
        val filtered = studio.media
            .filter { !mainStudioOnly || it.isMainStudio }
            .filter { !onListOnly || listStatuses.containsKey(it.mediaId) }
            .filter { formatIndex == 0 || it.format == formats.getOrNull(formatIndex - 1) }
        // Popularity is the order the API paged in, so leaving it alone is what keeps a later
        // page from landing above the viewport. Every other order re-sorts what is loaded.
        when (sortIndex) {
            1 -> filtered.sortedByDescending { it.year ?: 0 }
            2 -> filtered.sortedByDescending { it.averageScore ?: 0 }
            3 -> filtered.sortedBy { it.getTitle(titleLanguage).lowercase() }
            else -> filtered
        }
    }

    val hero: @Composable () -> Unit = {
        PersonHero(
            imageUrl = null,
            backdropUrl = backdrop?.first,
            name = studio.name,
            nativeName = null,
            metaLine = metaLine,
            favourites = studio.favourites,
            contentDescription = stringResource(R.string.cd_studio_mark, studio.name),
            transitionKey = TransitionKeys.cover(TransitionKeys.STUDIO, studio.id),
            onImageClick = null,
            imageContent = { StudioCoverMark(covers = markCovers) },
            backdropCredit = backdrop?.second
        )
    }

    val sidebar: @Composable ColumnScope.() -> Unit = {
        PersonFactsCard(facts = facts)
        if (facts.isNotEmpty() && studio.hasNextPage) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = pluralStringResource(
                    R.plurals.studio_stats_scope,
                    studio.media.size,
                    studio.media.size
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        studio.siteUrl?.takeUnless { it.isBlank() }?.let { url ->
            Spacer(Modifier.height(16.dp))
            StudioLinkRow(
                label = stringResource(R.string.studio_open_on_anilist),
                value = url.removePrefix("https://").removePrefix("http://").substringBefore('/'),
                onClick = { uriHandler.openUri(url) }
            )
        }
    }

    // A studio has one list, so the tab slot carries the section header and the controls that
    // used to live on a separate grid screen.
    val worksHeader: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = stringResource(R.string.studio_label_works),
                level = HeaderLevel.Section,
                padding = PaddingValues(0.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                PersonDropdownChip(
                    label = sortOptions[sortIndex],
                    options = sortOptions,
                    selectedIndex = sortIndex,
                    onSelect = { sortIndex = it },
                    leadingIcon = Icons.Default.SwapVert,
                    appliedWhenNotDefault = false
                )
                PersonToggleChip(
                    label = stringResource(R.string.studio_main_studio_chip),
                    selected = mainStudioOnly,
                    onToggle = { mainStudioOnly = !mainStudioOnly }
                )
                PersonToggleChip(
                    label = stringResource(R.string.filter_on_my_list),
                    selected = onListOnly,
                    onToggle = { onListOnly = !onListOnly }
                )
                if (formats.size > 1) {
                    PersonDropdownChip(
                        label = formatOptions[formatIndex],
                        options = formatOptions,
                        selectedIndex = formatIndex,
                        onSelect = { formatIndex = it }
                    )
                }
            }
        }
    }

    val mainStudioLabel = stringResource(R.string.studio_main_studio_chip)
    val coProductionLabel = stringResource(R.string.studio_role_co_production)
    val airingLabel = stringResource(R.string.media_status_airing)

    val listContent: LazyListScope.() -> Unit = {
        if (works.isEmpty()) {
            item(key = "works_empty") {
                PersonEmptyState(
                    text = stringResource(R.string.studio_empty_works),
                    modifier = gutter
                )
            }
        } else {
            personGridItems(
                items = works,
                columns = columns,
                key = { "studio_work_${it.mediaId}" },
                rowModifier = gutter.padding(bottom = 12.dp)
            ) { work ->
                AppearanceRow(
                    mediaId = work.mediaId,
                    coverUrl = work.coverUrl,
                    cover = work.cover,
                    title = work.getTitle(titleLanguage),
                    meta = workMeta(work, airingLabel),
                    role = if (work.isMainStudio) mainStudioLabel else coProductionLabel,
                    roleHighlighted = work.isMainStudio,
                    score = work.averageScore,
                    onClick = { onMediaClick(work.mediaId) },
                    transitionPrefix = TransitionKeys.STUDIO,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (studio.hasNextPage) {
            item(key = "works_footer") {
                // Reaching the footer is the request. Keyed on the loaded count rather than the
                // filtered one, so a filter that hides a whole page still walks forward.
                LaunchedEffect(studio.media.size) { onLoadMore() }
                PersonListFooter(modifier = gutter)
            }
        }
    }

    if (wide) {
        PersonWideLayout(
            backdropUrl = backdrop?.first,
            backdropCredit = backdrop?.second,
            onBackClick = onBackClick,
            actions = chromeActions,
            portraitUrl = null,
            portraitTransitionKey = TransitionKeys.cover(TransitionKeys.STUDIO, studio.id),
            onPortraitClick = null,
            portraitContent = { StudioCoverMark(covers = markCovers) },
            name = studio.name,
            nativeName = null,
            metaLine = metaLine,
            favourites = studio.favourites,
            identityKey = studio.id,
            aliasLine = null,
            identityContent = { sidebar() },
            tabs = worksHeader,
            listContent = listContent
        )
    } else {
        PersonScaffold(
            hero = hero,
            sidebar = sidebar,
            tabs = worksHeader,
            listContent = listContent
        )
    }
}

/** Format, year and whether it is still going out: the fields the old row threw away. */
private fun workMeta(work: StudioMediaEntry, airingLabel: String): String = listOfNotNull(
    work.format?.formatAsTitle(),
    work.year?.toString(),
    airingLabel.takeIf { work.status == "RELEASING" }
).joinToString(" · ")

/**
 * What the catalogue looks like, from the pages that are in: how much of it the reader has
 * watched, how it scores, how often the studio leads rather than co-produces, and the years it
 * spans. Every value here was already being fetched and thrown away.
 */
@Composable
private fun studioFacts(studio: StudioDetails, onListCount: Int): List<PersonFact> {
    val scheme = MaterialTheme.colorScheme
    val media = studio.media
    return buildList {
        if (media.isNotEmpty()) {
            add(
                PersonFact(
                    label = stringResource(R.string.studio_stat_on_your_list),
                    value = onListCount.toString(),
                    icon = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                    tint = scheme.primary
                )
            )
        }
        val scores = media.mapNotNull { it.averageScore }.filter { it > 0 }
        if (scores.isNotEmpty()) {
            add(
                PersonFact(
                    label = stringResource(R.string.studio_stat_average_score),
                    value = "${scores.average().roundToInt()}%",
                    icon = Icons.Default.Star,
                    tint = StarGold
                )
            )
        }
        if (media.isNotEmpty()) {
            add(
                PersonFact(
                    label = stringResource(R.string.studio_stat_lead_studio),
                    value = "${media.count { it.isMainStudio } * 100 / media.size}%",
                    icon = Icons.Default.Business,
                    tint = scheme.secondary
                )
            )
        }
        val years = media.mapNotNull { it.year }
        if (years.isNotEmpty()) {
            val first = years.min()
            val last = years.max()
            add(
                PersonFact(
                    label = stringResource(R.string.studio_stat_span),
                    value = if (first == last) "$first" else "$first–$last",
                    icon = Icons.Default.CalendarMonth,
                    tint = scheme.tertiary
                )
            )
        }
    }
}

@Composable
private fun StudioErrorState(
    message: String,
    onRetry: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.error_oops),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onBackClick) {
                Text(stringResource(R.string.action_go_back))
            }
        }
    }
}
