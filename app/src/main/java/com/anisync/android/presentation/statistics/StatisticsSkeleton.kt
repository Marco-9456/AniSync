package com.anisync.android.presentation.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.util.shimmerEffect
import com.anisync.android.ui.theme.ExpressiveShapes

/**
 * Skeleton loading state for the Statistics screen.
 * Shows placeholder shapes that match the layout of the actual content.
 * Uses MD3 Expressive shapes to match the updated design.
 */
@Composable
fun StatisticsSkeleton(modifier: Modifier = Modifier) {
    val spacingMedium = dimensionResource(R.dimen.spacing_medium)
    val spacingNormal = dimensionResource(R.dimen.spacing_normal)
    val sectionSpacing = dimensionResource(R.dimen.statistics_section_spacing)
    val horizontalCardSpacing = dimensionResource(R.dimen.statistics_horizontal_card_spacing)

    Column(
        modifier = modifier.padding(horizontal = spacingMedium),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        // Overview Cards Skeleton (2x2 grid)
        Column(verticalArrangement = Arrangement.spacedBy(spacingNormal)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacingNormal)
            ) {
                SkeletonStatCard(Modifier.weight(1f))
                SkeletonStatCard(Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacingNormal)
            ) {
                SkeletonStatCard(Modifier.weight(1f))
                SkeletonStatCard(Modifier.weight(1f))
            }
        }

        // Score Distribution Section Skeleton
        Column {
            // Section header skeleton
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(24.dp)
                    .clip(ExpressiveShapes.pill)
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(spacingNormal))
            // Chart card skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(ExpressiveShapes.large) // MD3 Expressive: 24dp
                    .shimmerEffect()
            )
        }

        // Genres Section Skeleton (Horizontal list)
        Column {
            // Section header skeleton
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(24.dp)
                    .clip(ExpressiveShapes.pill)
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(spacingNormal))
            // Horizontal cards skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalCardSpacing)
            ) {
                repeat(3) {
                    SkeletonGenreCard()
                }
            }
        }

        // Formats Section Skeleton
        Column {
            // Section header skeleton
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(24.dp)
                    .clip(ExpressiveShapes.pill)
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(spacingNormal))
            // Card skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(ExpressiveShapes.large) // MD3 Expressive: 24dp
                    .shimmerEffect()
            )
        }
    }
}

/**
 * Skeleton for individual stat cards (Total Anime, Episodes, etc.)
 * Uses MD3 Expressive extraLarge shape (28dp) to match StatCard.
 */
@Composable
private fun SkeletonStatCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(ExpressiveShapes.extraLarge) // MD3 Expressive: 28dp for hero cards
            .shimmerEffect()
    )
}

/**
 * Skeleton for genre cards in horizontal list.
 * Uses MD3 Expressive large shape (24dp) to match GenreCard.
 */
@Composable
private fun SkeletonGenreCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(150.dp)
            .height(140.dp)
            .clip(ExpressiveShapes.large) // MD3 Expressive: 24dp for content cards
            .shimmerEffect()
    )
}

/**
 * Skeleton for tab pills
 */
@Composable
private fun SkeletonTab(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(80.dp)
            .height(36.dp)
            .clip(CircleShape)
            .shimmerEffect()
    )
}
