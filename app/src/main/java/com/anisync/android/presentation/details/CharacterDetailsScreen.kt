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
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
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
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()

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

    // Extract colors for parser
    val spoilerBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val spoilerContentColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Parse description into Key-Values (attributes) and Body (bio)
    val (attributes, bio) = remember(character.description, spoilerBackgroundColor, spoilerContentColor) {
        parseCharacterDescription(character.description, spoilerBackgroundColor, spoilerContentColor)
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
                        StaggeredAnimatedVisibility(index = 2) {
                            CharacterInfoSection(displayAttributes)
                        }
                    }
                }
            }

            // Biography (stagger index 3)
            // Moved out of the previous column item to align properly with other sections
            if (bio.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    StaggeredAnimatedVisibility(index = 3) {
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
private fun parseCharacterDescription(
    description: String?,
    spoilerBackgroundColor: Color,
    spoilerContentColor: Color
): Pair<List<Pair<String, String>>, AnnotatedString> {
    if (description.isNullOrBlank()) return emptyList<Pair<String, String>>() to AnnotatedString("")

    val attributes = mutableListOf<Pair<String, String>>()
    val bioLines = mutableListOf<String>()

    // Normalize breaks
    val normalized = description.replace(Regex("<br\\s*/?>"), "\n")

    // Extended Regex to catch lines starting with bold keys or simple "Key: Value" patterns
    // Catch: "__Key__ Value", "**Key** Value", "Key: Value" (if line is short-ish)
    // Updated to allow () in keys for cases like "Devil Fruit (Type):"
    // Also handles spoiler-wrapped attributes like "~!Key: Value!~"
    val attributeRegex = Regex("^(~!)?\\s*(__|\\*\\*)?([a-zA-Z0-9\\s\\-_()]+?)(:|\\2)\\s*(.*)(!~)?$")

    normalized.lines().forEach { line ->
        val trimLine = line.trim()
        val match = attributeRegex.find(trimLine)
        var isAttribute = false

        if (trimLine.isNotBlank()) {
            if (match != null) {
                val hasSpoilerMarker = match.groupValues[1].isNotEmpty() // ~!
                val hasBoldMarkers = match.groupValues[2].isNotEmpty() // __ or **
                val key = match.groupValues[3].trim()
                val value = match.groupValues[5].trim()

                // Heuristic: It's an attribute if:
                // 1. It explicitly used bold markers (__ or **) OR spoiler markers (~!)
                // 2. OR the line is reasonably short AND contains a colon
                // Increased length limit to 200 to catch long attribute values (e.g. descriptions)
                // Increased key length limit to 50
                val isShortAndHasColon = trimLine.length < 200 && match.groupValues[4] == ":"

                if ((hasBoldMarkers || hasSpoilerMarker || isShortAndHasColon) && key.length < 50 && value.isNotEmpty()) {
                    // Clean spoiler tags, markdown, links, and italics from value
                    // Also trim leading colons that might have been captured
                    var cleanValue = value.trimStart { it == ':' || it.isWhitespace() }
                        .replace("~!", "")
                        .replace("!~", "")
                        .replace("__", "")
                        .replace("**", "")
                    
                    // Strip markdown links [text](url) -> text
                    cleanValue = cleanValue.replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
                    
                    // Strip single-underscore italics _text_ -> text
                    cleanValue = cleanValue.replace(Regex("(?<![_])_([^_]+)_(?![_])"), "$1")

                    attributes.add(key to cleanValue)
                    isAttribute = true
                }
            }

            if (!isAttribute) {
                bioLines.add(trimLine)
            }
        } else {
             // Preserve empty lines for paragraph spacing, but handle them in the builder
             if (bioLines.isNotEmpty() && bioLines.last().isNotEmpty()) {
                 bioLines.add("")
             }
        }
    }

    // Drop empty lines at start of bio
    val cleanBioLines = bioLines.dropWhile { it.isBlank() }
    var fullBioText = cleanBioLines.joinToString("\n")
    
    // Pre-process: Strip markdown links [text](url) -> text
    fullBioText = fullBioText.replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
    
    // Pre-process: Strip single-underscore italics _text_ -> text (but not double __ which is bold)
    fullBioText = fullBioText.replace(Regex("(?<![_])_([^_]+)_(?![_])"), "$1")
    
    // Build AnnotatedString for Bio - process entire text for multi-line spoilers
    // Spoilers are annotated with "SPOILER" tag for click-to-reveal functionality
    val bio = buildAnnotatedString {
        val hiddenSpoilerStyle = SpanStyle(
            background = spoilerBackgroundColor,
            color = spoilerBackgroundColor // Same color = text hidden
        )
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)

        var currentIndex = 0
        var spoilerIndex = 0
        // Regex to find markers: **bold**, __bold__, ~!spoiler!~
        // Use DOTALL flag (?s) to make . match newlines for multi-line spoilers
        val tokenRegex = Regex("(\\*\\*|__|~!)(.+?)(\\1|!~)", RegexOption.DOT_MATCHES_ALL)
        
        val matches = tokenRegex.findAll(fullBioText)
        for (match in matches) {
            // Append text before match
            if (match.range.first > currentIndex) {
                append(fullBioText.substring(currentIndex, match.range.first))
            }
            
            val token = match.groupValues[1] // **, __, or ~!
            val content = match.groupValues[2]
            
            if (token == "~!") {
                // Add annotation for click handling
                pushStringAnnotation(tag = "SPOILER", annotation = spoilerIndex.toString())
                withStyle(hiddenSpoilerStyle) {
                    append(content)
                }
                pop()
                spoilerIndex++
            } else {
                withStyle(boldStyle) {
                    append(content)
                }
            }
            
            currentIndex = match.range.last + 1
        }
        
        // Append remaining text
        if (currentIndex < fullBioText.length) {
            append(fullBioText.substring(currentIndex))
        }
    }

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
fun CharacterStatsCard(character: CharacterDetails, attributes: List<Pair<String, String>>) {
    fun findAttr(key: String): String? = attributes.find { it.first.equals(key, ignoreCase = true) }?.second

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
            val gender = character.gender.takeUnless { it.isNullOrEmpty() || it == "?" } ?: findAttr("Gender") ?: "?"
            StatItem(label = "Gender", value = gender, modifier = Modifier.weight(1f))
            
            VerticalDivider(Modifier.height(32.dp), color = MaterialTheme.colorScheme.outlineVariant)
            
            // Remove trailing dash from age (e.g., "17-" -> "17")
            val ageRaw = character.age?.replace("-", "")?.trim()
            val age = ageRaw.takeUnless { it.isNullOrEmpty() || it == "?" } ?: findAttr("Age") ?: "?"
            StatItem(label = "Age", value = age, modifier = Modifier.weight(1f))
            
            VerticalDivider(Modifier.height(32.dp), color = MaterialTheme.colorScheme.outlineVariant)
            
            val blood = character.bloodType.takeUnless { it.isNullOrEmpty() || it == "?" } ?: findAttr("Blood Type") ?: findAttr("Blood") ?: "?"
            StatItem(label = "Blood", value = blood, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
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
fun ExpandableCharacterSynopsis(text: AnnotatedString) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    // Track which spoiler indices have been revealed
    var revealedSpoilers by rememberSaveable { mutableStateOf(setOf<Int>()) }

    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = effectsSpec,
        label = "ArrowRotation"
    )

    // Colors for spoiler styles
    val spoilerBackground = MaterialTheme.colorScheme.surfaceVariant
    val spoilerRevealedColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    // Build display text with revealed/hidden spoiler styles
    val displayText = remember(text, revealedSpoilers) {
        buildAnnotatedString {
            // Copy the original text structure but update spoiler styles based on revealed state
            var i = 0
            while (i < text.length) {
                val annotations = text.getStringAnnotations(tag = "SPOILER", start = i, end = i + 1)
                if (annotations.isNotEmpty()) {
                    val annotation = annotations.first()
                    val spoilerIndex = annotation.item.toIntOrNull() ?: -1
                    val isRevealed = spoilerIndex in revealedSpoilers
                    
                    // Extract the spoiler content
                    val spoilerContent = text.substring(annotation.start, annotation.end)
                    
                    val listener = LinkInteractionListener {
                         revealedSpoilers = if (spoilerIndex in revealedSpoilers) {
                             revealedSpoilers - spoilerIndex
                         } else {
                             revealedSpoilers + spoilerIndex
                         }
                    }

                    // Add clickable link annotation for spoiler toggle
                    addLink(
                        LinkAnnotation.Clickable(
                            tag = "SPOILER",
                            linkInteractionListener = listener
                        ),
                        start = length,
                        end = length + spoilerContent.length
                    )

                    withStyle(SpanStyle(
                        background = spoilerBackground,
                        color = if (isRevealed) spoilerRevealedColor else spoilerBackground
                    )) {
                        append(spoilerContent)
                    }
                    
                    i = annotation.end
                } else {
                    // Check for other styles (bold, etc)
                    val spanStyles = text.spanStyles.filter { it.start <= i && it.end > i }
                    if (spanStyles.isNotEmpty()) {
                        val span = spanStyles.first()
                        withStyle(span.item) {
                            append(text.substring(i, minOf(i + 1, span.end)))
                        }
                        i++
                    } else {
                        append(text[i])
                        i++
                    }
                }
            }
        }
    }

    Surface(
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
                    text = displayText,
                    modifier = Modifier.clickable { expanded = !expanded },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    ),
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { expanded = !expanded }
            ) {
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