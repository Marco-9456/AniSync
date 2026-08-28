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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.StaffDetails
import com.anisync.android.domain.StaffProductionMedia
import com.anisync.android.domain.VoicedCharacter
import com.anisync.android.presentation.components.AnimatedFavoriteButton
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.components.ImageViewerDialog
import com.anisync.android.presentation.details.components.CharacterSkeletonContent
import com.anisync.android.presentation.details.components.CreditRow
import com.anisync.android.presentation.details.components.ExpandableBiography
import com.anisync.android.presentation.details.components.PersonCharacterRow
import com.anisync.android.presentation.details.components.PersonDropdownChip
import com.anisync.android.presentation.details.components.PersonEmptyState
import com.anisync.android.presentation.details.components.PersonFact
import com.anisync.android.presentation.details.components.PersonFactsCard
import com.anisync.android.presentation.details.components.PersonHero
import com.anisync.android.presentation.details.components.PersonNamesRow
import com.anisync.android.presentation.details.components.PersonNamesSheetContent
import com.anisync.android.presentation.details.components.PersonNoteStrip
import com.anisync.android.presentation.details.components.PersonListFooter
import com.anisync.android.presentation.details.components.PersonTab
import com.anisync.android.presentation.details.components.PersonTabs
import com.anisync.android.presentation.details.components.personGridItems
import com.anisync.android.presentation.share.ShareImageSheet
import com.anisync.android.presentation.share.StaffShareCard
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.LocalPaneIsRoot
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.rememberCopyToClipboard
import com.anisync.android.util.AniListUrls
import com.anisync.android.util.getName
import com.anisync.android.util.getTitle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun StaffDetailsScreen(
    staffId: Int,
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit = {},
    onCharacterClick: (Int) -> Unit = {},
    viewModel: StaffDetailsViewModel = hiltViewModel(),
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
    val details = (uiState as? StaffDetailsUiState.Success)?.details
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
                is StaffDetailsUiState.Loading -> {
                    CharacterSkeletonContent(onBackClick = onBackClick)
                }

                is StaffDetailsUiState.Success -> {
                    StaffDetailsContent(
                        staff = state.details,
                        titleLanguage = titleLanguage,
                        onMediaClick = onMediaClick,
                        onCharacterClick = onCharacterClick,
                        isLoadingMore = state.isLoadingMore,
                        onLoadMoreCharacters = viewModel::loadMoreMedia,
                        onLoadMoreCredits = viewModel::loadMoreProductionMedia,
                        onBackClick = onBackClick,
                        chromeActions = chromeActions,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }

                is StaffDetailsUiState.Error -> {
                    StaffErrorState(
                        message = state.message,
                        onRetry = viewModel::loadStaffDetails,
                        onBackClick = onBackClick
                    )
                }
            }
        }

        if (showShareSheet) {
            details?.let {
                ShareImageSheet(
                    onDismiss = { showShareSheet = false },
                    link = AniListUrls.staffUrl(it.id)
                ) {
                    StaffShareCard(
                        details = it,
                        displayName = it.getName(titleLanguage)
                    )
                }
            }
        }
    }
}

private enum class StaffTab { Characters, Credits }

/** How many rows a tab previews before handing off to the full grid. */
private const val StaffPreviewCount = 6

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun StaffDetailsContent(
    staff: StaffDetails,
    titleLanguage: TitleLanguage,
    onMediaClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    isLoadingMore: Boolean,
    onLoadMoreCharacters: () -> Unit,
    onLoadMoreCredits: () -> Unit,
    onBackClick: () -> Unit,
    chromeActions: @Composable RowScope.() -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    var showImageViewer by rememberSaveable { mutableStateOf(false) }
    var showNamesSheet by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var characterSortIndex by rememberSaveable { mutableIntStateOf(0) }
    var roleIndex by rememberSaveable { mutableIntStateOf(0) }
    var creditSortIndex by rememberSaveable { mutableIntStateOf(0) }


    val adaptive = LocalAdaptiveInfo.current
    val wide = adaptive.supportsTwoPane
    val columns = if (wide) 2 else 1
    val gutter = Modifier.padding(horizontal = 24.dp)

    // A staff member has no banner of their own, so the hero borrows the one from the title they
    // are best known for and says so — rather than blurring their portrait into a backdrop.
    // Voiced work first: for a voice actor the show people know them from is a role, not a
    // production credit. Directors and composers have no voiced characters and fall through.
    val backdrop = remember(staff.productionMedia, staff.voicedCharacters) {
        staff.voicedCharacters
            .flatMap { it.mediaAppearances }
            .firstOrNull { it.bannerUrl != null }
            ?.let { it.bannerUrl to it.mediaTitle }
            ?: staff.productionMedia
                .firstOrNull { it.bannerUrl != null }
                ?.let { it.bannerUrl to it.titleUserPreferred }
    }

    // Same rule as the character screen: show a count only when it is one. AniList caps these
    // totals at 500, which is why every voice actor used to claim exactly 500 roles.
    val charactersTotal = if (staff.hasNextPage) {
        staff.charactersTotal
    } else {
        staff.voicedCharacters.size
    }
    val creditsTotal = if (staff.productionMediaHasNextPage) {
        staff.productionTotal
    } else {
        staff.productionMedia.size
    }

    val characterSortOptions = listOf(
        stringResource(R.string.person_sort_favourites),
        stringResource(R.string.person_sort_name)
    )
    val roleOptions = listOf(
        stringResource(R.string.person_filter_any_role),
        stringResource(R.string.person_meta_role_main),
        stringResource(R.string.person_filter_supporting)
    )
    val creditSortOptions = listOf(
        stringResource(R.string.person_sort_popularity),
        stringResource(R.string.person_sort_newest)
    )

    val characters = remember(staff.voicedCharacters, characterSortIndex, roleIndex) {
        staff.voicedCharacters
            .filter { character ->
                when (roleIndex) {
                    1 -> character.mediaAppearances.any { it.characterRole.equals("MAIN", true) }
                    2 -> character.mediaAppearances.any {
                        it.characterRole.equals("SUPPORTING", true)
                    }
                    else -> true
                }
            }
            .let { list ->
                if (characterSortIndex == 1) {
                    list.sortedBy { it.characterName }
                } else {
                    list
                }
            }
    }

    val credits = remember(staff.productionMedia, creditSortIndex) {
        staff.productionMedia.sortedWith(
            when (creditSortIndex) {
                1 -> compareByDescending<StaffProductionMedia> { it.startYear ?: 0 }
                else -> compareByDescending<StaffProductionMedia> { it.popularity ?: 0 }
            }
        )
    }

    val tabs = listOf(
        PersonTab(stringResource(R.string.person_tab_characters), charactersTotal),
        PersonTab(stringResource(R.string.person_tab_credits), creditsTotal)
    )
    val tab = if (selectedTab == 1) StaffTab.Credits else StaffTab.Characters
    val facts = staffFacts(staff)

    val hero: @Composable () -> Unit = {
        PersonHero(
            imageUrl = staff.imageUrl,
            backdropUrl = backdrop?.first,
            name = staff.getName(titleLanguage),
            nativeName = staff.nativeName,
            metaLine = staffMetaLine(staff),
            favourites = staff.favourites,
            contentDescription = staff.getName(titleLanguage),
            transitionKey = TransitionKeys.staffImage(staff.id),
            onImageClick = { showImageViewer = true },
            backdropCredit = backdrop?.second,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    }

    val sidebar: @Composable ColumnScope.() -> Unit = {
        PersonFactsCard(facts = facts)
        if (staff.alternativeNames.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            PersonNamesRow(
                count = staff.alternativeNames.size,
                onClick = { showNamesSheet = true }
            )
        }
        if (!staff.description.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            ExpandableBiography(html = staff.description)
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
            StaffTab.Characters -> {
                item(key = "character_filters") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(gutter)
                    ) {
                        PersonDropdownChip(
                            label = characterSortOptions[characterSortIndex],
                            options = characterSortOptions,
                            selectedIndex = characterSortIndex,
                            onSelect = { characterSortIndex = it },
                            leadingIcon = Icons.Default.SwapVert,
                            appliedWhenNotDefault = false
                        )
                        PersonDropdownChip(
                            label = roleOptions[roleIndex],
                            options = roleOptions,
                            selectedIndex = roleIndex,
                            onSelect = { roleIndex = it }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (characters.isEmpty()) {
                    item(key = "characters_empty") {
                        PersonEmptyState(
                            text = stringResource(R.string.person_empty_characters),
                            modifier = gutter
                        )
                    }
                } else {
                    personGridItems(
                        items = characters,
                        columns = columns,
                        key = { "character_${it.characterId}" },
                        rowModifier = gutter.padding(bottom = 12.dp)
                    ) { character ->
                        val primary = character.mediaAppearances.firstOrNull()
                        PersonCharacterRow(
                            name = character.getName(titleLanguage),
                            nativeName = character.characterNameNative,
                            imageUrl = character.characterImageUrl,
                            role = primary?.characterRole,
                            primaryTitle = primary?.getTitle(titleLanguage),
                            otherTitles = (character.mediaAppearances.size - 1).coerceAtLeast(0),
                            onClick = { onCharacterClick(character.characterId) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (staff.hasNextPage) {
                        item(key = "characters_footer") {
                            LaunchedEffect(characters.size) { onLoadMoreCharacters() }
                            PersonListFooter(modifier = gutter)
                        }
                    }
                }
            }

            StaffTab.Credits -> {
                item(key = "credit_filters") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(gutter)
                    ) {
                        PersonDropdownChip(
                            label = creditSortOptions[creditSortIndex],
                            options = creditSortOptions,
                            selectedIndex = creditSortIndex,
                            onSelect = { creditSortIndex = it },
                            leadingIcon = Icons.Default.SwapVert,
                            appliedWhenNotDefault = false
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (credits.isEmpty()) {
                    item(key = "credits_empty") {
                        PersonEmptyState(
                            text = stringResource(R.string.person_empty_credits),
                            modifier = gutter
                        )
                    }
                } else {
                    personGridItems(
                        items = credits,
                        columns = columns,
                        key = { "credit_${it.mediaId}_${it.staffRole.orEmpty()}" },
                        rowModifier = gutter.padding(bottom = 12.dp)
                    ) { credit ->
                        CreditRow(
                            title = credit.getTitle(titleLanguage),
                            role = credit.staffRole,
                            meta = creditMeta(credit),
                            coverUrl = credit.coverUrl,
                            cover = credit.cover,
                            onClick = { onMediaClick(credit.mediaId) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (staff.productionMediaHasNextPage) {
                        item(key = "credits_footer") {
                            LaunchedEffect(credits.size) { onLoadMoreCredits() }
                            PersonListFooter(modifier = gutter)
                        }
                    }
                }

                // A voice actor's credit list is short by definition; without this the tab reads as
                // though the app lost their career.
                if (staff.voicedCharacters.isNotEmpty()) {
                    item(key = "credits_note") {
                        Spacer(Modifier.height(4.dp))
                        PersonNoteStrip(
                            text = stringResource(R.string.person_credits_note_plain),
                            icon = Icons.Default.Info,
                            modifier = gutter
                        )
                    }
                }
            }
        }
    }

    if (wide) {
        PersonWideLayout(
            backdropUrl = backdrop?.first,
            backdropCredit = backdrop?.second,
            onBackClick = onBackClick,
            actions = chromeActions,
            portraitUrl = staff.imageUrl,
            portraitTransitionKey = TransitionKeys.staffImage(staff.id),
            onPortraitClick = { showImageViewer = true },
            name = staff.getName(titleLanguage),
            nativeName = staff.nativeName,
            metaLine = staffMetaLine(staff),
            favourites = staff.favourites,
            identityKey = staff.id,
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

    if (showImageViewer && staff.imageUrl != null) {
        ImageViewerDialog(
            imageUrls = listOf(staff.imageUrl),
            initialIndex = 0,
            onDismiss = { showImageViewer = false }
        )
    }

    if (showNamesSheet) {
        val copyToClipboard = rememberCopyToClipboard()
        val clipLabel = stringResource(R.string.clip_label_staff_name)
        val copiedMessage = stringResource(R.string.copied_name)
        AppModalBottomSheet(onDismissRequest = { showNamesSheet = false }) {
            PersonNamesSheetContent(
                subject = staff.getName(titleLanguage),
                preferredName = staff.nameUserPreferred,
                fullName = staff.name,
                nativeName = staff.nativeName,
                alternativeNames = staff.alternativeNames,
                spoilerNames = emptyList(),
                onCopy = { copyToClipboard(clipLabel, it, copiedMessage) },
                onCopyAll = {
                    val all = buildList {
                        add(staff.nameUserPreferred)
                        if (staff.name != staff.nameUserPreferred) add(staff.name)
                        staff.nativeName?.let { add(it) }
                        addAll(staff.alternativeNames)
                    }
                    copyToClipboard(clipLabel, all.joinToString("\n"), copiedMessage)
                }
            )
        }
    }
}

@Composable
private fun StaffAliasLine(alternativeNames: List<String>, onClick: () -> Unit) {
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
private fun staffFacts(staff: StaffDetails): List<PersonFact> {
    val scheme = MaterialTheme.colorScheme
    return buildList {
        staff.age?.let {
            add(
                PersonFact(
                    stringResource(R.string.person_attr_age),
                    it.toString(),
                    Icons.Default.HourglassBottom,
                    scheme.tertiary
                )
            )
        }
        staff.gender?.takeUnless { it.isBlank() }?.let {
            add(
                PersonFact(
                    stringResource(R.string.person_attr_gender),
                    it,
                    Icons.Default.Person,
                    scheme.primary
                )
            )
        }
        staff.bloodType?.takeUnless { it.isBlank() }?.let {
            add(
                PersonFact(
                    stringResource(R.string.person_attr_blood_type),
                    it,
                    Icons.Default.Bloodtype,
                    scheme.error
                )
            )
        }
        staff.dateOfBirth?.takeUnless { it.isBlank() }?.let {
            add(
                PersonFact(
                    stringResource(R.string.person_attr_birthday),
                    it,
                    Icons.Default.Cake,
                    scheme.secondary
                )
            )
        }
        staff.dateOfDeath?.takeUnless { it.isBlank() }?.let {
            add(
                PersonFact(
                    stringResource(R.string.person_attr_date_of_death),
                    it,
                    Icons.Default.Event,
                    scheme.onSurfaceVariant
                )
            )
        }
        staff.homeTown?.takeUnless { it.isBlank() }?.let {
            add(
                PersonFact(
                    stringResource(R.string.person_attr_hometown),
                    it,
                    Icons.Default.Place,
                    scheme.primary
                )
            )
        }
        if (staff.yearsActive.isNotEmpty()) {
            val label = if (staff.yearsActive.size >= 2) {
                stringResource(R.string.person_attr_years_active)
            } else {
                stringResource(R.string.person_attr_active_since)
            }
            val value = if (staff.yearsActive.size >= 2) {
                "${staff.yearsActive[0]} – ${staff.yearsActive[1]}"
            } else {
                staff.yearsActive[0].toString()
            }
            add(PersonFact(label, value, Icons.Default.Update, scheme.tertiary))
        }
        staff.primaryOccupations.firstOrNull()?.let {
            add(
                PersonFact(
                    stringResource(R.string.person_attr_occupation),
                    staff.primaryOccupations.joinToString(", "),
                    Icons.Default.Work,
                    scheme.secondary
                )
            )
        }
        staff.language?.takeUnless { it.isBlank() }?.let {
            add(
                PersonFact(
                    stringResource(R.string.person_attr_language),
                    it,
                    Icons.Default.Translate,
                    scheme.primary
                )
            )
        }
    }
}

@Composable
private fun staffMetaLine(staff: StaffDetails): String = buildList {
    staff.primaryOccupations.firstOrNull()?.let { add(it) }
        ?: add(stringResource(R.string.person_meta_voice_actor))
    staff.language?.takeUnless { it.isBlank() }?.let { add(it) }
}.joinToString(" · ")

private fun creditMeta(credit: StaffProductionMedia): String = buildList {
    credit.type?.name?.let { add(it) }
    credit.startYear?.let { add(it.toString()) }
}.joinToString(" · ")

@Composable
private fun StaffErrorState(
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
