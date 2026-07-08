package com.anisync.android.presentation.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import com.anisync.android.presentation.util.LocalPaneIsRoot
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.CharacterDetails
import com.anisync.android.domain.CharacterMedia
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.ImageViewerDialog
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.details.components.AttributesCard
import com.anisync.android.presentation.details.components.CharacterSkeletonContent
import com.anisync.android.presentation.details.components.DetailHeroImage
import com.anisync.android.presentation.details.components.ExpandableBiography
import com.anisync.android.presentation.details.components.FeaturedMediaItem
import com.anisync.android.presentation.details.components.NameCard
import com.anisync.android.presentation.details.components.VoiceActorCard
import com.anisync.android.presentation.share.CharacterShareCard
import com.anisync.android.presentation.share.ShareImageSheet
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.util.AniListUrls
import com.anisync.android.util.getName
import com.anisync.android.util.getTitle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun CharacterDetailsScreen(
    characterId: Int,
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit = {},
    onMediaSeeAllClick: (Int, String) -> Unit = { _, _ -> },
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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            val title = (uiState as? CharacterDetailsUiState.Success)?.details
                ?.getName(titleLanguage) ?: ""

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
                                tint = androidx.compose.animation.animateColorAsState(
                                    if (isScrolled) MaterialTheme.colorScheme.onSurface else Color.White,
                                    label = "navIconTint"
                                ).value
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showShareSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.cd_share),
                            tint = androidx.compose.animation.animateColorAsState(
                                if (isScrolled) MaterialTheme.colorScheme.onSurface else Color.White,
                                label = "actionIconTint"
                            ).value
                        )
                    }
                    // At a two-pane detail root the close (✕) is the trailing-most action (right-thumb
                    // reach) in place of a leading back arrow.
                    if (LocalPaneIsRoot.current) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.pane_close),
                                tint = androidx.compose.animation.animateColorAsState(
                                    if (isScrolled) MaterialTheme.colorScheme.onSurface else Color.White,
                                    label = "closeIconTint"
                                ).value
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
                        onMediaSeeAllClick = {
                            onMediaSeeAllClick(state.details.id, state.details.getName(titleLanguage))
                        },
                        onStaffClick = onStaffClick,
                        onFavouriteClick = viewModel::toggleFavourite,
                        transitionPrefix = TransitionKeys.CHARACTER,
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
            (uiState as? CharacterDetailsUiState.Success)?.details?.let { details ->
                ShareImageSheet(
                    onDismiss = { showShareSheet = false },
                    link = AniListUrls.characterUrl(details.id)
                ) {
                    CharacterShareCard(
                        details = details,
                        displayName = details.getName(titleLanguage)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CharacterDetailsContent(
    character: CharacterDetails,
    titleLanguage: com.anisync.android.data.TitleLanguage,
    onMediaClick: (Int) -> Unit,
    onMediaSeeAllClick: () -> Unit,
    onStaffClick: (Int) -> Unit,
    onFavouriteClick: () -> Unit,
    transitionPrefix: String,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    var showImageViewer by rememberSaveable { mutableStateOf(false) }
    var imageViewerUrl by rememberSaveable { mutableStateOf<String?>(null) }

    val seriesBackdropUrl = remember(character.media) {
        character.media.firstOrNull()?.bannerUrl
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
        )
    ) {
        // Hero Image
        item(key = "hero") {
            DetailHeroImage(
                imageUrl = character.imageUrl,
                contentDescription = character.getName(titleLanguage),
                id = character.id,
                onImageClick = {
                    imageViewerUrl = character.imageUrl
                    showImageViewer = true
                },
                backdropUrl = seriesBackdropUrl,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }

        // Name Card
        item(key = "name") {
            Spacer(modifier = Modifier.height(12.dp))
            NameCard(
                name = character.getName(titleLanguage),
                nativeName = character.nativeName,
                alternativeNames = character.alternativeNames,
                favourites = character.favourites,
                isFavourite = character.isFavourite,
                onFavouriteClick = onFavouriteClick
            )
        }

        // Button Group
        item(key = "tabs") {
            Spacer(modifier = Modifier.height(16.dp))
            val uniqueVoiceActorsCount = remember(character.media) {
                character.media.flatMap { it.voiceActors }.distinctBy { it.id }.size
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                ToggleButton(
                    checked = pagerState.currentPage == 0,
                    onCheckedChange = {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    },
                    modifier = Modifier.weight(1f),
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.label_character),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ToggleButton(
                    checked = pagerState.currentPage == 1,
                    onCheckedChange = {
                        scope.launch { pagerState.animateScrollToPage(1) }
                    },
                    modifier = Modifier.weight(1f),
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.section_voice_actors),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (uniqueVoiceActorsCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = uniqueVoiceActorsCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Pager content
        item(key = "pager_content") {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> CharacterTabContent(
                        character = character,
                        titleLanguage = titleLanguage,
                        onMediaClick = onMediaClick,
                        onMediaSeeAllClick = onMediaSeeAllClick,
                        transitionPrefix = transitionPrefix,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )

                    1 -> VoiceActorsTabContent(
                        media = character.media,
                        titleLanguage = titleLanguage,
                        onStaffClick = onStaffClick
                    )
                }
            }
        }
    }

    if (showImageViewer && imageViewerUrl != null) {
        ImageViewerDialog(
            imageUrls = listOf(imageViewerUrl!!),
            initialIndex = 0,
            onDismiss = {
                showImageViewer = false
                imageViewerUrl = null
            }
        )
    }
}

@Composable
private fun CharacterTabContent(
    character: CharacterDetails,
    titleLanguage: com.anisync.android.data.TitleLanguage,
    onMediaClick: (Int) -> Unit,
    onMediaSeeAllClick: () -> Unit,
    transitionPrefix: String,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    // Build attributes list from structured API fields
    val months = listOf(
        "",
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December"
    )
    val displayAttributes = remember(character) {
        buildList {
            character.dateOfBirth?.let { dob ->
                val parts = dob.split("/")
                val formatted = if (parts.size >= 2) {
                    val month = parts[0].toIntOrNull()
                    val day = parts[1].toIntOrNull()
                    if (month != null && month in 1..12 && day != null) {
                        "${months[month]} $day"
                    } else dob
                } else dob
                add("Birthday" to formatted)
            }
            character.age?.takeUnless { it.isEmpty() || it == "?" }?.let {
                add("Age" to it)
            }
            character.gender?.takeUnless { it.isEmpty() || it == "?" }?.let {
                add("Gender" to it)
            }
            character.bloodType?.takeUnless { it.isEmpty() || it == "?" }?.let {
                add("Blood Type" to it)
            }
        }
    }

    val previewMedia = remember(character.media) {
        character.media.distinctBy { it.id }.take(10)
    }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        // Biography - expandable box with rich text renderer
        if (!character.description.isNullOrBlank()) {
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                ExpandableBiography(html = character.description)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Attributes
        if (displayAttributes.isNotEmpty()) {
            SectionHeader(
                title = stringResource(R.string.label_attributes),
                level = HeaderLevel.Section,
                iconColor = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                AttributesCard(attributes = displayAttributes)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Featured Media (preview with See All)
        if (previewMedia.isNotEmpty()) {
            Column {
                SectionHeader(
                    title = stringResource(R.string.featured_media),
                    level = HeaderLevel.Section,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onActionClick = if (character.media.size > 10) onMediaSeeAllClick else null
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        previewMedia,
                        key = { index, media -> "char_preview_${media.id}_$index" }
                    ) { _, media ->
                        FeaturedMediaItem(
                            mediaId = media.id,
                            coverUrl = media.coverUrl,
                            title = media.getTitle(titleLanguage),
                            role = media.characterRole,
                            year = media.startYear,
                            type = media.type?.name,
                            onClick = { onMediaClick(media.id) },
                            transitionPrefix = transitionPrefix,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceActorsTabContent(
    media: List<CharacterMedia>,
    titleLanguage: com.anisync.android.data.TitleLanguage,
    onStaffClick: (Int) -> Unit
) {
    val allVoiceActors = remember(media) {
        media.flatMap { it.voiceActors }
            .distinctBy { it.id }
    }

    val allLanguagesLabel = stringResource(R.string.label_all_languages)
    val languages = remember(allVoiceActors, allLanguagesLabel) {
        listOf(allLanguagesLabel) + allVoiceActors.mapNotNull { it.language }.distinct().sorted()
    }

    var selectedLanguageIndex by rememberSaveable { mutableIntStateOf(0) }
    var sortAscending by rememberSaveable { mutableStateOf(true) }

    val filteredActors = remember(allVoiceActors, selectedLanguageIndex, sortAscending) {
        val filtered = if (selectedLanguageIndex == 0) {
            allVoiceActors
        } else {
            val lang = languages[selectedLanguageIndex]
            allVoiceActors.filter { it.language == lang }
        }
        if (sortAscending) {
            filtered.sortedBy { it.nameFull }
        } else {
            filtered.sortedByDescending { it.nameFull }
        }
    }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        SectionHeader(
            title = "Voice Actors",
            level = HeaderLevel.Section,
            iconColor = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Filters
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortDropdown(
                options = languages,
                selectedIndex = selectedLanguageIndex,
                onSelected = { selectedLanguageIndex = it }
            )
            SortDropdown(
                options = listOf("Name (A-Z)", "Name (Z-A)"),
                selectedIndex = if (sortAscending) 0 else 1,
                onSelected = { sortAscending = it == 0 }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Voice actors grid
        val rows = filteredActors.chunked(3)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { va ->
                    VoiceActorCard(
                        name = va.getName(titleLanguage),
                        language = va.language,
                        imageUrl = va.imageUrl,
                        onClick = { onStaffClick(va.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SortDropdown(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        androidx.compose.material3.FilterChip(
            selected = false,
            onClick = { expanded = true },
            label = {
                Text(
                    text = options[selectedIndex],
                    style = MaterialTheme.typography.labelSmall
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
        com.anisync.android.presentation.components.menu.Menu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, option ->
                item(
                    text = option,
                    selected = index == selectedIndex,
                    onClick = {
                        onSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

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
