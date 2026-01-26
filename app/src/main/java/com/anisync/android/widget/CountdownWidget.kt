package com.anisync.android.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri


// ... (in check, existing imports)
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.anisync.android.MainActivity
import com.anisync.android.worker.CountdownData
import com.anisync.android.worker.CountdownWorker
import kotlinx.serialization.json.Json

class CountdownWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(CountdownWorker.PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(CountdownWorker.KEY_DATA, null)
        
        val data = if (json != null) {
            try {
                Json.decodeFromString<CountdownData>(json)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        val bitmap = if (data?.coverUrl != null) {
            WidgetImageUtils.loadBitmap(appContext, data.coverUrl, width = 300, height = 200)
        } else {
            null
        }

        provideContent {
            GlanceTheme {
                CountdownWidgetContent(data, bitmap)
            }
        }
    }
}

@Composable
fun CountdownWidgetContent(data: CountdownData?, bitmap: android.graphics.Bitmap?) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
    ) {
        if (data == null) {
            Box(
                 modifier = GlanceModifier.fillMaxSize(),
                 contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No upcoming episodes found in your list.",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                )
            }
        } else {
            val detailsIntent = Intent(
                Intent.ACTION_VIEW,
                "anisync://details/${data.mediaId}".toUri()
            ).apply {
                component = null
                setClass(androidx.glance.LocalContext.current, MainActivity::class.java)
            }

            // Full Background Image with darkening
            if (bitmap != null) {
                 Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                 Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.primaryContainer)
                ) {}
            }
            
            // Dark Overlay
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f)))
            ) {}

            // Foreground Content
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .clickable(actionStartActivity(detailsIntent)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NEXT UP",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color.LightGray), // Explicitly light for contrast
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Spacer(modifier = GlanceModifier.height(8.dp))
                
                Text(
                    text = data.title,
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color.White),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    maxLines = 2
                )
                
                Spacer(modifier = GlanceModifier.height(4.dp))
                
                Text(
                    text = "Episode ${data.episode}",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color.Cyan)
                    )
                )

                Spacer(modifier = GlanceModifier.height(16.dp))
                
                // Relative time calculation
                // Note: This is static until widget update loop runs.
                val now = System.currentTimeMillis() / 1000
                val secondsDiff = data.airingAt - now
                
                val timeString = if (secondsDiff < 0) {
                   "Aired recently"
                } else {
                   val days = secondsDiff / 86400
                   val hours = (secondsDiff % 86400) / 3600
                   val minutes = (secondsDiff % 3600) / 60
                   
                   if (days > 0) {
                       "$days days, $hours hours"
                   } else if (hours > 0) {
                       "$hours hours, $minutes mins"
                   } else {
                       "$minutes minutes"
                   }
                }

                Text(
                    text = timeString,
                     style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color.White),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                )
            }
        }
    }
}
