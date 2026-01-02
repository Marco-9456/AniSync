package com.anisync.android.presentation.details

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.anisync.android.type.MediaType

object MediaDetailsIcons {

    fun getFormatIcon(format: String?, type: MediaType?): ImageVector {
        if (type == MediaType.MANGA) {
            return Icons.Filled.Book
        }
        return when (format?.uppercase()) {
            "MOVIE" -> Icons.Filled.Movie
            "TV" -> Icons.Filled.LiveTv
            "OVA", "ONA" -> Icons.Filled.Videocam
            "SPECIAL" -> Icons.Filled.Videocam
            "MUSIC" -> Icons.Filled.PlayCircle
            else -> Icons.Filled.LiveTv
        }
    }

    fun getStatusIcon(status: String?): ImageVector {
        return when (status?.uppercase()) {
            "FINISHED", "COMPLETED" -> Icons.Filled.CheckCircle
            "RELEASING", "HIATUS" -> Icons.Filled.PlayCircle // Or a loader/pause
            "NOT_YET_RELEASED" -> Icons.Filled.Schedule
            "CANCELLED" -> Icons.Filled.Close
            else -> Icons.Filled.CheckCircle
        }
    }

    fun getStatusColor(status: String?): Color {
        return when (status?.uppercase()) {
            "FINISHED", "COMPLETED" -> Color(0xFF4CAF50) // Green
            "RELEASING" -> Color(0xFF2196F3) // Blue
            "NOT_YET_RELEASED" -> Color(0xFFFFC107) // Amber
            "CANCELLED" -> Color(0xFFF44336) // Red
            else -> Color.Gray
        }
    }
}
