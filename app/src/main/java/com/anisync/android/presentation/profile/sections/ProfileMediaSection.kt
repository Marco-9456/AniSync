package com.anisync.android.presentation.profile.sections

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.presentation.components.PosterCard
import com.anisync.android.presentation.profile.components.PlaceholderTabContent
import com.anisync.android.presentation.util.bouncyClickable

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
private fun PosterCardFallback(
    coverUrl: String?,
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
            model = coverUrl,
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