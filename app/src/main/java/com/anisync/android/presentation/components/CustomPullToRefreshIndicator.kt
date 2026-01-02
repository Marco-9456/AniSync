package com.anisync.android.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomPullToRefreshIndicator(
    isRefreshing: Boolean,
    state: PullToRefreshState,
    modifier: Modifier = Modifier
) {
    // Custom polygons for shape morphing during pull-to-refresh
    val customPolygons = listOf(
        androidx.compose.material3.MaterialShapes.Circle,
        androidx.compose.material3.MaterialShapes.Flower,
        androidx.compose.material3.MaterialShapes.Diamond,
        androidx.compose.material3.MaterialShapes.Heart,
        androidx.compose.material3.MaterialShapes.Clover4Leaf
    )

    // Show LoadingIndicator when refreshing or pulling
    if (isRefreshing || state.distanceFraction > 0f) {
        Box(modifier = modifier) {
            ContainedLoadingIndicator(
            modifier = Modifier.align(Alignment.Center),
            polygons = customPolygons
        )
        }
    }
}
