package com.anisync.android.presentation.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.CharacterDetails
import com.anisync.android.domain.CharacterMedia
import com.anisync.android.domain.VoiceActor
import com.anisync.android.presentation.components.AnimatedFavoriteButton
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.components.ImageViewerDialog
import com.anisync.android.presentation.details.components.AppearanceRow
import com.anisync.android.presentation.details.components.CharacterSkeletonContent
import com.anisync.android.presentation.details.components.ExpandableBiography
import com.anisync.android.presentation.details.components.PersonDropdownChip
import com.anisync.android.presentation.details.components.PersonEmptyState
import com.anisync.android.presentation.details.components.PersonFact
import com.anisync.android.presentation.details.components.PersonFactsCard
import com.anisync.android.presentation.details.components.PersonHero
import com.anisync.android.presentation.details.components.PersonNamesRow
import com.anisync.android.presentation.details.components.PersonNamesSheetContent
import com.anisync.android.presentation.details.components.PersonListFooter
import com.anisync.android.presentation.details.components.PersonTab
import com.anisync.android.presentation.details.components.PersonTabs
import com.anisync.android.presentation.details.components.PersonToggleChip
import com.anisync.android.presentation.details.components.VoiceActorRow
import com.anisync.android.presentation.details.components.formatPersonBirthday
import com.anisync.android.presentation.details.components.personGridItems
import com.anisync.android.presentation.share.CharacterShareCard
import com.anisync.android.presentation.share.ShareImageSheet
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.LocalPaneIsRoot
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.rememberCopyToClipboard
import com.anisync.android.util.AniListUrls
import com.anisync.android.util.getName
import com.anisync.android.util.getTitle
import com.anisync.android.type.MediaType

/** How many rows a tab previews before handing off to the full grid. */
private const val PreviewCount = 6

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun CharacterDetailsScreen(
    characterId: Int,
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    viewModel: CharacterDetailsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle()
    var showShareSheet by rememberSaveable { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isScrolled by remember {
        derivedStateOf { scrollBehavior.state.contentOffset < -50f }
    }
    val details = (uiState as? CharacterDetailsUiState.Success)?.details
    // The wide layout hands the name to the banner as the header collapses, so the app bar title
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
            onClick = { showShareSheet = true }
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
            // The wide layout carries its own chrome on the banner, the way the profile does; an app
            // bar above the banner card would leave an empty band and cover the collapsed strip.
            if (wideLayout) return@Scaffold
            val title = details?.getName(titleLanguage) ?: ""
            val iconTint by animateColorAsState(
                if (isScrolled) MaterialTheme.colorScheme.onSurface else Color.White,
                label = "personIconTint"
            )

            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = isScrolled && !wideLayout,
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
                    // Favouriting is a headline action, not a chip buried in the name card.
                    if (details != null) {
                        AnimatedFavoriteButton(
                            isFavorite = details.isFavourite,
                            onClick = viewModel::toggleFavourite,
                            inactiveColor = iconTint
                        )
                    }
                    IconButton(onClick = { showShareSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.cd_share),
                            tint = iconTint
                        )
                    }
                    // At a two-pane detail root the close (✕) is the trailing-most action (right-thumb
                    // reach) in place of a leading back arrow.
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
                is CharacterDetailsUiState.Loading -> {
                    CharacterSkeletonContent(onBackClick = onBackClick)
                }

                is CharacterDetailsUiState.Success -> {
                    CharacterDetailsContent(
                        character = state.details,
                        titleLanguage = titleLanguage,
                        onMediaClick = onMediaClick,
                        isLoadingMore = state.isLoadingMore,
                        onLoadMore = viewModel::loadMoreMedia,
                        onStaffClick = onStaffClick,
                        onBackClick = onBackClick,
                        chromeActions = chromeActions,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }

                is CharacterDetailsUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = viewModel::loadCharacterDetails,
                        onBackClick = onBackClick
                    )
                }
            }
        }

        if (showShareSheet) {
            details?.let {
                ShareImageSheet(
                    onDismiss = { showShareSheet = false },
                    link = AniListUrls.characterUrl(it.id)
                ) {
                    CharacterShareCard(
                        details = it,
                        displayName = it.getName(titleLanguage)
                    )
                }
            }
        }
    }
}

private enum class CharacterTab { Appearances, VoiceActors }

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CharacterDetailsContent(
    character: CharacterDetails,
    titleLanguage: TitleLanguage,
    onMediaClick: (Int) -> Unit,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    onStaffClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    chromeActions: @Composable RowScope.() -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    var showImageViewer by rememberSaveable { mutableStateOf(false) }
    var showNamesSheet by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var mainRolesOnly by rememberSaveable { mutableStateOf(false) }
    var mediaTypeIndex by rememberSaveable { mutableIntStateOf(0) }
    var sortIndex by rememberSaveable { mutableIntStateOf(0) }
    var languageIndex by rememberSaveable { mutableIntStateOf(0) }
    var actorSortIndex by rememberSaveable { mutableIntStateOf(0) }

    val adaptive = LocalAdaptiveInfo.current
    val wide = adaptive.supportsTwoPane
    // A 700dp pane fits two rows side by side; one stretched row wastes half the width.
    val columns = if (wide) 2 else 1

    val backdropUrl = remember(character.media) {
        character.media.firstNotNullOfOrNull { it.bannerUrl }
    }
    // Exact from the API for a short list, exact from what is loaded once the last page is in, and
    // absent while AniList is only willing to say "500".
    val appearanceTotal = if (character.hasNextPage) {
        character.mediaTotal
    } else {
        character.media.size
    }

    val gutter = Modifier.padding(horizontal = 24.dp)
    val mediaTypeOptions = listOf(
        stringResource(R.string.all),
        stringResource(R.string.media_type_anime),
        stringResource(R.string.media_type_manga)
    )
    val sortOptions = listOf(
        stringResource(R.string.person_sort_popularity),
        stringResource(R.string.person_sort_newest),
        stringResource(R.string.person_sort_score)
    )
    val actorSortOptions = listOf(
        stringResource(R.string.person_sort_most_titles),
        stringResource(R.string.person_sort_name)
    )

    val appearances = remember(character.media, mainRolesOnly, mediaTypeIndex, sortIndex) {
        character.media
            .filter { !mainRolesOnly || it.characterRole.equals("MAIN", ignoreCase = true) }
            .filter {
                when (mediaTypeIndex) {
                    1 -> it.type == MediaType.ANIME
                    2 -> it.type == MediaType.MANGA
                    else -> true
                }
            }
            .sortedWith(
                when (sortIndex) {
                    1 -> compareByDescending<CharacterMedia> { it.startYear ?: 0 }
                    2 -> compareByDescending<CharacterMedia> { it.averageScore ?: 0 }
                    else -> compareByDescending<CharacterMedia> { it.popularity ?: 0 }
                }
            )
    }

    // How many of this character's titles each actor voices — the answer the old flat list of
    // faces could not give.
    val actorEntries = remember(character.media) {
        val counts = linkedMapOf<Int, Pair<VoiceActor, Int>>()
        character.media.forEach { media ->
            media.voiceActors.forEach { actor ->
                val existing = counts[actor.id]
                counts[actor.id] = if (existing == null) actor to 1 else existing.first to existing.second + 1
            }
        }
        counts.values.toList()
    }
    val allLanguagesLabel = stringResource(R.string.label_all_languages)
    val languages = remember(actorEntries, allLanguagesLabel) {
        listOf(allLanguagesLabel) + actorEntries.mapNotNull { it.first.language }.distinct().sorted()
    }
    val actors = remember(actorEntries, languageIndex, actorSortIndex, languages) {
        actorEntries
            .filter { languageIndex == 0 || it.first.language == languages.getOrNull(languageIndex) }
            .sortedWith(
                when (actorSortIndex) {
                    1 -> compareBy<Pair<VoiceActor, Int>> { it.first.nameFull }
                    else -> compareByDescending<Pair<VoiceActor, Int>> { it.second }
                }
            )
    }

    val facts = characterFacts(character)
    val tabs = listOf(
        PersonTab(stringResource(R.string.person_tab_appearances), appearanceTotal),
        PersonTab(
            stringResource(R.string.person_tab_voice_actors),
            // The cast is only whole once every appearance page is in.
            actorEntries.size.takeUnless { character.hasNextPage }
        )
    )
    val tab = if (selectedTab == 1) CharacterTab.VoiceActors else CharacterTab.Appearances

    val hero: @Composable () -> Unit = {
        PersonHero(
            imageUrl = character.imageUrl,
            backdropUrl = backdropUrl,
            name = character.getName(titleLanguage),
            nativeName = character.nativeName,
            metaLine = characterMetaLine(character, appearanceTotal),
            favourites = character.favourites,
            contentDescription = character.getName(titleLanguage),
            transitionKey = TransitionKeys.characterImage(character.id),
            onImageClick = { showImageViewer = true },
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    }

    val sidebar: @Composable ColumnScope.() -> Unit = {
        PersonFactsCard(facts = facts)
        if (character.alternativeNames.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            PersonNamesRow(
                count = character.alternativeNames.size,
                onClick = { showNamesSheet = true }
            )
        }
        if (!character.description.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            ExpandableBiography(html = character.description)
        }
    }

    val tabsBar: @Composable () -> Unit = {
        PersonTabs(
            tabs = tabs,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it }
        )
    }

    val listContent: LazyListScope.() -> Unit = {
        when (tab) {
            CharacterTab.Appearances -> {
                item(key = "appearance_filters") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(gutter)
                    ) {
                        PersonToggleChip(
                            label = stringResource(R.string.person_filter_main_roles),
                            selected = mainRolesOnly,
                            onToggle = { mainRolesOnly = !mainRolesOnly }
                        )
                        PersonDropdownChip(
                            label = mediaTypeOptions[mediaTypeIndex],
                            options = mediaTypeOptions,
                            selectedIndex = mediaTypeIndex,
                            onSelect = { mediaTypeIndex = it }
                        )
                        PersonDropdownChip(
                            label = sortOptions[sortIndex],
                            options = sortOptions,
                            selectedIndex = sortIndex,
                            onSelect = { sortIndex = it },
                            leadingIcon = Icons.Default.SwapVert,
                            appliedWhenNotDefault = false
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (appearances.isEmpty()) {
                    item(key = "appearances_empty") {
                        PersonEmptyState(
                            text = stringResource(R.string.person_empty_appearances),
                            modifier = gutter
                        )
                    }
                } else {
                    personGridItems(
                        items = appearances,
                        columns = columns,
                        key = { "appearance_${it.id}" },
                        rowModifier = gutter.padding(bottom = 12.dp)
                    ) { media ->
                        AppearanceRow(
                            mediaId = media.id,
                            coverUrl = media.coverUrl,
                            cover = media.cover,
                            title = media.getTitle(titleLanguage),
                            meta = appearanceMeta(media),
                            role = media.characterRole,
                            score = media.averageScore,
                            onClick = { onMediaClick(media.id) },
                            transitionPrefix = TransitionKeys.CHARACTER,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (character.hasNextPage) {
                        item(key = "appearances_footer") {
                            // Reaching the footer is the request: the list pages itself as it is
                            // scrolled rather than making the reader tap for row seven.
                            LaunchedEffect(appearances.size) { onLoadMore() }
                            PersonListFooter(modifier = gutter)
                        }
                    }
                }
            }

            CharacterTab.VoiceActors -> {
                item(key = "actor_filters") {
                    Column(modifier = gutter) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PersonDropdownChip(
                                label = languages[languageIndex],
                                options = languages,
                                selectedIndex = languageIndex,
                                onSelect = { languageIndex = it }
                            )
                            PersonDropdownChip(
                                label = actorSortOptions[actorSortIndex],
                                options = actorSortOptions,
                                selectedIndex = actorSortIndex,
                                onSelect = { actorSortIndex = it },
                                leadingIcon = Icons.Default.SwapVert,
                                appliedWhenNotDefault = false
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                if (actors.isEmpty()) {
                    item(key = "actors_empty") {
                        PersonEmptyState(
                            text = stringResource(R.string.person_empty_voice_actors),
                            modifier = gutter
                        )
                    }
                } else {
                    personGridItems(
                        items = actors,
                        columns = columns,
                        key = { "actor_${it.first.id}" },
                        rowModifier = gutter.padding(bottom = 12.dp)
                    ) { (actor, count) ->
                        VoiceActorRow(
                            name = actor.getName(titleLanguage),
                            nativeName = actor.nameNative,
                            language = actor.language,
                            titleCount = count,
                            imageUrl = actor.imageUrl,
                            onClick = { onStaffClick(actor.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (character.hasNextPage) {
                        item(key = "actors_footer") {
                            // The cast grows with the appearances it is derived from.
                            LaunchedEffect(actors.size) { onLoadMore() }
                            PersonListFooter(modifier = gutter)
                        }
                    }
                }
            }
        }
    }

    if (wide) {
        PersonWideLayout(
            backdropUrl = backdropUrl,
            backdropCredit = null,
            onBackClick = onBackClick,
            actions = chromeActions,
            portraitUrl = character.imageUrl,
            portraitTransitionKey = TransitionKeys.characterImage(character.id),
            onPortraitClick = { showImageViewer = true },
            name = character.getName(titleLanguage),
            nativeName = character.nativeName,
            metaLine = characterMetaLine(character, appearanceTotal),
            favourites = character.favourites,
            identityKey = character.id,
            aliasLine = null,
            identityContent = { sidebar() },
            tabs = tabsBar,
            listContent = listContent,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    } else {
        PersonScaffold(
            hero = hero,
            sidebar = sidebar,
            tabs = tabsBar,
            listContent = listContent
        )
    }

    if (showImageViewer && character.imageUrl != null) {
        ImageViewerDialog(
            imageUrls = listOf(character.imageUrl),
            initialIndex = 0,
            onDismiss = { showImageViewer = false }
        )
    }

    if (showNamesSheet) {
        val copyToClipboard = rememberCopyToClipboard()
        val clipLabel = stringResource(R.string.clip_label_character_name)
        val copiedMessage = stringResource(R.string.copied_name)
        AppModalBottomSheet(onDismissRequest = { showNamesSheet = false }) {
            PersonNamesSheetContent(
                subject = character.getName(titleLanguage),
                preferredName = character.nameUserPreferred,
                fullName = character.name,
                nativeName = character.nativeName,
                alternativeNames = character.alternativeNames,
                spoilerNames = character.spoilerNames,
                onCopy = { copyToClipboard(clipLabel, it, copiedMessage) },
                onCopyAll = {
                    val all = buildList {
                        add(character.nameUserPreferred)
                        if (character.name != character.nameUserPreferred) add(character.name)
                        character.nativeName?.let { add(it) }
                        addAll(character.alternativeNames)
                    }
                    copyToClipboard(clipLabel, all.joinToString("\n"), copiedMessage)
                }
            )
        }
    }
}

/**
 * Phone: hero, identity block and the tabbed list in one scrolling column. The wide layout lives in
 * [PersonWideLayout], which follows the profile screen's supporting-pane shape instead.
 */
@Composable
internal fun PersonScaffold(
    hero: @Composable () -> Unit,
    sidebar: @Composable ColumnScope.() -> Unit,
    tabs: @Composable () -> Unit,
    listContent: LazyListScope.() -> Unit
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomInset + 24.dp)
    ) {
        item(key = "hero") { hero() }
        item(key = "sidebar") {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(16.dp))
                sidebar()
                Spacer(Modifier.height(24.dp))
            }
        }
        item(key = "tabs") {
            Box(modifier = Modifier.padding(horizontal = 24.dp)) { tabs() }
            Spacer(Modifier.height(16.dp))
        }
        listContent()
    }
}

@Composable
private fun AliasLine(alternativeNames: List<String>, onClick: () -> Unit) {
    val shown = alternativeNames.take(3)
    val overflow = alternativeNames.size - shown.size
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = stringResource(R.string.also_known_as, shown.joinToString(" · ")),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (overflow > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.person_names_more, overflow),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun characterFacts(character: CharacterDetails): List<PersonFact> {
    val scheme = MaterialTheme.colorScheme
    return buildList {
        character.gender?.takeUnless { it.isBlank() || it == "?" }?.let {
            add(
                PersonFact(
                    stringResource(R.string.person_attr_gender),
                    it,
                    Icons.Default.Person,
                    scheme.primary
                )
            )
        }
        character.age?.takeUnless { it.isBlank() || it == "?" }?.let {
            add(
                PersonFact(
                    stringResource(R.string.person_attr_age),
                    it,
                    Icons.Default.HourglassBottom,
                    scheme.tertiary
                )
            )
        }
        character.bloodType?.takeUnless { it.isBlank() || it == "?" }?.let {
            add(
                PersonFact(
                    stringResource(R.string.person_attr_blood_type),
                    it,
                    Icons.Default.Bloodtype,
                    scheme.error
                )
            )
        }
        character.dateOfBirth?.takeUnless { it.isBlank() }?.let {
            add(
                PersonFact(
                    stringResource(R.string.person_attr_birthday),
                    formatPersonBirthday(it),
                    Icons.Default.Cake,
                    scheme.secondary
                )
            )
        }
    }
}

@Composable
private fun characterMetaLine(character: CharacterDetails, total: Int?): String {
    val mainCount = character.media.count { it.characterRole.equals("MAIN", ignoreCase = true) }
    val role = if (mainCount >= character.media.size - mainCount) {
        stringResource(R.string.person_meta_role_main)
    } else {
        stringResource(R.string.person_meta_role_supporting)
    }
    return if (total == null) {
        role
    } else {
        "$role · " + stringResource(R.string.person_meta_titles, total)
    }
}

private fun appearanceMeta(media: CharacterMedia): String = buildList {
    media.type?.name?.let { add(it) }
    media.startYear?.let { add(it.toString()) }
}.joinToString(" · ")

@Composable
private fun ErrorState(
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
