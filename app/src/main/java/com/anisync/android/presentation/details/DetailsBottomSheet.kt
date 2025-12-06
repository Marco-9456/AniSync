package com.anisync.android.presentation.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.automirrored.filled.MenuBook
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.CharacterInfo
import com.anisync.android.domain.RelatedMedia
import com.anisync.android.type.MediaType
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.anisync.android.domain.MediaDetails
import com.anisync.android.ui.theme.PastelGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsBottomSheet(
    mediaId: Int,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    viewModel: DetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    LaunchedEffect(mediaId) {
        viewModel.loadMedia(mediaId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            // Custom Handle
            Box(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.LightGray.copy(alpha = 0.6f))
            )
        },
        contentWindowInsets = { WindowInsets(0) }
    ) {
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
                    DetailsSheetContent(
                        details = state.details,
                        onStatusSelected = { status, progress -> viewModel.saveMediaListEntry(status, progress) },
                        onRemove = { viewModel.deleteMediaListEntry() }
                    )
                }
                is DetailsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Error: ${state.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsSheetContent(
    details: MediaDetails,
    onStatusSelected: (LibraryStatus, Int) -> Unit,
    onRemove: () -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Characters", "Related")
    val isManga = details.type == MediaType.MANGA

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                // Cover Image
                AsyncImage(
                    model = details.coverUrl,
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(100.dp)
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Metadata
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Badges
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Format Badge
                        details.format?.let { fmt ->
                            Badge(
                                text = if (fmt == "TV") "TV" else fmt,
                                backgroundColor = Color(0xFF6A5A17),
                                contentColor = Color.White
                            )
                        }

                        // Score Badge
                        details.score?.let { score ->
                            Badge(
                                text = "$score%",
                                icon = Icons.Default.Star,
                                backgroundColor = Color(0xFFFFD700),
                                contentColor = Color.Black
                            )
                        }
                    }

                    // Title
                    Text(
                        text = details.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Studio • Year
                    val studioYear = listOfNotNull(
                        details.studio,
                        details.year?.toString()
                    ).joinToString(" • ")
                    
                    Text(
                        text = studioYear,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                divider = { HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f)) },
                indicator = {
                     TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = true),
                        width = Dp.Unspecified,
                        color = Color(0xFF8B7E28)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selectedTabIndex == index) Color.Black else Color.Gray
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> OverviewTab(details)
                1 -> CharactersTab(details.characters)
                2 -> RelatedTab(details.relations)
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }

        // FAB Menu
        FabMenu(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            isInList = details.listEntryId != null,
            currentStatus = details.listStatus,
            isManga = isManga,
            onStatusSelected = { status ->
                val progress = details.listProgress ?: 0
                onStatusSelected(status, progress)
            },
            onRemove = onRemove
        )
    }
}

@Composable
fun CharactersTab(characters: List<CharacterInfo>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        if (characters.isEmpty()) {
            Text(
                text = "No characters available.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(characters) { character ->
                    CharacterCard(character)
                }
            }
        }
    }
}

@Composable
fun CharacterCard(character: CharacterInfo) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        AsyncImage(
            model = character.imageUrl,
            contentDescription = character.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = character.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = character.role,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
fun RelatedTab(relations: List<RelatedMedia>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        if (relations.isEmpty()) {
            Text(
                text = "No related media available.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(relations) { related ->
                    RelatedMediaCard(related)
                }
            }
        }
    }
}

@Composable
fun RelatedMediaCard(related: RelatedMedia) {
    Column(
        modifier = Modifier.width(100.dp)
    ) {
        AsyncImage(
            model = related.coverUrl,
            contentDescription = related.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = related.relationType.replace("_", " "),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF8B7E28),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = related.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FabMenu(
    modifier: Modifier = Modifier,
    isInList: Boolean,
    currentStatus: LibraryStatus?,
    isManga: Boolean,
    onStatusSelected: (LibraryStatus) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Conditional labels based on media type
    val options = listOf(
        LibraryStatus.CURRENT to if (isManga) "Reading" else "Watching",
        LibraryStatus.PLANNING to if (isManga) "Plan to Read" else "Plan to Watch",
        LibraryStatus.COMPLETED to "Completed",
        LibraryStatus.DROPPED to "Dropped",
        LibraryStatus.PAUSED to "Paused"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = expanded,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 2 },
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { it / 2 }
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Options
                options.forEach { (status, label) ->
                    FabMenuItem(
                        text = label,
                        icon = getStatusIcon(status, isManga),
                        selected = status == currentStatus,
                        onClick = {
                            onStatusSelected(status)
                            expanded = false
                        }
                    )
                }

                // Remove Option (only if in list)
                if (isInList) {
                    FabMenuItem(
                        text = "Remove",
                        icon = Icons.Default.Delete,
                        selected = false,
                        color = MaterialTheme.colorScheme.error,
                        onClick = {
                            onRemove()
                            expanded = false
                        }
                    )
                }
            }
        }

        // Animated FAB
        val fabColor by animateColorAsState(
            targetValue = if (expanded) Color.LightGray 
                          else if (isInList) Color(0xFFFFD700) 
                          else PastelGreen,
            label = "fabColor"
        )

        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = fabColor,
            contentColor = Color.Black,
            modifier = Modifier.size(56.dp)
        ) {
            AnimatedContent(
                targetState = if (expanded) Icons.Default.Close 
                              else if (isInList) Icons.Default.Edit 
                              else Icons.Default.Add,
                label = "fabIcon"
            ) { targetIcon ->
                Icon(
                    imageVector = targetIcon,
                    contentDescription = if (expanded) "Close" else "Manage",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun FabMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    color: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        // Label
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        // Mini FAB
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (selected) Color(0xFFFFD700) else color)
                .border(
                    width = 1.dp,
                    color = if (selected) Color(0xFFFFD700) else Color.LightGray,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.Black else Color.Black, // Icon color
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun getStatusIcon(status: LibraryStatus, isManga: Boolean = false): androidx.compose.ui.graphics.vector.ImageVector {
    return when (status) {
        LibraryStatus.CURRENT -> if (isManga) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.PlayArrow
        LibraryStatus.PLANNING -> Icons.Default.Event
        LibraryStatus.COMPLETED -> Icons.Default.Check
        LibraryStatus.DROPPED -> Icons.Default.Delete // Or remove circle
        LibraryStatus.PAUSED -> Icons.Default.Pause
        LibraryStatus.REPEATING -> Icons.Default.Repeat
        else -> Icons.Default.QuestionMark
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverviewTab(details: MediaDetails) {
    val isManga = details.type == MediaType.MANGA
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Synopsis Header
        Text(
            text = "SYNOPSIS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = Color(0xFF8B7E28),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Synopsis Text
        ExpandableText(
            text = details.description,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Info Header
        Text(
            text = "INFO",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = Color(0xFF8B7E28),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Info Cards - Conditional based on type
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isManga) {
                InfoCard(
                    label = "CHAPTERS",
                    value = "${details.chapters ?: "?"} ch",
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFFEEE8C5)
                )
            } else {
                InfoCard(
                    label = "EPISODES",
                    value = "${details.episodes ?: "?"} eps",
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFFEEE8C5)
                )
            }
            InfoCard(
                label = "FORMAT",
                value = details.format ?: "Unknown",
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0xFFC5E8D0)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Genres
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            details.genres.forEach { genre ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Transparent)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = genre,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

@Composable
fun InfoCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.Black
        )
    }
}

@Composable
fun Badge(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    backgroundColor: Color,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp).padding(end = 2.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = contentColor
        )
    }
}

@Composable
fun ExpandableText(text: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.DarkGray),
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.animateContentSize()
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
             Text(
                text = if (expanded) "Read Less" else "Read More",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF8B7E28)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                 tint = Color(0xFF8B7E28),
                 modifier = Modifier.size(16.dp)
            )
        }
    }
}