package com.anisync.android.presentation.details.components

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.util.shimmerEffect

/**
 * Skeleton loading content for the Details screen.
 * Displays animated shimmer placeholders matching the actual content layout.
 */
@Composable
fun DetailsSkeletonContent(
    @Suppress("UNUSED_PARAMETER") onBackClick: () -> Unit // Kept for API compatibility, TopAppBar now handles the back button
) {
    val themeBackground = MaterialTheme.colorScheme.background

    // Cache the gradient brush to avoid recreating it on every recomposition
    val bottomScrimBrush = remember(themeBackground) {
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, themeBackground)
        )
    }

    // Cache common shapes to prevent repeated allocations
    val smallShape = remember { RoundedCornerShape(4.dp) }
    val mediumShape = remember { RoundedCornerShape(12.dp) }
    val largeShape = remember { RoundedCornerShape(16.dp) }

    // Cache reused base modifiers without the composable shimmerEffect()
    val baseTextModifier = remember(smallShape) {
        Modifier
            .height(24.dp)
            .clip(smallShape)
    }

    val baseTagModifier = remember(smallShape) {
        Modifier
            .height(14.dp)
            .clip(smallShape)
    }

    val baseChipModifier = remember {
        Modifier
            .height(32.dp)
            .clip(CircleShape)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp) // Sufficient padding for FAB as used in real content
    ) {
        // Header Skeleton (Cover, Banner, Title)
        item(key = "header_skeleton") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                // Banner Image Layer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .shimmerEffect()
                )

                // Gradient Overlays (Visual integration)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp) // Starts fading before the solid block
                        .background(bottomScrimBrush)
                )

                // Solid background block for the text area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(themeBackground)
                )

                // Content Row (Cover + Title + Metadata)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = dimensionResource(R.dimen.spacing_large)),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Cover Image Placeholder
                    Box(
                        modifier = Modifier
                            .width(115.dp)
                            .height(165.dp)
                            .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large)))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_medium)))

                    // Title and Metadata Placeholders
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(baseTextModifier)
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .then(baseTextModifier)
                                .shimmerEffect()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Metadata Tags (Year, Format, Score)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .then(baseTagModifier)
                                    .shimmerEffect()
                            )
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .then(baseTagModifier)
                                    .shimmerEffect()
                            )
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .then(baseTagModifier)
                                    .shimmerEffect()
                            )
                        }
                    }
                }
            }
        }

        // Action Row (Buttons)
        item(key = "action_buttons_skeleton") {
            Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_large))) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite Toggle Button
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .shimmerEffect()
                        )

                    // Share Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(CircleShape)
                            .shimmerEffect()
                    )
                }
            }
        }

        // Information (Info Cards)
        item(key = "info_cards_skeleton") {
            Column {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.spacing_large)),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
                ) {
                    val infoCardModifier = remember(largeShape) {
                        Modifier
                            .height(80.dp)
                            .clip(largeShape)
                    }
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .then(infoCardModifier)
                                .shimmerEffect()
                        )
                    }
                }
            }
        }

        // Synopsis
        item(key = "synopsis_skeleton") {
            Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_large))) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium)))
                        .shimmerEffect()
                )
            }
        }

        // Categories (Genres & Tags)
        item(key = "metadata_skeleton") {
            Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_large))) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))
                Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))) {
                    val widths = remember { listOf(80.dp, 60.dp, 100.dp, 70.dp) }
                    widths.forEach { w ->
                        Box(
                            modifier = Modifier
                                .width(w)
                                .then(baseChipModifier)
                                .shimmerEffect()
                        )
                    }
                }
            }
        }

        // Cast
        item(key = "cast_skeleton") {
            Column {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
                // Section Title Placeholder
                Box(
                    modifier = Modifier
                        .padding(horizontal = dimensionResource(R.dimen.spacing_large))
                        .width(100.dp)
                        .then(baseTextModifier)
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

                // Horizontal LazyRow Placeholder
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.spacing_large)),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
                ) {
                    val characterItemModifier = remember(mediumShape) {
                        Modifier
                            .width(105.dp) // Typical character card width
                            .clip(mediumShape)
                    }
                    repeat(3) {
                        Box(
                            modifier = characterItemModifier
                                .height(dimensionResource(R.dimen.character_item_height))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterSkeletonContent(onBackClick: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header Skeleton
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .shimmerEffect()
                )

                // Back Button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp)
                        .width(130.dp)
                        .height(190.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmerEffect()
                )
            }
        }

        // Content Skeleton
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark Mode"
)
@Composable
private fun DetailsSkeletonContentPreview() {
    MaterialTheme {
        DetailsSkeletonContent(onBackClick = {})
    }
}
