package com.anisync.android.presentation.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.util.shimmerEffect

/**
 * Skeleton loading content for the Details screen.
 * Displays animated shimmer placeholders matching the actual content layout.
 */
@Composable
fun DetailsSkeletonContent(onBackClick: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.list_bottom_padding_fab))
    ) {
        // Header Skeleton
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.details_header_height))
            ) {
                // Banner placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .shimmerEffect()
                )

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.details_scrim_height_bottom))
                        .align(Alignment.TopCenter)
                        .offset(y = dimensionResource(R.dimen.details_scrim_offset_y))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )

                // Back Button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.spacing_small))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_size_medium))
                    )
                }

                // Cover placeholder
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = dimensionResource(R.dimen.spacing_large))
                        .width(dimensionResource(R.dimen.details_cover_width))
                        .height(dimensionResource(R.dimen.details_cover_height))
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large)))
                        .shimmerEffect()
                )
            }
        }

        // Content Skeleton
        item {
            Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_large))) {
                // Title placeholder
                Box(
                    modifier = Modifier
                        .width(dimensionResource(R.dimen.skeleton_text_width_large))
                        .height(dimensionResource(R.dimen.skeleton_text_height_large))
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_small)))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_normal)))

                // Subtitle/badges row placeholder
                Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(dimensionResource(R.dimen.skeleton_text_height_medium))
                            .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium)))
                            .shimmerEffect()
                    )
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(dimensionResource(R.dimen.skeleton_text_height_medium))
                            .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium)))
                            .shimmerEffect()
                    )
                }

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

                // Stats card placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.skeleton_stats_height))
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_extra_large)))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

                // Genre chips placeholder
                Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .width(dimensionResource(R.dimen.genre_chip_width))
                                .height(dimensionResource(R.dimen.genre_chip_height))
                                .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_extra_large)))
                                .shimmerEffect()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

                // Synopsis placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_extra_large)))
                        .shimmerEffect()
                )
            }
        }

        // Cast section skeleton
        item {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))

            // Section title placeholder
            Box(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(R.dimen.spacing_large))
                    .width(80.dp)
                    .height(dimensionResource(R.dimen.skeleton_text_height_small))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_small)))
                    .shimmerEffect()
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

            // Cast items placeholder
            LazyRow(
                contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
            ) {
                items(5) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(dimensionResource(R.dimen.skeleton_cast_item_width))
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large)))
                                .shimmerEffect()
                        )
                        Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
                        Box(
                            modifier = Modifier
                                .width(dimensionResource(R.dimen.skeleton_text_width_small))
                                .height(14.dp)
                                .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_small)))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}
