package com.anisync.android.presentation.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.CharacterDescriptionParser
import com.anisync.android.domain.CharacterDetails
import com.anisync.android.domain.VoiceActor
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.ImageViewerDialog
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.components.StaggeredAnimatedVisibility
import com.anisync.android.presentation.details.components.CharacterHeaderSection
import com.anisync.android.presentation.details.components.CharacterInfoSection
import com.anisync.android.presentation.details.components.CharacterSkeletonContent
import com.anisync.android.presentation.details.components.CharacterStatsCard
import com.anisync.android.presentation.details.components.ExpandableCharacterSynopsis
import com.anisync.android.presentation.details.components.MediaRoleItem
import com.anisync.android.presentation.details.components.NameSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Custom stagger delay for character details (faster reveal)
private const val CharacterStaggerDelay = 10

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun CharacterDetailsScreen(
    characterId: Int,
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit = {},
    onMediaSeeAllClick: (Int, String) -> Unit = { _, _ -> },
    viewModel: CharacterDetailsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // PERFORMANCE: derivedStateOf is correctly used here, kept as is.
    val isScrolled by remember {
        derivedStateOf { scrollBehavior.state.contentOffset < -50f }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // Extract name safely for the title
            val title = (uiState as? CharacterDetailsUiState.Success)?.details?.name ?: ""

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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
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
                        onMediaClick = onMediaClick,
                        onMediaSeeAllClick = {
                            onMediaSeeAllClick(
                                state.details.id,
                                state.details.name
                            )
                        },
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
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CharacterDetailsContent(
    character: CharacterDetails,
    onMediaClick: (Int) -> Unit = {},
    onMediaSeeAllClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    // ImageViewerDialog state
    var showImageViewer by rememberSaveable { mutableStateOf(false) }
    var imageViewerUrl by rememberSaveable { mutableStateOf<String?>(null) }

    // Extract colors for parser
    val spoilerBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val spoilerContentColor = MaterialTheme.colorScheme.onSurfaceVariant

    // PERFORMANCE OPTIMIZATION: Moved parsing off the Main Thread.
    // CharacterDescriptionParser likely uses Regex and String manipulation.
    // Doing this synchronously in 'remember' blocks the UI thread during the initial composition frame.
    val parsedDescriptionState by produceState(
        initialValue = ParsedDescription(emptyList(), AnnotatedString("")),
        key1 = character.description,
        key2 = spoilerBackgroundColor
    ) {
        // Switch to Default dispatcher for CPU intensive string parsing
        value = withContext(Dispatchers.Default) {
            val (attrs, bio) = CharacterDescriptionParser.parse(
                character.description,
                spoilerBackgroundColor,
                spoilerContentColor
            )
            ParsedDescription(attrs, bio)
        }
    }

    val attributes = parsedDescriptionState.attributes
    val bio = parsedDescriptionState.bio


    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding() + 24.dp
            )
        ) {
            item {
                CharacterHeaderSection(
                    character = character,
                    onPosterClick = {
                        imageViewerUrl = character.imageUrl
                        showImageViewer = true
                    },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    // Name Section (stagger index 0)
                    StaggeredAnimatedVisibility(
                        key = "char_name",
                        index = 0,
                        delayPerItem = CharacterStaggerDelay
                    ) {
                        NameSection(character)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Stats (stagger index 1)
                    StaggeredAnimatedVisibility(
                        key = "char_stats",
                        index = 1,
                        delayPerItem = CharacterStaggerDelay
                    ) {
                        CharacterStatsCard(character, attributes)
                    }

                    // Attributes Table (stagger index 2)
                    // Filter out stats that are already shown in the StatsCard
                    val displayAttributes = remember(attributes) {
                        attributes.filterNot { (key, _) ->
                            key.equals("Age", ignoreCase = true) ||
                                    key.equals("Gender", ignoreCase = true) ||
                                    key.contains("Blood", ignoreCase = true)
                        }
                    }

                    if (displayAttributes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        StaggeredAnimatedVisibility(
                            key = "char_info",
                            index = 2,
                            delayPerItem = CharacterStaggerDelay
                        ) {
                            CharacterInfoSection(displayAttributes)
                        }
                    }
                }
            }

            // Biography (stagger index 3)
            if (bio.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    StaggeredAnimatedVisibility(
                        key = "char_bio",
                        index = 3,
                        delayPerItem = CharacterStaggerDelay
                    ) {
                        Column {
                            SectionHeader(title = "Biography", level = HeaderLevel.Section)
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                ExpandableCharacterSynopsis(bio)
                            }
                        }
                    }
                }
            }

            if (character.media.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    StaggeredAnimatedVisibility(
                        key = "char_va",
                        index = 4,
                        delayPerItem = CharacterStaggerDelay
                    ) {
                        VoiceActorsSection(
                            media = character.media,
                            onAvatarClick = { avatarUrl ->
                                imageViewerUrl = avatarUrl
                                showImageViewer = true
                            }
                        )
                    }
                }
            }

            // Media Roles (stagger index 5)
            if (character.media.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    StaggeredAnimatedVisibility(
                        key = "char_media",
                        index = 5,
                        delayPerItem = CharacterStaggerDelay
                    ) {
                        Column {
                            SectionHeader(
                                title = "Appears In",
                                level = HeaderLevel.Section,
                                onActionClick = if (character.media.size > 10) onMediaSeeAllClick else null
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.height(180.dp)
                            ) {
                                items(
                                    items = character.media.distinctBy { it.id }.take(10),
                                    key = { it.id }
                                ) { media ->
                                    MediaRoleItem(
                                        media = media,
                                        onClick = { onMediaClick(media.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ImageViewerDialog
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

// Helper data class for state hoisting
private data class ParsedDescription(
    val attributes: List<Pair<String, String>>,
    val bio: AnnotatedString
)

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
                text = "Oops!",
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
                Text("Retry")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onBackClick) {
                Text("Go Back")
            }
        }
    }
}

@Composable
private fun VoiceActorsSection(
    media: List<com.anisync.android.domain.CharacterMedia>,
    onAvatarClick: (String) -> Unit = {}
) {
    // Extract unique voice actors
    val voiceActors = remember(media) {
        media.mapNotNull { it.voiceActor }
            .distinctBy { it.id }
            .take(10)
    }

    if (voiceActors.isEmpty()) return

    Column {
        SectionHeader(title = stringResource(R.string.section_voice_actors), level = HeaderLevel.Section)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = voiceActors,
                key = { it.id }
            ) { va ->
                VoiceActorChip(
                    voiceActor = va,
                    onAvatarClick = { va.imageUrl?.let(onAvatarClick) }
                )
            }
        }
    }
}

@Composable
private fun VoiceActorChip(
    voiceActor: VoiceActor,
    onAvatarClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        // Circular avatar
        AsyncImage(
            model = voiceActor.imageUrl,
            contentDescription = voiceActor.nameUserPreferred,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onAvatarClick)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = voiceActor.nameUserPreferred,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        voiceActor.nameNative?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
