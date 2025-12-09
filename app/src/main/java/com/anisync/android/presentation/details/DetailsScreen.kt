package com.anisync.android.presentation.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.anisync.android.domain.CharacterInfo
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaDetails
import com.anisync.android.domain.RelatedMedia
import com.anisync.android.type.MediaType

/**
 * Material 3 Standard Motion Easing (Emphasized)
 * Reference: https://m3.material.io/styles/motion/easing-and-duration/tokens-specs
 */
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
private const val AnimationDurationShort = 400
private const val AnimationDurationLong = 600

@Composable
fun DetailsScreen(
    mediaId: Int,
    onBackClick: () -> Unit,
    onRelationClick: (Int) -> Unit = {},
    viewModel: DetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(mediaId) {
        viewModel.loadMedia(mediaId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is DetailsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
                is DetailsUiState.Success -> {
                    DetailsPageContent(
                        details = state.details,
                        onBackClick = onBackClick,
                        onRelationClick = onRelationClick,
                        onStatusUpdate = { status, progress -> viewModel.saveMediaListEntry(status, progress) },
                        onRemove = { viewModel.deleteMediaListEntry() }
                    )
                }
                is DetailsUiState.Error -> {
                    ErrorStateContent(message = state.message, onBackClick = onBackClick)
                }
            }
        }
    }
}

@Composable
fun ErrorStateContent(message: String, onBackClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete, // Or an error icon
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onBackClick) {
                Text("Go Back")
            }
        }
    }
}

@Composable
fun DetailsPageContent(
    details: MediaDetails,
    onBackClick: () -> Unit,
    onRelationClick: (Int) -> Unit,
    onStatusUpdate: (LibraryStatus, Int) -> Unit,
    onRemove: () -> Unit
) {
    val listState = rememberLazyListState()
    // Using 0 as a threshold is strict, let's give it a little buffer or just check index
    val isExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    var showStatusPicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp) // Space for FAB
        ) {
            item {
                PageHeaderSection(details, onBackClick)
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    // Title Group
                    TitleSection(details)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Stats
                    StatsCard(details)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Genres
                    GenreFlow(details.genres)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Synopsis
                    ExpandableSynopsis(details.description)
                }
            }

            item {
                if (details.characters.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    SectionTitle("Cast", Modifier.padding(horizontal = 24.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(details.characters) { CharacterItem(it) }
                    }
                }
            }

            item {
                if (details.relations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    SectionTitle("Related", Modifier.padding(horizontal = 24.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(details.relations) { relation -> 
                            RelationItem(
                                relation = relation,
                                onClick = { onRelationClick(relation.id) }
                            )
                        }
                    }
                }
            }
        }

        // Animated FAB
        val fabIcon = if (details.listEntryId != null) Icons.Default.Edit else Icons.Default.Add
        val fabText = if (details.listEntryId != null) {
            getStatusLabel(details.listStatus ?: LibraryStatus.UNKNOWN, details.type == MediaType.MANGA)
        } else {
            "Add to Library"
        }

        // Using AnimatedVisibility for a smoother entrance/exit of text if strictly needed,
        // but ExtendedFloatingActionButton handles text expansion internally.
        ExtendedFloatingActionButton(
            text = { Text(text = fabText) },
            icon = { Icon(imageVector = fabIcon, contentDescription = null) },
            onClick = { showStatusPicker = true },
            expanded = isExpanded,
            containerColor = if (details.listEntryId != null)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (details.listEntryId != null)
                MaterialTheme.colorScheme.onSecondaryContainer
            else
                MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        )
    }

    if (showStatusPicker) {
        StatusPickerDialog(
            currentStatus = details.listStatus,
            isManga = details.type == MediaType.MANGA,
            onStatusSelected = { status ->
                val progress = details.listProgress ?: 0
                onStatusUpdate(status, progress)
                showStatusPicker = false
            },
            onRemove = {
                onRemove()
                showStatusPicker = false
            },
            onDismiss = { showStatusPicker = false }
        )
    }
}

@Composable
fun TitleSection(details: MediaDetails) {
    Column {
        Text(
            text = details.title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            details.score?.let { ScoreBadge(it) }

            details.format?.let { format ->
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (format == "TV") "TV Series" else format.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = details.year?.toString() ?: "",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (details.studio != null) {
                Text(
                    "•",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    text = details.studio,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 120.dp)
                )
            }
        }
    }
}

@Composable
fun PageHeaderSection(details: MediaDetails, onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp) // Slightly taller for better proportions
    ) {
        // Banner Image
        AsyncImage(
            model = details.bannerUrl ?: details.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        // Gradient Scrim (Top for status bar/back button visibility)
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

        // Gradient Scrim (Bottom to blend into background)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp) // Taller fade for smoother transition
                .align(Alignment.TopCenter) // Align to top box but offset
                .offset(y = 120.dp) // Start where the banner ends
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
        // Hard cut gradient overlay at the bottom of the banner image itself
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
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp, start = 8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(28.dp) // Slightly larger touch target visual
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
                model = details.coverUrl,
                contentDescription = "Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun ScoreBadge(score: Int) {
    // Semantic colors based on score
    val (containerColor, contentColor) = when {
        score >= 75 -> Color(0xFFE6F4EA) to Color(0xFF1E8E3E) // Light Green / Dark Green
        score >= 60 -> Color(0xFFFFF7E0) to Color(0xFFF9A825) // Light Yellow / Dark Yellow
        else -> Color(0xFFFCE8E6) to Color(0xFFC62828) // Light Red / Dark Red
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.2f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                Icons.Default.Star,
                null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "$score%",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
    }
}

@Composable
fun StatsCard(details: MediaDetails) {
    val isManga = details.type == MediaType.MANGA
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
            StatItem(if (isManga) "Chapters" else "Episodes", if (isManga) "${details.chapters ?: "?"}" else "${details.episodes ?: "?"}")
            VerticalDivider(Modifier.height(32.dp), color = MaterialTheme.colorScheme.outlineVariant)
            StatItem("Status", details.status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
            VerticalDivider(Modifier.height(32.dp), color = MaterialTheme.colorScheme.outlineVariant)
            StatItem("Source", "Original") // Replace with actual source if available in MediaDetails
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenreFlow(genres: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        genres.forEach { genre ->
            SuggestionChip(
                onClick = {},
                label = { Text(genre, style = MaterialTheme.typography.labelMedium) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = null,
                shape = CircleShape
            )
        }
    }
}

@Composable
fun ExpandableSynopsis(text: String) {
    var expanded by remember { mutableStateOf(false) }

    // Using Surface for better elevation handling
    Surface(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = AnimationDurationShort,
                    easing = EmphasizedEasing
                )
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "SYNOPSIS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))

            // Text Content with crossfade effect
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

            // Interaction hint
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (expanded) "Show less" else "Read more",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if(expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

@Composable
fun CharacterItem(character: CharacterInfo) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        AsyncImage(
            model = character.imageUrl,
            contentDescription = character.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = character.name,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = character.role,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RelationItem(
    relation: RelatedMedia,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = relation.coverUrl,
            contentDescription = relation.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = relation.relationType.replace("_", " "),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = relation.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatusPickerDialog(
    currentStatus: LibraryStatus?,
    isManga: Boolean,
    onStatusSelected: (LibraryStatus) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Update Status",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val statuses = listOf(
                    LibraryStatus.CURRENT,
                    LibraryStatus.PLANNING,
                    LibraryStatus.COMPLETED,
                    LibraryStatus.PAUSED,
                    LibraryStatus.DROPPED
                )
                statuses.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { status ->
                            val isSelected = status == currentStatus
                            StatusOptionCard(
                                status = status,
                                isManga = isManga,
                                isSelected = isSelected,
                                onClick = { onStatusSelected(status) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                if (currentStatus != null) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    OutlinedButton(
                        onClick = onRemove,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Remove from Library")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun StatusOptionCard(
    status: LibraryStatus,
    isManga: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(70.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                getStatusIcon(status, isManga),
                null,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                getStatusLabel(status, isManga),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun getStatusLabel(status: LibraryStatus, isManga: Boolean): String {
    return when (status) {
        LibraryStatus.CURRENT -> if (isManga) "Reading" else "Watching"
        LibraryStatus.PLANNING -> "Planning"
        LibraryStatus.COMPLETED -> "Completed"
        LibraryStatus.DROPPED -> "Dropped"
        LibraryStatus.PAUSED -> "Paused"
        LibraryStatus.REPEATING -> "Rewatching"
        else -> "Unknown"
    }
}

fun getStatusIcon(status: LibraryStatus, isManga: Boolean): androidx.compose.ui.graphics.vector.ImageVector {
    return when (status) {
        LibraryStatus.CURRENT -> if (isManga) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.PlayArrow
        LibraryStatus.PLANNING -> Icons.Default.Event
        LibraryStatus.COMPLETED -> Icons.Default.Check
        LibraryStatus.DROPPED -> Icons.Default.Delete
        LibraryStatus.PAUSED -> Icons.Default.Pause
        LibraryStatus.REPEATING -> Icons.Default.Repeat
        else -> Icons.Default.Add
    }
}