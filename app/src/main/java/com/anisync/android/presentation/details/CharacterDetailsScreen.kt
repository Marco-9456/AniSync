package com.anisync.android.presentation.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.domain.CharacterDetails
import com.anisync.android.domain.CharacterMedia
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.util.shimmerEffect
import kotlinx.coroutines.delay

// Stagger delay constant for content reveal animations
private const val StaggerDelayPerItem = 40

/**
 * Staggered animation helper for content sections.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StaggeredAnimatedVisibility(
    index: Int,
    delayPerItem: Int = StaggerDelayPerItem,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index * delayPerItem).toLong())
        visible = true
    }

    // Use spring physics for both fade and slide
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = effectsSpec) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = spatialSpec
        )
    ) {
        content()
    }
}

@Composable
fun CharacterDetailsScreen(
    characterId: Int,
    onBackClick: () -> Unit,
    viewModel: CharacterDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
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
                        onBackClick = onBackClick
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

@Composable
private fun CharacterDetailsContent(
    character: CharacterDetails,
    onBackClick: () -> Unit
) {
    val listState = rememberLazyListState()

    // Parse description into Key-Values (attributes) and Body (bio)
    val (attributes, bio) = remember(character.description) {
        parseCharacterDescription(character.description)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp)
        ) {
            item {
                CharacterHeaderSection(character, onBackClick)
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    // Name Section (stagger index 0)
                    StaggeredAnimatedVisibility(index = 0) {
                        NameSection(character)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Stats (stagger index 1)
                    StaggeredAnimatedVisibility(index = 1) {
                        CharacterStatsCard(character)
                    }

                    // Attributes Table (stagger index 2)
                    if (attributes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        StaggeredAnimatedVisibility(index = 2) {
                            CharacterInfoSection(attributes)
                        }
                    }
                }
            }

            // Biography (stagger index 3)
            // Moved out of the previous column item to align properly with other sections
            if (bio.isNotBlank()) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    StaggeredAnimatedVisibility(index = 3) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionHeader(title = "Biography", level = HeaderLevel.Section)
                            Spacer(modifier = Modifier.height(12.dp))
                            ExpandableCharacterSynopsis(bio)
                        }
                    }
                }
            }

            // Media Roles (stagger index 4)
            if (character.media.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    StaggeredAnimatedVisibility(index = 4) {
                        Column {
                            // Using standard SectionHeader for consistency
                            SectionHeader(title = "Appears In", level = HeaderLevel.Section)
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(character.media) { media ->
                                    MediaRoleItem(media)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Parses markdown-like description into Attributes (Height: 170cm) and Bio text.
 * Handles formats like "__Height:__ 172cm" or "**Affiliations** Straw Hats".
 */
private fun parseCharacterDescription(description: String?): Pair<List<Pair<String, String>>, String> {
    if (description.isNullOrBlank()) return emptyList<Pair<String, String>>() to ""

    val attributes = mutableListOf<Pair<String, String>>()
    val bioLines = mutableListOf<String>()

    // Normalize breaks
    val normalized = description.replace(Regex("<br\\s*/?>"), "\n")

    // Extended Regex to catch lines starting with bold keys or simple "Key: Value" patterns
    // Catch: "__Key__ Value", "**Key** Value", "Key: Value" (if line is short-ish)
    // Updated to allow () in keys for cases like "Devil Fruit (Type):"
    val attributeRegex = Regex("^(__|\\*\\*)?([a-zA-Z0-9\\s\\-_()]+?)(:|\\1)\\s*(.*)")

    normalized.lines().forEach { line ->
        val trimLine = line.trim()
        if (trimLine.isBlank()) return@forEach

        val match = attributeRegex.find(trimLine)
        var isAttribute = false

        if (match != null) {
            val key = match.groupValues[2].trim()
            val value = match.groupValues[4].trim()

            // Heuristic: It's an attribute if:
            // 1. It explicitly used bold markers (__ or **)
            // 2. OR the line is reasonably short AND contains a colon
            val hasBoldMarkers = match.groupValues[1].isNotEmpty()
            // Increased length limit to 200 to catch long attribute values (e.g. descriptions)
            // Increased key length limit to 50
            val isShortAndHasColon = trimLine.length < 200 && match.groupValues[3] == ":"

            if ((hasBoldMarkers || isShortAndHasColon) && key.length < 50 && value.isNotEmpty()) {
                // Clean spoiler tags and markdown from value
                val cleanValue = value
                    .replace("~!", "")
                    .replace("!~", "")
                    .replace("__", "")
                    .replace("**", "")

                attributes.add(key to cleanValue)
                isAttribute = true
            }
        }

        if (!isAttribute) {
            // It's a bio line. Clean it up.
            val cleanLine = trimLine
                .replace("~!", "")
                .replace("!~", "")
                .replace("__", "") // Remove bold markers from normal text
                .replace("**", "")

            bioLines.add(cleanLine)
        }
    }

    // Drop empty lines at start of bio
    val bio = bioLines.dropWhile { it.isBlank() }.joinToString("\n")
    return attributes to bio
}

@Composable
fun CharacterInfoSection(attributes: List<Pair<String, String>>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            attributes.forEachIndexed { index, (key, value) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.4f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.6f)
                        )
                    }
                }
                if (index < attributes.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun CharacterHeaderSection(
    character: CharacterDetails,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        // Banner Background (Using character image cropped/zoomed as banner)
        AsyncImage(
            model = character.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        // Gradient Scrim (Top)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Gradient Scrim (Bottom blend)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
                .offset(y = 120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Hard cut gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .offset(y = 140.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // Back Button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                    start = 8.dp
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Cover Image (Poster)
        Card(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp)
                .width(130.dp)
                .height(190.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(character.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun NameSection(character: CharacterDetails) {
    Column {
        Text(
            text = character.name,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        character.nativeName?.let { native ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = native,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Favourites Badge
        character.favourites?.let { favs ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$favs Favourites",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun CharacterStatsCard(character: CharacterDetails) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(label = "Gender", value = character.gender ?: "?")
            VerticalDivider(Modifier.height(32.dp), color = MaterialTheme.colorScheme.outlineVariant)
            // Remove trailing dash from age (e.g., "17-" -> "17")
            val ageDisplay = character.age?.replace("-", "")?.trim() ?: "?"
            StatItem(label = "Age", value = ageDisplay)
            VerticalDivider(Modifier.height(32.dp), color = MaterialTheme.colorScheme.outlineVariant)
            StatItem(label = "Blood", value = character.bloodType ?: "?")
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpandableCharacterSynopsis(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = effectsSpec,
        label = "ArrowRotation"
    )

    Surface(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Note: Header is now external to this component
            Box {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (expanded) "Show less" else "Read more",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = arrowRotation }
                )
            }
        }
    }
}

@Composable
fun MediaRoleItem(media: CharacterMedia) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = media.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            // Voice Actor Overlay (Small Bubble)
            media.voiceActor?.let { va ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    AsyncImage(
                        model = va.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = media.title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )

        media.voiceActor?.let { va ->
            Text(
                text = va.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
            androidx.compose.material3.Button(onClick = onRetry) {
                Text("Retry")
            }
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.TextButton(onClick = onBackClick) {
                Text("Go Back")
            }
        }
    }
}

// -----------------------------------------------------------------------------
// SKELETON LOADING STATE
// -----------------------------------------------------------------------------

@Composable
fun CharacterSkeletonContent(onBackClick: () -> Unit) {
    // ... existing skeleton code ...
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header Skeleton
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .shimmerEffect()
                )

                // Back Button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp, start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp)
                        .width(130.dp)
                        .height(190.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmerEffect()
                )
            }
        }

        // Content Skeleton
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}