package com.anisync.android.presentation.details

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.anisync.android.domain.CharacterInfo
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaDetails
import com.anisync.android.domain.RelatedMedia
import com.anisync.android.type.MediaType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsBottomSheet(
    mediaId: Int,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    viewModel: DetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load data when mediaId changes
    LaunchedEffect(mediaId) {
        viewModel.loadMedia(mediaId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0) }, // Handle insets manually for full edge-to-edge feel
        dragHandle = null // Custom drag handle in HeaderSection
    ) {
        // Main Content Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            when (val state = uiState) {
                is DetailsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is DetailsUiState.Success -> {
                    DetailsContent(
                        details = state.details,
                        onStatusUpdate = { status, progress -> viewModel.saveMediaListEntry(status, progress) },
                        onRemove = { viewModel.deleteMediaListEntry() }
                    )
                }
                is DetailsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailsContent(
    details: MediaDetails,
    onStatusUpdate: (LibraryStatus, Int) -> Unit,
    onRemove: () -> Unit
) {
    val listState = rememberLazyListState()
    val isExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }

    var showStatusPicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp) // Extra space for FAB
        ) {
            // 1. Immersive Header
            item {
                HeaderSection(details)
            }

            // 2. Metadata & Synopsis
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    // Title & Badges
                    Text(
                        text = details.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Info Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Score Badge
                        details.score?.let { score ->
                            ScoreBadge(score)
                        }

                        // Format Badge
                        details.format?.let { format ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (format == "TV") "TV Series" else format.replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Year
                        Text(
                            text = details.year?.toString() ?: "",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (details.studio != null) {
                            Text(
                                text = "•",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Text(
                                text = details.studio,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 120.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Stats Grid
                    StatsGrid(details)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Genres
                    GenreFlow(details.genres)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Description
                    ExpandableSynopsis(details.description)
                }
            }

            // 3. Characters Section
            item {
                if (details.characters.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    SectionTitle(title = "Cast", modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(details.characters) { character ->
                            CharacterItem(character)
                        }
                    }
                }
            }

            // 4. Relations Section
            item {
                if (details.relations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    SectionTitle(title = "Related", modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(details.relations) { relation ->
                            RelationItem(relation)
                        }
                    }
                }
            }
        }

        // FAB (Primary Action)
        val fabIcon = if (details.listEntryId != null) Icons.Default.Edit else Icons.Default.Add
        val fabText = if (details.listEntryId != null) {
            getStatusLabel(details.listStatus ?: LibraryStatus.UNKNOWN, details.type == MediaType.MANGA)
        } else {
            "Add to Library"
        }
        val fabContainerColor = if (details.listEntryId != null) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.primary
        }
        val fabContentColor = if (details.listEntryId != null) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimary
        }

        ExtendedFloatingActionButton(
            text = { Text(text = fabText) },
            icon = { Icon(imageVector = fabIcon, contentDescription = null) },
            onClick = { showStatusPicker = true },
            expanded = isExpanded,
            containerColor = fabContainerColor,
            contentColor = fabContentColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
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
fun HeaderSection(details: MediaDetails) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        // Banner Backdrop
        AsyncImage(
            model = details.bannerUrl ?: details.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        // Gradient Fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )

        // Cover Image (Overlapping)
        AsyncImage(
            model = details.coverUrl,
            contentDescription = "Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp)
                .offset(y = (-20).dp) // Pull up
                .width(110.dp)
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(4.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        // Drag Handle Visual (Subtle line at top center)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .width(32.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.8f))
        )
    }
}

@Composable
fun ScoreBadge(score: Int) {
    // Colors inspired by AniList
    val color = when {
        score >= 75 -> Color(0xFF4CAF50) // Green
        score >= 60 -> Color(0xFFFFC107) // Amber
        else -> Color(0xFFF44336) // Red
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$score%",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

@Composable
fun StatsGrid(details: MediaDetails) {
    val isManga = details.type == MediaType.MANGA

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            label = if (isManga) "Chapters" else "Episodes",
            value = if (isManga) "${details.chapters ?: "?"}" else "${details.episodes ?: "?"}"
        )
        VerticalDivider(modifier = Modifier.height(32.dp), color = MaterialTheme.colorScheme.outlineVariant)
        StatItem(
            label = "Status",
            value = details.status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        )
        VerticalDivider(modifier = Modifier.height(32.dp), color = MaterialTheme.colorScheme.outlineVariant)
        StatItem(
            label = "Source",
            value = "Original" // Placeholder as API query didn't request source
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
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
            AssistChip(
                onClick = {},
                label = { Text(genre, style = MaterialTheme.typography.labelMedium) },
                colors = AssistChipDefaults.assistChipColors(
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { expanded = !expanded }
            .padding(16.dp)
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
    ) {
        Text(
            text = "SYNOPSIS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )

            // Subtle Gradient overlay for collapsed state
            if (!expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surfaceContainerLowest
                                )
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (expanded) "Show less" else "Read more",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = if(expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
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
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = character.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
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
fun RelationItem(relation: RelatedMedia) {
    Column(modifier = Modifier.width(100.dp)) {
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
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = relation.relationType.replace("_", " "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = relation.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
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
                text = "Update Status",
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

                // Grid-like layout for statuses
                statuses.chunked(2).forEach { rowStatuses ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowStatuses.forEach { status ->
                            val isSelected = status == currentStatus
                            val label = getStatusLabel(status, isManga)
                            val icon = getStatusIcon(status, isManga)

                            Card(
                                onClick = { onStatusSelected(status) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(70.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                border = if (isSelected)
                                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                else
                                    null
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        if (rowStatuses.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                if (currentStatus != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    OutlinedButton(
                        onClick = onRemove,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove from Library")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp)
    )
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