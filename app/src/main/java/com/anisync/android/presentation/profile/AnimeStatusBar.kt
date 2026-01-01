package com.anisync.android.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.UserProfile
import com.anisync.android.ui.theme.StatusCompleted
import com.anisync.android.ui.theme.StatusDropped
import com.anisync.android.ui.theme.StatusOnHold
import com.anisync.android.ui.theme.StatusWatching

/**
 * Displays a horizontal bar showing the distribution of anime statuses
 * (Watching, Completed, On Hold, Dropped) with a legend.
 */
@Composable
fun AnimeStatusBar(profile: UserProfile) {
    // Use real status counts from the profile
    val statusCounts = profile.animeStatusCounts
    val watching = statusCounts.watching
    val completed = statusCounts.completed
    val onHold = statusCounts.onHold
    val dropped = statusCounts.dropped
    val total = watching + completed + onHold + dropped
    
    if (total == 0) return
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.section_anime_status),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Status Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Watching (Green)
                if (watching > 0) {
                    Box(
                        modifier = Modifier
                            .weight(watching.toFloat() / total)
                            .fillMaxSize()
                            .background(StatusWatching)
                    )
                }
                // Completed (Blue)
                if (completed > 0) {
                    Box(
                        modifier = Modifier
                            .weight(completed.toFloat() / total)
                            .fillMaxSize()
                            .background(StatusCompleted)
                    )
                }
                // On Hold (Yellow)
                if (onHold > 0) {
                    Box(
                        modifier = Modifier
                            .weight(onHold.toFloat() / total)
                            .fillMaxSize()
                            .background(StatusOnHold)
                    )
                }
                // Dropped (Red)
                if (dropped > 0) {
                    Box(
                        modifier = Modifier
                            .weight(dropped.toFloat() / total)
                            .fillMaxSize()
                            .background(StatusDropped)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusLegendItem(color = StatusWatching, label = stringResource(R.string.status_watching_count, watching))
            StatusLegendItem(color = StatusCompleted, label = stringResource(R.string.status_completed_count, completed))
            StatusLegendItem(color = StatusOnHold, label = stringResource(R.string.status_on_hold_count, onHold))
            StatusLegendItem(color = StatusDropped, label = stringResource(R.string.status_dropped_count, dropped))
        }
    }
}

/**
 * A single legend item with a colored dot and label.
 */
@Composable
fun StatusLegendItem(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
