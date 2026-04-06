package com.anisync.android.presentation.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
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
import com.anisync.android.domain.StaffDetails
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.ImageViewerDialog
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.details.components.AttributesCard
import com.anisync.android.presentation.details.components.CharacterSkeletonContent
import com.anisync.android.presentation.details.components.DetailHeroImage
import com.anisync.android.presentation.details.components.ExpandableBiography
import com.anisync.android.presentation.details.components.NameCard
import com.anisync.android.presentation.details.components.VoicedCharacterItem
import com.anisync.android.util.getName

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
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val isScrolled by remember {
        derivedStateOf { scrollBehavior.state.contentOffset < -50f }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            val title = (uiState as? StaffDetailsUiState.Success)?.details
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
                actions = {
                    IconButton(onClick = { viewModel.shareStaff(context) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = animateColorAsState(
                                if (isScrolled) MaterialTheme.colorScheme.onSurface else Color.White,
                                label = "actionIconTint"
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
                        titleLanguage = titleLanguage,
                        onMediaClick = onMediaClick,
                        onCharacterClick = onCharacterClick,
                        onMediaSeeAllClick = {
                            onMediaSeeAllClick(state.details.id, state.details.getName(titleLanguage))
                        },
                        onFavouriteClick = viewModel::toggleFavourite
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
    titleLanguage: com.anisync.android.data.TitleLanguage,
    onMediaClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onMediaSeeAllClick: () -> Unit,
    onFavouriteClick: () -> Unit
) {
    var showImageViewer by rememberSaveable { mutableStateOf(false) }

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
                contentDescription = staff.getName(titleLanguage),
                id = staff.id,
                onImageClick = { showImageViewer = true }
            )
        }

        // Name Card
        item(key = "name") {
            Spacer(modifier = Modifier.height(12.dp))
            NameCard(
                name = staff.getName(titleLanguage),
                nativeName = staff.nativeName,
                alternativeNames = staff.alternativeNames,
                favourites = staff.favourites,
                isFavourite = staff.isFavourite,
                onFavouriteClick = onFavouriteClick
            )
        }

        // Biography - expandable box with rich text renderer
        if (!staff.description.isNullOrBlank()) {
            item(key = "bio") {
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    ExpandableBiography(html = staff.description)
                }
            }
        }

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

        if (previewCharacters.isNotEmpty()) {
            item(key = "vc_header") {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(
                        title = "Voiced Characters",
                        level = HeaderLevel.Section,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onActionClick = if (staff.voicedCharacters.size > 5) onMediaSeeAllClick else null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    previewCharacters.forEach { voicedChar ->
                        VoicedCharacterItem(
                            voicedCharacter = voicedChar,
                            titleLanguage = titleLanguage,
                            onCharacterClick = { onCharacterClick(voicedChar.characterId) },
                            onMediaClick = onMediaClick,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                }
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