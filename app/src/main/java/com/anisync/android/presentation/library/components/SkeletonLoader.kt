package com.anisync.android.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import com.anisync.android.presentation.util.posterGridColumns
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.util.shimmerEffect

/**
 * Skeleton loader for Library grid card.
 * Matches the dimensions and structure of NewGridCard.
 */
@Composable
fun SkeletonGridCard(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(16.dp))
            .shimmerEffect()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Image placeholder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .shimmerEffect()
            )
            
            // Info section placeholder
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Badge placeholder
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress bar placeholder
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .shimmerEffect()
                    )
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .shimmerEffect()
                    )
                }
            }
            
            // Buttons section placeholder
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}

/**
 * Skeleton loader for Library list card.
 * Matches the dimensions and structure of NewListCard.
 */
@Composable
fun SkeletonListCard(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .shimmerEffect()
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Image placeholder
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shimmerEffect()
            )
            
            // Content placeholder
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Title placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Badge placeholder
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }
                
                // Progress bar placeholder
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .shimmerEffect()
                    )
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .shimmerEffect()
                    )
                }
            }
            
            // Buttons placeholder
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}

/**
 * Skeleton loader for Details screen header.
 * Matches the dimensions of PageHeaderSection.
 */
@Composable
fun SkeletonDetailsHeader(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        // Banner placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .shimmerEffect()
        )
        
        // Cover and info overlay
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cover image placeholder
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(190.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shimmerEffect()
            )
            
            // Title and info placeholders
            Column(
                modifier = Modifier.padding(top = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
        }
        
        // Back button placeholder
        Box(
            modifier = Modifier
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .shimmerEffect()
        )
    }
}

/**
 * Grid skeleton loader showing multiple skeleton cards.
 * Use as loading state for LibraryScreen grid view.
 * 
 * @param itemCount Number of skeleton items to display
 */
@Composable
fun SkeletonGrid(
    itemCount: Int = 6,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = posterGridColumns(baseMinSize = 160.dp),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = false
    ) {
        items(itemCount) {
            SkeletonGridCard()
        }
    }
}

/**
 * List skeleton loader showing multiple skeleton cards.
 * Use as loading state for LibraryScreen list view.
 * 
 * @param itemCount Number of skeleton items to display
 */
@Composable
fun SkeletonList(
    itemCount: Int = 6,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = false
    ) {
        items(itemCount) {
            SkeletonListCard()
        }
    }
}
