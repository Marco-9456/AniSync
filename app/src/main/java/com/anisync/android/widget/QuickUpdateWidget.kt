package com.anisync.android.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.anisync.android.MainActivity
import com.anisync.android.R
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.entity.LibraryEntryEntity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface QuickUpdateWidgetEntryPoint {
    fun libraryDao(): LibraryDao
}

class QuickUpdateWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(appContext, QuickUpdateWidgetEntryPoint::class.java)
        val dao = entryPoint.libraryDao()
        var entry = try {
             dao.getMostRecentWatching()
        } catch (e: Exception) {
            null
        }
        
        val bitmap = if (entry != null) {
            WidgetImageUtils.loadBitmap(appContext, entry.coverUrl, width = 300, height = 300)
        } else {
            null
        }

        provideContent {
            GlanceTheme {
                QuickUpdateWidgetContent(entry, bitmap)
            }
        }
    }
}

@Composable
fun QuickUpdateWidgetContent(entry: LibraryEntryEntity?, bitmap: android.graphics.Bitmap?) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (entry != null) {
            val detailsIntent = Intent(
                Intent.ACTION_VIEW,
                "anisync://details/${entry.mediaId}".toUri()
            ).apply {
                component = null
                setClass(androidx.glance.LocalContext.current, MainActivity::class.java)
            }

            // Cover Image Background
            if (bitmap != null) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = entry.titleUserPreferred,
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity(detailsIntent)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder
                 Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surfaceVariant)
                        .clickable(actionStartActivity(detailsIntent))
                ) {}
            }

            // Plus Badge
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {
                 Button(
                    text = "+",
                    onClick = actionRunCallback<IncrementEpisodeCallback>(
                        actionParametersOf(IncrementEpisodeCallback.MediaIdKey to entry.mediaId)
                    ),
                    modifier = GlanceModifier.size(40.dp)
                )
            }
        } else {
             Text(
                text = "Empty",
                style = TextStyle(color = GlanceTheme.colors.onSurface)
            )
        }
    }
}
