package com.anisync.android.presentation.details

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.StudioDetails
import com.anisync.android.domain.StudioMediaEntry
import com.anisync.android.domain.url
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.details.components.AttributesCard
import com.anisync.android.presentation.details.components.NameCard
import com.anisync.android.presentation.util.formatAsTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioDetailsScreen(
    studioId: Int,
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit = {},
    viewModel: StudioDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
            val title = (uiState as? StudioDetailsUiState.Success)?.details?.name ?: ""
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
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    val isLoaded = uiState is StudioDetailsUiState.Success
                    IconButton(
                        onClick = { viewModel.shareStudio(context) },
                        enabled = isLoaded
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.cd_share)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
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
                is StudioDetailsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is StudioDetailsUiState.Success -> {
                    StudioDetailsContent(
                        studio = state.details,
                        onMediaClick = onMediaClick,
                        onFavouriteClick = viewModel::toggleFavourite,
                        onLoadMore = viewModel::loadMoreMedia
                    )
                }

                is StudioDetailsUiState.Error -> {
                    StudioErrorState(
                        message = state.message,
                        onRetry = viewModel::loadStudioDetails,
                        onBackClick = onBackClick
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioDetailsContent(
    studio: StudioDetails,
    onMediaClick: (Int) -> Unit,
    onFavouriteClick: () -> Unit,
    onLoadMore: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()

    LaunchedEffect(listState, studio.media.size, studio.hasNextPage) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= info.totalItemsCount - 5
        }.collect { nearEnd ->
            if (nearEnd && studio.hasNextPage) onLoadMore()
        }
    }

    val typeLabel = stringResource(
        if (studio.isAnimationStudio) R.string.studio_label_animation_studio
        else R.string.studio_label_other_studio
    )
    val typeRowLabel = stringResource(R.string.studio_label_type)
    val totalWorksLabel = stringResource(R.string.studio_label_total_works)
    val displayAttributes = remember(studio, typeLabel, typeRowLabel, totalWorksLabel) {
        buildList {
            add(typeRowLabel to typeLabel)
            val total = if (studio.hasNextPage) "${studio.media.size}+" else studio.media.size.toString()
            add(totalWorksLabel to total)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
        )
    ) {
        item(key = "hero") {
            StudioHero(studio = studio)
        }

        item(key = "name") {
            Spacer(modifier = Modifier.height(12.dp))
            NameCard(
                name = studio.name,
                nativeName = null,
                alternativeNames = emptyList(),
                favourites = studio.favourites,
                isFavourite = studio.isFavourite,
                onFavouriteClick = onFavouriteClick,
                nameClipLabel = stringResource(R.string.clip_label_studio_name)
            )
        }

        item(key = "attr_header") {
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader(
                title = stringResource(R.string.label_attributes),
                level = HeaderLevel.Section,
                iconColor = MaterialTheme.colorScheme.primary
            )
        }

        item(key = "attr") {
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                AttributesCard(attributes = displayAttributes)
            }
            studio.siteUrl?.takeUnless { it.isBlank() }?.let { url ->
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    TextButton(onClick = { uriHandler.openUri(url) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.studio_label_website))
                    }
                }
            }
        }

        if (studio.media.isNotEmpty()) {
            item(key = "works_header") {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(
                    title = stringResource(R.string.studio_label_works),
                    level = HeaderLevel.Section,
                    iconColor = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(
                items = studio.media,
                key = { "studio_work_${it.mediaId}" }
            ) { media ->
                StudioWorkItem(
                    media = media,
                    onClick = { onMediaClick(media.mediaId) },
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }
        }

        if (studio.hasNextPage) {
            item(key = "load_more") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun StudioHero(studio: StudioDetails) {
    val tertiary = MaterialTheme.colorScheme.tertiaryContainer
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val gradient = remember(tertiary, surfaceHigh) {
        Brush.verticalGradient(colors = listOf(tertiary, surfaceHigh))
    }
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(gradient)
    ) {
        // Decorative icon stays clear of the system bar and top app bar.
        Icon(
            imageVector = Icons.Default.Business,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = statusBarPadding + 72.dp, end = 24.dp)
                .size(72.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.18f)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            if (studio.isAnimationStudio) {
                StudioHeroChip(label = stringResource(R.string.studio_label_animation_studio))
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = studio.name,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (studio.favourites > 0) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = studio.favourites.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioHeroChip(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun StudioWorkItem(
    media: StudioMediaEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(24.dp)
    val coverShape = RoundedCornerShape(8.dp)
    val mainChipLabel = stringResource(R.string.studio_main_studio_chip)
    val itemDescription = remember(media.titleUserPreferred, media.format, media.year, media.isMainStudio, mainChipLabel) {
        buildString {
            append(media.titleUserPreferred)
            media.format?.let { append(", $it") }
            media.year?.let { append(", $it") }
            if (media.isMainStudio) append(", $mainChipLabel")
        }
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = cardShape,
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = itemDescription }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = media.cover.url() ?: media.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(60.dp)
                    .wrapContentHeight()
                    .clip(coverShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = media.titleUserPreferred,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = remember(media.format, media.year) {
                    listOfNotNull(
                        media.format?.formatAsTitle() ?: media.format,
                        media.year?.toString()
                    ).joinToString(" · ")
                }
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (media.isMainStudio) {
                    StudioMainChip(label = mainChipLabel)
                }
            }
        }
    }
}

@Composable
private fun StudioMainChip(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun StudioErrorState(
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
