package com.anisync.android.presentation.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.library.LibraryTab
import com.anisync.android.presentation.library.components.LibraryQueueRow
import com.anisync.android.presentation.library.components.LibraryRail
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaType

private val PreviewTabs = listOf(
    LibraryTab.Standard(LibraryStatus.CURRENT),
    LibraryTab.Standard(LibraryStatus.REPEATING),
    LibraryTab.Standard(LibraryStatus.PLANNING),
    LibraryTab.Standard(LibraryStatus.COMPLETED),
)

private val PreviewCounts = mapOf(PreviewTabs.first().toId() to PreviewEntriesCount)

private const val PreviewEntriesCount = 2

/**
 * A slice of the Library drawn with its real rail and list rows, in whatever theme the app is in.
 * Theme choices apply app-wide as they are made, so this is always the result, not a guess at it.
 */
@Composable
fun LookAndFeelPreview(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var mediaType by remember { mutableStateOf(MediaType.ANIME) }

    // The Library sits on the page background. This card sits two surface steps higher, so every
    // surface role inside moves up by the same steps and each layer keeps the contrast it has
    // against the real page, in light and dark alike.
    val scheme = MaterialTheme.colorScheme
    val cardScheme = remember(scheme) {
        scheme.copy(
            background = scheme.surfaceContainer,
            surfaceContainerLow = scheme.surfaceContainerHigh,
            surfaceContainer = scheme.surfaceContainerHighest,
            surfaceContainerHigh = scheme.surfaceContainerHighest
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainer
    ) {
        MaterialTheme(colorScheme = cardScheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
            Column(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LibraryRail(
                    tabs = PreviewTabs,
                    selectedIndex = selectedTab,
                    mediaType = mediaType,
                    counts = PreviewCounts,
                    onTabClick = { selectedTab = it },
                    onMediaTypeChange = { mediaType = it }
                )
                PreviewEntries.forEach { entry ->
                    LibraryQueueRow(
                        entry = entry,
                        mediaType = MediaType.ANIME,
                        onClick = {},
                        onIncrement = {},
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

private val PreviewEntries = listOf(
    LibraryEntry(
        id = 1,
        mediaId = 1,
        titleRomaji = "Frieren: Beyond Journey's End",
        titleEnglish = "Frieren: Beyond Journey's End",
        titleNative = "葬送のフリーレン",
        titleUserPreferred = "Frieren: Beyond Journey's End",
        coverUrl = null,
        progress = 12,
        totalEpisodes = 28,
        totalChapters = null,
        totalVolumes = null,
        type = MediaType.ANIME,
        format = MediaFormat.TV,
        status = LibraryStatus.CURRENT,
        nextAiringEpisode = 17,
    ),
    LibraryEntry(
        id = 2,
        mediaId = 2,
        titleRomaji = "Dandadan",
        titleEnglish = "Dandadan",
        titleNative = "ダンダダン",
        titleUserPreferred = "Dandadan",
        coverUrl = null,
        progress = 4,
        totalEpisodes = 12,
        totalChapters = null,
        totalVolumes = null,
        type = MediaType.ANIME,
        format = MediaFormat.TV,
        status = LibraryStatus.CURRENT,
        nextAiringEpisode = 9,
    ),
)
