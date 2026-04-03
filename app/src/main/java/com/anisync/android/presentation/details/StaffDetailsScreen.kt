package com.anisync.android.presentation.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.StaffDetails
import com.anisync.android.domain.VoicedCharacter
import com.anisync.android.presentation.components.AsyncRichTextRenderer
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.ImageViewerDialog
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.details.components.AttributesCard
import com.anisync.android.presentation.details.components.CharacterSkeletonContent
import com.anisync.android.presentation.details.components.DetailHeroImage
import com.anisync.android.presentation.details.components.NameCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDetailsScreen(
    staffId: Int,
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit = {},
    onCharacterClick: (Int) -> Unit = {},
    onMediaSeeAllClick: (Int, String) -> Unit = { _, _ -> },
    viewModel: StaffDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val isScrolled by remember {
        derivedStateOf { scrollBehavior.state.contentOffset < -50f }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            val title = (uiState as? StaffDetailsUiState.Success)?.details?.name ?: ""

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
                            tint = animateColorAsState(
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
                is StaffDetailsUiState.Loading -> {
                    CharacterSkeletonContent(onBackClick = onBackClick)
                }

                is StaffDetailsUiState.Success -> {
                    StaffDetailsContent(
                        staff = state.details,
                        onMediaClick = onMediaClick,
                        onCharacterClick = onCharacterClick,
                        onMediaSeeAllClick = {
                            onMediaSeeAllClick(state.details.id, state.details.name)
                        }
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
    }
}

@Composable
private fun StaffDetailsContent(
    staff: StaffDetails,
    onMediaClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onMediaSeeAllClick: () -> Unit
) {
    var showImageViewer by rememberSaveable { mutableStateOf(false) }

    // Build attributes for the card
    val displayAttributes = remember(staff) {
        buildList {
            staff.age?.let { add("Age" to it.toString()) }
            staff.gender?.takeUnless { it.isEmpty() }?.let { add("Gender" to it) }
            staff.bloodType?.takeUnless { it.isEmpty() }?.let { add("Blood Type" to it) }
            staff.dateOfBirth?.let { add("Birthday" to it) }
            staff.dateOfDeath?.let { add("Date of Death" to it) }
            staff.homeTown?.takeUnless { it.isEmpty() }?.let { add("Hometown" to it) }
            if (staff.yearsActive.isNotEmpty()) {
                val yearsText = if (staff.yearsActive.size == 2) {
                    "${staff.yearsActive[0]} - ${staff.yearsActive[1]}"
                } else {
                    "${staff.yearsActive[0]} - Present"
                }
                add("Years Active" to yearsText)
            }
            if (staff.primaryOccupations.isNotEmpty()) {
                add("Occupations" to staff.primaryOccupations.joinToString(", "))
            }
        }
    }

    // Preview of voiced characters (first 5)
    val previewCharacters = remember(staff.voicedCharacters) {
        staff.voicedCharacters.take(5)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
        )
    ) {
        // Hero Image
        item(key = "hero") {
            DetailHeroImage(
                imageUrl = staff.imageUrl,
                contentDescription = staff.name,
                id = staff.id,
                onImageClick = { showImageViewer = true }
            )
        }

        // Name Card
        item(key = "name") {
            Spacer(modifier = Modifier.height(12.dp))
            NameCard(
                name = staff.name,
                nativeName = staff.nativeName,
                favourites = staff.favourites
            )
        }

        // Biography
        if (!staff.description.isNullOrBlank()) {
            item(key = "bio_header") {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(
                    title = "Biography",
                    level = HeaderLevel.Section,
                    iconColor = MaterialTheme.colorScheme.primary
                )
            }
            item(key = "bio") {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    AsyncRichTextRenderer(
                        html = staff.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Attributes
        if (displayAttributes.isNotEmpty()) {
            item(key = "attr_header") {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(
                    title = "Attributes",
                    level = HeaderLevel.Section,
                    iconColor = MaterialTheme.colorScheme.primary
                )
            }
            item(key = "attr") {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    AttributesCard(attributes = displayAttributes)
                }
            }
        }

        // Voiced Characters (preview with See All)
        if (previewCharacters.isNotEmpty()) {
            item(key = "vc_header") {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(
                    title = "Voiced Characters",
                    level = HeaderLevel.Section,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onActionClick = if (staff.voicedCharacters.size > 5) onMediaSeeAllClick else null
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(
                items = previewCharacters,
                key = { it.characterId }
            ) { voicedChar ->
                VoicedCharacterItem(
                    voicedCharacter = voicedChar,
                    onCharacterClick = { onCharacterClick(voicedChar.characterId) },
                    onMediaClick = onMediaClick
                )
            }
        }
    }

    if (showImageViewer && staff.imageUrl != null) {
        ImageViewerDialog(
            imageUrls = listOf(staff.imageUrl),
            initialIndex = 0,
            onDismiss = { showImageViewer = false }
        )
    }
}

@Composable
private fun VoicedCharacterItem(
    voicedCharacter: VoicedCharacter,
    onCharacterClick: () -> Unit,
    onMediaClick: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Character header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onCharacterClick)
                    .padding(4.dp)
            ) {
                AsyncImage(
                    model = voicedCharacter.characterImageUrl,
                    contentDescription = voicedCharacter.characterName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = voicedCharacter.characterName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Media appearances
            voicedCharacter.mediaAppearances.forEach { appearance ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onMediaClick(appearance.mediaId) }
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    appearance.startYear?.let { year ->
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(40.dp)
                        )
                    }
                    Text(
                        text = appearance.mediaTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

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
