package com.anisync.android.widget.data

import android.content.Context
import android.graphics.Bitmap
import com.anisync.android.data.local.dao.AiringScheduleDao
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.dao.MediaDetailsDao
import com.anisync.android.data.local.entity.AiringScheduleEntity
import com.anisync.android.data.local.entity.LibraryEntryEntity
import com.anisync.android.domain.ExternalLinkType
import com.anisync.android.widget.core.WidgetImageLoader
import com.anisync.android.widget.data.model.AiringScheduleUiModel
import com.anisync.android.widget.data.model.UpNextUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Centralized repository for widget data access.
 * Handles data fetching, mapping, and image loading.
 */
class WidgetRepository(
    private val context: Context,
    private val airingScheduleDao: AiringScheduleDao,
    private val libraryDao: LibraryDao,
    private val mediaDetailsDao: MediaDetailsDao
) {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    /**
     * Get today's airing schedule for the widget.
     *
     * @param filterMyList If true, only returns shows the user is watching
     * @param loadImages If true, pre-loads cover images as bitmaps
     * @return List of airing schedule UI models
     */
    suspend fun getAiringToday(
        filterMyList: Boolean = false,
        loadImages: Boolean = true
    ): List<AiringScheduleUiModel> = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis / 1000
        val endOfDay = startOfDay + 86400

        val entities = try {
            val allSchedules = airingScheduleDao.getAiringBetween(startOfDay, endOfDay)
            if (filterMyList) {
                allSchedules.filter { it.isWatching }
            } else {
                allSchedules
            }
        } catch (e: Exception) {
            emptyList()
        }

        if (loadImages) {
            loadAiringSchedulesWithImages(entities)
        } else {
            entities.map { it.toUiModel() }
        }
    }

    /**
     * Get up-next items for the widget.
     *
     * @param limit Maximum number of items to return
     * @param loadImages If true, pre-loads cover images as bitmaps
     * @param preferredStreamingService Preferred streaming service for URL lookup
     * @return List of up-next UI models sorted by airing time
     */
    suspend fun getUpNext(
        limit: Int = 10,
        loadImages: Boolean = true,
        preferredStreamingService: String? = null
    ): List<UpNextUiModel> = withContext(Dispatchers.IO) {
        val entries = try {
            libraryDao.getUpNext()
                .filter { it.mediaStatus == null || it.mediaStatus == "RELEASING" }
                .take(limit)
        } catch (e: Exception) {
            emptyList()
        }

        // Fetch airing times for all entries
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        val airingSchedules = try {
            airingScheduleDao.getAiringBetweenForUser(
                currentTimeSeconds,
                currentTimeSeconds + 30L * 24 * 60 * 60 // 30 days ahead
            )
        } catch (e: Exception) {
            emptyList()
        }

        // Build airing times map
        val airingTimes = entries.associate { entry ->
            val nextEpNumber = entry.progress + 1
            val schedule = airingSchedules.find {
                it.mediaId == entry.mediaId && it.episode >= nextEpNumber
            }
            entry.mediaId to (schedule?.airingAt ?: 0L)
        }

        // Fetch streaming URLs if service is specified
        val streamingUrls = if (preferredStreamingService != null) {
            entries.associate { entry ->
                val url = findStreamingUrl(entry.mediaId, preferredStreamingService)
                entry.mediaId to url
            }
        } else {
            emptyMap()
        }

        // Sort by airing time (soonest first)
        val sortedEntries = entries.sortedWith(
            compareBy<LibraryEntryEntity> { airingTimes[it.mediaId] == 0L }
                .thenBy { airingTimes[it.mediaId] ?: Long.MAX_VALUE }
        )

        if (loadImages) {
            loadUpNextWithImages(sortedEntries, airingTimes, streamingUrls)
        } else {
            sortedEntries.map { it.toUiModel(airingTimes[it.mediaId] ?: 0L, streamingUrls[it.mediaId]) }
        }
    }

    private suspend fun loadAiringSchedulesWithImages(
        entities: List<AiringScheduleEntity>
    ): List<AiringScheduleUiModel> = coroutineScope {
        entities.map { entity ->
            async {
                val bitmap = loadBitmap(entity.coverUrl, 300, 450)
                entity.toUiModel(bitmap)
            }
        }.awaitAll()
    }

    private suspend fun loadUpNextWithImages(
        entries: List<LibraryEntryEntity>,
        airingTimes: Map<Int, Long>,
        streamingUrls: Map<Int, String?>
    ): List<UpNextUiModel> = coroutineScope {
        entries.map { entry ->
            async {
                val bitmap = loadBitmap(entry.coverUrl, 300, 450)
                entry.toUiModel(
                    airingTimeSeconds = airingTimes[entry.mediaId] ?: 0L,
                    streamingUrl = streamingUrls[entry.mediaId],
                    bitmap = bitmap
                )
            }
        }.awaitAll()
    }

    private suspend fun loadBitmap(url: String?, width: Int, height: Int): Bitmap? {
        return if (url != null) {
            try {
                WidgetImageLoader.loadBitmap(context, url, width, height)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    private suspend fun findStreamingUrl(mediaId: Int, preferredService: String): String? {
        val mediaDetails = mediaDetailsDao.getById(mediaId) ?: return null
        return mediaDetails.externalLinks
            .filter { it.type == ExternalLinkType.STREAMING && it.url != null }
            .find { link ->
                link.site.equals(preferredService, ignoreCase = true) ||
                    link.site.contains(preferredService.split(" ").first(), ignoreCase = true)
            }?.url
    }

    // Extension functions for entity-to-model mapping

    private fun AiringScheduleEntity.toUiModel(bitmap: Bitmap? = null): AiringScheduleUiModel {
        return AiringScheduleUiModel(
            mediaId = mediaId,
            title = titleUserPreferred,
            coverUrl = coverUrl,
            coverBitmap = bitmap,
            episode = episode,
            airingAtSeconds = airingAt,
            formattedTime = timeFormat.format(Date(airingAt * 1000)),
            isWatching = isWatching,
            format = format
        )
    }

    private fun LibraryEntryEntity.toUiModel(
        airingTimeSeconds: Long,
        streamingUrl: String?,
        bitmap: Bitmap? = null
    ): UpNextUiModel {
        return UpNextUiModel(
            mediaId = mediaId,
            title = titleUserPreferred,
            coverUrl = coverUrl,
            coverBitmap = bitmap,
            nextEpisode = progress + 1,
            progress = progress,
            totalEpisodes = totalEpisodes,
            airingTimeSeconds = airingTimeSeconds,
            streamingUrl = streamingUrl,
            mediaStatus = mediaStatus
        )
    }
}
