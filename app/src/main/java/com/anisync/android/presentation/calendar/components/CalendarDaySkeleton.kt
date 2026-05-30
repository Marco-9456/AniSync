package com.anisync.android.presentation.calendar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.util.shimmerEffect

/** Loading placeholder rows that mirror the [AiringEpisodeCard] layout. */
@Composable
fun CalendarDaySkeleton(
    modifier: Modifier = Modifier,
    rows: Int = 6
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(
                    modifier = Modifier
                        .width(58.dp)
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(10.dp))
                        .shimmerEffect()
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShimmerLine(widthFraction = 0.85f)
                    ShimmerLine(widthFraction = 0.4f)
                    ShimmerLine(widthFraction = 0.25f)
                }
            }
        }
    }
}

@Composable
private fun ShimmerLine(widthFraction: Float) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .shimmerEffect()
    )
}
