package com.anisync.android.presentation.profile.sections

import com.anisync.android.domain.url

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.components.AnimatedTab
import com.anisync.android.presentation.components.PosterCard
import com.anisync.android.presentation.profile.components.PlaceholderTabContent
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.toIcon
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaType

private val mediaStatusTabOrder = listOf(
    LibraryStatus.CURRENT,
    LibraryStatus.REPEATING,
    LibraryStatus.PAUSED,
    LibraryStatus.COMPLETED,
    LibraryStatus.PLANNING,
    LibraryStatus.DROPPED
)

@OptIn(ExperimentalSharedTransitionApi::class)
fun LazyListScope.profileMediaTab(
    itemsByStatus: Map<LibraryStatus, List<LibraryEntry>>,
    selectedStatus: LibraryStatus,
    onStatusSelected: (LibraryStatus) -> Unit,
    isLoading: Boolean,
    mediaType: MediaType,
    @StringRes emptyMessageRes: Int,
    onMediaClick: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    transitionPrefix: String,
    modifier: Modifier = Modifier
) {
    val statuses = buildList {
        addAll(mediaStatusTabOrder)
        val hasUnknownEntries = itemsByStatus[LibraryStatus.UNKNOWN].orEmpty().isNotEmpty()
        if (hasUnknownEntries || selectedStatus == LibraryStatus.UNKNOWN) {
            add(LibraryStatus.UNKNOWN)
        }
    }

    item(key = "media_status_filters_$transitionPrefix", contentType = "filters") {
        val selectedIndex = statuses.indexOf(selectedStatus).coerceAtLeast(0)

        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            itemsIndexed(statuses) { index, status ->
                AnimatedTab(
                    index = index,
                    selectedIndex = selectedIndex,
                    selected = status == selectedStatus,
                    onClick = { onStatusSelected(status) },
                    icon = status.toIcon(mediaType),
                    label = status.toLabel(mediaType)
                )
            }
        }
    }

    val items = itemsByStatus[selectedStatus].orEmpty()
    if (isLoading) {
        item(key = "media_loading_${transitionPrefix}", contentType = "loading") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    } else if (items.isEmpty()) {
        item(key = "media_empty_${transitionPrefix}_${selectedStatus.name}", contentType = "empty") {
            PlaceholderTabContent(
                message = statusEmptyMessage(selectedStatus = selectedStatus, mediaType = mediaType, emptyMessageRes = emptyMessageRes),
                modifier = modifier
            )
        }
    } else {
        val rowItems = items.chunked(3)
        item(key = "media_top_spacer_${transitionPrefix}") { Spacer(modifier = Modifier.height(16.dp)) }
        
        itemsIndexed(
            items = rowItems,
            key = { index, _ -> "media_row_${transitionPrefix}_${selectedStatus.name}_$index" },
            contentType = { _, _ -> "media_row" }
        ) { _, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                            PosterCard(
                                item = item,
                                onClick = { onMediaClick(item.mediaId) },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                transitionPrefix = transitionPrefix
                            )
                        } else {
                            PosterCardFallback(
                                coverUrl = item.coverUrl,
                                title = item.titleUserPreferred,
                                onClick = { onMediaClick(item.mediaId) }
                            )
                        }
                    }
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun LazyListScope.profileMediaTab(
    items: List<LibraryEntry>,
    @StringRes emptyMessageRes: Int,
    onMediaClick: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    transitionPrefix: String,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        item(key = "media_empty_${transitionPrefix}", contentType = "empty") {
            PlaceholderTabContent(
                message = stringResource(emptyMessageRes),
                modifier = modifier
            )
        }
    } else {
        val rowItems = items.chunked(3)
        item(key = "media_top_spacer_${transitionPrefix}") { Spacer(modifier = Modifier.height(16.dp)) }

        itemsIndexed(
            items = rowItems,
            key = { index, _ -> "media_row_${transitionPrefix}_$index" },
            contentType = { _, _ -> "media_row" }
        ) { _, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                            PosterCard(
                                item = item,
                                onClick = { onMediaClick(item.mediaId) },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                transitionPrefix = transitionPrefix
                            )
                        } else {
                            PosterCardFallback(
                                coverUrl = item.coverUrl,
                                title = item.titleUserPreferred,
                                onClick = { onMediaClick(item.mediaId) }
                            )
                        }
                    }
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun statusEmptyMessage(
    selectedStatus: LibraryStatus,
    mediaType: MediaType,
    @StringRes emptyMessageRes: Int
): String {
    return when (selectedStatus) {
        LibraryStatus.CURRENT -> if (mediaType == MediaType.MANGA) {
            stringResource(R.string.empty_reading)
        } else {
            stringResource(R.string.empty_watching)
        }
        LibraryStatus.PLANNING -> stringResource(R.string.empty_planning)
        LibraryStatus.COMPLETED -> stringResource(R.string.empty_completed)
        LibraryStatus.REPEATING,
        LibraryStatus.PAUSED,
        LibraryStatus.DROPPED,
        LibraryStatus.UNKNOWN -> stringResource(emptyMessageRes)
    }
}

@Composable
private fun PosterCardFallback(
    coverUrl: String?,
    cover: com.anisync.android.domain.CoverImage? = null,
    title: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .bouncyClickable(onClick = onClick)
    ) {
        AsyncImage(
            model = cover.url() ?: coverUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .aspectRatio(0.7f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
