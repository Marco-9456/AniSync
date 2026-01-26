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
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
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
interface UpNextWidgetEntryPoint {
    fun libraryDao(): LibraryDao
}

class UpNextWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(appContext, UpNextWidgetEntryPoint::class.java)
        val dao = entryPoint.libraryDao()
        val data = try {
             dao.getUpNext().take(10) // Limit to 10 for widget performance
        } catch (e: Exception) {
            emptyList()
        }

        // Fetch images in parallel
        val dataWithBitmaps = kotlinx.coroutines.coroutineScope {
             data.map { entry ->
                 async {
                     val bitmap = WidgetImageUtils.loadBitmap(appContext, entry.coverUrl, width = 150, height = 220)
                     entry to bitmap
                 }
             }.awaitAll()
        }

        provideContent {
            GlanceTheme {
                UpNextWidgetContent(dataWithBitmaps)
            }
        }
    }
}

@Composable
fun UpNextWidgetContent(data: List<Pair<LibraryEntryEntity, android.graphics.Bitmap?>>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(8.dp)
    ) {
        Text(
            text = "Up Next",
            style = TextStyle(color = GlanceTheme.colors.onSurface),
            modifier = GlanceModifier.padding(bottom = 8.dp)
        )

        if (data.isEmpty()) {
            Text(
                text = "No episodes to watch",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
            )
        } else {
            LazyColumn(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                items(data) { (entry, bitmap) ->
                    UpNextItem(entry, bitmap)
                    Spacer(modifier = GlanceModifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun UpNextItem(entry: LibraryEntryEntity, bitmap: android.graphics.Bitmap?) {
    val totalEp = entry.totalEpisodes ?: "?"
    val progress = entry.progress
    val nextEp = progress + 1
    
    val detailsIntent = Intent(
        Intent.ACTION_VIEW,
        "anisync://details/${entry.mediaId}".toUri()
    ).apply {
        component = null // Ensure it goes through the deep link dispatcher
        // explicitly targeting MainActivity might be safer if deep links are handled there
        setClass(androidx.glance.LocalContext.current, MainActivity::class.java)
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .padding(8.dp)
            .clickable(actionStartActivity(detailsIntent)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        
        if (bitmap != null) {
             Image(
                provider = ImageProvider(bitmap),
                contentDescription = null,
                modifier = GlanceModifier.size(40.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder
            Image(
                provider = ImageProvider(R.mipmap.ic_launcher),
                contentDescription = null,
                modifier = GlanceModifier.size(40.dp),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = entry.titleUserPreferred,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                maxLines = 1
            )
            Text(
                text = "Ep $nextEp of $totalEp",
                style = TextStyle(color = GlanceTheme.colors.secondary)
            )
        }

        Button(
            text = "+",
            onClick = actionRunCallback<IncrementEpisodeCallback>(
                actionParametersOf(IncrementEpisodeCallback.MediaIdKey to entry.mediaId)
            )
        )
    }
}
