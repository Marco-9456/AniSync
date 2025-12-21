package com.anisync.android.presentation.discover.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.presentation.components.MediaCard
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.formatAsTitle
import com.anisync.android.presentation.util.shimmerEffect
import com.anisync.android.presentation.util.toLabel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HorizontalMediaList(
    items: List<LibraryEntry>,
    onItemClick: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = items,
            key = { it.mediaId }
        ) { item ->
            MediaCard(
                item = item,
                onClick = { onItemClick(item.mediaId) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier.animateItem()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchResultItem(
    item: LibraryEntry,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            val formatLabel = item.format?.toLabel() ?: stringResource(R.string.media_type_media)
            val status = item.mediaStatus.formatAsTitle() ?: stringResource(R.string.unknown)
            Text(
                text = "$formatLabel • $status",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            AsyncImage(
                model = item.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.bouncyClickable(onClick = onClick)
    )
}

@Composable
fun DiscoverShimmer() {
    Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Box(Modifier.fillMaxWidth().height(380.dp).clip(RoundedCornerShape(28.dp)).shimmerEffect())
        Spacer(Modifier.height(48.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(150.dp, 24.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(3) {
                Box(Modifier.width(140.dp).height(200.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect())
            }
        }
    }
}
