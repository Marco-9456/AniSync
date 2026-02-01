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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.presentation.util.shimmerEffect
import com.anisync.android.data.TitleLanguage
import com.anisync.android.presentation.util.AppMotion

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HorizontalMediaList(
    items: List<LibraryEntry>,
    onItemClick: (Int) -> Unit,
    titleLanguage: TitleLanguage,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val placementSpec = AppMotion.rememberOffsetSpatialSpec()
    val fadeSpec = AppMotion.rememberEffectsSpec()
    
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = items,
            key = { it.mediaId }
        ) { item ->
            val onClick = remember(item.mediaId) { { onItemClick(item.mediaId) } }
            DiscoverMediaCard(
                item = item,
                style = CardStyle.Standard(),
                onClick = onClick,
                titleLanguage = titleLanguage,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                transitionPrefix = "discover",
                modifier = Modifier.animateItem(
                    fadeInSpec = fadeSpec,
                    fadeOutSpec = fadeSpec,
                    placementSpec = placementSpec
                )
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchResultItem(
    item: LibraryEntry,
    onClick: () -> Unit,
    titleLanguage: TitleLanguage,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    DiscoverMediaCard(
        item = item,
        style = CardStyle.ListItem,
        onClick = onClick,
        titleLanguage = titleLanguage,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        transitionPrefix = "search"
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
