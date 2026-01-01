package com.anisync.android.presentation.profile

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.UserActivity
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader

/**
 * Displays a list of recent user activities in a timeline format.
 */
@Composable
fun RecentUpdatesSection(
    activities: List<UserActivity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.section_recent_updates),
            level = HeaderLevel.Section,
            padding = PaddingValues(bottom = 16.dp)
        )

        activities.take(5).forEach { activity ->
            UpdateItem(activity = activity, context = context)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * A single activity/update item in the timeline.
 *
 * @param activity The user activity to display.
 * @param context Context for formatting relative time.
 */
@Composable
fun UpdateItem(activity: UserActivity, context: Context) {
    
    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline Dot
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))

        // Card Content
        Row(modifier = Modifier.fillMaxWidth()) {
             // Media Image Box
             Box(
                 modifier = Modifier
                     .size(56.dp)
                     .clip(RoundedCornerShape(12.dp))
                     .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                 contentAlignment = Alignment.Center
             ) {
                 if (activity.mediaCoverUrl != null) {
                     AsyncImage(
                         model = activity.mediaCoverUrl,
                         contentDescription = stringResource(R.string.content_description_media_cover, activity.mediaTitle),
                         contentScale = ContentScale.Crop,
                         modifier = Modifier.fillMaxSize()
                     )
                 } else {
                     // Fallback Initials
                     Text(
                         text = activity.mediaTitle.take(2).uppercase(),
                         style = MaterialTheme.typography.titleMedium,
                         color = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                 }
             }

             Spacer(modifier = Modifier.width(16.dp))

             Column {
                 // Memoize relative time calculation
                 val timeAgo = remember(activity.timestamp) {
                     formatRelativeTime(activity.timestamp, context)
                 }
                 
                 Text(
                     text = timeAgo,
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                 )
                 
                 // Construct styled text: "Watched Episode X of Title"
                 val statusText = activity.status ?: "Updated"
                 val progressText = activity.progress ?: ""
                 
                 // Capitalize status
                 val capStatus = statusText.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                 
                 Text(
                     text = buildAnnotatedString {
                         append("$capStatus ")
                         if (progressText.isNotEmpty()) {
                             withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
                                 append(progressText)
                             }
                             append(" of ")
                         }
                         withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                             append(activity.mediaTitle)
                         }
                     },
                     style = MaterialTheme.typography.bodyMedium,
                     color = MaterialTheme.colorScheme.onSurface,
                     lineHeight = 20.sp,
                     maxLines = 2,
                     overflow = TextOverflow.Ellipsis
                 )
                 
                 if (activity.mediaScore != null && activity.mediaScore > 0) {
                     Spacer(modifier = Modifier.height(8.dp))
                     Surface(
                         color = MaterialTheme.colorScheme.surfaceVariant,
                         shape = RoundedCornerShape(8.dp)
                     ) {
                         Text(
                             text = "Score: ${activity.mediaScore}",
                             style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                             color = MaterialTheme.colorScheme.onSurfaceVariant,
                             modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                         )
                     }
                 }
             }
        }
    }
}

/**
 * Formats a timestamp as a relative time string (e.g., "5 minutes ago").
 */
fun formatRelativeTime(timeMillis: Long?, context: Context): String {
    if (timeMillis == null || timeMillis == 0L) return "Unknown"
    return try {
        val now = System.currentTimeMillis()
        DateUtils.getRelativeTimeSpanString(timeMillis, now, DateUtils.MINUTE_IN_MILLIS).toString()
    } catch (e: Exception) {
        "Unknown"
    }
}
